package data.kaysaar.aotd.vok.timeline.unique;

import data.scripts.models.TimelineEventType;
import data.scripts.timelineevents.templates.FirstIndustryEvent;

public class FirstGrandWonderEvent extends FirstIndustryEvent {
    public FirstGrandWonderEvent(String industryId) {
        super(industryId, null);
    }

    @Override
    public TimelineEventType getEventType() {
        return TimelineEventType.UNIQUE;
    }

    @Override
    public int getPointsForGoal() {
        return 70;
    }
}
