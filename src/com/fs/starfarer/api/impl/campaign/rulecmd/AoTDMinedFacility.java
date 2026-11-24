package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper.FacilitySweeperDialog;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class AoTDMinedFacility extends BaseCommandPlugin {
    public static final String SITE = "$aotd_pcf_mined_facility";
    public static final String COMPLETE = "$aotd_pcf_mined_complete";
    public static final String SALVAGE = "$aotd_pcf_mined_salvage";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty() || !"play".equals(params.get(0).getString(memoryMap))) return false;
        SectorEntityToken target = dialog.getInteractionTarget();
        if (!(target instanceof PlanetAPI) || target.getMarket() == null) return false;
        MemoryAPI memory = target.getMarket().getMemoryWithoutUpdate();
        if (!memory.getBoolean(SITE) || memory.getBoolean("$aotd_fac_explored")
                || memory.getBoolean("$aotd_failed_pre_collapse")) return false;
        if (memory.getBoolean(COMPLETE)) return showPage(ruleId, dialog, memoryMap, "AoTDMinedResult");

        Random random = Misc.getRandom(Misc.getSalvageSeed(target), 73);
        CargoAPI loot = new PreCollapseFacLoot(target).generateSalvage();
        String colonyItem = loot.getStacksCopy().stream()
                .filter(CargoStackAPI::isSpecialStack)
                .map(stack -> stack.getSpecialDataIfSpecial().getId())
                .filter(ItemEffectsRepo.ITEM_EFFECTS::containsKey)
                .findFirst().orElse(null);
        FacilitySweeperDialog sweeper = new FacilitySweeperDialog(loot, colonyItem, random, salvage -> {
            if (dialog.getInteractionTarget() != target) return;
            if (salvage == null) {
                showPage(ruleId, dialog, memoryMap, "AoTDMinedBriefing");
                return;
            }
            memory.set(SALVAGE, salvage);
            memory.set(COMPLETE, true);
            showPage(ruleId, dialog, memoryMap, "AoTDMinedResult");
        });
        if (!sweeper.show()) return showPage(ruleId, dialog, memoryMap, "AoTDMinedBriefing");
        dialog.getOptionPanel().clearOptions();
        return true;
    }

    private boolean showPage(String ruleId, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, String trigger) {
        dialog.getOptionPanel().clearOptions();
        return FireBest.fire(ruleId, dialog, memoryMap, trigger);
    }
}
