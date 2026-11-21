package data.kaysaar.aotd.vok.events;

import ashlib.data.plugins.events.AshlibBaseEvent;
import ashlib.data.plugins.events.AshlibBaseEventIntel;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.Ids.AoTDIndustries;
import data.kaysaar.aotd.vok.misc.AoTDMisc;

public class AbyssalColonizationProposal extends AshlibBaseEvent {
    public static String memkeyForColnization = "$aotd_can_colonize";
    public AbyssalColonizationProposal() {
        super("aotd_hades_colony_proposal");
    }
    @Override
    public String getEventName() {
        return "Interesting Development";
    }

    @Override
    public boolean canEvenFireEvent() {
        return AoTDMisc.isPLayerHavingIndustry(AoTDIndustries.EXOMATTER_PROCESSING);
    }

    @Override
    public boolean shouldFireImmediately() {
        return true;
    }
    @Override
    public void configureEvent() {
        maxAmount = 1;
    }

    @Override
    public boolean isMinorEvent() {
        return false;
    }

    @Override
    public void createTooltipForEvent(TooltipMakerAPI tooltip) {
        tooltip.addPara(
                "Continued production of Tenebrium has raised the need for efficient exomatter sourcing." +
                        "%s is an ideal location for development, as its location at the boundary of abyssal hyperspace allows for easy access to semi-permeable \"realspace\" through which exomatter substrate can be extracted. ",
                5f, Misc.getHighlightColor(), "Limbo"
        );
        tooltip.addSectionHeading("Effects of Event", Alignment.MID, 5f);
        tooltip.addPara(
                "You can now colonize Hades. A colony there can build %s, a new Grand Wonder.",
                10f, Global.getSettings().getFactionSpec(Factions.DWELLER).getBaseUIColor(), "Charon's Ferry"
        );
    }
    @Override
    public AshlibBaseEventIntel createIntelPlugin() {
        return null;
    }

    @Override
    public float getImmediateEventDelayDays() {
        return 30f;
    }

    @Override
    public float getImageCropY() {
        return 0F;
    }

    @Override
    public String getImageName() {
        return Global.getSettings().getSpriteName("illustrations", "shrouded_weapon_assembly");
    }

    @Override
    public boolean shouldPublishIntel(AshlibBaseEventIntel intel) {
        return false;
    }

    @Override
    public void executeEvent() {
        Global.getSector().getMemoryWithoutUpdate().set(memkeyForColnization,true);
    }
}
