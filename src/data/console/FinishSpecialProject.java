package data.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import data.kaysaar.aotd.vok.scripts.specialprojects.BlackSiteProjectManager;
import data.kaysaar.aotd.vok.scripts.specialprojects.models.AoTDSpecialProject;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class FinishSpecialProject implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        if (!args.isBlank()) return CommandResult.BAD_SYNTAX;
        AoTDSpecialProject project = BlackSiteProjectManager.getInstance().getCurrentlyOnGoingProject();
        if (project == null) {
            Console.showMessage("No Black Site project is underway.");
            return CommandResult.ERROR;
        }
        if (!hasRewardStorage()) return CommandResult.ERROR;
        project.finishProject();
        Console.showMessage("Finished " + project.getNameOverride() + ".");
        return CommandResult.SUCCESS;
    }

    static boolean hasRewardStorage() {
        MarketAPI market = Global.getSector().getPlayerFaction().getProduction().getGatheringPoint();
        if (market != null && market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) return true;
        Console.showMessage("Select a colony with storage as your production gathering point for project rewards.");
        return false;
    }
}
