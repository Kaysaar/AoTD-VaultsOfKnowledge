package data.kaysaar.aotd.vok.campaign.econ.stablestructures;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.campaign.econ.industry.CrystalCasinoPlanetaryIndustry;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl.CrystalCasinoImpl;

import java.util.ArrayList;
import java.util.List;

public class CrystalCasinoGuestManager implements EveryFrameScript, EconomyTickListener, TooltipMakerAPI.TooltipCreator {
    public static final String SAVE_KEY = "aotd_crystal_casino_guests";
    public static final String BLACKLIST_KEY = "$aotdCasinoBlacklisted";
    public static final int CAPACITY = 10;
    public static final int MONTHLY_COST = 10000;
    public static final float INVITE_REPUTATION = 0.30f;
    public static final float DISMISS_REPUTATION = -0.50f;

    /** Rough passenger passage time: two days of preparation plus 0.6 days per LY. */
    public static final float TRAVEL_DAYS_PER_LY = 0.6f;
    public static final int MIN_TRAVEL_DAYS = 2;
    public static final int MAX_TRAVEL_DAYS = 30;

    protected final List<CrystalCasinoGuest> guests = new ArrayList<>();
    protected float elapsedDays;

    public static CrystalCasinoGuestManager getInstance() {
        Object saved = Global.getSector().getPersistentData().get(SAVE_KEY);
        if (saved instanceof CrystalCasinoGuestManager) return (CrystalCasinoGuestManager) saved;
        CrystalCasinoGuestManager manager = new CrystalCasinoGuestManager();
        Global.getSector().getPersistentData().put(SAVE_KEY, manager);
        return manager;
    }

    public static void register() {
        CrystalCasinoGuestManager manager = getInstance();
        Global.getSector().removeTransientScriptsOfClass(CrystalCasinoGuestManager.class);
        Global.getSector().addTransientScript(manager);
        Global.getSector().getListenerManager().removeListenerOfClass(CrystalCasinoGuestManager.class);
        Global.getSector().getListenerManager().addListener(manager, true);
        manager.validateGuests();
    }

    public CrystalCasinoGuest getGuest(PersonAPI person) {
        for (CrystalCasinoGuest guest : guests) if (guest.person == person) return guest;
        return null;
    }

    protected CrystalCasinoGuest getGuestById(String personId) {
        if (personId == null) return null;
        for (CrystalCasinoGuest guest : guests) {
            if (guest.person != null && personId.equals(guest.person.getId())) return guest;
        }
        return null;
    }

    /** Allocated slots, including contacts who have accepted and are on their way. */
    public int getGuestCount() {
        int count = 0;
        for (CrystalCasinoGuest guest : guests) if (guest.active) count++;
        return count;
    }

    /** Guests who have actually arrived and whose penthouse is being charged. */
    public int getArrivedGuestCount() {
        int count = 0;
        for (CrystalCasinoGuest guest : guests) if (guest.active && !guest.travelling) count++;
        return count;
    }

    public int getTravellingGuestCount() {
        int count = 0;
        for (CrystalCasinoGuest guest : guests) if (guest.active && guest.travelling) count++;
        return count;
    }

    public CrystalCasinoImpl findCasino() {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.isPlayerOwned()) continue;
            Industry industry = market.getIndustry(CrystalCasinoImpl.PLANETARY_INDUSTRY_ID);
            if (!(industry instanceof CrystalCasinoPlanetaryIndustry)) continue;
            CrystalCasinoImpl casino = ((CrystalCasinoPlanetaryIndustry) industry).getCasino();
            if (casino != null && casino.isHostOperational()) return casino;
        }
        return null;
    }

    public boolean isContactEligible(PersonAPI person) {
        if (person == null || person.getMarket() == null || !person.getMarket().isInEconomy()
                || getGuest(person) != null || !ContactIntel.playerHasContact(person, false)) return false;
        ContactIntel intel = ContactIntel.getContactIntel(person);
        if (intel == null || intel.isEnding() || intel.isEnded()) return false;
        // Moving an administrator would detach the person from their actual job.
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getAdmin() == person) return false;
        }
        return true;
    }

    public boolean isLuddic(PersonAPI person) {
        return person != null && person.getFaction() != null
                && (Factions.LUDDIC_CHURCH.equals(person.getFaction().getId())
                || Factions.LUDDIC_PATH.equals(person.getFaction().getId()));
    }

    public int getTravelDays(MarketAPI origin, MarketAPI destination) {
        if (origin == null || destination == null) return MIN_TRAVEL_DAYS;
        float distance = Misc.getDistanceLY(origin.getLocationInHyperspace(), destination.getLocationInHyperspace());
        int days = (int) Math.ceil(MIN_TRAVEL_DAYS + distance * TRAVEL_DAYS_PER_LY);
        return Math.max(MIN_TRAVEL_DAYS, Math.min(MAX_TRAVEL_DAYS, days));
    }

    public String invite(PersonAPI person) {
        if (isLuddic(person)) return "luddic";
        if (person == null) return "invalid";
        if (person.getMemoryWithoutUpdate().getBoolean(BLACKLIST_KEY)) return "blacklisted";
        if (!isContactEligible(person)) return "invalid";
        if (getGuestCount() >= CAPACITY) return "full";
        CrystalCasinoImpl casino = findCasino();
        if (casino == null) return "unavailable";

        // Preserve the original directory entry/index/hidden state before departure.
        // The contact stays associated with the origin market while travelling, but
        // disappears from its comm directory immediately after accepting.
        CrystalCasinoGuest guest = new CrystalCasinoGuest(person, casino.getTargetMarket(), casino.getCurrentlyTiedEntity());
        guest.travelling = true;
        guest.travelDaysRemaining = getTravelDays(guest.origin, guest.host);
        guests.add(guest);
        if (guest.origin != null) {
            guest.origin.getCommDirectory().removePerson(person);
        }

        // Persistent script survives saving/loading mid-journey and completes only once.
        Global.getSector().addScript(new CrystalCasinoGuestTravelScript(person.getId()));
        person.getRelToPlayer().adjustRelationship(INVITE_REPUTATION, null);
        return "accepted";
    }

    /** Called by CrystalCasinoGuestTravelScript. True only while still travelling. */
    public boolean advanceTravel(String personId, float days) {
        CrystalCasinoGuest guest = getGuestById(personId);
        if (guest == null || !guest.active || !guest.travelling) return false;

        if (!isGuestCasinoValid(guest)) {
            endStay(guest);
            return false;
        }
        // Do not override another mod or vanilla story logic moving this contact.
        if (guest.person.getMarket() != guest.origin) {
            guest.active = false;
            guests.remove(guest);
            return false;
        }
        guest.travelDaysRemaining -= days;
        if (guest.travelDaysRemaining > 0f) return true;
        // The existing move method preserves ContactIntel importance and comm directory state.
        move(guest.person, guest.host);
        guest.travelDaysRemaining = 0f;
        guest.travelling = false;
        return false;
    }

    protected void move(PersonAPI person, MarketAPI destination) {
        MarketAPI previous = person.getMarket();
        if (previous != null) {
            previous.getCommDirectory().removePerson(person);
            previous.removePerson(person);
        }
        ContactIntel intel = ContactIntel.getContactIntel(person);
        if (intel != null) {
            // Vanilla relocation reduces importance; hospitality must not demote contacts.
            PersonImportance importance = person.getImportance();
            intel.relocateToMarket(destination, false);
            person.setImportance(importance);
        } else {
            person.setMarket(destination);
        }
        if (!destination.getPeopleCopy().contains(person)) destination.addPerson(person);
        if (destination.getCommDirectory().getEntryForPerson(person) == null) destination.getCommDirectory().addPerson(person);
        destination.getCommDirectory().getEntryForPerson(person).setHidden(false);
    }

    public boolean dismiss(PersonAPI person) {
        CrystalCasinoGuest guest = getGuest(person);
        if (guest == null || !guest.active) return false;
        endStay(guest);
        return true;
    }

    protected void endStay(CrystalCasinoGuest guest) {
        if (guest.active) {
            guest.active = false; // Save the decision before returning; penalty is applied exactly once.
            guest.person.getRelToPlayer().adjustRelationship(DISMISS_REPUTATION, null);
        }
        returnGuest(guest);
    }

    public void casinoRemoved(SectorEntityToken casino) {
        if (casino == null) return;
        for (CrystalCasinoGuest guest : new ArrayList<>(guests)) {
            if (guest.casino == casino) endStay(guest);
        }
    }

    protected MarketAPI returnDestination(CrystalCasinoGuest guest) {
        if (guest.origin != null && guest.origin.isInEconomy()) return guest.origin;
        // A decivilized/destroyed home cannot provide a usable directory.
        MarketAPI fallback = null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == guest.host || !market.isInEconomy()) continue;
            if (market.getFactionId().equals(guest.person.getFaction().getId())) return market;
            if (fallback == null && market.isPlayerOwned()) fallback = market;
        }
        return fallback;
    }

    /**
     * Reinstate the pre-invitation comm entry if travel is cancelled before
     * arrival. Never manufacture an entry for someone who was not listed.
     */
    protected void restoreOriginDirectory(CrystalCasinoGuest guest) {
        if (!guest.originallyInDirectory || guest.origin == null
                || guest.person.getMarket() != guest.origin) return;
        CommDirectoryEntryAPI entry = guest.origin.getCommDirectory().getEntryForPerson(guest.person);
        if (entry == null) {
            int index = Math.max(0, Math.min(guest.originalDirectoryIndex,
                    guest.origin.getCommDirectory().getEntriesCopy().size()));
            guest.origin.getCommDirectory().addPerson(guest.person, index);
            entry = guest.origin.getCommDirectory().getEntryForPerson(guest.person);
        }
        if (entry != null) entry.setHidden(guest.originallyHidden);
    }

    protected void returnGuest(CrystalCasinoGuest guest) {
        if (guest.travelling) {
            // The contact has not reached the Casino. Cancel the reservation
            // and put them back into their original comm directory.
            // Do not undo a relocation performed by another script.
            restoreOriginDirectory(guest);
            guests.remove(guest);
            return;
        }
        MarketAPI destination = returnDestination(guest);
        if (destination == null) return; // Retry daily, without charging rent.
        move(guest.person, destination);
        if (destination == guest.origin) {
            destination.getCommDirectory().removePerson(guest.person);
            if (guest.originallyInDirectory) {
                int index = Math.max(0, Math.min(guest.originalDirectoryIndex, destination.getCommDirectory().getEntriesCopy().size()));
                destination.getCommDirectory().addPerson(guest.person, index);
                CommDirectoryEntryAPI entry = destination.getCommDirectory().getEntryForPerson(guest.person);
                entry.setHidden(guest.originallyHidden);
            }
            if (!guest.originallyInPeople) destination.removePerson(guest.person);
        }
        guests.remove(guest);
    }

    protected boolean isGuestCasinoValid(CrystalCasinoGuest guest) {
        return guest.casino != null && !guest.casino.isExpired()
                && guest.host != null && guest.host.isInEconomy()
                && guest.host.isPlayerOwned() && guest.host.getConnectedEntities().contains(guest.casino)
                && guest.host.hasIndustry(CrystalCasinoImpl.PLANETARY_INDUSTRY_ID);
    }

    protected void validateGuests() {
        for (CrystalCasinoGuest guest : new ArrayList<>(guests)) {
            if (!guest.active) {
                returnGuest(guest);
                continue;
            }
            // Disruption/shortages do not evict guests. Destruction, detachment,
            // decivilization or ownership loss ends the lease and travel.
            if (!isGuestCasinoValid(guest)) {
                endStay(guest);
            } else if (guest.travelling) {
                // Keep the original comm directory clear during transit, even if
                // another routine reconstructed its entry after acceptance.
                if (guest.person.getMarket() != guest.origin) {
                    guest.active = false;
                    guests.remove(guest);
                } else if (guest.origin != null &&
                        guest.origin.getCommDirectory().getEntryForPerson(guest.person) != null) {
                    guest.origin.getCommDirectory().removePerson(guest.person);
                }
            } else if (guest.person.getMarket() != guest.host) {
                // Another script deliberately relocated this person: don't drag them back.
                guest.active = false;
                guests.remove(guest);
            }
        }
    }

    @Override public void advance(float amount) {
        elapsedDays += Global.getSector().getClock().convertToDays(amount);
        if (elapsedDays >= 1f) {
            elapsedDays = 0f;
            validateGuests();
        }
    }
    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override public void reportEconomyTick(int iterIndex) {
        validateGuests();
        // A reservation consumes capacity, but rent starts only on arrival.
        int arrived = getArrivedGuestCount();
        if (arrived == 0) return;
        MonthlyReport report = SharedData.getData().getCurrentReport();
        FDNode node = report.getNode(report.getNode(MonthlyReport.FLEET), SAVE_KEY);
        node.name = "Crystal Casino penthouses";
        node.icon = Global.getSettings().getSpriteName("income_report", "generic_expense");
        node.tooltipCreator = this;
        node.upkeep += arrived * MONTHLY_COST / Math.max(1f, Global.getSettings().getFloat("economyIterPerMonth"));
    }
    @Override public void reportEconomyMonthEnd() { }
    @Override public float getTooltipWidth(Object tooltipParam) { return 420f; }
    @Override public boolean isTooltipExpandable(Object tooltipParam) { return false; }
    @Override public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addPara("Penthouses allocated: %s / %s (%s arrived, %s travelling).", 0f,
                Misc.getHighlightColor(), "" + getGuestCount(), "" + CAPACITY,
                "" + getArrivedGuestCount(), "" + getTravellingGuestCount());
        tooltip.addPara("Each arrived resident costs %s credits per month. Guests in transit do not incur rent.",
                5f, Misc.getHighlightColor(), Misc.getWithDGS(MONTHLY_COST));
        tooltip.addPara("Costs accrue each economy tick. Expenses already incurred remain due after a guest leaves.", 5f);
        for (CrystalCasinoGuest guest : guests) {
            if (guest.active) tooltip.addPara(guest.person.getNameString()
                    + (guest.travelling ? " (arriving in ~" + (int) Math.ceil(guest.travelDaysRemaining) + " days)" : ""), 3f);
        }
    }
}
