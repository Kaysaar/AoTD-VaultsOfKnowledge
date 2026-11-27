package data.kaysaar.aotd.vok.timeline.templates;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.models.BaseFactionTimelineEvent;
import data.scripts.models.TimelineEventType;

import java.awt.Color;

public class FirstSystemStructureEvent extends BaseFactionTimelineEvent {
    private final String structureId;

    public FirstSystemStructureEvent(String structureId) {
        this.structureId = structureId;
    }

    @Override
    public String getID() {
        return "FirstSystemStructure_" + structureId;
    }

    @Override
    public boolean checkForCondition() {
        for (SectorEntityToken entity : Global.getSector().getCustomEntitiesWithTag(structureId)) {
            if (structureId.equals(entity.getCustomEntityType()) && !entity.isExpired()
                    && entity.getFaction() != null && entity.getFaction().isPlayerFaction()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getTitleOfEvent() {
        return Global.getSettings().getCustomEntitySpec(structureId).getDefaultName();
    }

    @Override
    public String getImagePath() {
        return Global.getSettings().getCustomEntitySpec(structureId).getInteractionImage();
    }

    @Override
    public void createSmallNoteForEvent(TooltipMakerAPI tooltip) {
        tooltip.addPara("%s has completed its first %s.", 0f, Color.ORANGE,
                Global.getSector().getPlayerFaction().getDisplayNameLong(), getTitleOfEvent()).setAlignment(Alignment.MID);
    }

    @Override
    public void createDetailedTooltipOnHover(TooltipMakerAPI tooltip) {
        tooltip.addPara("%s has finished construction of its first %s, a milestone in the development of its orbital infrastructure.",
                5f, Color.ORANGE, Global.getSector().getPlayerFaction().getDisplayNameLong(), getTitleOfEvent());
    }

    @Override
    public TimelineEventType getEventType() {
        return TimelineEventType.UNIQUE;
    }
}
