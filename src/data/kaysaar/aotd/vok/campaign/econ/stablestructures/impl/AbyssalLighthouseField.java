package data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;

public class AbyssalLighthouseField implements EveryFrameScript {
    private static final float REFRESH_DAYS = 0.01f;
    private static final String REDUCED_PENALTY = "In abyssal hyperspace (reduced penalty)";
    private final AbyssalLighthouseImpl lighthouse;
    private float elapsedDays = REFRESH_DAYS;
    private transient ArrayList<CampaignFleetAPI> coveredFleets;

    public AbyssalLighthouseField(AbyssalLighthouseImpl lighthouse) {
        this.lighthouse = lighthouse;
    }

    @Override
    public void advance(float amount) {
        elapsedDays += Global.getSector().getClock().convertToDays(amount);
        if (coveredFleets != null && elapsedDays < REFRESH_DAYS) return;
        elapsedDays = 0f;
        if (coveredFleets == null) coveredFleets = new ArrayList<>();
        coveredFleets.clear();
        if (!lighthouse.isFunctional()) return;

        int range = lighthouse.getRangeLY(lighthouse.getAICoreId());
        for (CampaignFleetAPI fleet : Global.getSector().getHyperspace().getFleets()) {
            if (Misc.getDistanceLY(fleet, lighthouse.getCurrentlyTiedEntity()) <= range) coveredFleets.add(fleet);
        }
    }

    public static void applyPenaltyReductions(CampaignFleetAPI player, boolean compound, HyperspaceTerrainPlugin terrain) {
        String sensorPenalty = terrain.getModId() + "_4";
        String burnPenalty = terrain.getModId() + "_5";
        for (EveryFrameScript script : Global.getSector().getHyperspace().getScripts()) {
            if (!(script instanceof AbyssalLighthouseField field) || field.coveredFleets == null) continue;
            for (CampaignFleetAPI fleet : field.coveredFleets) {
                if (!fleet.isInHyperspace()) continue;
                reducePenalty(fleet.getStats().getSensorRangeMod(), sensorPenalty, 0.3f);
                reducePenalty(fleet.getStats().getFleetwideMaxBurnMod(), burnPenalty, fleet == player && compound ? 0.8f : 0.3f);
            }
        }
        if (compound && player.isInHyperspace()) {
            reducePenalty(player.getStats().getFleetwideMaxBurnMod(), burnPenalty, 0.5f);
        }
    }

    private static void reducePenalty(StatBonus stat, String penaltyId, float reduction) {
        stat.unmodifyMult("aotd_abyssal_lighthouse_" + penaltyId);
        StatMod penalty = stat.getMultBonus(penaltyId);
        if (penalty == null || penalty.value < 0f || penalty.value >= 1f || REDUCED_PENALTY.equals(penalty.desc)) return;
        stat.modifyMult(penaltyId, penalty.value + (1f - penalty.value) * reduction, REDUCED_PENALTY);
    }

    @Override
    public boolean isDone() {
        return !lighthouse.isCurrentImplementation();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
