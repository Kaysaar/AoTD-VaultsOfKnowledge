package data.kaysaar.aotd.vok.campaign.econ.recipes;


import com.fs.starfarer.api.campaign.econ.Industry;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;

public abstract class ResearchRecipe extends AoTDIndustryRecipePlugin {
    @Override
    public boolean canBeUsed(Industry industry) {
        return getMissingResearch(industry) == null;
    }

    @Override
    public String getUnavailableReason(Industry industry) {
        String research = getMissingResearch(industry);
        return research == null ? super.getUnavailableReason(industry) : "Required technology: " + research;
    }

    private String getMissingResearch(Industry industry) {
        return AoTDMainResearchManager.getInstance().getMissingRecipeResearch(getId(), industry.getMarket());
    }
}
