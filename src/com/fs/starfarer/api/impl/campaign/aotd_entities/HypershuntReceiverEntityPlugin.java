package com.fs.starfarer.api.impl.campaign.aotd_entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.industries.AoTDBaseIndustry;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import data.kaysaar.aotd.vok.campaign.econ.megastructures.impl.scripts.CoronalHypershuntMegastructure;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl.HypershuntReciverImpl;
import data.kaysaar.aotd.vok.misc.AoTDMisc;

import java.util.Map;

public class HypershuntReceiverEntityPlugin extends BaseCampaignObjectivePlugin {
    private static final float REFRESH_INTERVAL_DAYS = 0.5f;
    private static final float BETA_INDUSTRY_UPKEEP_MULT = 0.90f;
    private static final String EFFECT_NAME = "Hypershunt Receiver";
    private static final String LEGACY_MOD_ID = "aotd_hypershunt";

    private transient float refreshDays = REFRESH_INTERVAL_DAYS;
    private transient boolean connected;
    private transient int activeCoreTier;
    private transient boolean shadowSized;

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (!shadowSized && layer == CampaignEngineLayers.BELOW_STATIONS) shadowSized = AoTDMisc.sizeStationShadow(entity);
    }

    @Override
    public void advance(float amount) {
        if (entity == null || Global.getSector() == null || Global.getSector().isPaused()) return;
        if (entity.isExpired()) {
            unmodify();
            return;
        }

        float days = Global.getSector().getClock().convertToDays(amount);
        if (days <= 0f) return;
        refreshDays += days;
        if (refreshDays >= REFRESH_INTERVAL_DAYS) refreshEffects();
    }

    public boolean isWithinRange() {
        return entity != null && !entity.isExpired() && isOperational()
                && CoronalHypershuntMegastructure.isWithinRangeOfAtLeastOneHypershunt(entity);
    }

    private boolean isOperational() {
        if (entity == null || entity.isExpired() || entity.getFaction() == null
                || !entity.getFaction().isPlayerFaction()) return false;

        Object attached = entity.getMemoryWithoutUpdate()
                .get(AoTDStableStructureSpecManager.memKeyInEntities);
        if (attached instanceof HypershuntReciverImpl receiver) return receiver.isFunctional();
        // An unrelated structure implementation must never activate this receiver.
        if (attached instanceof AoTDStableStructureImpl) return false;
        // Existing receivers built before the System Structures conversion remain usable.
        return true;
    }

    private int getCoreTier() {
        if (entity == null) return 0;
        Object attached = entity.getMemoryWithoutUpdate()
                .get(AoTDStableStructureSpecManager.memKeyInEntities);
        if (!(attached instanceof HypershuntReciverImpl receiver)) return 0;

        String core = receiver.getAICoreId();
        if (Commodities.ALPHA_CORE.equals(core)) return 3;
        if (Commodities.BETA_CORE.equals(core)) return 2;
        if (Commodities.GAMMA_CORE.equals(core)) return 1;
        return 0;
    }

    private boolean isEligibleMarket(MarketAPI market) {
        return market != null && market.getFaction() != null && entity.getFaction() != null
                && market.getFactionId().equals(entity.getFaction().getId());
    }

    private String getModifierId() {
        return "aotd_hypershunt_receiver_" + entity.getId();
    }

    public void refreshEffects() {
        if (entity == null || Global.getSector() == null || entity.getStarSystem() == null) return;
        refreshDays = 0f;
        boolean valid = isWithinRange();
        int tier = valid ? getCoreTier() : 0;
        String modifierId = getModifierId();

        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(entity.getStarSystem())) {
            // Remove existing modifiers first so only commodities with real supply
            // receive the +1 bonus, and deactivated industries lose it correctly.
            clearMarketModifiers(market, modifierId);
            if (!valid || !isEligibleMarket(market)) continue;

            // Base receiver: +1 slot. Alpha: an additional +1 slot.
            int slots = tier >= 3 ? 2 : 1;
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES)
                    .modifyFlat(modifierId, slots, EFFECT_NAME);

            if (tier >= 1) applyProductionBonus(market, modifierId);
            // Beta and Alpha retain Gamma's production bonus and reduce
            // the upkeep of all industries in this star system by 10%.
            if (tier >= 2) applyIndustryUpkeepReduction(market, modifierId);
        }

        activeCoreTier = tier;
        connected = valid;
    }

    private void applyProductionBonus(MarketAPI market, String modifierId) {
        for (Industry industry : market.getIndustries()) {
            if (!industry.isFunctional()) continue;

            if (industry instanceof AoTDBaseIndustry exactIndustry) {
                for (Map.Entry<String, MutableStat> entry : exactIndustry.getExactProduction().entrySet()) {
                    MutableStat stat = entry.getValue();
                    if (stat != null && stat.getModifiedValue() > 0f) {
                        stat.modifyFlat(modifierId, 1f, EFFECT_NAME);
                    }
                }
            }

            for (MutableCommodityQuantity supply : industry.getAllSupply()) {
                if (supply == null || supply.getQuantity() == null) continue;
                if (industry instanceof AoTDBaseIndustry exactIndustry
                        && exactIndustry.hasExactProduction(supply.getCommodityId())) continue;
                if (supply.getQuantity().getModifiedValue() > 0f) {
                    supply.getQuantity().modifyFlat(modifierId, 1f, EFFECT_NAME);
                }
            }
        }
    }

    private void applyIndustryUpkeepReduction(MarketAPI market, String modifierId) {
        for (Industry industry : market.getIndustries()) {
            industry.getUpkeep().modifyMult(modifierId, BETA_INDUSTRY_UPKEEP_MULT,
                    EFFECT_NAME);
        }
    }

    private void clearMarketModifiers(MarketAPI market, String modifierId) {
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(modifierId);
        // Migration from the original entity plugin's shared +2-slot modifier.
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(LEGACY_MOD_ID);

        for (Industry industry : market.getIndustries()) {
            industry.getUpkeep().unmodifyMult(modifierId);
            if (industry instanceof AoTDBaseIndustry exactIndustry) {
                for (MutableStat stat : exactIndustry.getExactProduction().values()) {
                    if (stat != null) stat.unmodifyFlat(modifierId);
                }
            }
            for (MutableCommodityQuantity supply : industry.getAllSupply()) {
                if (supply != null && supply.getQuantity() != null) {
                    supply.getQuantity().unmodifyFlat(modifierId);
                }
            }
        }
    }


    public void unmodify() {
        connected = false;
        activeCoreTier = 0;
        if (entity == null || Global.getSector() == null || entity.getStarSystem() == null) return;
        String modifierId = getModifierId();
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(entity.getStarSystem())) {
            clearMarketModifiers(market, modifierId);
        }
    }

    @Override
    public void printEffect(TooltipMakerAPI text, float pad) {
        int coreTier = getCoreTier();
        int slots = coreTier >= 3 ? 2 : 1;
        text.addPara(BaseIntelPlugin.INDENT + "%s maximum industry slots for same-faction colonies in this star system.",
                pad, Misc.getHighlightColor(), "+" + slots);
        if (coreTier >= 1) {
            text.addPara(BaseIntelPlugin.INDENT + "%s to every existing commodity output from functional industries.",
                    5f, Misc.getHighlightColor(), "+1");
        }
        if (coreTier >= 2) {
            text.addPara(BaseIntelPlugin.INDENT + "%s industry upkeep for same-faction colonies in this star system.",
                    5f, Misc.getHighlightColor(), "-10%");
        }
        text.addPara(BaseIntelPlugin.INDENT + "Enables unique bonuses for connected megastructures.", 5f);
        text.addPara(isWithinRange() ? "Receiver connected to a Coronal Hypershunt."
                        : "Receiver is not operational or outside Hypershunt range.",
                isWithinRange() ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor(), 5f);
    }
}
