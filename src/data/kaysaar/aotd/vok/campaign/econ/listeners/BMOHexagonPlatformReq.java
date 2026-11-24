package data.kaysaar.aotd.vok.campaign.econ.listeners;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.impl.HexagonPlatform;
import kaysaar.bmo.buildingmenu.additionalreq.BaseAdditionalReq;

import java.util.ArrayList;

public class BMOHexagonPlatformReq extends BaseAdditionalReq {
    @Override
    public boolean metCriteria(MarketAPI market, String indId) {
        return !HexagonPlatform.blocksIndustry(market, indId);
    }

    @Override
    public String getReason(MarketAPI market, String indId) {
        return HexagonPlatform.blocksIndustry(market, indId) ? HexagonPlatform.BUILD_RESTRICTION : "";
    }

    @Override
    public ArrayList<String> getIndustriesAffected() {
        return new ArrayList<>(HexagonPlatform.BLOCKED_INDUSTRIES);
    }
}
