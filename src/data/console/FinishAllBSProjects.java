package data.console;

import data.kaysaar.aotd.vok.scripts.specialprojects.BlackSiteProjectManager;
import data.kaysaar.aotd.vok.scripts.specialprojects.models.AoTDSpecialProject;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;

public class FinishAllBSProjects implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        if (!args.isBlank()) return CommandResult.BAD_SYNTAX;
        BlackSiteProjectManager manager = BlackSiteProjectManager.getInstance();
        int finished = 0;
        for (AoTDSpecialProject project : new ArrayList<>(manager.getProjects().values())) {
            if (project.wasCompleted || (project.getCountOfCompletion() > 0 && !manager.isCurrentOnGoing(project))) continue;
            if (finished == 0 && !FinishSpecialProject.hasRewardStorage()) return CommandResult.ERROR;
            project.finishProject();
            finished++;
        }
        Console.showMessage("Finished " + finished + " Black Site projects and granted their rewards.");
        return CommandResult.SUCCESS;
    }
}
