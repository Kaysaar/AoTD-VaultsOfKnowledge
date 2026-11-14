package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.CrystalCasinoGuest;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.CrystalCasinoGuestManager;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl.CrystalCasinoImpl;

import java.util.List;
import java.util.Map;

/** Commands used by rules.csv; acceptance is deferred until all invitation rules run. */
public class AoTDCrystalCasinoContacts extends BaseCommandPlugin {
    public static final String RESULT = "$aotdCasinoInviteResult";
    public static final String REQUEST = "$aotdCasinoInvitePerson";
    public static final String ACCEPT = "$aotdCasinoInviteAccepted";
    public static final String DENY = "$aotdCasinoInviteDenied";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (!Global.getSettings().getModManager().isModEnabled("aotd_theory_of_toolbox")) return false;
        if (dialog == null || dialog.getInteractionTarget() == null || params.isEmpty()) return false;
        PersonAPI person = dialog.getInteractionTarget().getActivePerson();
        if (person == null) return false;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) return false;
        CrystalCasinoGuestManager manager = CrystalCasinoGuestManager.getInstance();
        String action = params.get(0).getString(memoryMap);
        CrystalCasinoGuest guest = manager.getGuest(person);
        switch (action) {
            case "canOffer":
                return manager.isContactEligible(person) && manager.findCasino() != null;
            case "isResident":
                return guest != null && guest.active;
            case "describe": {
                CrystalCasinoImpl casino = manager.findCasino();
                dialog.getTextPanel().addPara("The Crystal Casino" + (casino == null ? "" : " at " + casino.getTargetMarket().getName())
                        + " has " + manager.getGuestCount() + " / " + CrystalCasinoGuestManager.CAPACITY + " occupied penthouses.");
                dialog.getTextPanel().addPara("You will pay 10,000 credits per month for this contact. Accepting grants +30 personal reputation. "
                        + "Dismissing the guest or losing the Casino costs 50 personal reputation. The contact will move to the host planet's comm directory.");
                return true;
            }
            case "prepare":
                local.set(REQUEST, person);
                local.set(ACCEPT, false);
                local.set(DENY, false);
                local.unset(RESULT);
                return true;
            case "accept":
                if (local.get(REQUEST) != person) return false;
                local.set(ACCEPT, true);
                return true;
            case "decline":
                if (local.get(REQUEST) != person) return false;
                local.set(DENY, true);
                return true;
            case "blacklist":
                person.getMemoryWithoutUpdate().set(CrystalCasinoGuestManager.BLACKLIST_KEY, true);
                if (local.get(REQUEST) == person) local.set(DENY, true);
                return true;
            case "unblacklist":
                person.getMemoryWithoutUpdate().unset(CrystalCasinoGuestManager.BLACKLIST_KEY);
                return true;
            case "resolve": {
                if (local.get(REQUEST) != person) return false;
                String result;
                if (manager.isLuddic(person)) result = "luddic";
                else if (person.getMemoryWithoutUpdate().getBoolean(CrystalCasinoGuestManager.BLACKLIST_KEY)) result = "blacklisted";
                else if (local.getBoolean(DENY) || !local.getBoolean(ACCEPT)) result = "declined";
                else result = manager.invite(person);
                local.set(RESULT, result);
                local.unset(REQUEST);
                local.unset(ACCEPT);
                local.unset(DENY);
                return true;
            }
            case "describeDismiss":
                if (guest == null || !guest.active) return false;
                dialog.getTextPanel().addPara("End this contact's penthouse stay? This costs 50 personal reputation. "
                        + "They will return to " + guest.origin.getName() + " if it is still inhabited; otherwise a suitable alternative colony. "
                        + "Penthouse charges stop, but already accrued expenses remain due.");
                return true;
            case "dismiss":
                if (!manager.dismiss(person)) {
                    dialog.getTextPanel().addPara("This contact is no longer a Casino resident.");
                    return false;
                }
                dialog.getTextPanel().addPara("The penthouse stay has ended. Personal reputation decreased by 50. "
                        + "Return arrangements have been made; if no destination is currently available, they remain pending without further charges.");
                return true;
            default:
                return false;
        }
    }
}
