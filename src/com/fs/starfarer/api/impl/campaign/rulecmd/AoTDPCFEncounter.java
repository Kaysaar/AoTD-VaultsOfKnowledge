package com.fs.starfarer.api.impl.campaign.rulecmd;


import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.plugins.AoTDSettingsManager;
import data.kaysaar.aotd.vok.plugins.ReflectionUtilis;
import data.kaysaar.aotd.vok.scripts.research.ResearchFleetRouteManager;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class AoTDPCFEncounter extends BaseCommandPlugin {

    public static final String DERELICT_SITE = "$aotd_pcf_derelicts";
    public static int waiting = 10;
    FleetInteractionDialogPluginImpl.FIDConfig config;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        String arg = params.get(0).getString(memoryMap);
        if (arg.equals("bribery")) {
            persuade(dialog, memoryMap);
            return true;
        }
        if (arg.equals("init")) {
            init(dialog, memoryMap);
            return true;
        }
        if (arg.equals("derelictEngage")) {
            return initDerelictBattle(dialog, memoryMap);
        }
        return false;
    }

    public void init(InteractionDialogAPI dialogAPI, Map<String, MemoryAPI> memoryMap) {
        dialogAPI.getOptionPanel().clearOptions();
        SectorEntityToken entity = dialogAPI.getInteractionTarget();
        MemoryAPI memory = entity.getMemoryWithoutUpdate();
        if (memory.getBoolean("$aotd_r_expedition_present")) {
            CampaignFleetAPI expedition = getNearby(entity);
            if (expedition != null) {
                dialogAPI.getTextPanel().addPara("We have been intercepted by a fleet that is orbiting this planet.");
                initBattle(dialogAPI, memoryMap, expedition, false);
                return;
            }
        }
        if (memory.getBoolean("$aotd_defeated_pcf")) {
            showSalvageOptions(dialogAPI);
            return;
        }
        if (entity instanceof PlanetAPI && entity.getMarket() != null) {
            MemoryAPI market = entity.getMarket().getMemoryWithoutUpdate();
            if (market.getBoolean(TechHunters.SITE)) {
                FireBest.fire(null, dialogAPI, memoryMap, "AoTDTechHuntersOpening");
                return;
            }
            if (market.getBoolean(DERELICT_SITE)) {
                String page = memory.contains("$aotd_precollapse_fleet") ? "AoTDDerelictRetry" : "AoTDDerelictOpening";
                FireBest.fire(null, dialogAPI, memoryMap, page);
                return;
            }
        }
        initBattle(dialogAPI, memoryMap, getFacilityDefenders(entity), false);
    }

    public static void showSalvageOptions(InteractionDialogAPI dialog) {
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        options.addOption("Send a salvage team down to the facility", "explore_PreFac");
        options.addOption("Leave", "defaultLeave");
        options.setShortcut("defaultLeave", Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private boolean initDerelictBattle(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        if (!(dialog.getInteractionTarget() instanceof PlanetAPI planet)
                || !planet.getMarket().getMemoryWithoutUpdate().getBoolean(DERELICT_SITE)) return false;

        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        CampaignFleetAPI fleet = memory.getFleet("$aotd_precollapse_fleet");
        if (fleet == null) {
            FleetParamsV3 params = new FleetParamsV3(planet.getLocationInHyperspace(), Factions.DERELICT, 1f,
                    FleetTypes.PATROL_LARGE, rollFacilityDefencePoints(), 0f, 0f, 0f, 0f, 0f, 0f);
            params.random = Misc.getRandom(Misc.getSalvageSeed(planet), 31);
            fleet = FleetFactoryV3.createFleet(params);
            fleet.setName("Domain Defense Drones");
            fleet.setNoFactionInName(true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
            memory.set("$aotd_precollapse_fleet", fleet);
        }
        dialog.getOptionPanel().clearOptions();
        return initBattle(dialog, memoryMap, fleet, false);
    }

    private CampaignFleetAPI getFacilityDefenders(SectorEntityToken entity) {
        if (!(entity instanceof PlanetAPI)) return null;
        MemoryAPI memory = entity.getMemoryWithoutUpdate();
        CampaignFleetAPI fleet = memory.getFleet("$aotd_precollapse_fleet");
        if (fleet != null) return fleet;

        FleetParamsV3 params = new FleetParamsV3(entity.getLocationInHyperspace(), Factions.REMNANTS, 1f,
                FleetTypes.PATROL_LARGE, rollFacilityDefencePoints(), 0f, 0f, 0f, 0f, 0f, 0f);
        fleet = FleetFactoryV3.createFleet(params);
        fleet.setName("Automatic Defence Fleet");
        fleet.setNoFactionInName(true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, "aotd_expedition");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        memory.set("$aotd_precollapse_fleet", fleet);
        return fleet;
    }

    private int rollFacilityDefencePoints() {
        int min = AoTDSettingsManager.getIntValue(AoTDSettingsManager.AOTD_DEFENCE_MIN_VAL);
        int max = AoTDSettingsManager.getIntValue(AoTDSettingsManager.AOTD_DEFENCE_MAX_VAL);
        return MathUtils.getRandomNumberInRange(min, max);
    }

    public boolean initCybermazeBattle(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CampaignFleetAPI defenders = getFacilityDefenders(dialog.getInteractionTarget());
        if (defenders == null) return false;
        dialog.getOptionPanel().clearOptions();
        return initBattle(dialog, memoryMap, defenders, true);
    }


    public boolean bribery(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(500000);
        config.delegate.notifyLeave(dialog);
        return true;
    }

    public boolean persuade(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {

        if ((dialog.getInteractionTarget() instanceof PlanetAPI)) {
            FleetInteractionDialogPluginImpl conf = (FleetInteractionDialogPluginImpl) dialog.getPlugin();
            dialog.getInteractionTarget().getMemory().unset("$aotd_r_expedition_present");
            FleetInteractionDialogPluginImpl.FIDConfig config = (FleetInteractionDialogPluginImpl.FIDConfig)  ReflectionUtilis.getPrivateVariable("config", conf);
            conf.cleanUpBattle();
            config.delegate.notifyLeave(dialog);
            CampaignFleetAPI campaignFleetAPI = getNearby(dialog.getInteractionTarget());
            assert campaignFleetAPI != null;
            campaignFleetAPI.clearAssignments();
            campaignFleetAPI.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, Misc.getFactionMarkets(campaignFleetAPI.getFaction()).get(0).getPrimaryEntity(), 10000, "Returning to homeworld");
            dialog.getOptionPanel().clearOptions();
            dialog.getVisualPanel().finishFadeFast();
            dialog.getVisualPanel().showPlanetInfo(dialog.getInteractionTarget());
            dialog.getOptionPanel().addOption("Send a salvage team down to the facility.", "explore_PreFac");
            dialog.getOptionPanel().addOption("Leave", "defaultLeave");
        } else {
            FleetInteractionDialogPluginImpl conf = (FleetInteractionDialogPluginImpl) dialog.getPlugin();
            conf.cleanUpBattle();

            CampaignFleetAPI campaignFleetAPI = (CampaignFleetAPI) dialog.getInteractionTarget();
            for (EveryFrameScript script : campaignFleetAPI.getScripts()) {
                if(script instanceof ResearchFleetRouteManager){
                    ((ResearchFleetRouteManager) script).target.getMemory().unset("$aotd_r_expedition_present");
                }
            }
            campaignFleetAPI.clearAssignments();
            campaignFleetAPI.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, Misc.getFactionMarkets(campaignFleetAPI.getFaction()).get(0).getPrimaryEntity(), 10000, "Returning to homeworld");
            dialog.dismiss();

        }


        return true;
    }


    public boolean initBattle(InteractionDialogAPI dialog, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        final SectorEntityToken entity = dialog.getInteractionTarget();
        CampaignFleetAPI dummy = getNearby(entity);
        if (dummy == null) {
            dummy = dialog.getInteractionTarget().getMemory().getFleet("$aotd_precollapse_fleet");

        }
        return initBattle(dialog, memoryMap, dummy, false);
    }

    private boolean initBattle(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, CampaignFleetAPI defenders,
                               boolean cybermazeDefense) {
        if (defenders == null) return false;
        SectorEntityToken entity = dialog.getInteractionTarget();
        if(defenders.getFaction().getId().equals(Factions.REMNANTS)){
            defenders.getMemoryWithoutUpdate().set("$knowsWhoPlayerIs",false);

        }
        boolean derelictDefense = Factions.DERELICT.equals(defenders.getFaction().getId());
        if (defenders.isEmpty() || (!cybermazeDefense && entity.getMemory().is("$aotd_defeated_pcf", true))) {
            defenders.deflate();
            if (cybermazeDefense) {
                AoTDFacilityCybermaze.reportDefendersDefeated(dialog, memoryMap);
                return true;
            }
            if (derelictDefense) entity.getMemory().set("$aotd_defeated_pcf", true);
            showSalvageOptions(dialog);
            return true;
        }
        dialog.setInteractionTarget(defenders);
        defenders.getMemoryWithoutUpdate().set("$LP_titheAskedFor", true);

        config = new FleetInteractionDialogPluginImpl.FIDConfig();
        boolean firstCybermazeBattle = cybermazeDefense && !entity.getMarket().getMemoryWithoutUpdate().getBoolean(AoTDFacilityCybermaze.BATTLE_STARTED);
        config.noLeaveOptionOnFirstEngagement = firstCybermazeBattle;
        config.leaveAlwaysAvailable = !firstCybermazeBattle;
        config.showCommLinkOption = !cybermazeDefense && !derelictDefense;
        config.alwaysAttackVsAttack = derelictDefense;
        //config.showEngageText = false;
        if (defenders.getFaction().equals(Factions.INDEPENDENT)) {
            config.showCommLinkOption = true;
        }
        config.showFleetAttitude = false;
        //config.alwaysAttackVsAttack = true;
        config.pullInStations = false;

        config.impactsAllyReputation = false;
        config.showWarningDialogWhenNotHostile = false;
        config.impactsEnemyReputation = false;
        config.pullInEnemies = false;
        //config.pullInAllies = false;
        config.noSalvageLeaveOptionText = Misc.ucFirst("Continue");
        config.dismissOnLeave = false;
        config.printXPToDialog = true;

        config.salvageRandom = new Random();
        if(defenders.getFaction().getId().equals(Factions.REMNANTS)){
            defenders.getMemoryWithoutUpdate().set("$knowsWhoPlayerIs",false);

        }
        final AoTDNonSalvageInteract plugin = new AoTDNonSalvageInteract(config);
        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
        config.delegate = new AoTDNonSalvageInteract.BaseFIDDelegate() {

            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);//Global.getSector().getCampaignUI().clearMessages();

                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                    if (context.didPlayerWinMostRecentBattleOfEncounter()) {
                        if (!cybermazeDefense) entity.getMemory().unset("$aotd_r_expedition_present");

                        SalvageGenFromSeed.SDMParams p = new SalvageGenFromSeed.SDMParams();
                        p.entity = entity;
                        p.factionId = defenders.getFaction().getId();

                        SalvageGenFromSeed.SalvageDefenderModificationPlugin plugin = Global.getSector().getGenericPlugins().pickPlugin(
                                SalvageGenFromSeed.SalvageDefenderModificationPlugin.class, p);
                        if (plugin != null && !derelictDefense) {
                            plugin.reportDefeated(p, entity, defenders);
                        }
                        entity.removeScriptsOfClass(FleetAdvanceScript.class);
                        dialog.getOptionPanel().clearOptions();
                        dialog.getVisualPanel().finishFadeFast();
                        dialog.getVisualPanel().showPlanetInfo(dialog.getInteractionTarget());
                        defenders.deflate();
                        if (cybermazeDefense) {
                            AoTDFacilityCybermaze.reportDefendersDefeated(dialog, memoryMap);
                        } else {
                            entity.getMemory().set("$aotd_defeated_pcf",true);
                            if (derelictDefense) {
                                dialog.getTextPanel().addPara("The last drone goes dark. Your sensors officer confirms that the starport is clear and the salvage shuttles can land.");
                            }
                            showSalvageOptions(dialog);
                        }

                    } else {
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }

            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                if (firstCybermazeBattle) entity.getMarket().getMemoryWithoutUpdate().set(AoTDFacilityCybermaze.BATTLE_STARTED, true);
            }
        };

        dialog.setPlugin(plugin);
        plugin.init(dialog);


        return true;
    }

    public static CampaignFleetAPI getNearby(SectorEntityToken target) {

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        for (CampaignFleetAPI fleet : target.getContainingLocation().getFleets()) {
            if ((fleet.hasTag("aotd_expedition"))) {
                //Global.getLogger(this.getClass()).info("Checking vulture " + fleet.getNameWithFaction());
                if (fleet.getBattle() != null) {
                    continue;
                }

                if (!player.isVisibleToSensorsOf(fleet)) {
                    //Global.getLogger(this.getClass()).info("Cannot see");
                    continue;
                }

                // check join range
                if (MathUtils.getDistance(target, fleet) > Misc.getBattleJoinRange()) {
                    //Global.getLogger(this.getClass()).info("Out of range");
                    continue;
                }

                // don't pick a fight with a much stronger player
                CampaignFleetAIAPI ai = (CampaignFleetAIAPI) fleet.getAI();
                if (ai == null) continue;
                CampaignFleetAIAPI.EncounterOption option = ai.pickEncounterOption(null, player, true);
                if (option != CampaignFleetAIAPI.EncounterOption.ENGAGE)
                    continue;

                return fleet;
            }
        }
        return null;
    }

}
