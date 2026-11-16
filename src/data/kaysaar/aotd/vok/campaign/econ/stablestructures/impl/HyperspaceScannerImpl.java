package data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl;

import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry.AICoreDescriptionMode;
import com.fs.starfarer.api.impl.campaign.aotd_entities.HyperspaceScannerEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTScanFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.ChooseOrbitForBuildingMap;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.StructureLocationPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;


public class HyperspaceScannerImpl extends AoTDStableStructureImpl {
    public static final String SCANNER_ID = "aotd_hyperspace_scanner";
    public static final String GAS_ILLUSTRATION = "aotd_hyperspace_scanner_gas";
    public static final String ICE_ILLUSTRATION = "aotd_hyperspace_scanner_ice";
    private transient boolean interactionImageSynced;

    public static boolean isIceGiant(SectorEntityToken token) {
        return token instanceof PlanetAPI
                && Planets.ICE_GIANT.equals(((PlanetAPI) token).getTypeId());
    }
    public String getScannerIllustration() {
        SectorEntityToken token = getCurrentlyTiedEntity();
        SectorEntityToken focus = token == null ? null : token.getOrbitFocus();
        return Global.getSettings().getSpriteName("illustrations",
                isIceGiant(focus) ? ICE_ILLUSTRATION : GAS_ILLUSTRATION);
    }

    @Override
    public String getImageName() {
        if (currentlyAssignedToken != null && isSuitableGiant(currentlyAssignedToken.getOrbitFocus())) {
            return getScannerIllustration();
        }
        return super.getImageName();
    }
    private void synchronizeInteractionImage() {
        if (!isCurrentImplementation() || currentlyAssignedToken == null
                || !SCANNER_ID.equals(currentlyAssignedToken.getCustomEntityType())
                || !isSuitableGiant(currentlyAssignedToken.getOrbitFocus())) return;
        currentlyAssignedToken.setCustomInteractionDialogImageVisual(
                new InteractionDialogImageVisual(getScannerIllustration(), 480f, 300f));
        interactionImageSynced = true;
    }

    @Override
    public void setCurrentlyAssignedToken(SectorEntityToken token) {
        super.setCurrentlyAssignedToken(token);
        synchronizeInteractionImage();
    }
    public static final float REPORT_INTERVAL_DAYS = 30f;
    public static final float BASE_RANGE_LY = 10f;
    public static final float BETA_RANGE_LY = 12f;
    public static final float ALPHA_RANGE_LY = 15f;
    public static final float BASE_FUEL_MULT = 0.90f;
    public static final float GAMMA_FUEL_MULT = 0.88f;
    public static final float BETA_FUEL_MULT = 0.85f;
    public static final float ALPHA_FUEL_MULT = 0.80f;
    public static final int BASE_TOPOGRAPHY_POINTS = 15;
    public static final int GAMMA_TOPOGRAPHY_POINTS = 20;
    public static final int BETA_TOPOGRAPHY_POINTS = 30;
    public static final int ALPHA_TOPOGRAPHY_POINTS = 50;

    protected float daysSinceLastReport;

    public static boolean isSuitableGiant(SectorEntityToken token) {
        if (!(token instanceof PlanetAPI)) return false;
        PlanetAPI planet = (PlanetAPI) token;
        return !planet.isExpired() && !planet.isStar()
                && (planet.isGasGiant() || Planets.GAS_GIANT.equals(planet.getTypeId())
                || Planets.ICE_GIANT.equals(planet.getTypeId()));
    }

    private static boolean hasSuitableGiant(StarSystemAPI system) {
        for (PlanetAPI planet : system.getPlanets()) {
            if (isSuitableGiant(planet)) return true;
        }
        return false;
    }

    private boolean hasAnotherScanner(StarSystemAPI system) {
        for (SectorEntityToken token : system.getAllEntities()) {
            if (token == currentlyAssignedToken || token.isExpired()) continue;
            Object attached = token.getMemoryWithoutUpdate().get(AoTDStableStructureSpecManager.memKeyInEntities);
            if (attached instanceof HyperspaceScannerImpl || SCANNER_ID.equals(token.getCustomEntityType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAvailableToBuild(StarSystemAPI system, FactionAPI faction) {
        if (faction == null || !AoTDMainResearchManager.getInstance()
                .isAvailableForFaction("aotd_hyperwave_detection", faction)) return false;
        return system != null && faction != null && faction.isPlayerFaction()
                && hasSuitableGiant(system) && !hasAnotherScanner(system);
    }

    @Override
    public boolean showWhenUnavailable(StarSystemAPI system, FactionAPI faction) {
        return false;
    }

    @Override
    public String getUnavailableReason(StarSystemAPI system, FactionAPI faction) {
        if (system == null || faction == null || !faction.isPlayerFaction()) {
            return "Requires a player-controlled star system.";
        }
        if (!hasSuitableGiant(system)) {
            return "Requires at least one gas giant or ice giant in the system.";
        }
        if (hasAnotherScanner(system)) {
            return "Only one Hyperspace Scanner may be constructed in a star system.";
        }
        return super.getUnavailableReason(system, faction);
    }


    @Override
    public boolean startBuilding(String ultimateTargetId) {
        if (currentlyAssignedToken == null || !isSuitableGiant(currentlyAssignedToken.getOrbitFocus())) return false;
        PlanetAPI giant = (PlanetAPI) currentlyAssignedToken.getOrbitFocus();
        float radius = currentlyAssignedToken.getCircularOrbitRadius();
        if (radius < giant.getRadius() * 1.5f - 0.1f || radius > giant.getRadius() * 3f + 0.1f) return false;
        return super.startBuilding(ultimateTargetId);
    }

    @Override
    public StructureLocationPicker createLocationPicker(float width, float height, StarSystemAPI system) {
        return new ScannerOrbitPicker(width, height, system);
    }

    public static class ScannerOrbitPicker extends ChooseOrbitForBuildingMap {
        public ScannerOrbitPicker(float width, float height, StarSystemAPI system) {
            super(width, height, system, SCANNER_ID);
        }

        @Override
        public boolean hasSelection() {
            if (!super.hasSelection() || getOrbitComponent() == null) return false;
            if (!isSuitableGiant(getOrbitComponent().getCurrentlyCenteredAround().getToken())) return false;
            PlanetAPI giant = (PlanetAPI) getOrbitComponent().getCurrentlyCenteredAround().getToken();
            Float radius = getOrbitComponent().getSavedRadiusWorld();
            return radius != null && radius >= giant.getRadius() * 1.5f - 0.1f
                    && radius <= giant.getRadius() * 3f + 0.1f;
        }

        @Override
        public void createSelectionText(TooltipMakerAPI tooltip) {
            if (hasSelection()) {
                tooltip.addPara("Hyperspace Scanner will orbit %s. Press %s to clear.",
                        0f, Color.ORANGE, getSelectedEntity().getName(), "D");
            } else {
                tooltip.addPara("Select a gas giant or ice giant and place the scanner. Press %s to clear.",
                        0f, Color.ORANGE, "D");
            }
        }
    }

    @Override public boolean canMountAICores() { return true; }

    public boolean isScannerOperational() {
        return isCurrentImplementation() && !isBuilding() && isFunctional()
                && isPlayerOwned() && currentlyAssignedToken.getStarSystem() != null
                && isSuitableGiant(currentlyAssignedToken.getOrbitFocus());
    }

    public int getMonthlyPoints() {
        String core = getAICoreId();
        if (Commodities.ALPHA_CORE.equals(core)) return ALPHA_TOPOGRAPHY_POINTS;
        if (Commodities.BETA_CORE.equals(core)) return BETA_TOPOGRAPHY_POINTS;
        if (Commodities.GAMMA_CORE.equals(core)) return GAMMA_TOPOGRAPHY_POINTS;
        return BASE_TOPOGRAPHY_POINTS;
    }

    public float getRangeLY() {
        if (Commodities.ALPHA_CORE.equals(getAICoreId())) return ALPHA_RANGE_LY;
        if (Commodities.BETA_CORE.equals(getAICoreId())) return BETA_RANGE_LY;
        return BASE_RANGE_LY;
    }

    public float getFuelUseMultiplier() {
        if (Commodities.ALPHA_CORE.equals(getAICoreId())) return ALPHA_FUEL_MULT;
        if (Commodities.BETA_CORE.equals(getAICoreId())) return BETA_FUEL_MULT;
        if (Commodities.GAMMA_CORE.equals(getAICoreId())) return GAMMA_FUEL_MULT;
        return BASE_FUEL_MULT;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (!interactionImageSynced && !isBuilding()) synchronizeInteractionImage();
        if (amount <= 0f || Global.getSector().isPaused() || !isScannerOperational()) return;
        daysSinceLastReport += Global.getSector().getClock().convertToDays(amount);
        if (daysSinceLastReport < REPORT_INTERVAL_DAYS) return;

        int reports = (int) (daysSinceLastReport / REPORT_INTERVAL_DAYS);
        daysSinceLastReport -= reports * REPORT_INTERVAL_DAYS;
        for (int i = 0; i < reports; i++) {
            HyperspaceTopographyEventIntel.addFactorCreateIfNecessary(
                    new HTScanFactor("Hyperspace Scanner (" + currentlyAssignedToken.getStarSystem().getName() + ")",
                            getMonthlyPoints()), null);
        }
    }

    @Override
    public void apply() {
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    @Override
    public void unapply() {
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    @Override
    public void applyEffectsOnChangeOfAICore(String currentAiCore) {
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    @Override
    protected void buildingFinished() {
        super.buildingFinished();
        // The final entity is already assigned here; choose the image using its orbit focus.
        synchronizeInteractionImage();
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    @Override
    protected void notifyDisrupted() {
        super.notifyDisrupted();
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    @Override
    protected void disruptionFinished() {
        super.disruptionFinished();
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    @Override
    public void notifyRemoved() {
        super.notifyRemoved();
        HyperspaceScannerEntityPlugin.refreshAllFuelEffects();
    }

    private void addCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode,
                                    String name, int points, float range, float mult) {
        tooltip.addPara(getAICoreDescriptionPrefix(name, mode)
                        + "Generates %s Hyperspace Topography points every 30 days and reduces player-fleet "
                        + "hyperspace fuel consumption by %s within %s light-years of the host system.",
                0f, Misc.getHighlightColor(), String.valueOf(points),
                Math.round((1f - mult) * 100f) + "%", String.valueOf((int) range));
    }

    @Override public void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Gamma-level AI core", GAMMA_TOPOGRAPHY_POINTS,
                BASE_RANGE_LY, GAMMA_FUEL_MULT);
    }

    @Override public void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Beta-level AI core", BETA_TOPOGRAPHY_POINTS,
                BETA_RANGE_LY, BETA_FUEL_MULT);
    }

    @Override public void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Alpha-level AI core", ALPHA_TOPOGRAPHY_POINTS,
                ALPHA_RANGE_LY, ALPHA_FUEL_MULT);
    }

    @Override
    public void createTooltipForEffectSection(TooltipMakerAPI tooltip, AoTDStableStructureTooltipMode mode) {
        super.createTooltipForEffectSection(tooltip, mode);
        tooltip.addPara("Studies hyperspace currents and gravitational signatures from the orbit of a gas or ice giant, "
                        + "producing %s Hyperspace Topography points every %s days.",
                5f, Misc.getHighlightColor(), String.valueOf(getMonthlyPoints()), "30");
        tooltip.addPara("Optimized hyperspace routes reduce the player's fleet fuel consumption by %s "
                        + "within %s light-years of this system.", 3f, Misc.getHighlightColor(),
                Math.round((1f - getFuelUseMultiplier()) * 100f) + "%", String.valueOf((int) getRangeLY()));
        tooltip.addPara("Fuel savings from multiple Hyperspace Scanners do not stack.",
                Misc.getGrayColor(), 3f);
        if (mode == AoTDStableStructureTooltipMode.PANEL && isScannerOperational()) {
            tooltip.addPara("Next topography report in approximately %s days.", 3f, Misc.getHighlightColor(),
                    String.valueOf(Math.max(1, (int) Math.ceil(REPORT_INTERVAL_DAYS - daysSinceLastReport))));
        }
    }
}
