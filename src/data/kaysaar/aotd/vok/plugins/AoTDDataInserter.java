package data.kaysaar.aotd.vok.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDFacilityCybermaze;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDMegastructureRules;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDMinedFacility;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDPCFEncounter;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDReactorPlatform;
import com.fs.starfarer.api.impl.campaign.rulecmd.AoTDVoidSignal;
import com.fs.starfarer.api.impl.campaign.rulecmd.TechHunters;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.Ids.AoTDConditions;
import data.kaysaar.aotd.vok.Ids.AoTDIndustries;

import data.kaysaar.aotd.vok.campaign.econ.megastructures.models.BaseMegastructureScript;
import data.kaysaar.aotd.vok.misc.AoTDMisc;
import data.kaysaar.aotd.vok.scripts.pcf.unique.MeetTheSpy;
import data.kaysaar.aotd.vok.scripts.research.AoTDFactionResearchManager;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;

import java.util.*;

import static data.kaysaar.aotd.vok.Ids.AoTDMemFlags.preCollapseFacList;

public class AoTDDataInserter {
    public static String sophia = "aotd_sophia";
    private int maxTriTachyonElectronics = 2;
    public static String AOTD = "aotd_";

    public void generatePreCollapseFacilities() {
        int target = AoTDSettingsManager.getIntValue(AoTDSettingsManager.AOTD_PCF_AMOUNT);
        List<StarSystemAPI> starSystems = Global.getSector().getStarSystems();
        List<PlanetAPI> planetsWithFac = new ArrayList<>();
        Collections.shuffle(starSystems);
        for (StarSystemAPI starSystem : starSystems) {
            if (planetsWithFac.size() >= target) break;
            if (starSystem.getTags().contains(Tags.THEME_RUINS_MAIN) || starSystem.getTags().contains(Tags.THEME_REMNANT) || starSystem.getTags().contains(Tags.THEME_DERELICT) || starSystem.getTags().contains("")) {
                for (PlanetAPI planet : starSystem.getPlanets()) {
                    if (planet.isStar()) continue;
                    if (!planet.getMarket().isPlanetConditionMarketOnly()) continue;
                    if (planet.hasTag(Tags.NOT_RANDOM_MISSION_TARGET)) continue;
                    if (planet.hasTag(Tags.MISSION_ITEM)) continue;
                    if (planet.isGasGiant()) continue;
                    addPreCollapseFacility(planet);
                    planetsWithFac.add(planet);
                    break;
                }

            }
        }
        if (target > 0) reserveReactorFacility(planetsWithFac, starSystems, target);
        Global.getSector().getPersistentData().put(preCollapseFacList, planetsWithFac);
    }

    private void addPreCollapseFacility(PlanetAPI planet) {
        MarketAPI market = planet.getMarket();
        String token = market.addCondition("pre_collapse_facility");
        market.getSpecificCondition(token).setSurveyed(false);
        planet.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
    }

    private void reserveReactorFacility(List<PlanetAPI> facilities, List<StarSystemAPI> systems, int target) {
        PlanetAPI reactor = facilities.stream().filter(planet -> Planets.PLANET_WATER.equals(planet.getTypeId()))
                .findFirst().orElse(null);
        if (reactor == null) {
            Set<StarSystemAPI> occupied = new HashSet<>();
            for (PlanetAPI facility : facilities) occupied.add(facility.getStarSystem());
            List<StarSystemAPI> preferred = systems.stream().filter(system -> !occupied.contains(system)).toList();
            reactor = spawnFallbackPlanet(preferred, "Pelagos", Planets.PLANET_WATER);
            if (reactor == null) return;

            int replacement = facilities.size() >= target ? facilities.size() - 1 : -1;
            for (int i = 0; i < facilities.size(); i++) {
                if (facilities.get(i).getStarSystem() == reactor.getStarSystem()) {
                    replacement = i;
                    break;
                }
            }
            if (replacement >= 0) {
                PlanetAPI replaced = facilities.remove(replacement);
                replaced.getMarket().removeCondition("pre_collapse_facility");
                replaced.removeTag(Tags.NOT_RANDOM_MISSION_TARGET);
            }
            if (!reactor.hasCondition(Conditions.WATER_SURFACE)) reactor.getMarket().addCondition(Conditions.WATER_SURFACE);
            addPreCollapseFacility(reactor);
            facilities.add(reactor);
        }
        reactor.getMarket().getMemoryWithoutUpdate().set(AoTDReactorPlatform.SITE, true);
        Global.getLogger(AoTDDataInserter.class).info("Unique PCF [Reactor]: " + reactor.getName() + " [" + reactor.getId()
                + "] in " + reactor.getStarSystem().getName() + " [" + reactor.getStarSystem().getId() + "]");
    }

    public void assignGenericFacilities() {
        MemoryAPI sector = Global.getSector().getMemoryWithoutUpdate();
        String assigned = "$aotd_pcf_generic_encounters_assigned";
        if (sector.getBoolean(assigned)) return;
        if (!(Global.getSector().getPersistentData().get(preCollapseFacList) instanceof List<?> facilities)) return;

        List<String> encounters = List.of(AoTDFacilityCybermaze.SITE, TechHunters.SITE,
                AoTDPCFEncounter.DERELICT_SITE, AoTDMinedFacility.SITE, AoTDVoidSignal.SITE);
        List<PlanetAPI> candidates = new ArrayList<>();
        int totalFacilities = 0, assignedEncounters = 0;
        for (Object candidate : facilities) {
            if (!(candidate instanceof PlanetAPI planet) || planet.getStarSystem() == null || planet.getMarket() == null
                    || !planet.getMarket().isPlanetConditionMarketOnly() || !planet.hasCondition("pre_collapse_facility")) continue;
            totalFacilities++;
            MemoryAPI market = planet.getMarket().getMemoryWithoutUpdate();
            if (market.getBoolean(MeetTheSpy.SITE) || market.getBoolean(AoTDReactorPlatform.SITE)
                    || encounters.stream().anyMatch(market::getBoolean)) {
                assignedEncounters++;
                continue;
            }
            MemoryAPI entity = planet.getMemoryWithoutUpdate();
            if (market.getBoolean("$aotd_fac_explored") || market.getBoolean("$aotd_failed_pre_collapse")
                    || entity.contains("$aotd_precollapse_fleet") || entity.getBoolean("$aotd_r_expedition_present")
                    || entity.getBoolean("$aotd_defeated_pcf") || entity.contains("$defenderFleet")) continue;
            candidates.add(planet);
        }

        int standardCount = Math.round(totalFacilities * 0.4f);
        int genericCount = Math.min(candidates.size(), totalFacilities - standardCount - assignedEncounters);
        Collections.shuffle(candidates, Misc.random);
        for (int i = 0; i < genericCount; i++) {
            PlanetAPI planet = candidates.get(i);
            String encounter = encounters.get(Misc.random.nextInt(encounters.size()));
            planet.getMarket().getMemoryWithoutUpdate().set(encounter, true);
            String name = switch (encounter) {
                case AoTDFacilityCybermaze.SITE -> "Cybermaze";
                case AoTDVoidSignal.SITE -> "Signal Triangulation";
                default -> null;
            };
            if (name != null) {
                Global.getLogger(AoTDDataInserter.class).info("Generic PCF [" + name + "]: " + planet.getName() + " [" + planet.getId()
                        + "] in " + planet.getStarSystem().getName() + " [" + planet.getStarSystem().getId() + "]");
            }
        }
        sector.set(assigned, true);
    }

    public void insertSophia() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        PersonAPI sophiaAshley = Global.getFactory().createPerson();
        sophiaAshley.setId(sophia);
        sophiaAshley.setFaction(Factions.INDEPENDENT);
        sophiaAshley.setGender(FullName.Gender.FEMALE);
        sophiaAshley.setRankId(Ranks.POST_SCIENTIST);
        sophiaAshley.setPostId(Ranks.POST_SCIENTIST);
        sophiaAshley.setImportance(PersonImportance.HIGH);
        sophiaAshley.setVoice(Voices.SCIENTIST);
        sophiaAshley.getName().setFirst("Sophia");
        sophiaAshley.getName().setLast("Anderson");
        sophiaAshley.getTags().add("aotd_researcher");
        sophiaAshley.getTags().add("aotd_resourceful");
        sophiaAshley.setPortraitSprite(Global.getSettings().getSpriteName("characters", "sophia"));
        if (!ip.containsPerson(sophiaAshley)) {
            ip.addPerson(sophiaAshley);
        }
    }

    public void setVanilaIndustriesDowngrades() {
        for (IndustrySpecAPI allIndustrySpec : Global.getSettings().getAllIndustrySpecs()) {
            if (allIndustrySpec.getId().equals(Industries.FARMING)) {
                allIndustrySpec.setDowngrade(AoTDIndustries.MONOCULTURE);
                allIndustrySpec.addTag(AOTD + Industries.FARMING);
            }
            if (allIndustrySpec.getId().equals(Industries.AQUACULTURE)) {
                List<String> str = new ArrayList<>();
                for (String tag : allIndustrySpec.getTags()) {
                    if (tag.equals("farming")) continue;
                    str.add(tag);
                }
                allIndustrySpec.addTag("starter");
                allIndustrySpec.getTags().clear();
                str.add(AOTD + Industries.AQUACULTURE);
                for (String s : str) {
                    allIndustrySpec.addTag(s);
                }
            }
            if (allIndustrySpec.getId().equals(Industries.MINING)) {
                allIndustrySpec.setDowngrade(AoTDIndustries.EXTRACTIVE_OPERATION);
                allIndustrySpec.addTag(AOTD + Industries.MINING);
            }
            if (allIndustrySpec.getId().equals(Industries.REFINING)) {
                allIndustrySpec.setDowngrade(AoTDIndustries.SMELTING);
                allIndustrySpec.addTag(AOTD + Industries.REFINING);
            }
            if (allIndustrySpec.getId().equals(Industries.LIGHTINDUSTRY)) {
                allIndustrySpec.setDowngrade(AoTDIndustries.LIGHT_PRODUCTION);
                allIndustrySpec.addTag(AOTD + Industries.LIGHTINDUSTRY);
            }
            if (allIndustrySpec.getId().equals(Industries.COMMERCE)) {
                allIndustrySpec.setDowngrade(AoTDIndustries.TRADE_OUTPOST);
                allIndustrySpec.addTag(AOTD + AoTDIndustries.TRADE_OUTPOST);
            }
            if (allIndustrySpec.getId().equals(Industries.HEAVYINDUSTRY)) {
                allIndustrySpec.addTag(AOTD + Industries.HEAVYINDUSTRY);
                allIndustrySpec.setUpgrade(null);
            }
            if (allIndustrySpec.getId().equals(Industries.ORBITALWORKS)) {
                allIndustrySpec.addTag(AOTD + Industries.HEAVYINDUSTRY);
                allIndustrySpec.addTag("advanced_heavy_industry");

            }
            if (allIndustrySpec.getId().equals(Industries.SPACEPORT)) {
                allIndustrySpec.addTag(AOTD + Industries.SPACEPORT);
                allIndustrySpec.setUpgrade(null);
            }
            if (allIndustrySpec.getId().equals(Industries.WAYSTATION)) {
                allIndustrySpec.addTag("starter");
                allIndustrySpec.addTag(AOTD + Industries.WAYSTATION);
            }
        }
    }

    public void setStarterIndustriesUpgrades() {
        AoTDFactionResearchManager manager = AoTDMainResearchManager.getInstance().getManagerForPlayer();

    }

    public void spawnVeilPlanet() {
        if (!Global.getSector().getPersistentData().containsKey("$aotd_v_planet")) {
            List<StarSystemAPI> starSystems = Global.getSector().getStarSystems();
            Collections.shuffle(starSystems);

            for (StarSystemAPI starSystem : starSystems) {
                if (starSystem.getTags().contains(Tags.THEME_RUINS_MAIN)) {
                    for (PlanetAPI planet : starSystem.getPlanets()) {
                        if (planet.isStar()) continue;
                        if (planet.isMoon()) continue;
                        if (!planet.getMarket().isPlanetConditionMarketOnly()) continue;
                        if (planet.hasTag(Tags.NOT_RANDOM_MISSION_TARGET)) continue;
                        if (planet.hasTag(Tags.MISSION_ITEM)) continue;
                        if (planet.isStar()) continue;
                        if (planet.isGasGiant()) continue;

                        if (planet.getMemory().contains("$IndEvo_ArtilleryStation")) continue;
                        long seed = StarSystemGenerator.random.nextLong();
                        planet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, seed);
                        planet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, "aotd_beyond_veil");
                        planet.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
                        Global.getSector().getPersistentData().put("$aotd_v_planet", planet);
                        planet.getMemoryWithoutUpdate().set("$aotd_quest_veil", true);
                        planet.setName("Veil of Knowledge");
                        break;
                    }
                }
                if (Global.getSector().getPersistentData().containsKey("$aotd_v_planet")) {
                    break;
                }
            }
        }
    }

    public boolean spawnNidavleir(List<StarSystemAPI> systems) {
        PlanetAPI planet = getEntityWithCriteria(systems);
        if (planet == null) {
            planet = spawnFallbackPlanet(systems, "Nidavelir", Planets.BARREN);
        }
        if (planet == null) return false;
        planet.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        planet.getMemoryWithoutUpdate().set("$aotd_mega_already", true);
        String token = planet.getMarket().addCondition("aotd_nidavelir_complex");
        planet.getMarket().getSpecificCondition(token).setSurveyed(false);
        planet.getMarket().removeCondition(Conditions.RUINS_EXTENSIVE);
        planet.getMarket().removeCondition(Conditions.RUINS_SCATTERED);
        planet.getMarket().removeCondition(Conditions.RUINS_WIDESPREAD);
        planet.getMarket().addCondition("aotd_crashed_ring_segmnets");
        planet.getMarket().getPrimaryEntity().setInteractionImage("illustrations","nidavelir_destroyed");

        if (!planet.getMarket().hasCondition(Conditions.RUINS_VAST)) {
            planet.getMarket().addCondition(Conditions.RUINS_VAST);
        }
//        Global.getSector().getPlayerMemoryWithoutUpdate().set("$aotd_mega_system_id_"+mega.getSpec().getMegastructureID(),planet.getStarSystem().getId());
        return true;


    }

    private PlanetAPI getEntityWithCriteria(List<StarSystemAPI> availableSystems, String... criteria) {
        List<String> types = Arrays.asList(criteria);
        for (StarSystemAPI system : getMegastructureSystems(availableSystems)) {
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar() || planet.isMoon() || planet.isGasGiant() || planet.getRadius() <= 40f) continue;
                if (!types.isEmpty() && !types.contains(planet.getTypeId())) continue;
                MarketAPI market = planet.getMarket();
                if (market == null || !market.isPlanetConditionMarketOnly()) continue;
                if (planet.hasTag(Tags.NOT_RANDOM_MISSION_TARGET) || planet.hasTag(Tags.MISSION_ITEM)) continue;
                MemoryAPI memory = planet.getMemoryWithoutUpdate();
                if (memory.getBoolean("$aotd_mega_already") || memory.contains("$IndEvo_ArtilleryStation")) continue;
                return planet;
            }
        }
        return null;
    }

    private List<StarSystemAPI> getMegastructureSystems(List<StarSystemAPI> availableSystems) {
        List<StarSystemAPI> systems = new ArrayList<>();
        for (StarSystemAPI system : availableSystems) {
            if (isSuitableMegastructureSystem(system)) systems.add(system);
        }
        Collections.shuffle(systems, Misc.random);
        systems.sort(Comparator.comparing(this::hasMegastructure));
        return systems;
    }

    private boolean isSuitableMegastructureSystem(StarSystemAPI system) {
        return system.getStar() != null && system.getCenter() != null && !system.hasPulsar() && !system.isNebula()
                && !system.hasTag(Tags.THEME_CORE) && !system.hasTag(Tags.THEME_CORE_POPULATED)
                && !system.hasTag(Tags.THEME_CORE_UNPOPULATED) && !system.hasTag(Tags.THEME_HIDDEN)
                && !system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
    }

    private boolean hasMegastructure(StarSystemAPI system) {
        return system.getCustomEntities().stream().anyMatch(entity -> Entities.CORONAL_TAP.equals(entity.getCustomEntityType()))
                || system.getPlanets().stream().anyMatch(planet -> planet.getMemoryWithoutUpdate().getBoolean("$aotd_mega_already")
                || !BaseMegastructureScript.getAllMegasFromEntity(planet).isEmpty());
    }

    private StarSystemAPI getFallbackSystem(List<StarSystemAPI> availableSystems) {
        for (StarSystemAPI system : getMegastructureSystems(availableSystems)) {
            String starType = system.getStar().getSpec().getPlanetType();
            if (Global.getSettings().getSpec(StarGenDataSpec.class, starType, false) != null) return system;
        }
        return null;
    }

    private PlanetAPI spawnFallbackPlanet(List<StarSystemAPI> preferredSystems, String name, String type) {
        StarSystemAPI system = getFallbackSystem(preferredSystems);
        if (system == null) system = getFallbackSystem(Global.getSector().getStarSystems());
        if (system == null) {
            Global.getLogger(AoTDDataInserter.class).warn("No suitable star system found for " + name);
            return null;
        }

        PlanetGenDataSpec spec = (PlanetGenDataSpec) Global.getSettings().getSpec(PlanetGenDataSpec.class, type, true);
        float radius = StarSystemGenerator.getRadius(spec.getMinRadius(), spec.getMaxRadius());
        float orbitRadius = BaseThemeGenerator.getOuterRadius(system) + radius + StarSystemGenerator.BASE_INCR;
        Random random = StarSystemGenerator.random;
        float orbitDays = orbitRadius / (20f + random.nextFloat() * 5f);
        PlanetAPI planet = system.addPlanet(null, system.getCenter(), name, type, random.nextFloat() * 360f,
                radius, orbitRadius, orbitDays);
        StarAge age = system.getAge();
        PlanetConditionGenerator.generateConditionsForPlanet(planet, age == null || age == StarAge.ANY ? StarAge.AVERAGE : age);
        return planet;
    }

    public void spawnMegas() {
        ArrayList<StarSystemAPI> hypershuntSystems = AoTDMisc.getStarSystemWithMegastructure("coronal_tap");
        List<StarSystemAPI> systems = List.of();
        for (StarSystemAPI hypershuntSystem : hypershuntSystems) {
            List<StarSystemAPI> nearby = Misc.getNearbyStarSystems(hypershuntSystem.getCenter(), 10).stream()
                    .filter(this::isSuitableMegastructureSystem).toList();
            if (nearby.size() > systems.size()) systems = nearby;
        }
        spawnPluto(systems);
        spawnNidavleir(systems);


    }

    public void spawnPluto(List<StarSystemAPI> possibleSystems) {
        PlanetAPI planet = getEntityWithCriteria(possibleSystems, Planets.PLANET_LAVA, Planets.PLANET_LAVA_MINOR);
        if (planet == null) {
            planet = spawnFallbackPlanet(possibleSystems, "Pluto", Planets.PLANET_LAVA);
        }
        if (planet == null) return;
        for (Map.Entry<String, String> entry : ResourceDepositsCondition.COMMODITY.entrySet()) {
            if (entry.getValue().equals(Commodities.RARE_ORE) || entry.getValue().equals(Commodities.ORE)) {
                planet.getMarket().removeCondition(entry.getKey());
            }
        }
        if (!planet.hasCondition(Conditions.VERY_HOT)) planet.getMarket().addCondition(Conditions.VERY_HOT);
        planet.getMarket().removeCondition(Conditions.HOT);
        planet.getMarket().removeCondition(Conditions.COLD);
        planet.getMarket().removeCondition(Conditions.VERY_COLD);
        planet.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
        planet.getMarket().addCondition(Conditions.ORE_ULTRARICH);
        planet.getMarket().addCondition("aotd_logistic_boneyards");
        planet.getMemoryWithoutUpdate().set("$aotd_mega_already", true);
        String t = planet.getMarket().addCondition("aotd_pluto_station");
        planet.getMarket().getSpecificCondition(t).setSurveyed(false);
//        GPBaseMegastructure mega = AoTDMegastructureRules.putMegastructure(planet, "aotd_pluto_station");
//        SectorEntityToken token = planet.getMarket().getStarSystem().addCustomEntity("aotd_pluto_station", "Pluto Mining Station", "aotd_pluto_station", Factions.NEUTRAL);
//        float angle = planet.getCircularOrbitAngle();
//        float period = planet.getCircularOrbitPeriod(); // 270 : height
//        token.setCircularOrbitPointingDown(planet, angle, planet.getRadius() + 270 + 70, period);
//        token.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "aotd_mega");
//        MiscellaneousThemeGenerator.makeDiscoverable(token, 40000, 3000f);

    }

    public void RandomSetIndustryOnPlanet(String industryId, int amount, String PlanetType) {
        int count = 0;
        int max_tritachyon = 0;
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (count > amount) {
                break;
            }
            if (faction.isPlayerFaction() || faction.getId().equals("luddic_church") || faction.getId().equals("luddic_path") || faction.getId().equals("pirates") || faction.getId().equals("derelicts")) {
                continue;
            }
            for (MarketAPI aiMarket : Misc.getFactionMarkets(faction.getId())) {


                for (Industry industry : aiMarket.getIndustries()) {
                    if (industry.isIndustry()) {
                        if (industry.getId().equals("heavyindustry")
                                || industry.getId().equals("orbitalworks")
                                || industry.getId().equals("militarybase")
                                || industry.getId().equals("highcommand")
                        ) {
                            continue;
                        }
                        aiMarket.removeIndustry(industry.getId(), null, false);
                        aiMarket.addIndustry(industryId);
                        count++;
                        break;
                    }

                }

                if (aiMarket.getFactionId().equals("tritachyon")) {
                    if (max_tritachyon >= maxTriTachyonElectronics) {
                        break;
                    }
                    max_tritachyon++;

                } else {
                    break;
                }

            }
        }
    }

    public void setIndustryOnPlanet(String SystemName, String Planetname, String industryId, String removeIndustry, String potentialSwitch, boolean toImprove, String aiCore, String itemToInsert) {
        if (Global.getSector().getStarSystem(SystemName) == null) return;
        List<PlanetAPI> planets = Global.getSector().getStarSystem(SystemName).getPlanets();
        for (PlanetAPI planet : planets) {
            if (planet.getName().equals(Planetname)) {

                if (planet.getMarket() == null) continue;
                SpecialItemData data = null;
                if (removeIndustry != null && planet.getMarket().getIndustry(removeIndustry) != null) {

                    data = planet.getMarket().getIndustry(removeIndustry).getSpecialItem();
                    planet.getMarket().removeIndustry(removeIndustry, null, false);

                }
                if (industryId != null) {
                    planet.getMarket().addIndustry(industryId);
                    planet.getMarket().getIndustry(industryId).setImproved(toImprove);
                    planet.getMarket().getIndustry(industryId).setAICoreId(aiCore);
                    if (data != null) {
                        planet.getMarket().getIndustry(industryId).setSpecialItem(data);
                    }
                    if (itemToInsert != null) {
                        SpecialItemData daten = new SpecialItemData(itemToInsert, null);
                        planet.getMarket().getIndustry(industryId).setSpecialItem(daten);
                    }
                }


            }
        }
    }

    public static MarketAPI getMarketBasedOnName(String SystemName, String Planetname) {
        if (Global.getSector().getStarSystem(SystemName) == null) return null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(Global.getSector().getStarSystem(SystemName).getCenter().getContainingLocation())) {
            if (market.getName().equals(Planetname)) {
                return market;
            }
        }

        return null;
    }

    private void setIndustriesOnModdedPlanets() {
        int increased_farming = 3;
        int increased_biotiocs = 0;
        int increased_reci = 0;
        boolean chooseReci = false;
        List<FactionAPI> factionAPIS = Global.getSector().getAllFactions();
        Collections.shuffle(factionAPIS);
        for (FactionAPI faction : factionAPIS) {
            if (faction.getId().equals(Factions.HEGEMONY)) continue;
            if (faction.getId().equals(Factions.TRITACHYON)) continue;
            if (faction.getId().equals(Factions.DIKTAT)) continue;
            if (faction.getId().equals(Factions.PIRATES)) continue;
            if (faction.getId().equals(Factions.LUDDIC_PATH)) continue;
            if (faction.getId().equals(Factions.LUDDIC_CHURCH)) continue;
            if (faction.getId().equals(Factions.INDEPENDENT)) continue;

            for (MarketAPI market : Misc.getFactionMarkets(faction)) {
                if (market.getPlanetEntity() == null) continue;
                if (AoDUtilis.getFoodQuantityBonus(market) >= -1) {
                    if (market.hasIndustry(Industries.FARMING)) {
                        if (increased_biotiocs + increased_reci >= increased_farming) {
                            increased_farming++;

                        } else {
                            if (chooseReci) {
                                setIndustryOnPlanet(market.getStarSystem().getBaseName(), market.getPlanetEntity().getName(), null, null, AoTDConditions.SWITCH_RECITIFICATES, false, null, null);
                                increased_reci++;
                                chooseReci = false;
                            } else {
                                setIndustryOnPlanet(market.getStarSystem().getBaseName(), market.getPlanetEntity().getName(), null, null, AoTDConditions.SWITCH_BIOTICS, false, null, null);
                                increased_biotiocs++;
                                chooseReci = true;
                            }


                        }

                    }

                }
            }

        }
    }

    public void initalizeEconomy(boolean random) {
        if (!random) {
            setIndustryOnPlanet("Hybrasil", "Culann", AoTDIndustries.ORBITAL_SKUNKWORK, Industries.ORBITALWORKS, null, false, Commodities.ALPHA_CORE, null);
            setIndustryOnPlanet("Hybrasil", "Culann", Industries.MEGAPORT, Industries.SPACEPORT, null, true, null, null);
            setIndustryOnPlanet("Aztlan", "Chicomoztoc", AoTDIndustries.ORBITAL_FLEETWORK, Industries.ORBITALWORKS, null, false, null, Items.PRISTINE_NANOFORGE);
            setIndustryOnPlanet("Aztlan", "Chicomoztoc", AoTDIndustries.STAR_CITADEL_LOW, Industries.STARFORTRESS, null, false, null, null);
            setIndustryOnPlanet("Hybrasil", "Culann", AoTDIndustries.STAR_CITADEL_HIGH, Industries.BATTLESTATION_HIGH, null, false, null, null);
            setIndustryOnPlanet("Hybrasil", "Eouchu Bres", AoTDIndustries.ARTISANAL_FARMING, Industries.FARMING, null, false, null, null);
            setIndustryOnPlanet("Zagan", "Mazalot", AoTDIndustries.ARTISANAL_FARMING, Industries.FARMING, AoTDConditions.SWITCH_RECITIFICATES, false, null, null);
            setIndustryOnPlanet("Samarra", "Tartessus", AoTDIndustries.ARTISANAL_FARMING, Industries.FARMING, null, false, null, null);
            setIndustryOnPlanet("Corvus", "Jangala", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, null, false, null, null);
            setIndustryOnPlanet("Naraka", "Yama", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, null, false, null, null);
            setIndustryOnPlanet("Westernesse", "Ailmar", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, AoTDConditions.SWITCH_BIOTICS, false, null, null);
            setIndustryOnPlanet("Kumari Kandam", "Chalcedon", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, null, false, null, null);
            setIndustryOnPlanet("Yma", "Qaras", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, AoTDConditions.SWITCH_BIOTICS, false, null, null);
            setIndustryOnPlanet("Galatia", "Ancyra", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, null, false, null, null);
            setIndustryOnPlanet("Mayasura", "Mairaath", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, AoTDConditions.SWITCH_BIOTICS, false, null, null);
            setIndustryOnPlanet("Corvus", "Asharu", AoTDIndustries.SUBSIDISED_FARMING, Industries.FARMING, AoTDConditions.SWITCH_RECITIFICATES, false, null, null);
            setIndustryOnPlanet("Aztlan", "Chicomoztoc", AoTDIndustries.CRYSTALIZATOR, Industries.REFINING, null, false, null, null);
            setIndustryOnPlanet("Aztlan", "Chicomoztoc", Industries.MEGAPORT, Industries.MEGAPORT, null, true, null, null);
            setIndustryOnPlanet("Hybrasil", "Culann", AoTDIndustries.ENRICHMENT_FACILITY, Industries.REFINING, null, true, null, null);
            setIndustryOnPlanet("Hybrasil", "Culann", Industries.HEAVYBATTERIES, Industries.HEAVYBATTERIES, null, true, Commodities.ALPHA_CORE, null);
            setIndustryOnPlanet("Thule", "Kazeron", AoTDIndustries.TERMINUS, null, null, true, null, null);
            setIndustryOnPlanet("Aztlan", "Chicomoztoc", AoTDIndustries.TERMINUS, Industries.WAYSTATION, null, false, null, null);
            setIndustryOnPlanet("Aztlan", "Chicomoztoc", AoTDIndustries.MAGLEV_CENTRAL_HUB, null, null, false, null, null);
        }

        setIndustriesOnModdedPlanets();

    }
}
