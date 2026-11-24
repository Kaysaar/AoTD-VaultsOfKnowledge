package data.kaysaar.aotd.vok.campaign.econ.industry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.econ.impl.HeavyIndustry;
import data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.impl.HexagonPlatform;

public class AoTDHeavyIndustry extends HeavyIndustry {
    public boolean ignorePolutionStat = false;

    public void setIgnorePolutionStat(boolean ignorePolutionStat) {
        this.ignorePolutionStat = ignorePolutionStat;
    }

    @Override
    public boolean isAvailableToBuild() {
        return !HexagonPlatform.blocksIndustry(market, getId()) && super.isAvailableToBuild();
    }

    @Override
    public String getUnavailableReason() {
        if (HexagonPlatform.blocksIndustry(market, getId())) return HexagonPlatform.BUILD_RESTRICTION;
        return super.getUnavailableReason();
    }

    @Override
    public boolean canUpgrade() {
        return !HexagonPlatform.blocksIndustry(market, getSpec().getUpgrade()) && super.canUpgrade();
    }

    @Override
    protected void updatePollutionStatus() {
        if(ignorePolutionStat)return;
        if(special!=null&& Global.getSettings().getSpecialItemSpec(special.getId()).hasTag("no_pollution")){
            return;
        }
        super.updatePollutionStatus();
    }

    @Override
    public void setSpecialItem(SpecialItemData special) {
        super.setSpecialItem(special);
    }
}
