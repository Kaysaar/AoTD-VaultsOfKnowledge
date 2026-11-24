package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor;

import ashlib.data.plugins.ui.models.BaseStoryPointPopUP;
import ashlib.data.plugins.ui.models.ProgressBarComponentV2;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.MinigameDialog;
import data.kaysaar.aotd.vok.ui.onhover.ButtonOnHoverInfo;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static ashlib.data.plugins.ui.models.ProgressBarComponentV2.PROGRESS_STEPS;
import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor.ReactorPuzzle.ROD_COUNT;

public class ReactorPuzzleDialog extends MinigameDialog {
    private static final int PREVIEW_WIDTH = 660, PREVIEW_HEIGHT = 450;
    private static final int BOARD_TOP = 76, LANE_WIDTH = 102, BOARD_HEIGHT = 218;
    private static final int BOARD_WIDTH = ROD_COUNT * LANE_WIDTH;
    private static final int BAR_WIDTH = BOARD_WIDTH + 15;
    private static final int PUMP_COST = 2;
    private static final float FLOOD_DISPLAY_RATE = 0.5f;
    private static final Color BOARD_EDGE = new Color(105, 149, 166);
    private static final Color STEEL = new Color(190, 213, 223);
    private static final Color WATER = new Color(36, 112, 148, 130);
    private static final Color WATERLINE = new Color(106, 210, 236, 205);
    private static final Color WARNING = new Color(255, 181, 80);
    private static final Color LATCHED = new Color(151, 230, 192);
    private final ReactorPuzzle reactor = new ReactorPuzzle();
    private final Consumer<ReactorPuzzle> onClose;
    private final SpriteAPI fill = Global.getSettings().getSprite("rendering", "GlitchSquare");
    private final ButtonAPI[] pullButtons = new ButtonAPI[ROD_COUNT];
    private final float[] rodPositions = new float[ROD_COUNT];
    private ProgressBarComponentV2 waterBar, salvageBar;
    private ButtonAPI pumpButton;
    private BaseStoryPointPopUP warning;
    private int selected = -1;
    private float boardLeft, boardTop;
    private float shakeTime, displayedFlood;
    private boolean closed, preview;

    public ReactorPuzzleDialog(Consumer<ReactorPuzzle> onClose) {
        super("Hexagon Platform Reactor");
        this.onClose = onClose;
    }

    public boolean show() {
        return showInInteraction();
    }

    public CustomPanelAPI createPreviewPanel(CustomPanelAPI parent) {
        preview = true;
        panelToInfluence = parent.createCustomPanel(PREVIEW_WIDTH, PREVIEW_HEIGHT, this);
        reachedMaxHeight = true;
        createUI(panelToInfluence);
        return panelToInfluence;
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        if (!preview) {
            setConfirmText("Close");
            createConfirmSection(panel);
            confirmButton.setShortcut(Keyboard.KEY_ESCAPE, true);
        }

        boardLeft = (panel.getWidth() - BAR_WIDTH) / 2f;
        boardTop = (panel.getHeight() - PREVIEW_HEIGHT) / 2f + BOARD_TOP;
        waterBar = addStatusBar(panel, boardTop - 33, getWaterReadout(), WATERLINE, 0f);

        float controlsTop = boardTop + BOARD_HEIGHT + 9;
        TooltipMakerAPI controls = panel.createUIElement(BOARD_WIDTH, 85, false);
        for (int i = 0; i < ROD_COUNT; i++) {
            ButtonAPI button = controls.addButton("Pull " + (i + 1), i, 88, 25, 0f);
            button.getPosition().inTL(i * LANE_WIDTH + 7, 0);
            controls.addTooltipTo(new ButtonOnHoverInfo(300, false, null,
                    getRodHint(i), "Raise all six rods to contain the leak. Rising water reduces salvage.",
                    null, null, "Rod " + (i + 1)), button, TooltipMakerAPI.TooltipLocation.BELOW, false);
            pullButtons[i] = button;
        }
        addTooltip(controls);
        panel.addUIElement(controls).inTL(boardLeft, controlsTop);
        salvageBar = addStatusBar(panel, controlsTop + 35, getSalvageReadout(1f), WARNING, 1f);

        if (preview) return;
        TooltipMakerAPI footer = addFooter(panel, 225f, buttonConfirmWidth + FOOTER_BUTTON_GAP);
        pumpButton = footer.addButton("Backup Pumps (" + PUMP_COST + " SP)", "pumps", Misc.getStoryBrightColor(),
                Misc.getStoryDarkColor(), Alignment.MID, CutStyle.TL_BR, 225f, FOOTER_BUTTON_HEIGHT, 0f);
        pumpButton.getPosition().inTL(0, 0);
        boolean canPump = Global.getSector().getPlayerStats().getStoryPoints() >= PUMP_COST;
        if (pumpButton.isEnabled() != canPump) pumpButton.setEnabled(canPump);
        pumpButton.setShowTooltipWhileInactive(true);
        footer.addTooltipTo(new ButtonOnHoverInfo(300, false, null,
                "Drain the reactor room and complete the operation with full salvage.",
                null, null, null, "Backup pumps"), pumpButton, TooltipMakerAPI.TooltipLocation.BELOW, false);
    }

    private String getRodHint(int rod) {
        StringJoiner linked = new StringJoiner(", ", "Linked rods (", ") will drop if already raised. Their indicators shake while you hover.");
        linked.setEmptyValue("Pulling this rod leaves the other rods in place.");
        for (int i = 0; i < ROD_COUNT; i++) {
            if (reactor.isLinked(rod, i)) linked.add(String.valueOf(i + 1));
        }
        return linked.toString();
    }

    private ProgressBarComponentV2 addStatusBar(CustomPanelAPI panel, float top, String text, Color color, float progress) {
        ProgressBarComponentV2 bar = new ProgressBarComponentV2(BAR_WIDTH, 23, text, Fonts.DEFAULT_SMALL, color, BOARD_EDGE, progress);
        centerStatusLabel(bar);
        CustomPanelAPI barPanel = bar.getRenderingPanel();
        addPanel(barPanel);
        panel.addComponent(barPanel).inTL(boardLeft, top);
        return bar;
    }

    private void centerStatusLabel(ProgressBarComponentV2 bar) {
        LabelAPI label = bar.getProgressLabel();
        label.getPosition().setWidth(BOARD_WIDTH);
        label.setAlignment(Alignment.MID);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (reactor.isPumping()) {
            for (InputEventAPI event : events) event.consume();
            return;
        }
        if (!preview && (warning == null || warning.closed)) super.processInput(events);
    }

    @Override
    public void advance(float amount) {
        if (warning != null && !warning.closed) return;
        boolean pumping = reactor.isPumping();
        if (!preview && !pumping) super.advance(amount);
        if (closed || !reachedMaxHeight || pressedConfirmCancel) return;

        if (pumpButton != null) {
            boolean canPump = !pumping && !reactor.isComplete()
                    && Global.getSector().getPlayerStats().getStoryPoints() >= PUMP_COST;
            if (pumpButton.isEnabled() != canPump) pumpButton.setEnabled(canPump);
            if (pumpButton.isChecked()) {
                pumpButton.setChecked(false);
                if (canPump) {
                    selected = -1;
                    warning = new BaseStoryPointPopUP(PUMP_COST, reactor::startPumps);
                    if (!showInInteraction(warning, 590f, 135f)) warning = null;
                    return;
                }
            }
        }

        reactor.advance(amount);
        shakeTime = (shakeTime + amount * 48f) % ((float) Math.PI * 2f);
        for (int i = 0; i < ROD_COUNT; i++) {
            ButtonAPI button = pullButtons[i];
            if (button.isChecked()) {
                button.setChecked(false);
                reactor.pull(i);
                break;
            }
        }
        int hovered = -1;
        for (int i = 0; i < ROD_COUNT; i++) {
            boolean raised = reactor.isRaised(i);
            ButtonAPI button = pullButtons[i];
            boolean enabled = !pumping && !raised;
            if (button.isEnabled() != enabled) {
                button.setEnabled(enabled);
                button.setText((raised ? "Raised " : "Pull ") + (i + 1));
            }
            float target = raised ? 1f : 0f;
            rodPositions[i] += Math.signum(target - rodPositions[i]) * Math.min(Math.abs(target - rodPositions[i]), amount * 5f);
            if (enabled && isHovered(button.getPosition())) hovered = i;
        }
        selected = hovered;
        displayedFlood = Math.min(reactor.getFlood(), displayedFlood + amount * FLOOD_DISPLAY_RATE);
        updateReadout();
        if (pumping && reactor.isComplete()) forceDismiss();
    }

    private boolean isHovered(PositionAPI position) {
        float mouseX = Global.getSettings().getMouseX();
        float mouseY = Global.getSettings().getMouseY();
        return mouseX >= position.getX() && mouseX < position.getX() + position.getWidth()
                && mouseY >= position.getY() && mouseY < position.getY() + position.getHeight();
    }

    private String getSalvageReadout(float salvage) {
        return "Salvage  " + formatPercent(salvage) + "     Rods raised  " + reactor.getRaisedCount() + "/" + ROD_COUNT;
    }

    private String getWaterReadout() {
        return "Water level  " + (int) (displayedFlood * 100f) + " %";
    }

    private String formatPercent(float progress) {
        return Math.round(progress * 100f) + " %";
    }

    private void updateBar(ProgressBarComponentV2 bar, String text, float progress) {
        if (Math.round(bar.getProgress() * PROGRESS_STEPS) != Math.round(progress * PROGRESS_STEPS)) {
            bar.setBarText(text);
            bar.setProgress(progress);
            centerStatusLabel(bar);
        }
        LabelAPI label = bar.getProgressLabel();
        if (!text.equals(label.getText())) label.setText(text);
    }

    private void updateReadout() {
        updateBar(waterBar, getWaterReadout(), displayedFlood);
        float displayedSalvage = ReactorPuzzle.getSalvageFraction(displayedFlood);
        updateBar(salvageBar, getSalvageReadout(displayedSalvage), displayedSalvage);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (closed || panelToInfluence == null) return;
        PositionAPI position = panelToInfluence.getPosition();
        draw(position.getX(), position.getY(), position.getWidth(), position.getHeight(), Color.BLACK, alphaMult);
        if (!preview) super.renderBelow(alphaMult);
        if (!reachedMaxHeight || pressedConfirmCancel) return;
        float left = position.getX() + boardLeft;
        float bottom = position.getY() + position.getHeight() - boardTop - BOARD_HEIGHT;
        draw(left, bottom, BOARD_WIDTH, 2, BOARD_EDGE, alphaMult);
        draw(left, bottom + BOARD_HEIGHT - 2, BOARD_WIDTH, 2, BOARD_EDGE, alphaMult);
        draw(left, bottom, 2, BOARD_HEIGHT, BOARD_EDGE, alphaMult);
        draw(left + BOARD_WIDTH - 2, bottom, 2, BOARD_HEIGHT, BOARD_EDGE, alphaMult);
        draw(left, bottom + 152, BOARD_WIDTH, 2, BOARD_EDGE, alphaMult);

        for (int i = 0; i < ROD_COUNT; i++) {
            float x = left + (i + 0.5f) * LANE_WIDTH;
            draw(x - 14, bottom + 16, 28, 2, BOARD_EDGE, alphaMult);
            draw(x - 14, bottom + 16, 2, 127, BOARD_EDGE, alphaMult);
            draw(x + 12, bottom + 16, 2, 127, BOARD_EDGE, alphaMult);
            float rodBottom = bottom + 22 + 59 * rodPositions[i];
            Color rodColor = reactor.isRaised(i) ? LATCHED : STEEL;
            draw(x - 5, rodBottom, 10, 55, rodColor, alphaMult);
            draw(x - 9, rodBottom + 50, 18, 5, rodColor, alphaMult);
        }

        float floodHeight = displayedFlood * 148f;
        if (floodHeight > 0f) {
            draw(left + 2, bottom + 2, BOARD_WIDTH - 4, floodHeight, WATER, alphaMult);
            draw(left + 2, bottom + 2 + floodHeight, BOARD_WIDTH - 4, 2, WATERLINE, alphaMult);
        }
        for (int i = 0; i < ROD_COUNT; i++) {
            boolean linked = selected >= 0 && reactor.isLinked(selected, i);
            float x = left + (i + 0.5f) * LANE_WIDTH;
            float shake = linked ? (float) Math.sin(shakeTime + i) * 4f : 0f;
            Color color = linked ? WARNING : STEEL;
            draw(x - 5 + shake, bottom + 169, 10, 32, color, alphaMult);
            draw(x - 9 + shake, bottom + 196, 18, 5, color, alphaMult);
        }
    }

    private void draw(float left, float bottom, float width, float height, Color color, float alphaMult) {
        fill.setSize(width, height);
        fill.setColor(color);
        fill.setAlphaMult(alphaMult);
        fill.render(left, bottom);
    }

    @Override
    public void onExit() {
        if (closed) return;
        closed = true;
        onClose.accept(reactor);
    }
}
