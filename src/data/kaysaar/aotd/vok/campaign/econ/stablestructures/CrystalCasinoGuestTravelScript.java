package data.kaysaar.aotd.vok.campaign.econ.stablestructures;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;


public class CrystalCasinoGuestTravelScript implements EveryFrameScript {
    private final String personId;
    private boolean done;

    public CrystalCasinoGuestTravelScript(String personId) {
        this.personId = personId;
    }

    @Override
    public void advance(float amount) {
        if (done || amount <= 0f) return;
        float days = Global.getSector().getClock().convertToDays(amount);
        if (days <= 0f) return;
        done = !CrystalCasinoGuestManager.getInstance().advanceTravel(personId, days);
    }

    @Override public boolean isDone() { return done; }
    @Override public boolean runWhilePaused() { return false; }
}
