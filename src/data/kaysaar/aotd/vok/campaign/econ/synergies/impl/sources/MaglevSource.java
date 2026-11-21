package data.kaysaar.aotd.vok.campaign.econ.synergies.impl.sources;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipeManager;
import data.kaysaar.aotd.vok.campaign.econ.recipes.MaglevMaintenanceRecipe;

public class MaglevSource extends BaseIndustrySynergySource{
    public MaglevSource(float baseEfficiency, String id) {
        super(baseEfficiency, id);
    }

    @Override
    public float calculateEfficiencyFromIndustry(Industry industry, boolean includeDemand) {
        if (!industry.isFunctional() || !(AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry) instanceof MaglevMaintenanceRecipe recipe)) {
            return super.calculateEfficiencyFromIndustry(industry, includeDemand);
        }
        return super.calculateEfficiencyFromIndustry(industry, false) + recipe.getEfficiencyBonus(industry);
    }

    @Override
    public float getBonusForImproved() {
        return 0.3f;
    }

    @Override
    public float getBonusForAICore(String aiCoreID) {
        if(Commodities.ALPHA_CORE.equals(aiCoreID)){
            return 0.2f;
        }
        return 0;
    }
}
