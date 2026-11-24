package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class TechHunters extends BaseCommandPlugin {
    public static final String SITE = "$aotd_pcf_tech_hunters";
    public static final String RAIDED = "$aotd_pcf_tech_hunters_raided";
    private static final String CAPTAIN = "$aotd_pcf_tech_hunters_captain";
    private static final String BUY = "aotdTechHuntersBuy";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty() || !(dialog.getInteractionTarget() instanceof PlanetAPI planet)) return false;
        MemoryAPI market = planet.getMarket().getMemoryWithoutUpdate();
        MemoryAPI entity = planet.getMemoryWithoutUpdate();
        if (!market.getBoolean(SITE) || market.getBoolean("$aotd_fac_explored")
                || market.getBoolean("$aotd_failed_pre_collapse") || entity.getBoolean("$aotd_defeated_pcf")) return false;

        int price = 120000 + 10000 * Misc.getRandom(Misc.getSalvageSeed(planet), 47).nextInt(14);
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        String action = params.get(0).getString(memoryMap);
        if ("buy".equals(action) && entity.contains(CAPTAIN) && cargo.getCredits().get() >= price) {
            cargo.getCredits().subtract(price);
            entity.set("$aotd_defeated_pcf", true);
            AddRemoveCommodity.addCreditsLossText(price, dialog.getTextPanel());
            dialog.getTextPanel().addPara("The captain comes aboard an hour later. Over a pot of recaff, you sign over the credits in exchange for the entire claim and four months of silence.");
            dialog.getTextPanel().addPara("As their last shuttle clears the surface, the crew starts its burn out of orbit. Your own salvage teams are already on their way down.");
            dialog.getVisualPanel().showPlanetInfo(planet);
            AoTDPCFEncounter.showSalvageOptions(dialog);
            return true;
        }
        if (!"contact".equals(action) && !"buy".equals(action)) return false;

        PersonAPI captain = (PersonAPI) entity.get(CAPTAIN);
        if (captain == null) {
            captain = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson();
            entity.set(CAPTAIN, captain);
        }
        dialog.getVisualPanel().showPersonInfo(captain, true);
        dialog.getTextPanel().addPara("Your comms officer transmits your IFF and requests a private channel. After a long pause, a young captain appears on the screen.");
        dialog.getTextPanel().addPara("\"Sorry, I'm here! Captain %s. My crew got here first, but if you want to buy the claim, I'm listening.\"",
                Misc.getHighlightColor(), captain.getNameString());
        dialog.getTextPanel().addPara("You offer to buy out the crew in exchange for the claim and a four-month nondisclosure agreement. The captain asks for %s.",
                Misc.getHighlightColor(), Misc.getDGSCredits(price));
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        options.addOption("Buy the claim for " + Misc.getDGSCredits(price), BUY);
        if (cargo.getCredits().get() < price) {
            options.setEnabled(BUY, false);
            options.setTooltip(BUY, "You do not have enough credits.");
        }
        options.addOption("Return to the bridge", "aotdTechHuntersBack");
        return true;
    }
}
