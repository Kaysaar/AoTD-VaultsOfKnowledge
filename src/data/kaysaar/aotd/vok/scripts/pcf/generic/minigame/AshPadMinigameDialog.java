package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.rules.Memory;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMazeDialog;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor.ReactorPuzzleDialog;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper.FacilitySweeperDialog;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.voidsignal.VoidSignalDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AshPadMinigameDialog extends MinigameDialog {
    private final Runnable onClose;
    private final List<ButtonAPI> games = new ArrayList<>();
    private CustomPanelAPI gameColumn, gamePanel;
    private ButtonAPI chosen, restartButton, cutLossesButton;

    public AshPadMinigameDialog(Runnable onClose) {
        super("AshPad");
        this.onClose = onClose;
    }

    public void show() {
        AshMisc.initPopUpDialog(this, WIDTH, HEIGHT);
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        setConfirmText("Close");
        createConfirmSection(panel);
        float buttonSpacing = buttonConfirmWidth + FOOTER_BUTTON_GAP;
        TooltipMakerAPI footer = addFooter(panel, buttonConfirmWidth + buttonSpacing, buttonSpacing);
        cutLossesButton = footer.addButton("Cut Losses", "cut_losses", Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, buttonConfirmWidth, FOOTER_BUTTON_HEIGHT, 0f);
        cutLossesButton.getPosition().inTL(0f, 0f);
        restartButton = footer.addButton("Restart Game", "restart", Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, buttonConfirmWidth, FOOTER_BUTTON_HEIGHT, 0f);
        restartButton.getPosition().inTL(buttonSpacing, 0f);

        float width = panel.getWidth() - 30f;
        float height = panel.getHeight() - 95f;
        float listWidth = Math.min(width * 0.25f - 10f, Math.max(145f, width - 680f));
        TooltipMakerAPI list = panel.createUIElement(listWidth, height, false);
        list.addSectionHeading("Minigames", Alignment.MID, 0f);
        addGame(list, "Reactor Puzzle", "reactor", listWidth);
        addGame(list, "Cybermaze", "cybermaze", listWidth);
        addGame(list, "FacilitySweeper", "sweeper", listWidth);
        addGame(list, "Void Signal", "voidsignal", listWidth);
        panel.addUIElement(list).inTL(15f, 45f);
        addTooltip(list);

        gameColumn = panel.createCustomPanel(width - listWidth - 20f, height, null);
        panel.addComponent(gameColumn).inTL(35f + listWidth, 45f);
        addPanel(gameColumn);
        selectGame(games.get(0));
    }

    private void addGame(TooltipMakerAPI list, String name, String id, float width) {
        ButtonAPI button = list.addButton(name, id, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, CutStyle.NONE, width, 40f, 5f);
        button.getPosition().inTL(0f, 23f + 45f * games.size());
        games.add(button);
    }

    private void selectGame(ButtonAPI button) {
        if (gamePanel != null) gameColumn.removeComponent(gamePanel);
        if (chosen != null) chosen.unhighlight();
        chosen = button;
        String game = (String) chosen.getCustomData();
        cutLossesButton.setChecked(false);
        if (cutLossesButton.isEnabled()) cutLossesButton.setEnabled(false);
        cutLossesButton.setOpacity("sweeper".equals(game) ? 1f : 0f);

        gamePanel = switch (game) {
            case "reactor" -> {
                ReactorPuzzleDialog reactor = new ReactorPuzzleDialog(result -> {});
                reactor.title = chosen.getText();
                yield reactor.createPreviewPanel(gameColumn);
            }
            case "cybermaze" -> {
                FacilityMazeDialog maze = new FacilityMazeDialog(new Random().nextLong(), new Memory(), () -> {});
                maze.title = chosen.getText();
                yield maze.createPreviewPanel(gameColumn);
            }
            case "sweeper" -> new FacilitySweeperDialog(new Random()).createPreviewPanel(gameColumn, cutLossesButton);
            case "voidsignal" -> new VoidSignalDialog(new Random()).createPreviewPanel(gameColumn);
            default -> throw new IllegalArgumentException(game);
        };
        gameColumn.addComponent(gamePanel).inTMid(0f);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (!reachedMaxHeight || pressedConfirmCancel) return;
        for (ButtonAPI button : games) {
            if (!button.isChecked()) continue;
            button.setChecked(false);
            selectGame(button);
            return;
        }
        if (restartButton.isChecked()) {
            restartButton.setChecked(false);
            selectGame(chosen);
        }
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);
        if (chosen != null) chosen.highlight();
    }

    @Override
    public void onExit() {
        onClose.run();
    }
}
