package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.CrystalCasinoGuest;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.CrystalCasinoGuestManager;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl.CrystalCasinoImpl;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class AoTDCrystalCasinoContacts extends BaseCommandPlugin {
    public static final String RESULT = "$aotdCasinoInviteResult";
    public static final String REQUEST = "$aotdCasinoInvitePerson";
    public static final String ACCEPT = "$aotdCasinoInviteAccepted";
    public static final String DENY = "$aotdCasinoInviteDenied";
    public static final String REP_DELTA = "$aotdCasinoInviteRepDelta";
    public static final String REP_PERSON = "$aotdCasinoInviteRepPerson";

    public static final String INVITE_OPTION = "aotdCasinoInviteConfirm";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params == null || params.isEmpty() || memoryMap == null) return false;

        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) return false;
        String action = params.get(0).getString(memoryMap);

        // The invited contact can be removed from the comm directory as soon as
        // the invitation succeeds. Use the captured reference when rendering
        // the reputation message, rather than requiring getActivePerson().
        if ("printEffects".equals(action)) {
            if (!"accepted".equals(local.getString(RESULT))) return false;
            Object storedDelta = local.get(REP_DELTA);
            Object storedPerson = local.get(REP_PERSON);
            if (!(storedDelta instanceof Number) || !(storedPerson instanceof PersonAPI)) return false;

            PersonAPI invited = (PersonAPI) storedPerson;
            CrystalCasinoGuest invitedGuest = CrystalCasinoGuestManager.getInstance().getGuest(invited);
            if (invitedGuest != null && invitedGuest.active && invitedGuest.travelling) {
                dialog.getTextPanel().addPara("Passage has been arranged to " + invitedGuest.host.getName()
                        + ". The contact is expected to arrive in approximately "
                        + (int) Math.ceil(invitedGuest.travelDaysRemaining)
                        + " days. Penthouse upkeep begins on arrival.");
            }
            showReputationChange(dialog, invited, ((Number) storedDelta).floatValue());
            local.unset(REP_DELTA);
            local.unset(REP_PERSON);
            return true;
        }

        if (dialog.getInteractionTarget() == null) return false;
        final PersonAPI person = dialog.getInteractionTarget().getActivePerson();
        if (person == null) return false;

        CrystalCasinoGuestManager manager = CrystalCasinoGuestManager.getInstance();
        CrystalCasinoGuest guest = manager.getGuest(person);

        switch (action) {
            case "canOffer":
                return manager.isContactEligible(person) && manager.findCasino() != null;

            case "isResident":
                return guest != null && guest.active && !guest.travelling;

            case "personIs":
                return params.size() > 1 && person.getId().equals(params.get(1).getString(memoryMap));

            case "factionIs":
                return params.size() > 1 && person.getFaction() != null
                        && person.getFaction().getId().equals(params.get(1).getString(memoryMap));

            case "addInviteTooltip": {
                // PopulateOptions owns creating/reordering the option from rules.csv.
                // Adding it directly here can be overwritten by the rule engine.
                final OptionPanelAPI options = dialog.getOptionPanel();
                if (options == null || !options.hasOption(INVITE_OPTION)) return true;

                options.addOptionTooltipAppender(INVITE_OPTION, new OptionPanelAPI.OptionTooltipCreator() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean hadOtherText) {
                        CrystalCasinoGuestManager current = CrystalCasinoGuestManager.getInstance();
                        CrystalCasinoImpl casino = current.findCasino();
                        int occupied = current.getGuestCount();
                        int free = Math.max(0, CrystalCasinoGuestManager.CAPACITY - occupied);
                        float pad = hadOtherText ? 10f : 0f;

                        tooltip.addPara("Crystal Casino penthouse invitation", pad);
                        if (casino != null && casino.getTargetMarket() != null) {
                            String destination = casino.getTargetMarket().getName();
                            String travelDays = String.valueOf(current.getTravelDays(person.getMarket(), casino.getTargetMarket()));
                            tooltip.addPara("This contact will travel to %s and move into the Casino after approximately %s days.",
                                    5f, Misc.getHighlightColor(), destination, travelDays);
                        }
                        tooltip.addPara("A penthouse costs %s credits per month, beginning on arrival.",
                                5f, java.awt.Color.ORANGE, Misc.getWithDGS(CrystalCasinoGuestManager.MONTHLY_COST));
                        tooltip.addPara("Available penthouses: %s / %s", 5f,
                                free > 0 ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor(),
                                String.valueOf(free), String.valueOf(CrystalCasinoGuestManager.CAPACITY));
                        tooltip.addPara("Inviting the contact grants %s personal reputation; dismissal or losing the Casino costs %s.",
                                5f, new Color[]{Misc.getHighlightColor(),Misc.getNegativeHighlightColor()}, "+30", "-50");
                        if (free <= 0) tooltip.addPara("No penthouses are currently available.", 5f);
                    }
                });
                if (manager.getGuestCount() >= CrystalCasinoGuestManager.CAPACITY) {
                    options.setEnabled(INVITE_OPTION, false);
                }
                return true;
            }

            case "describe": {
                CrystalCasinoImpl casino = manager.findCasino();
                String location = casino == null || casino.getTargetMarket() == null
                        ? "" : " at " + casino.getTargetMarket().getName();
                dialog.getTextPanel().addPara("The Crystal Casino" + location + " has "
                        + manager.getGuestCount() + " / " + CrystalCasinoGuestManager.CAPACITY
                        + " occupied penthouses.");
                dialog.getTextPanel().addPara("Your offer reserves one penthouse. The contact remains at their present "
                        + "location until their transport arrives, with travel time based on distance. "
                        + "The 10,000-credit monthly fee begins on arrival. Inviting a contact grants "
                        + "+30 personal reputation; dismissing them or losing the Casino costs 50.");
                return true;
            }

            case "prepare":
                local.set(REQUEST, person);
                local.set(ACCEPT, false);
                local.set(DENY, false);
                local.unset(RESULT);
                local.unset(REP_DELTA);
                local.set(REP_PERSON, person);
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
                float before = person.getRelToPlayer().getRel();
                if (manager.isLuddic(person)) result = "luddic";
                else if (person.getMemoryWithoutUpdate().getBoolean(CrystalCasinoGuestManager.BLACKLIST_KEY)) result = "blacklisted";
                else if (local.getBoolean(DENY) || !local.getBoolean(ACCEPT)) result = "declined";
                else result = manager.invite(person);

                local.set(RESULT, result);
                if ("accepted".equals(result)) {
                    // Actual difference, not the nominal +30: reputation can
                    // be capped, and modded adjustments can vary.
                    local.set(REP_DELTA, person.getRelToPlayer().getRel() - before);
                    local.set(REP_PERSON, person);
                } else {
                    local.unset(REP_DELTA);
                    local.unset(REP_PERSON);
                }
                local.unset(REQUEST);
                local.unset(ACCEPT);
                local.unset(DENY);
                return true;
            }

            case "describeDismiss":
                if (guest == null || !guest.active) return false;
                String origin = guest.origin == null ? "their previous location" : guest.origin.getName();
                dialog.getTextPanel().addPara("End this contact's penthouse stay? This costs 50 personal reputation. "
                        + "They will return to " + origin + " if it is still inhabited; otherwise, "
                        + "a suitable alternative colony. Penthouse charges stop, but already accrued "
                        + "expenses remain due.");
                return true;

            case "dismiss": {
                float before = person.getRelToPlayer().getRel();
                if (!manager.dismiss(person)) {
                    dialog.getTextPanel().addPara("This contact is no longer a Casino resident.");
                    return false;
                }
                dialog.getTextPanel().addPara("The penthouse stay has ended. "
                        + "Return arrangements have been made; if no destination is currently available, "
                        + "they remain pending without further charges.");
                // The manager has already applied the penalty. Display the
                // difference without applying a second reputation change.
                showReputationChange(dialog, person, person.getRelToPlayer().getRel() - before);
                return true;
            }

            default:
                return false;
        }
    }

    /** Uses the vanilla Starsector reputation display, including final standing. */
    private static void showReputationChange(InteractionDialogAPI dialog, PersonAPI person, float delta) {
        CoreReputationPlugin.addDeltaMessage(delta, person.getFaction(), person,
                dialog.getTextPanel(), null, null, true, 0f);
        // The manager adjusts RelationshipAPI directly, so CoreScript's
        // reputation listener is not notified and won't play this sound for us.
        if (delta > 0.0049f) {
            Global.getSoundPlayer().playUISound("ui_rep_raise", 1f, 1f);
        } else if (delta < -0.0049f) {
            Global.getSoundPlayer().playUISound("ui_rep_drop", 1f, 1f);
        }
    }
}
