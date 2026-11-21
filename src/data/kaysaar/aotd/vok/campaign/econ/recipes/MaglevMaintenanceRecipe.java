package data.kaysaar.aotd.vok.campaign.econ.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;

public class MaglevMaintenanceRecipe extends ResearchRecipe {
    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        int demand = Math.max(1, industry.getMarket().getSize() - 3);
        industry.demand(getId(), AoTDCommodities.DOMAIN_GRADE_MACHINERY, demand, getName());
        industry.demand(getId(), AoTDCommodities.ADVANCED_COMPONENTS, demand, getName());
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.getDemand(AoTDCommodities.DOMAIN_GRADE_MACHINERY).getQuantity().unmodify(getId());
        industry.demand(getId(), AoTDCommodities.ADVANCED_COMPONENTS, 0, getName());
    }

    public float getEfficiencyBonus(Industry industry) {
        int demand = Math.max(1, industry.getMarket().getSize() - 3);
        int deficit = industry.getMaxDeficit(AoTDCommodities.DOMAIN_GRADE_MACHINERY, AoTDCommodities.ADVANCED_COMPONENTS).two;
        return 0.3f * Math.max(0f, 1f - (float) deficit / demand);
    }

    @Override
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) {
        tooltip.addPara("Maintenance requires %s and %s.", 8f, Misc.getHighlightColor(),
                "Domain-Grade Machinery", "Electronic Components");
        tooltip.addPara("Full supply adds %s to %s. Shortages reduce this bonus.", 4f,
                Misc.getPositiveHighlightColor(), "30 percentage points", "synergy efficiency");
    }
}
