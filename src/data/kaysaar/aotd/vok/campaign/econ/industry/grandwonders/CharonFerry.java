package data.kaysaar.aotd.vok.campaign.econ.industry.grandwonders;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.grandwonders.GrandWonderAPI;
import data.kaysaar.aotd.tot.grandwonders.GrandWonderTypeManager;
import data.kaysaar.aotd.vok.Ids.AoTDCommodities;
import data.kaysaar.aotd.vok.Ids.AoTDItems;
import data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.models.ColonyDevelopmentManager;
import data.kaysaar.aotd.vok.ui.basecomps.ImageViewer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class CharonFerry extends BaseIndustry implements GrandWonderAPI {
    private static final int CELLS = 3;
    private static final int DAYS = 15;
    private float productionDays;
    private int shortage;

    @Override
    public void apply() {
        super.apply(true);
        demand(AoTDCommodities.DOMAIN_GRADE_MACHINERY, 4);
        demand(AoTDCommodities.ADVANCED_COMPONENTS, 4);
        demand(Commodities.ORGANICS, 3);
        demand(AoTDCommodities.REFINED_METAL, 2);
        demand(Commodities.MARINES, 6);
        shortage = getMaxDeficit(AoTDCommodities.DOMAIN_GRADE_MACHINERY, AoTDCommodities.ADVANCED_COMPONENTS,
                Commodities.ORGANICS, AoTDCommodities.REFINED_METAL, Commodities.MARINES).two;
    }

    @Override
    public String getWonderTypeId() {
        return "dangerous";
    }

    @Override
    public LinkedHashMap<String, Integer> getDemandCostForRestoration() {
        LinkedHashMap<String, Integer> demand = new LinkedHashMap<>();
        demand.put(AoTDCommodities.REFINED_METAL, 6);
        demand.put(Commodities.ORGANICS, 5);
        demand.put(AoTDCommodities.ADVANCED_COMPONENTS, 5);
        demand.put(AoTDCommodities.DOMAIN_GRADE_MACHINERY, 4);
        return demand;
    }

    @Override
    public void finishedConstruction(MarketAPI market) {
    }

    @Override
    public LinkedHashMap<String, String> getRequirementsToBuildWonder() {
        LinkedHashMap<String, String> requirements = new LinkedHashMap<>();
        requirements.put("abyss_colony", "Requires the Abyssal Frontier development plan.");
        return requirements;
    }

    @Override
    public boolean hasReqBeenMetOnMarket(String id) {
        return "abyss_colony".equals(id) && id.equals(ColonyDevelopmentManager.getColonyDevPlanIfPresent(market));
    }

    @Override
    public boolean shouldShowInListOfWonders(MarketAPI market) {
        return market != null && market.isPlayerOwned() && "abyss_colony".equals(ColonyDevelopmentManager.getColonyDevPlanIfPresent(market))
                && GrandWonderTypeManager.getSpec(getWonderTypeId()).canBuildAdditionalWonderOfType(getId(), market);
    }

    @Override
    public boolean isAvailableToBuild() {
        return shouldShowInListOfWonders(market);
    }

    @Override
    public LinkedHashSet<String> getIndustriesToPreventFromAppearingInMenu(MarketAPI market) {
        return new LinkedHashSet<>();
    }

    @Override
    public boolean canShutDown() {
        return false;
    }

    @Override
    public boolean showShutDown() {
        return false;
    }

    private int getCellBonus(String coreId) {
        if (Commodities.ALPHA_CORE.equals(coreId)) return 2;
        if (Commodities.BETA_CORE.equals(coreId) || Commodities.GAMMA_CORE.equals(coreId)) return 1;
        return 0;
    }

    @Override
    public void addAICoreSection(TooltipMakerAPI tooltip, String coreId, AICoreDescriptionMode mode) {
        super.addAICoreSection(tooltip, coreId, mode);
        int bonus = getCellBonus(coreId);
        if (bonus > 0) {
            tooltip.addPara("Increases Tenebrium Cells produced per cycle by %s.", 3f, Misc.getHighlightColor(), "+" + bonus);
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (!isFunctional() || !market.isPlayerOwned()) return;
        productionDays += Global.getSector().getClock().convertToDays(amount) / (1f + shortage);
        int batches = (int) (productionDays / DAYS);
        if (batches == 0) return;
        MarketAPI storage = Global.getSector().getPlayerFaction().getProduction().getGatheringPoint();
        if (storage == null || !storage.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) storage = market;
        if (!storage.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) return;
        storage.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSpecial(new SpecialItemData(AoTDItems.TENEBRIUM_CELL, null), batches * (CELLS + getCellBonus(getAICoreId())));
        productionDays -= batches * DAYS;
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        addToCustomSectionInTooltip(tooltip);
    }

    @Override
    public void addToCustomSectionInTooltip(TooltipMakerAPI tooltip) {
        tooltip.addSectionHeading("Tenebrium Production", Alignment.MID, 5f);
        float width = tooltip.getWidthSoFar();
        CustomPanelAPI row = Global.getSettings().createCustom(width, 30, null);
        String icon = Global.getSettings().getSpecialItemSpec(AoTDItems.TENEBRIUM_CELL).getIconName();
        int cells = CELLS + getCellBonus(getAICoreId());
        for (int i = 0; i < cells; i++) {
            ImageViewer cell = new ImageViewer(30, 30, icon);
            row.addComponent(cell.getComponentPanel()).inTL((width - 30 - (cells - 1) * 15) / 2f + i * 15, 0);
        }
        tooltip.addCustom(row, 5f);
        tooltip.addPara("Produces %s every %s at full supply without consuming Shrouded Substrate.", 5f, Misc.getHighlightColor(), cells + " Tenebrium Cells", DAYS + " days");
        if (shortage > 0) {
            tooltip.addPara("Current shortages extend each production cycle to %s.", 3f, Misc.getNegativeHighlightColor(),
                    DAYS * (1 + shortage) + " days");
        }
    }
}
