package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.voidsignal;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.MinigameDialog;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMazeDialog.color;
import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.voidsignal.VoidSignal.ANTENNA_ROW;

public class VoidSignalDialog extends MinigameDialog {
    private static final float TILE_SIZE = 34f;
    private static final float BOARD_LEFT = 24f, BOARD_TOP = 45f;
    private static final float DISH_AREA_HEIGHT = 70f, FOOTER_HEIGHT = 126f;
    private static final float DISH_SIZE = 58f;
    private static final float DISH_PIVOT = 0.175f, DISH_TIP = 0.86f;
    private static final float AIM_STEP_TIME = 0.09f;
    private static final Color TILE_COLOR = new Color(32, 56, 71);
    private final VoidSignal signal;
    private final Consumer<Boolean> onClose;
    private final float[] displayedColumns, displayedRows;
    private final LabelAPI[] signalReadouts;
    private final SpriteAPI dish = Global.getSettings().getSprite("graphics/weapons/decorative/sensor_array_dish.png");
    private CustomPanelAPI board;
    private LabelAPI readout;
    private float tileSize, repeatIn;
    private int selected, heldKey;
    private boolean confirmed, closed;

    public VoidSignalDialog(Random random) {
        this(random, null);
    }

    public VoidSignalDialog(Random random, Consumer<Boolean> onClose) {
        super(onClose == null ? "Void Signal" : "Signal Triangulation");
        this.onClose = onClose;
        signal = new VoidSignal(random);
        displayedColumns = new float[signal.getAntennaCount()];
        displayedRows = new float[signal.getAntennaCount()];
        signalReadouts = new LabelAPI[signal.getAntennaCount()];
        for (int i = 0; i < signal.getAntennaCount(); i++) {
            displayedColumns[i] = signal.getAimColumn(i);
            displayedRows[i] = signal.getAimRow(i);
        }
    }

    public CustomPanelAPI createPreviewPanel(CustomPanelAPI parent) {
        int gridSize = signal.getGridSize();
        fitBoard(parent.getWidth(), parent.getHeight());
        goalSizeX = tileSize * gridSize + 2f * BOARD_LEFT;
        goalSizeY = tileSize * gridSize + BOARD_TOP + FOOTER_HEIGHT;
        panelToInfluence = parent.createCustomPanel(goalSizeX, goalSizeY, this);
        reachedMaxHeight = true;
        createUI(panelToInfluence);
        return panelToInfluence;
    }

    public boolean show() {
        fitBoard(WIDTH, HEIGHT - FOOTER_BUTTON_HEIGHT - 20f);
        return showInInteraction();
    }

    private void fitBoard(float width, float height) {
        int gridSize = signal.getGridSize();
        tileSize = Math.min(TILE_SIZE, Math.min((width - 2f * BOARD_LEFT) / gridSize,
                (height - BOARD_TOP - FOOTER_HEIGHT) / gridSize));
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        float boardSize = tileSize * signal.getGridSize();
        float boardLeft = (panel.getWidth() - boardSize) / 2f;
        board = panel.createCustomPanel(boardSize, boardSize, null);
        panel.addComponent(board).inTL(boardLeft, BOARD_TOP);

        TooltipMakerAPI status = panel.createUIElement(boardSize, FOOTER_HEIGHT - DISH_AREA_HEIGHT, false);
        float slotWidth = boardSize / signal.getAntennaCount();
        status.setParaFont(Fonts.ORBITRON_12);
        for (int i = 0; i < signalReadouts.length; i++) {
            LabelAPI label = status.addPara(" ", 0f);
            label.getPosition().setWidth(slotWidth);
            label.getPosition().inTL(getAntennaOffsetX(i) - slotWidth * 0.5f, 0f);
            label.setAlignment(Alignment.MID);
            signalReadouts[i] = label;
            updateSignalReadout(i);
        }
        status.setParaFont(Fonts.ORBITRON_20AA);
        readout = status.addPara(" ", 0f);
        readout.getPosition().inTL(0f, 22f);
        readout.setAlignment(Alignment.MID);
        panel.addUIElement(status).inTL(boardLeft, BOARD_TOP + boardSize + DISH_AREA_HEIGHT + 6f);
        if (onClose != null) {
            setConfirmText("Continue");
            setCancelText("Return");
            createConfirmAndCancelSection(panel);
        }
        updateStatus();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (board == null || closed || pressedConfirmCancel) {
            if (onClose != null) super.processInput(events);
            return;
        }
        PositionAPI bounds = board.getPosition();
        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;
            if (event.isLMBDownEvent()) {
                float x = event.getX() - bounds.getX(), y = bounds.getY() - event.getY();
                if (x < 0f || x >= bounds.getWidth() || y <= 0f || y > DISH_AREA_HEIGHT) continue;
                float first = getAntennaOffsetX(0);
                float spacing = getAntennaOffsetX(1) - first;
                int antenna = Math.round((x - first) / spacing);
                selected = Math.max(0, Math.min(signal.getAntennaCount() - 1, antenna));
                heldKey = 0;
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
                event.consume();
            } else if (event.isKeyboardEvent()) {
                int key = event.getEventValue();
                if (key != Keyboard.KEY_LEFT && key != Keyboard.KEY_RIGHT
                        && key != Keyboard.KEY_UP && key != Keyboard.KEY_DOWN) continue;
                if (event.isKeyDownEvent() && !event.isRepeat()) {
                    heldKey = key;
                    repeatIn = 0.17f;
                    moveAim(key);
                } else if (event.isKeyUpEvent() && heldKey == key) {
                    heldKey = 0;
                }
                event.consume();
            }
        }
        if (onClose != null) super.processInput(events);
    }

    @Override
    public void advance(float amount) {
        if (onClose != null) super.advance(amount);
        if (board == null || closed || pressedConfirmCancel) return;
        repeatAim(amount);
        float step = amount / AIM_STEP_TIME;
        for (int i = 0; i < signal.getAntennaCount(); i++) {
            displayedColumns[i] = approachAim(displayedColumns[i], signal.getAimColumn(i), step);
            float row = approachAim(displayedRows[i], signal.getAimRow(i), step);
            if (row == displayedRows[i]) continue;
            displayedRows[i] = row;
            updateSignalReadout(i);
        }
    }

    private float approachAim(float current, int target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    private void repeatAim(float amount) {
        if (heldKey == 0) return;
        if (!Keyboard.isKeyDown(heldKey)) {
            heldKey = 0;
            return;
        }
        repeatIn -= amount;
        while (repeatIn <= 0f) {
            moveAim(heldKey);
            repeatIn += AIM_STEP_TIME;
        }
    }

    private float getAntennaOffsetX(int antenna) {
        return (signal.getAntennaColumn(antenna) + 0.5f) * tileSize;
    }

    private void moveAim(int key) {
        boolean wasAligned = signal.isAligned(selected);
        int dx = key == Keyboard.KEY_LEFT ? -1 : key == Keyboard.KEY_RIGHT ? 1 : 0;
        int dy = key == Keyboard.KEY_DOWN ? -1 : key == Keyboard.KEY_UP ? 1 : 0;
        signal.moveAim(selected, dx, dy);
        if (signal.isAligned(selected) != wasAligned) updateStatus();
    }

    private void updateStatus() {
        int aligned = signal.getAlignedCount();
        int count = signal.getAntennaCount();
        boolean complete = aligned == count;
        readout.setText(complete ? "Signal triangulated" : "Antennas aligned " + aligned + "/" + count);
        readout.setColor(complete ? Misc.getPositiveHighlightColor() : Misc.getTextColor());
        if (confirmButton != null && confirmButton.isEnabled() != complete) confirmButton.setEnabled(complete);
    }

    private void updateSignalReadout(int antenna) {
        LabelAPI label = signalReadouts[antenna];
        String text = Math.round(displayedRows[antenna] * 100f / (signal.getGridSize() - 1f)) + "%";
        if (!text.equals(label.getText())) label.setText(text);
    }

    @Override
    public void applyConfirmScript() {
        confirmed = signal.getAlignedCount() == signal.getAntennaCount();
    }

    @Override
    public void onExit() {
        if (closed || onClose == null) return;
        closed = true;
        onClose.accept(confirmed);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (onClose != null) super.renderBelow(alphaMult);
        if (board == null || pressedConfirmCancel) return;
        PositionAPI bounds = board.getPosition();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT | GL11.GL_POINT_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        renderGrid(bounds.getX(), bounds.getY(), alphaMult);

        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        dish.setColor(Color.WHITE);
        dish.setNormalBlend();
        dish.setAlphaMult(alphaMult);
        int count = signal.getAntennaCount();
        float slotWidth = bounds.getWidth() / count;
        for (int i = 1; i <= count; i++) {
            int antenna = (selected + i) % count;
            renderAntenna(antenna, bounds, slotWidth, alphaMult);
        }
        GL11.glPopAttrib();
    }

    private void renderGrid(float left, float bottom, float alphaMult) {
        color(TILE_COLOR, alphaMult);
        int gridSize = signal.getGridSize();
        GL11.glBegin(GL11.GL_QUADS);
        for (int column = 0; column < gridSize; column++) {
            for (int row = 0; row < gridSize; row++) {
                float x = left + column * tileSize, y = bottom + row * tileSize;
                GL11.glVertex2f(x + 1f, y + 1f);
                GL11.glVertex2f(x + tileSize - 1f, y + 1f);
                GL11.glVertex2f(x + tileSize - 1f, y + tileSize - 1f);
                GL11.glVertex2f(x + 1f, y + tileSize - 1f);
            }
        }
        GL11.glEnd();
    }

    private void renderAntenna(int antenna, PositionAPI bounds, float slotWidth, float alphaMult) {
        float x = bounds.getX() + getAntennaOffsetX(antenna);
        float pivotY = bounds.getY() + (ANTENNA_ROW + 0.5f) * tileSize;
        float mountY = pivotY - 4f;
        float column = displayedColumns[antenna], row = displayedRows[antenna];
        float endX = bounds.getX() + (column + 0.5f) * tileSize;
        float endY = bounds.getY() + (row + 0.5f) * tileSize;
        float size = Math.min(DISH_SIZE, Math.min(slotWidth * 0.62f, tileSize * 1.85f));
        float dx = endX - x, dy = endY - pivotY;
        float distance = (float) Math.hypot(dx, dy);
        float upX = dx / distance, upY = dy / distance;
        float centerOffset = size * (0.5f - DISH_PIVOT);
        float tipOffset = size * (DISH_TIP - DISH_PIVOT);
        boolean active = antenna == selected;

        color(active ? Misc.getBasePlayerColor() : TILE_COLOR, alphaMult);
        GL11.glRectf(x - size * 0.3f, mountY, x + size * 0.3f, pivotY);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        dish.setSize(size, size);
        dish.setAngle((float) Math.toDegrees(Math.atan2(-dx, dy)));
        dish.renderAtCenter(x + upX * centerOffset, pivotY + upY * centerOffset);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);

        Color beamColor = Misc.interpolateColor(Misc.getNegativeHighlightColor(),
                Misc.getPositiveHighlightColor(), signal.getSignalProximity(antenna, column, row));
        float beamAlpha = alphaMult * (active || signal.isAligned(antenna) ? 1f : 0.65f);
        renderBeam(x + upX * tipOffset, pivotY + upY * tipOffset, endX, endY,
                active ? 0.95f : 0.6f, beamColor, beamAlpha);
        color(beamColor, beamAlpha);
        GL11.glPointSize(6f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(endX, endY);
        GL11.glEnd();
    }

    private void renderBeam(float x, float y, float endX, float endY, float halfWidth, Color beamColor, float alphaMult) {
        float dx = endX - x, dy = endY - y;
        float length = (float) Math.hypot(dx, dy);
        float sideX = -dy / length, sideY = dx / length;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i < 4; i++) {
            boolean edge = i == 0 || i == 3;
            float offset = (i < 2 ? -1f : 1f) * (halfWidth + (edge ? 1.35f : 0f));
            color(beamColor, edge ? 0f : alphaMult);
            GL11.glVertex2f(x + sideX * offset * 0.65f, y + sideY * offset * 0.65f);
            GL11.glVertex2f(endX + sideX * offset, endY + sideY * offset);
        }
        GL11.glEnd();
    }
}
