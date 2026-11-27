package com.fs.starfarer.api.impl.campaign.aotd_entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureAdvanceScript;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl.HyperspaceScannerImpl;
import data.kaysaar.aotd.vok.misc.AoTDMisc;

public class HyperspaceScannerEntityPlugin extends BaseCampaignObjectivePlugin {
    public static final String FUEL_MOD_ID = "aotd_hyperspace_scanner_fuel";
    private static final float REFRESH_INTERVAL_DAYS = 0.10f;
    private float refreshDays = REFRESH_INTERVAL_DAYS;
    private transient boolean shadowSized;

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (!shadowSized && layer == CampaignEngineLayers.BELOW_STATIONS) shadowSized = AoTDMisc.sizeStationShadow(entity);
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().isPaused() || amount <= 0f) return;
        refreshDays += Global.getSector().getClock().convertToDays(amount);
        if (refreshDays < REFRESH_INTERVAL_DAYS) return;
        refreshDays = 0f;
        refreshAllFuelEffects();
    }

    public static void refreshAllFuelEffects() {
        if (Global.getSector() == null) return;
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;

        float bestMult = 1f;
        if (fleet.isInHyperspace()) {
            for (StarSystemAPI system : AoTDStableStructureAdvanceScript.getColonizedSystems()) {
                float distanceLY = Misc.getDistanceLY(fleet.getLocationInHyperspace(), system.getLocation());
                for (AoTDStableStructureImpl structure : AoTDStableStructureAdvanceScript.getStructures(system)) {
                    if (!(structure instanceof HyperspaceScannerImpl)) continue;
                    HyperspaceScannerImpl scanner = (HyperspaceScannerImpl) structure;
                    if (!scanner.isScannerOperational() || distanceLY > scanner.getRangeLY()) continue;
                    bestMult = Math.min(bestMult, scanner.getFuelUseMultiplier());
                }
            }
        }
        if (bestMult < 1f) {
            fleet.getStats().getFuelUseHyperMult().modifyMult(FUEL_MOD_ID, bestMult,
                    "Hyperspace Scanner route optimization");
        } else {
            fleet.getStats().getFuelUseHyperMult().unmodifyMult(FUEL_MOD_ID);
        }
    }

    @Override
    public void printEffect(TooltipMakerAPI tooltip, float pad) {
        if (entity == null || entity.isExpired()) return;
        tooltip.addPara("Reconstructs regional hyperspace topography and provides optimized fuel-efficient "
                + "routes for the player fleet. Scanner bonuses do not stack.", pad);
    }
}
