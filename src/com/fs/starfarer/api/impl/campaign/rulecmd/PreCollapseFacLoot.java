package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.Ids.AoTDItems;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMazeDialog;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor.ReactorPuzzle;
import data.kaysaar.aotd.vok.scripts.pcf.unique.MeetTheSpy;

import org.magiclib.achievements.MagicAchievementManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PreCollapseFacLoot extends BaseCommandPlugin {

    protected CampaignFleetAPI playerFleet;
    protected SectorEntityToken entity;
    protected PlanetAPI planet;
    protected FactionAPI playerFaction;
    protected FactionAPI entityFaction;
    protected TextPanelAPI text;
    protected OptionPanelAPI options;
    protected CargoAPI playerCargo;
    protected MemoryAPI memory;
    protected MarketAPI market;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;
    protected FactionAPI faction;


    public PreCollapseFacLoot() {
    }

    public PreCollapseFacLoot(SectorEntityToken entity) {
        init(entity);
    }

    protected void init(SectorEntityToken entity) {
        memory = entity.getMemoryWithoutUpdate();
        this.entity = entity;
        planet = (PlanetAPI) entity;
        playerFleet = Global.getSector().getPlayerFleet();
        playerCargo = playerFleet.getCargo();

        playerFaction = Global.getSector().getPlayerFaction();
        entityFaction = entity.getFaction();

        faction = entity.getFaction();

        market = entity.getMarket();


    }

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        entity = dialog.getInteractionTarget();
        init(entity);

        memory = getEntityMemory(memoryMap);

        text = dialog.getTextPanel();
        options = dialog.getOptionPanel();

        if (command.equals("genLoot")) {
            genLoot();
        }

        return true;
    }
    protected void genLoot() {
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        if (marketMemory.getBoolean(AoTDMinedFacility.SITE)) {
            if (marketMemory.getBoolean(AoTDMinedFacility.COMPLETE) && !marketMemory.getBoolean("$aotd_fac_explored")
                    && marketMemory.get(AoTDMinedFacility.SALVAGE) instanceof CargoAPI recovered) {
                showSalvage(recovered);
                marketMemory.set("$aotd_fac_explored", true);
                marketMemory.unset(AoTDMinedFacility.SALVAGE);
            }
            return;
        }
        showSalvage(generateSalvage());
    }

    protected CargoAPI generateSalvage() {
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        long seed = memory.getLong(MemFlags.SALVAGE_SEED);
        Random random = Misc.getRandom(seed, 100);
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        int mazeSizeIndex = getCybermazeSizeIndex(marketMemory);
        boolean reactorComplete = marketMemory.getBoolean(AoTDReactorPlatform.SITE) && marketMemory.getBoolean(AoTDReactorPlatform.COMPLETE);
        float flood = reactorComplete ? marketMemory.getFloat(AoTDReactorPlatform.FLOOD) : 0f;
        float recovery = ReactorPuzzle.getSalvageFraction(flood);
        List<SalvageEntityGenDataSpec.DropData> drops = new ArrayList<>();

        SalvageEntityGenDataSpec.DropData d = new SalvageEntityGenDataSpec.DropData();
        d.chances = Math.round(2 * recovery);
        d.group = "blueprints";
        drops.add(d);

        d = new SalvageEntityGenDataSpec.DropData();
        d.chances = Math.round((mazeSizeIndex >= 2 ? 2 : 1) * recovery);
        d.group = "rare_tech";
        drops.add(d);

        d = new SalvageEntityGenDataSpec.DropData();
        int researchRolls = marketMemory.getBoolean(AoTDMinedFacility.SITE) ? 30 : 25;
        d.chances = Math.round((researchRolls + 5 * mazeSizeIndex) * recovery);
        d.group = "ashes_research";
        drops.add(d);
        int rand = getRandomNumber(1,10);

        CargoAPI salvage = SalvageEntity.generateSalvage(random, 1f, 1f, 1f, 1f, List.of(), drops);

        if(rand<2){
            for (int i = 0; i < rand; i++) {
                /// TODO - Implement this later down the line
//                salvage.addSpecial(new SpecialItemData(AoTDItems.BASE_ITEM_BLUEPRINT,GPManager.getInstance().getItemsProductionWithoutAICores().get(getRandomNumber(0,GPManager.getInstance().getItemProductionOptionFiltered().size()-1)).getSpec().getItemSpecAPI().getId()),1);
            }
        }
        if (marketMemory.getBoolean(MeetTheSpy.SITE)) {
            salvage.addCommodity("research_databank", 5);
            salvage.addCommodity("domain_heavy_machinery", 20);
            salvage.addCommodity(Commodities.PARTS, 20 + random.nextInt(51));
            salvage.addCommodity(Commodities.HAND_WEAPONS, 900);
            salvage.addCommodity(Commodities.ALPHA_CORE, 1);
            salvage.addCommodity(Commodities.BETA_CORE, 3);
            salvage.addSpecial(new SpecialItemData(Items.DRONE_REPLICATOR, null), 1);
        } else {
            salvage.addCommodity(Commodities.PARTS, Math.round((5 + random.nextInt(21)) * recovery));
        }
        if (marketMemory.getBoolean(TechHunters.RAIDED)) {
            salvage.addCommodity(Commodities.SUPPLIES, 40 + random.nextInt(41));
            salvage.addCommodity(Commodities.FUEL, 60 + random.nextInt(61));
        }
        if (reactorComplete) addReactorRewards(salvage, flood, random);
        return salvage;
    }

    private void showSalvage(CargoAPI salvage) {
        salvage.sort();
        if(Global.getSettings().getModManager().isModEnabled("MagicLib") && MagicAchievementManager.getInstance().areAchievementsEnabled()){
            try {
                MagicAchievementManager.getInstance().getAchievement("aotd_pcf").completeAchievement();
            }
            catch (Exception e) {

            }

        }
        dialog.getVisualPanel().showLoot("Salvaged", salvage, false, true, true, () -> {
            dialog.dismiss();
            dialog.hideTextPanel();
            dialog.hideVisualPanel();
        });
        options.clearOptions();

        dialog.setPromptText("");
    }

    private static void addReactorRewards(CargoAPI salvage, float flood, Random random) {
        if (flood <= 0.8f) salvage.addCommodity(Commodities.GAMMA_CORE, 1 + random.nextInt(3));
        if (flood <= 0.5f) salvage.addCommodity(Commodities.ALPHA_CORE, 1);
        if (flood <= 0.3f) salvage.addSpecial(new SpecialItemData(AoTDItems.AQUAORB, null), 1);
    }

    private int getCybermazeSizeIndex(MemoryAPI marketMemory) {
        if (!marketMemory.getBoolean(AoTDFacilityCybermaze.SITE) || marketMemory.getBoolean(MeetTheSpy.SITE)
                || !marketMemory.getBoolean(FacilityMazeDialog.ACCESS) || !marketMemory.contains(FacilityMazeDialog.SEED)) return 0;
        return FacilityMazeDialog.getSizeIndex(new Random(marketMemory.getLong(FacilityMazeDialog.SEED)));
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public String retrieveFromRepo(ArrayList<String> repo){
        if(repo.isEmpty()){
            return null;
        }
        else {
            return repo.remove(0);
        }
    }
}
