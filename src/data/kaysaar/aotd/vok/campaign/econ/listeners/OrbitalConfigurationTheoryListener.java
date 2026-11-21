package data.kaysaar.aotd.vok.campaign.econ.listeners;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.MutableStat;
import data.kaysaar.aotd.tot.misc.AoTDToolboxMisc;
import data.kaysaar.aotd.vok.Ids.AoTDTechIds;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;

public class OrbitalConfigurationTheoryListener implements EconomyTickListener {
    public static void register() {
        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        if (!listeners.hasListenerOfClass(OrbitalConfigurationTheoryListener.class)) {
            listeners.addListener(new OrbitalConfigurationTheoryListener(), true);
        }
        updateIfResearched();
    }
    @Override
    public void reportEconomyTick(int iterIndex) {
        updateIfResearched();
    }

    @Override
    public void reportEconomyMonthEnd() {
    }

    private static void updateIfResearched() {
        if (AoTDMainResearchManager.getInstance().haveFactionResearchedCertainTech(
                Global.getSector().getPlayerFaction(), AoTDTechIds.ORBITAL_CONFIGURATION_THEORY)) {
            updateCapacity();
        }
    }

    private static void updateCapacity() {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            FactionAPI owner = system.getCenter() == null ? null : AshMisc.getClaimingFaction(system.getCenter());
            Object stored = system.getMemoryWithoutUpdate().get(AoTDToolboxMisc.SYSTEM_STRUCTURE_CAPACITY_MEMORY_KEY);
            if (owner != null && owner.isPlayerFaction()) {
                if (!(stored instanceof MutableStat capacity) || capacity.getFlatStatMod(AoTDTechIds.ORBITAL_CONFIGURATION_THEORY) == null) {
                    AoTDToolboxMisc.getSystemStructureCapacityStat(system).modifyFlat(
                            AoTDTechIds.ORBITAL_CONFIGURATION_THEORY, 1f, "Orbital Configuration Theory");
                }
            } else if (stored instanceof MutableStat capacity) {
                capacity.unmodifyFlat(AoTDTechIds.ORBITAL_CONFIGURATION_THEORY);
            }
        }
    }
}
