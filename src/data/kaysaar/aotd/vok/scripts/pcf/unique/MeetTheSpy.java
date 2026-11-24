package data.kaysaar.aotd.vok.scripts.pcf.unique;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDReactorPlatform;
import data.kaysaar.aotd.vok.Ids.AoTDMemFlags;

import java.util.List;

public class MeetTheSpy {
    public static final String SITE = "$aotd_spy_site";
    public static final String CODE = "$aotd_spy_code";
    public static final String HITS = "$aotd_spy_hits";

    public static void assignFacility() {
        MemoryAPI sector = Global.getSector().getMemoryWithoutUpdate();
        if (sector.contains(SITE)) return;
        Object facilities = Global.getSector().getPersistentData().get(AoTDMemFlags.preCollapseFacList);
        if (!(facilities instanceof List<?>)) return;
        for (Object candidate : (List<?>) facilities) {
            if (!(candidate instanceof PlanetAPI planet) || planet.getStarSystem() == null || planet.getMarket() == null
                    || !planet.getMarket().isPlanetConditionMarketOnly() || !planet.hasCondition("pre_collapse_facility")) continue;
            MemoryAPI market = planet.getMarket().getMemoryWithoutUpdate();
            MemoryAPI entity = planet.getMemoryWithoutUpdate();
            if (market.getBoolean(AoTDReactorPlatform.SITE) || market.getBoolean("$aotd_fac_explored") || market.getBoolean("$aotd_failed_pre_collapse")
                    || entity.contains("$aotd_precollapse_fleet") || entity.getBoolean("$aotd_r_expedition_present")
                    || entity.getBoolean("$aotd_defeated_pcf") || entity.contains("$defenderFleet")) continue;
            market.set(SITE, true);
            sector.set(SITE, planet.getId());
            planet.setName("Teufort");
            if ("aotd_pre_collapse_fac".equals(entity.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE))) {
                entity.unset(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
                entity.unset("$hasDefenders");
            }
            Global.getLogger(MeetTheSpy.class).info("Unique PCF [Vault]: " + planet.getName() + " [" + planet.getId()
                    + "] in " + planet.getStarSystem().getName() + " [" + planet.getStarSystem().getId() + "]");
            return;
        }
    }
}
