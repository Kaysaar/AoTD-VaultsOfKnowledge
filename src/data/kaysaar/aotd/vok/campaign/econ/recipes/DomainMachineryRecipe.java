package data.kaysaar.aotd.vok.campaign.econ.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;

import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;

public class DomainMachineryRecipe extends ResearchRecipe {
    private String[] getProducts() {
        return switch (getIndustryTiedTo()) {
            case "aotd_artisanal_farming", "aotd_garden_of_elysium" -> new String[]{Commodities.FOOD, Commodities.LUXURY_GOODS};
            case "aotd_subsidised_farming" -> new String[]{Commodities.FOOD};
            case "aotd_commercial_manu" -> new String[]{Commodities.DOMESTIC_GOODS, Commodities.LUXURY_GOODS};
            default -> new String[]{Commodities.DRUGS, "wwlb_cerulean_vapors"};
        };
    }

    private boolean reducesHeavyMachineryDemand() {
        return "aotd_garden_of_elysium".equals(getIndustryTiedTo()) || "aotd_artisanal_farming".equals(getIndustryTiedTo()) || "aotd_subsidised_farming".equals(getIndustryTiedTo());
    }

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        int demand = Math.max(1, industry.getMarket().getSize() - 2);
        String machinery = AoTDCommodities.DOMAIN_GRADE_MACHINERY;
        industry.demand(getId(), machinery, demand, getName());
        String[] products = getProducts();
        if (reducesHeavyMachineryDemand()) {
            industry.getDemand(Commodities.HEAVY_MACHINERY).getQuantity().modifyMult(getId(), 0.5f, getName());
            applyDeficitToProduction(industry, new String[]{Commodities.HEAVY_MACHINERY, machinery}, products);
        }

        int deficit = industry.getMaxDeficit(machinery).two;
        float fulfillment = Math.max(0f, 1f - (float) deficit / demand);
        for (MutableCommodityQuantity supply : industry.getAllSupply()) {
            for (String product : products) {
                if (supply.getCommodityId().equals(product)) {
                    supply.getQuantity().modifyMult(getId(), 1f + 0.5f * fulfillment, getName());
                }
            }
        }
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.demand(getId(), AoTDCommodities.DOMAIN_GRADE_MACHINERY, 0, getName());
        if (reducesHeavyMachineryDemand()) {
            industry.getDemand(Commodities.HEAVY_MACHINERY).getQuantity().unmodifyMult(getId());
        }
        for (MutableCommodityQuantity supply : industry.getAllSupply()) {
            supply.getQuantity().unmodify(getId());
        }
    }

    @Override
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) {
        if (reducesHeavyMachineryDemand()) {
            tooltip.addPara("Reduces %s demand by %s and adds a demand for %s.", 8f, Misc.getHighlightColor(),
                    "Heavy Machinery", "50%", "Domain-Grade Machinery");
            tooltip.addPara("Full Domain-Grade Machinery supply increases output by %s. Shortages of either input reduce output.", 4f,
                    Misc.getPositiveHighlightColor(), "50%");
        } else {
            tooltip.addPara("Adds %s to the existing inputs.", 8f, Misc.getHighlightColor(), "Domain-Grade Machinery");
            tooltip.addPara("Full supply increases the plan's affected goods by %s.", 4f,
                    Misc.getPositiveHighlightColor(), "50%");
        }
    }
}
