package data.kaysaar.aotd.vok.campaign.econ.recipes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;

public class CompoundRefuellingRecipe extends AoTDIndustryRecipePlugin {
    @Override
    public boolean canBeUsed(Industry industry) {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean("$aotd_compound_unlocked");
    }

    @Override
    public String getUnavailableReason(Industry industry) {
        return canBeUsed(industry) ? super.getUnavailableReason(industry) : "Requires completion of Exotic-Mass Repolarisation.";
    }

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        String compound = AoTDCommodities.COMPOUND;
        industry.demand(getId(), compound, Math.max(1, industry.getMarket().getSize() / 2), getName());
        int required = industry.getDemand(compound).getQuantity().getModifiedInt();
        float supplied = required <= 0 ? 1f : Math.max(0f, 1f - (float) industry.getMaxDeficit(compound).two / required);
        industry.getDemand(Commodities.FUEL).getQuantity().modifyMult(getId(), 1f - 0.5f * supplied, getName());
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.demand(getId(), AoTDCommodities.COMPOUND, 0, getName());
        industry.getDemand(Commodities.FUEL).getQuantity().unmodifyMult(getId());
    }

    @Override
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) {
        tooltip.addPara("Reduces %s demand by up to %s in proportion to the available supply of %s.", 5f,
                Misc.getHighlightColor(), "Fuel", "50%", "Compound");
    }
}
