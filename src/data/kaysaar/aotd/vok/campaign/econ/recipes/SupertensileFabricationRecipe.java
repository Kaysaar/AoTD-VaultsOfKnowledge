package data.kaysaar.aotd.vok.campaign.econ.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;
import data.kaysaar.aotd.tot.industries.AoTDBaseIndustry;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;

public class SupertensileFabricationRecipe extends ResearchRecipe {
    private static final String[] INPUTS = {AoTDCommodities.REFINED_METAL, AoTDCommodities.DOMAIN_GRADE_MACHINERY,
            AoTDCommodities.ADVANCED_COMPONENTS, Commodities.ORGANICS};
    private static final String[] PRODUCTS = {"aotd_spaceframe_modules", DeployFrontierKit.KIT};

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        AoTDBaseIndustry foundry = (AoTDBaseIndustry) industry;
        int size = foundry.getMarket().getSize();
        foundry.getExactDemandStat(Commodities.METALS).modifyMult(getId(), 0f, getName());
        foundry.demand(getId(), AoTDCommodities.REFINED_METAL, size, getName());
        foundry.demand(getId(), AoTDCommodities.DOMAIN_GRADE_MACHINERY, Math.max(1, size - 2), getName());
        foundry.demand(getId(), AoTDCommodities.ADVANCED_COMPONENTS, Math.max(1, size - 1), getName());
        float fulfillment = foundry.getDemandFulfillment(INPUTS);
        for (String product : PRODUCTS) {
            foundry.getSupply(product).getQuantity().modifyMult("aotd_input_shortage", fulfillment, "Input shortages");
            foundry.getSupply(product).getQuantity().modifyMult(getId(), 2f, getName());
        }
    }

    @Override
    public void unapply(BaseIndustry industry) {
        AoTDBaseIndustry foundry = (AoTDBaseIndustry) industry;
        if (foundry.hasExactDemand(Commodities.METALS)) foundry.getExactDemandStat(Commodities.METALS).unmodifyMult(getId());
        for (String input : INPUTS) {
            if (!Commodities.ORGANICS.equals(input)) foundry.demand(getId(), input, 0, getName());
        }
        for (String product : PRODUCTS) {
            foundry.getSupply(product).getQuantity().unmodifyMult(getId());
        }
    }

    @Override
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) {
        tooltip.addPara("Uses %s, %s, %s and %s to double production of %s and %s at full supply.", 5f, Misc.getHighlightColor(),
                "Refined Metal", "Domain-Grade Machinery", "Electronic Components", "Organics", "Spaceframe Modules", "Frontier Development Kits");
    }
}
