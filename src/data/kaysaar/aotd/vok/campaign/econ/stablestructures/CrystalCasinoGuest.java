package data.kaysaar.aotd.vok.campaign.econ.stablestructures;

import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;

public class CrystalCasinoGuest {
    public final PersonAPI person;
    public final MarketAPI origin;
    public final MarketAPI host;
    public final SectorEntityToken casino;
    public final boolean originallyInPeople;
    public final boolean originallyInDirectory;
    public final boolean originallyHidden;
    public final int originalDirectoryIndex;
    public boolean active = true;


    public boolean travelling;
    public float travelDaysRemaining;

    public CrystalCasinoGuest(PersonAPI person, MarketAPI host, SectorEntityToken casino) {
        this.person = person;
        this.origin = person.getMarket();
        this.host = host;
        this.casino = casino;
        originallyInPeople = origin.getPeopleCopy().contains(person);
        CommDirectoryEntryAPI entry = origin.getCommDirectory().getEntryForPerson(person);
        originallyInDirectory = entry != null;
        originallyHidden = entry != null && entry.isHidden();
        originalDirectoryIndex = entry == null ? -1 : origin.getCommDirectory().getEntriesCopy().indexOf(entry);
    }
}
