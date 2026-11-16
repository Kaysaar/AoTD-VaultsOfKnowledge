package data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry.AICoreDescriptionMode;
import com.fs.starfarer.api.impl.campaign.aotd_entities.HypershuntReceiverEntityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;

public class HypershuntReciverImpl extends AoTDStableStructureImpl {

    @Override
    public boolean canMountAICores() {
        return true;
    }

    @Override
    public boolean isAvailableToBuild(StarSystemAPI systemAPI, FactionAPI factionAttemptingToBuildIt) {
        return factionAttemptingToBuildIt != null && AoTDMainResearchManager.getInstance()
                .isAvailableForFaction("aotd_tech_hp_distribution", factionAttemptingToBuildIt);
    }

    @Override
    public boolean showWhenUnavailable(StarSystemAPI systemAPI, FactionAPI factionAttemptingToBuildIt) {
        return false;
    }

    @Override
    public void apply() {
        if (isFunctional()) {
            HypershuntReceiverEntityPlugin plugin = getReceiverPlugin();
            if (plugin != null) plugin.refreshEffects();
        }
    }

    @Override
    public void unapply() {
        HypershuntReceiverEntityPlugin plugin = getReceiverPlugin();
        if (plugin != null) plugin.unmodify();
    }

    @Override
    public void applyEffectsOnChangeOfAICore(String currentAiCore) {
        HypershuntReceiverEntityPlugin plugin = getReceiverPlugin();
        if (plugin != null) plugin.refreshEffects();
    }

    @Override
    protected void buildingFinished() {
        super.buildingFinished();
        apply();
    }

    @Override
    protected void notifyDisrupted() {
        unapply();
        super.notifyDisrupted();
    }

    @Override
    protected void disruptionFinished() {
        super.disruptionFinished();
        apply();
    }

    private HypershuntReceiverEntityPlugin getReceiverPlugin() {
        if (currentlyAssignedToken == null || currentlyAssignedToken.isExpired()) return null;
        return currentlyAssignedToken.getCustomPlugin() instanceof HypershuntReceiverEntityPlugin plugin
                ? plugin : null;
    }

    @Override
    public void createTooltipForEffectSection(TooltipMakerAPI tooltip, AoTDStableStructureTooltipMode mode) {
        tooltip.addPara(
                "Distributes energy from a restored Coronal Hypershunt to same-faction colonies " +
                        "in this star system while the receiver is operational and within transmission range.",
                5f
        );
        tooltip.addPara("Increases maximum industry slots on those colonies by %s.",
                5f, Misc.getHighlightColor(), "+1");
        tooltip.addPara("AI cores provide additional, cumulative system-wide bonuses.",
                Misc.getGrayColor(), 5f);
    }

    @Override
    public void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        tooltip.addPara(
                getAICoreDescriptionPrefix("Gamma-level AI core", mode)
                        + "Increases every existing commodity output from functional industries " +
                        "on same-faction colonies in the star system by %s unit.",
                0f, Misc.getHighlightColor(), "+1"
        );
    }

    @Override
    public void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        tooltip.addPara(
                getAICoreDescriptionPrefix("Beta-level AI core", mode)
                        + "Retains the Gamma-level %s commodity production bonus and additionally " +
                        "reduces industry upkeep by %s for same-faction colonies in the star system.",
                0f, Misc.getHighlightColor(), "+1", "10%"
        );
    }

    @Override
    public void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        tooltip.addPara(
                getAICoreDescriptionPrefix("Alpha-level AI core", mode)
                        + "Retains the Gamma-level %s commodity production bonus and the Beta-level " +
                        "%s industry upkeep reduction. Also provides another %s maximum industry " +
                        "slot per same-faction colony, for a total of %s slots from the receiver.",
                0f, Misc.getHighlightColor(), "+1", "10%", "+1", "+2"
        );
    }
}
