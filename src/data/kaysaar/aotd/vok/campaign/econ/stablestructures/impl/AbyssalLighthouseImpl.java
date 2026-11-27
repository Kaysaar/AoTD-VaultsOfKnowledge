package data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry.AICoreDescriptionMode;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;

import java.awt.Color;

public class AbyssalLighthouseImpl extends AoTDStableStructureImpl {
    @Override
    public Color getHologramColor() {
        return new Color(107, 4, 11);
    }

    @Override
    public boolean hasHologramRipple() {
        return true;
    }

    @Override
    public boolean isAvailableToBuild(StarSystemAPI system, FactionAPI faction) {
        return getBuildRestriction(system) == null && super.isAvailableToBuild(system, faction);
    }

    @Override
    public String getUnavailableReason(StarSystemAPI system, FactionAPI faction) {
        String reason = getBuildRestriction(system);
        return reason == null ? super.getUnavailableReason(system, faction) : reason;
    }

    private String getBuildRestriction(StarSystemAPI system) {
        if (system == null || !"limbo".equals(system.getId())) return "Can only be built in the Limbo star system.";
        if (hasAnotherStructure("aotd_abyssal_lighthouse", null)) {
            return "An Abyssal Lighthouse already exists or is under construction.";
        }
        return null;
    }

    @Override
    protected void buildingFinished() {
        super.buildingFinished();
        Global.getSector().getHyperspace().addScript(new AbyssalLighthouseField(this));
    }

    @Override
    public boolean canMountAICores() {
        return true;
    }

    public int getRangeLY(String coreId) {
        if (Commodities.ALPHA_CORE.equals(coreId)) return 18;
        if (Commodities.BETA_CORE.equals(coreId)) return 15;
        if (Commodities.GAMMA_CORE.equals(coreId)) return 12;
        return 10;
    }

    @Override
    public void addAICoreDescription(TooltipMakerAPI tooltip, String coreId, AICoreDescriptionMode mode) {
        if (getRangeLY(coreId) == getRangeLY(null)) {
            super.addAICoreDescription(tooltip, coreId, mode);
            return;
        }
        String name = Global.getSettings().getCommoditySpec(coreId).getName();
        tooltip.addPara(getAICoreDescriptionPrefix(name, mode) + "Extends the lighthouse's range to %s light-years.",
                0f, Misc.getHighlightColor(), String.valueOf(getRangeLY(coreId)));
    }

    @Override
    public void createTooltipForEffectSection(TooltipMakerAPI tooltip, AoTDStableStructureTooltipMode mode) {
        tooltip.addPara("Reduces the abyssal hyperspace penalties to fleet sensor range and maximum burn by %s "
                        + "within %s light-years of this system. Using Compound in range reduces the burn penalty by %s in total. "
                        + "Only one Abyssal Lighthouse may exist at a time.",
                5f, Misc.getHighlightColor(), "30%", String.valueOf(getRangeLY(getAICoreId())), "80%");
    }
}
