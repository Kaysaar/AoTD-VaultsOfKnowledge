package data.kaysaar.aotd.vok.campaign.econ.items;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.Ids.AoTDItems;

public class AquaOrbEffect extends BoostIndustryInstallableItemEffect {
    public AquaOrbEffect() {
        super(AoTDItems.AQUAORB, ItemEffectsRepo.SOIL_NANITES_BONUS, 0);
    }

    @Override
    protected void addItemDescriptionImpl(Industry industry, TooltipMakerAPI text, SpecialItemData data,
                                          InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode, String pre, float pad) {
        text.addPara(pre + "Increases production by %s units.", pad, Misc.getHighlightColor(), String.valueOf(supplyIncrease));
    }
}
