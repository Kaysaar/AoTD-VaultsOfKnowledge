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

    public int getGuestCount() {
        int count = 0;
        for (CrystalCasinoGuest guest : guests) if (guest.active) count++;
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


    public String invite(PersonAPI person) {
        if (isLuddic(person)) return "luddic";
        if (person == null) return "invalid";
        if (person.getMemoryWithoutUpdate().getBoolean(BLACKLIST_KEY)) return "blacklisted";
        if (!isContactEligible(person)) return "invalid";
        if (getGuestCount() >= CAPACITY) return "full";
        CrystalCasinoImpl casino = findCasino();
        if (casino == null) return "unavailable";
        CrystalCasinoGuest guest = new CrystalCasinoGuest(person, casino.getTargetMarket(), casino.getCurrentlyTiedEntity());
        move(person, guest.host);
        guests.add(guest);
        person.getRelToPlayer().adjustRelationship(INVITE_REPUTATION, null);
        return "accepted";
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
        // A decivilized/destroyed home cannot provide a usable directory. Preserve the
        // person at another same-faction market, then a player colony as a fallback.
        MarketAPI fallback = null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == guest.host || !market.isInEconomy()) continue;
            if (market.getFactionId().equals(guest.person.getFaction().getId())) return market;
            if (fallback == null && market.isPlayerOwned()) fallback = market;
        }
        return fallback;
    }

    protected void returnGuest(CrystalCasinoGuest guest) {
        MarketAPI destination = returnDestination(guest);
        if (destination == null) return; // Retain saved person/origin and retry daily, without charging rent.
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

    protected void validateGuests() {
        for (CrystalCasinoGuest guest : new ArrayList<>(guests)) {
            if (!guest.active) {
                returnGuest(guest);
                continue;
            }
            // Disruption and shortages do not evict guests. Destruction, detachment,
            // decivilization, or loss of ownership ends the player's penthouse lease.
            if (guest.casino == null || guest.casino.isExpired() || !guest.host.isInEconomy()
                    || !guest.host.isPlayerOwned() || !guest.host.getConnectedEntities().contains(guest.casino)
                    || !guest.host.hasIndustry(CrystalCasinoImpl.PLANETARY_INDUSTRY_ID)) {
                endStay(guest);
            } else if (guest.person.getMarket() != guest.host) {
                // A story/mod deliberately relocated this person: don't drag them back.
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
        int count = getGuestCount();
        if (count == 0) return;
        MonthlyReport report = SharedData.getData().getCurrentReport();
        FDNode node = report.getNode(report.getNode(MonthlyReport.FLEET), SAVE_KEY);
        node.name = "Crystal Casino penthouses";
        node.icon = Global.getSettings().getSpriteName("income_report", "generic_expense");
        node.tooltipCreator = this;
        node.upkeep += count * MONTHLY_COST / Math.max(1f, Global.getSettings().getFloat("economyIterPerMonth"));
    }
    @Override public void reportEconomyMonthEnd() { }
    @Override public float getTooltipWidth(Object tooltipParam) { return 420f; }
    @Override public boolean isTooltipExpandable(Object tooltipParam) { return false; }
    @Override public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addPara("Penthouse residents: %s / %s. Each resident costs %s per month.", 0f,
                Misc.getHighlightColor(), "" + getGuestCount(), "" + CAPACITY, Misc.getDGSCredits(MONTHLY_COST));
        tooltip.addPara("Costs accrue each economy tick. Expenses already incurred remain due after a guest leaves.", 5f);
        for (CrystalCasinoGuest guest : guests) if (guest.active) tooltip.addPara(guest.person.getNameString(), 3f);
    }
}
