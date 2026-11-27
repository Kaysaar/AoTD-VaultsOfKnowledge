package data.kaysaar.aotd.vok.campaign.econ.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;
import data.kaysaar.aotd.tot.industries.AoTDBaseIndustry;
import data.kaysaar.aotd.tot.plugins.AoTDBaseDemSupCalc;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;

public class RefinedSpaceframeFabricationRecipe extends ResearchRecipe {
    private static final String[] PRODUCTS = {"aotd_spaceframe_modules", DeployFrontierKit.KIT};
    private static final float PRODUCTION_MULT = 1.25f;

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        AoTDBaseIndustry foundry = (AoTDBaseIndustry) industry;
        foundry.getExactDemandStat(Commodities.METALS).modifyMult(getId(), 0f, getName());
        foundry.demand(getId(), AoTDCommodities.REFINED_METAL, foundry.getMarket().getSize(), getName());
        float fulfillment = foundry.getDemandFulfillment(AoTDCommodities.REFINED_METAL, Commodities.ORGANICS);
        for (String product : PRODUCTS) {
            MutableStat quantity = foundry.getSupply(product).getQuantity();
            quantity.modifyMult("aotd_input_shortage", fulfillment, "Input shortages");
            quantity.modifyMult(AoTDBaseDemSupCalc.RAW_PRODUCTION_MULT, PRODUCTION_MULT, getName());
        }
    }

    @Override
    public void unapply(BaseIndustry industry) {
        AoTDBaseIndustry foundry = (AoTDBaseIndustry) industry;
        if (foundry.hasExactDemand(Commodities.METALS)) foundry.getExactDemandStat(Commodities.METALS).unmodifyMult(getId());
        foundry.demand(getId(), AoTDCommodities.REFINED_METAL, 0, getName());
        for (String product : PRODUCTS) {
            MutableStat quantity = foundry.getSupply(product).getQuantity();
            quantity.unmodifyMult("aotd_input_shortage");
            quantity.unmodifyMult(AoTDBaseDemSupCalc.RAW_PRODUCTION_MULT);
        }
    }

    @Override
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) {
        tooltip.addPara("Replaces %s with %s. At full supply, production of %s and %s increases by up to %s.",
                5f, Misc.getHighlightColor(), "Metals", "Refined Metal", "Spaceframe Modules", "Frontier Development Kits", "25%");
    }
}
