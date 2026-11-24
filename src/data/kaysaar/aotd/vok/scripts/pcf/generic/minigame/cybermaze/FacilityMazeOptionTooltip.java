package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

public class FacilityMazeOptionTooltip implements TooltipMakerAPI.TooltipCreator {
    private final String description;
    private final String commodityId;
    private final int cost;

    public FacilityMazeOptionTooltip(String description, String commodityId, int cost) {
        this.description = description;
        this.commodityId = commodityId;
        this.cost = cost;
    }

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return false;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return 360f;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addPara(description, 0f);
        if (commodityId == null) {
            int available = Global.getSector().getPlayerStats().getStoryPoints();
            tooltip.addPara("Spends %s.", 6f, Misc.getStoryBrightColor(), cost + (cost == 1 ? " story point" : " story points"));
            tooltip.addPara("You have %s story points.", 3f, balanceColor(available, Misc.getStoryBrightColor()), String.valueOf(available));
            return;
        }

        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        int available = (int) cargo.getCommodityQuantity(commodityId);
        CommoditySpecAPI commodity = Global.getSettings().getCommoditySpec(commodityId);
        TooltipMakerAPI item = tooltip.beginImageWithText(commodity.getIconName(), 32f);
        item.addPara("Consumes %s.", 0f, Misc.getHighlightColor(), cost + " " + commodity.getName());
        item.addPara("You have %s in fleet cargo.", 3f, balanceColor(available, Misc.getHighlightColor()), String.valueOf(available));
        tooltip.addImageWithText(6f);
    }

    private Color balanceColor(int available, Color color) {
        return available < cost ? Misc.getNegativeHighlightColor() : color;
    }
}
