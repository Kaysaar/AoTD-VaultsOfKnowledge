package data.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.ids.AoTDTags;
import data.kaysaar.aotd.tot.stablestructures.AoTDLinkedStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureConstruction;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpec;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddSystemStructure implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        AoTDStableStructureSpec spec = AoTDStableStructureSpecManager.getSpec(args.trim());
        if (spec == null) {
            Console.showMessage("Available structure IDs: " + String.join(", ", AoTDStableStructureSpecManager.getSpecsList().keySet()));
            return CommandResult.BAD_SYNTAX;
        }
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        StarSystemAPI system = fleet.getStarSystem();
        if (system == null) {
            Console.showMessage("Enter a star system to place the structure.");
            return CommandResult.WRONG_CONTEXT;
        }
        SectorEntityToken focus = system.getCenter();
        float nearest = Float.MAX_VALUE;
        for (PlanetAPI planet : system.getPlanets()) {
            float distance = Misc.getDistance(fleet, planet);
            if (distance < nearest) {
                nearest = distance;
                focus = planet;
            }
        }
        AoTDStableStructureImpl structure = spec.getImplClassInstance();
        CustomCampaignEntityAPI entity = system.addCustomEntity(Misc.genUID(), null, spec.getId(), Factions.PLAYER);
        float radius = Math.max(Misc.getDistance(fleet, focus), focus.getRadius() + entity.getRadius() + 50f);
        entity.setCircularOrbitPointingDown(focus, Misc.getAngleInDegrees(focus.getLocation(), fleet.getLocation()), radius, 120f);
        entity.addTag(AoTDTags.AOTD_OBJECTIVE);
        entity.getMemoryWithoutUpdate().set(AoTDStableStructureConstruction.USES_STABLE_LOCATION, false);
        structure.setCurrentlyAssignedToken(entity);
        structure.isBuilding = true;
        if (structure instanceof AoTDLinkedStructureImpl linked) {
            linked.setConstructionFocus(focus);
            linked.onConstructionStarted();
        }
        structure.finishBuildingOrUpgrading();
        Console.showMessage("Added " + entity.getName() + " in " + system.getName() + ".");
        return CommandResult.SUCCESS;
    }
}
