package data.kaysaar.aotd.vok.campaign.econ.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;

public class MetalOutputRecipe extends ResearchRecipe {
    private boolean isBulk() {
        return "aotd_crystalizator_bulk".equals(getId()) || "aotd_enrichment_bulk".equals(getId());
    }

    private boolean isCrystalizator() {
        return "aotd_crystalizator".equals(getIndustryTiedTo());
    }

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        String standard = isCrystalizator() ? Commodities.METALS : Commodities.RARE_METALS;
        String refined = isCrystalizator() ? AoTDCommodities.REFINED_METAL : AoTDCommodities.PURIFIED_TRANSPLUTONICS;
        industry.getSupply(standard).getQuantity().modifyMult(getId(), isBulk() ? 1.25f : 0.75f, getName());
        industry.getSupply(refined).getQuantity().modifyMult(getId(), isBulk() ? 0.75f : 1.25f, getName());
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.getSupply(isCrystalizator() ? Commodities.METALS : Commodities.RARE_METALS).getQuantity().unmodify(getId());
        industry.getSupply(isCrystalizator() ? AoTDCommodities.REFINED_METAL : AoTDCommodities.PURIFIED_TRANSPLUTONICS)
                .getQuantity().unmodify(getId());
    }

    @Override
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) {
        String standard = isCrystalizator() ? "metals" : "transplutonics";
        String refined = isCrystalizator() ? "refined metals" : "enriched transplutonics";
        tooltip.addPara("Output: %s.", 8f, Misc.getPositiveHighlightColor(), "+25% " + (isBulk() ? standard : refined));
        tooltip.addPara("Trade-off: %s.", 4f, Misc.getNegativeHighlightColor(), "-25% " + (isBulk() ? refined : standard));
    }
}
