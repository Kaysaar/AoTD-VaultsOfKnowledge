package data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.Ids.AoTDIndustries;
import data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.models.BaseColonyDevelopment;
import data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.models.ColonyDevelopmentManager;

import java.awt.Color;
import java.util.List;

public class HexagonPlatform extends BaseColonyDevelopment {
    public static final String ID = "hexagon_platform";
    public static final String UNLOCKED = "$aotd_hexagon_platform_unlocked";
    public static final String BUILD_RESTRICTION = "Unavailable on colonies using the Hexagon Platform development plan.";
    public static final List<String> BLOCKED_INDUSTRIES = List.of(Industries.ORBITALWORKS, AoTDIndustries.HIGHTECH_INDUSTRY);

    @Override
    public String getName() {
        return "Hexagon Platform";
    }

    @Override
    public float getOrder() {
        return 110f;
    }

    @Override
    public Color getBrightButtonColour(MarketAPI market) {
        return new Color(82, 188, 236);
    }

    @Override
    public Color getDarkButtonColour(MarketAPI market) {
        return new Color(16, 60, 92);
    }

    public static boolean isWaterWorld(MarketAPI market) {
        PlanetAPI planet = market.getPlanetEntity();
        return planet != null && (Planets.PLANET_WATER.equals(planet.getTypeId()) || market.hasCondition(Conditions.WATER_SURFACE));
    }

    public static boolean blocksIndustry(MarketAPI market, String industry) {
        return industry != null && BLOCKED_INDUSTRIES.contains(industry)
                && ID.equals(ColonyDevelopmentManager.getColonyDevPlanIfPresent(market));
    }

    @Override
    public boolean canShowOnMarket(MarketAPI market) {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(UNLOCKED) && isWaterWorld(market);
    }

    @Override
    public boolean canBeAppliedOnMarket(MarketAPI market) {
        return canShowOnMarket(market);
    }

    @Override
    public void apply(MarketAPI market) {
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(ID, 1.2f, getName());
        market.getStability().modifyFlat(ID, market.getSize() / 2, getName());
        market.getAccessibilityMod().modifyFlat(ID, -0.2f, getName());
        for (Industry industry : market.getIndustries()) {
            if (industry.isIndustry()) {
                industry.getDemand(Commodities.FUEL).getQuantity().modifyFlat(ID, 1, getName());
            }
            if (industry.isFunctional() && industry.getSpec().hasTag(Industries.AQUACULTURE)) {
                industry.getSupply(Commodities.FOOD).getQuantity().modifyFlat(ID, 4, getName());
            }
        }
    }

    @Override
    public void unapply(MarketAPI market) {
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(ID);
        market.getStability().unmodify(ID);
        market.getAccessibilityMod().unmodify(ID);
        for (Industry industry : market.getIndustries()) {
            if (industry.isIndustry()) industry.getDemand(Commodities.FUEL).getQuantity().unmodify(ID);
            if (industry.getSpec().hasTag(Industries.AQUACULTURE)) industry.getSupply(Commodities.FOOD).getQuantity().unmodify(ID);
        }
    }

    @Override
    public void generateDescriptionSection(MarketAPI market, TooltipMakerAPI tooltip) {
        tooltip.addPara(
                "The plans recovered from the sunken reactor describe how the Domain settled worlds with little dry land. " +
                        "A colony can begin with one moored hexagonal deck and tow in new sections as it needs housing, workshops, and docks.",
                5f
        );
        tooltip.addPara(
                "The decks shelter fisheries in the waters between them. Cargo ships berth along the exposed perimeter, " +
                        "where ocean swells limit deliveries and make it impractical to bring in the heaviest industrial equipment.",
                5f
        );
    }

    @Override
    public void generateEffects(MarketAPI market, TooltipMakerAPI tooltip, String fontForSections) {
        tooltip.setParaFont(fontForSections);
        tooltip.addPara("Interlocking Districts", Misc.getTooltipTitleAndLightHighlightColor(), 5f);
        tooltip.setParaFont(Fonts.DEFAULT_SMALL);
        tooltip.addPara("Ground defenses and synergy efficiency each gain %s.", 3f, Misc.getPositiveHighlightColor(), "20%");
        tooltip.addPara("Gain %s for every two colony size levels.", 3f, Misc.getPositiveHighlightColor(), "+1 stability");

        tooltip.setParaFont(fontForSections);
        tooltip.addPara("Sheltered Fisheries", Misc.getTooltipTitleAndLightHighlightColor(), 5f);
        tooltip.setParaFont(Fonts.DEFAULT_SMALL);
        tooltip.addPara("Aquaculture and Fishing Harbour produce %s.", 3f, Misc.getPositiveHighlightColor(), "4 additional units of food");

        tooltip.setParaFont(fontForSections);
        tooltip.addPara("Offshore Logistics", Misc.getTooltipTitleAndLightHighlightColor(), 5f);
        tooltip.setParaFont(Fonts.DEFAULT_SMALL);
        tooltip.addPara("Each industry requires %s.", 3f, Misc.getNegativeHighlightColor(), "1 additional unit of fuel");
        tooltip.addPara("Reduce accessibility by %s.", 3f, Misc.getNegativeHighlightColor(), "20%");
        tooltip.addPara("The colony cannot build %s or %s.", 3f, Misc.getNegativeHighlightColor(), "Orbital Works", "High-tech Industry");
    }
}
