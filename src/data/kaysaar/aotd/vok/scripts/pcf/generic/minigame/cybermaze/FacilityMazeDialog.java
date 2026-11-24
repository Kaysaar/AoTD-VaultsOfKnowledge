package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import ashlib.data.plugins.ui.models.BaseStoryPointPopUP;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.MinigameDialog;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMaze.*;

public class FacilityMazeDialog extends MinigameDialog {
    public static final int TILE_SIZE = 36;
    public static final int MAX_FAILURES = 2;
    public static final String SEED = "$aotd_pcf_cybermaze_seed";
    public static final String FAILURES = "$aotd_pcf_cybermaze_failures";
    public static final String ACCESS = "$aotd_pcf_cybermaze_access";
    private static final String GAMMA_REVEAL = "$aotd_pcf_cybermaze_gamma";
    private static final int RESULT_WIDTH = 300, RESULT_HEIGHT = 96;
    private static final int[][] SIZES = {{16, 10}, {18, 12}, {22, 15}};
    private final FacilityMaze maze;
    private final MemoryAPI memory;
    private final Runnable onClose;
    private final SpriteAPI endpointFill = Global.getSettings().getSprite("rendering", "GlitchSquare");
    private final IntervalUtil guardInterval;
    private int moving, heldKey, queuedDirection;
    private float progress = 1f;
    private float tileSize = TILE_SIZE;
    private boolean resultShown, denied, preview;
    private CustomPanelAPI board;
    static final int OVERRIDE_COST = 1;
    private ButtonAPI optionsButton;
    private FacilityMazeOptionsPopup options;
    boolean gammaCoreUsed;
    private ArrayDeque<Integer> overrideSteps;
    private float overrideSpeed;
    private BaseStoryPointPopUP warning;
    private FacilityMazeCoreConfirmation coreWarning;
    public boolean closed;

    public FacilityMazeDialog(long seed, MemoryAPI memory, Runnable onClose) {
        super("Facility access");
        this.memory = memory;
        this.onClose = onClose;
        gammaCoreUsed = memory.getBoolean(GAMMA_REVEAL);
        float patrolInterval = 1.5f;
        guardInterval = new IntervalUtil(patrolInterval, patrolInterval);
        Random random = new Random(seed);
        int[] size = SIZES[getSizeIndex(random)];
        maze = new FacilityMaze(size[0], size[1], random);
    }

    public static int getSizeIndex(Random random) {
        return random.nextInt(SIZES.length);
    }

    public void show() {
        showInInteraction();
    }

    public CustomPanelAPI createPreviewPanel(CustomPanelAPI parent) {
        preview = true;
        tileSize = Math.min(TILE_SIZE, Math.min((parent.getWidth() - 50f) / maze.columns,
                (parent.getHeight() - 110f) / maze.rows));
        goalSizeX = maze.columns * tileSize + 50f;
        goalSizeY = maze.rows * tileSize + 110f;
        panelToInfluence = parent.createCustomPanel(goalSizeX, goalSizeY, this);
        reachedMaxHeight = true;
        createUI(panelToInfluence);
        return panelToInfluence;
    }

    static PositionAPI getInteractionPosition() {
        return ProductionUtil.getCoreUIForDialog().getPosition();
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        if (!preview) {
            setConfirmText("Access");
            setCancelText("Exit");
            createConfirmAndCancelSection(panel);
            TooltipMakerAPI controls = addFooter(panel, 200f, 2f * buttonConfirmWidth + FOOTER_BUTTON_GAP);
            optionsButton = controls.addButton("Security Options", "security_options", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                    Alignment.MID, CutStyle.TL_BR, 200f, FOOTER_BUTTON_HEIGHT, 0f);
            optionsButton.getPosition().inTL(0, 0);
            updateButtons();
        } else {
            TooltipMakerAPI controls = panel.createUIElement(goalSizeX - 50f, 25f, false);
            controls.addPara("Use the arrow keys to move.", 0f);
            panel.addUIElement(controls).inTL(25f, goalSizeY - 40f);
        }
        float tile = tileSize;
        float boardWidth = maze.columns * tile, boardHeight = maze.rows * tile;
        board = panel.createCustomPanel(boardWidth, boardHeight, null);
        panel.addComponent(board).inTL((panel.getWidth() - boardWidth) / 2f,
                45f + (panel.getHeight() - boardHeight - 110f) / 2f);
        UILinesRenderer border = new UILinesRenderer(0);
        border.setPanel(board);
        addInternalLinesRenderer(border);
        for (int x = 0; x < maze.columns; x++) {
            for (int y = 0; y < maze.rows; y++) {
                FacilityMazeCell cell = maze.cells[x][y];
                float left = x * tile;
                float top = (maze.rows - y - 1) * tile;
                cell.panel = board.createCustomPanel(tile, tile, null);
                board.addComponent(cell.panel).inTL(left, top);
            }
        }
    }

    void confirmOverride() {
        warning = new BaseStoryPointPopUP(OVERRIDE_COST, this::overrideSystem);
        showInInteraction(warning, 590, 135);
    }

    void confirmCoreUse(String commodityId, String description, Runnable onConfirm) {
        coreWarning = new FacilityMazeCoreConfirmation(commodityId, description, onConfirm);
        coreWarning.show();
    }

    private boolean hasOpenPopup() {
        return (options != null && !options.closed) || (warning != null && !warning.closed)
                || (coreWarning != null && !coreWarning.closed);
    }

    public void overrideSystem() {
        overrideSteps = maze.planOverride(maze.targetX(moving), maze.targetY(moving));
        overrideSpeed = Math.max(32f, overrideSteps.size() / 4f);
        heldKey = 0;
        queuedDirection = 0;
        updateButtons();
    }

    public void updateButtons() {
        if (preview) return;
        boolean running = overrideSteps != null;
        boolean canUseOptions = !running && !denied && !maze.unlocked();
        if (optionsButton.isEnabled() != canUseOptions) optionsButton.setEnabled(canUseOptions);
        boolean canExit = !running && !maze.unlocked();
        if (cancelButton.isEnabled() != canExit) cancelButton.setEnabled(canExit);
        boolean canAccess = !running && !denied && maze.unlocked();
        if (confirmButton.isEnabled() != canAccess) confirmButton.setEnabled(canAccess);
    }

    void revealSecurity() {
        gammaCoreUsed = true;
        memory.set(GAMMA_REVEAL, true);
    }

    @Override
    public void applyConfirmScript() {
        if (!denied && maze.unlocked()) memory.set(ACCESS, true);
    }

    @Override
    public void onExit() {
        if (closed) return;
        closed = true;
        onClose.run();
    }

    public int direction(int key) {
        switch (key) {
            case Keyboard.KEY_UP: return NORTH;
            case Keyboard.KEY_DOWN: return SOUTH;
            case Keyboard.KEY_LEFT: return WEST;
            case Keyboard.KEY_RIGHT: return EAST;
            default: return 0;
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (hasOpenPopup()) return;
        if (overrideSteps != null) {
            for (InputEventAPI event : events) event.consume();
            return;
        }
        if (resultShown) {
            if (preview) return;
            if (maze.unlocked()) {
                for (InputEventAPI event : events) {
                    if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) event.consume();
                }
            }
            super.processInput(events);
            return;
        }
        for (InputEventAPI event : events) {
            int key = event.getEventValue();
            if (event.isConsumed() || !event.isKeyboardEvent() || direction(key) == 0) continue;
            if (event.isKeyDownEvent() && !event.isRepeat()) {
                heldKey = key;
                queuedDirection = direction(key);
            } else if (event.isKeyUpEvent() && heldKey == key) {
                heldKey = 0;
            }
            if (preview) event.consume();
        }
        if (!preview) super.processInput(events);
    }

    @Override
    public void advance(float amount) {
        if (hasOpenPopup()) return;
        options = null;
        warning = null;
        coreWarning = null;
        if (cancelButton != null && !cancelButton.isEnabled() && cancelButton.isChecked()) cancelButton.setChecked(false);
        if (!preview && overrideSteps == null) super.advance(amount);
        if (closed || !reachedMaxHeight || pressedConfirmCancel || resultShown) return;
        updateButtons();
        if (!preview && optionsButton.isEnabled() && optionsButton.isChecked()) {
            optionsButton.setChecked(false);
            heldKey = 0;
            queuedDirection = 0;
            options = new FacilityMazeOptionsPopup(this);
            options.show(optionsButton);
            return;
        }
        if (overrideSteps != null) {
            progress += amount * overrideSpeed;
            while (progress >= 1f) {
                if (moving != 0) maze.move(moving);
                if (overrideSteps.isEmpty()) {
                    moving = 0;
                    progress = 1f;
                    overrideSteps = null;
                    break;
                }
                moving = overrideSteps.removeFirst();
                progress -= 1f;
            }
        } else {
            progress = Math.min(1f, progress + amount * 7f);
            guardInterval.advance(amount);
            boolean advanceGuards = guardInterval.intervalElapsed();
            int px = progress >= 0.5f ? maze.targetX(moving) : maze.x;
            int py = progress >= 0.5f ? maze.targetY(moving) : maze.y;
            for (FacilityMazeGuard guard : maze.guards) {
                denied |= advanceGuards ? guard.advance(maze, px, py) : guard.sees(maze, px, py);
            }
        }
        if (!denied && progress < 1f) return;
        if (!denied && moving != 0) {
            maze.move(moving);
            moving = 0;
        }
        if (denied || maze.unlocked()) {
            if (denied) {
                memory.set(FAILURES, Math.min(MAX_FAILURES, memory.getInt(FAILURES) + 1));
                memory.unset(SEED);
                memory.unset(GAMMA_REVEAL);
            }
            resultShown = true;
            updateButtons();
            TooltipMakerAPI result = panelToInfluence.createUIElement(RESULT_WIDTH, RESULT_HEIGHT, false);
            result.setParaFont(Fonts.ORBITRON_20AA);
            LabelAPI label = result.addPara(denied ? "Access Denied" : "Access Granted", resultColor(), 0f);
            float height = label.getPosition().getHeight();
            label.getPosition().setSize(RESULT_WIDTH, height);
            label.getPosition().inTL(0, (RESULT_HEIGHT - height) / 2f);
            label.setAlignment(Alignment.MID);
            label.blink(1.5f, Float.MAX_VALUE);
            panelToInfluence.addUIElement(result).inTL((goalSizeX - RESULT_WIDTH) / 2f, (goalSizeY - RESULT_HEIGHT) / 2f);
            return;
        }
        int next = queuedDirection;
        queuedDirection = 0;
        if (next == 0 && heldKey != 0 && Keyboard.isKeyDown(heldKey)) next = direction(heldKey);
        if (maze.canMove(next)) {
            moving = next;
            progress = 0f;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (!preview) super.renderBelow(alphaMult);
        if (!reachedMaxHeight || pressedConfirmCancel) return;
        float tile = tileSize;
        float left = board.getPosition().getX();
        float bottom = board.getPosition().getY();
        endpointFill.setSize(tile, tile);
        endpointFill.setColor(Misc.getPositiveHighlightColor());
        endpointFill.setAlphaMult(alphaMult * 0.25f);
        endpointFill.render(left, bottom + maze.startRow * tile);
        endpointFill.render(left + (maze.columns - 1) * tile, bottom + maze.finishRow * tile);
        Color faction = Global.getSector().getPlayerFaction().getBaseUIColor();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_POINT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPointSize(2f);
        GL11.glBegin(GL11.GL_POINTS);
        color(faction, alphaMult * 0.35f);
        for (int x = 0; x <= maze.columns; x++) {
            for (int y = 0; y <= maze.rows; y++) GL11.glVertex2f(left + x * tile, bottom + y * tile);
        }
        GL11.glEnd();
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINES);
        for (int x = 0; x < maze.columns; x++) {
            for (int y = 0; y < maze.rows; y++) {
                maze.cells[x][y].renderLines(tile, alphaMult, gammaCoreUsed);
            }
        }
        float startX = left + (maze.x + 0.5f) * tile, startY = bottom + (maze.y + 0.5f) * tile;
        float endX = left + (maze.targetX(moving) + 0.5f) * tile;
        float endY = bottom + (maze.targetY(moving) + 0.5f) * tile;
        float dotX = startX + (endX - startX) * progress;
        float dotY = startY + (endY - startY) * progress;
        boolean backtracking = maze.isBacktracking(moving);
        int routeLength = maze.route.size();
        if (backtracking) routeLength--;
        color(faction, alphaMult);
        for (int i = 1; i < routeLength; i++) {
            PositionAPI from = maze.route.get(i - 1).panel.getPosition();
            PositionAPI to = maze.route.get(i).panel.getPosition();
            line(from.getCenterX(), from.getCenterY(), to.getCenterX(), to.getCenterY());
        }
        if (backtracking) line(endX, endY, dotX, dotY);
        else line(startX, startY, dotX, dotY);
        GL11.glEnd();
        for (FacilityMazeGuard guard : maze.guards) {
            float gx = left + (guard.x() + 0.5f) * tile, gy = bottom + (guard.y() + 0.5f) * tile;
            if (!gammaCoreUsed && Math.abs(gx - dotX) + Math.abs(gy - dotY) > tile * 6) continue;
            int dx = guard.dx(), dy = guard.dy();
            int sight = guard.sight(maze);
            for (int step = 1; step <= sight; step++) {
                float cx = gx + step * dx * tile, cy = gy + step * dy * tile;
                float half = tile * 0.44f;
                color(Misc.getNegativeHighlightColor(), alphaMult * 0.32f);
                GL11.glRectf(cx - half, cy - half, cx + half, cy + half);
                color(Misc.getNegativeHighlightColor(), alphaMult * 0.85f);
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex2f(cx - half, cy - half);
                GL11.glVertex2f(cx + half, cy - half);
                GL11.glVertex2f(cx + half, cy + half);
                GL11.glVertex2f(cx - half, cy + half);
                GL11.glEnd();
                float arrow = tile * 0.16f;
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glVertex2f(cx - (dx + dy) * arrow, cy + (dx - dy) * arrow);
                GL11.glVertex2f(cx + dx * arrow, cy + dy * arrow);
                GL11.glVertex2f(cx + (dy - dx) * arrow, cy - (dx + dy) * arrow);
                GL11.glEnd();
            }
            float radius = tile * 0.24f;
            color(Misc.getNegativeHighlightColor(), alphaMult);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(gx + dx * radius, gy + dy * radius);
            GL11.glVertex2f(gx + (-dx - dy) * radius, gy + (-dy + dx) * radius);
            GL11.glVertex2f(gx + (-dx + dy) * radius, gy + (-dy - dx) * radius);
            GL11.glEnd();
        }
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glPointSize(9f);
        GL11.glBegin(GL11.GL_POINTS);
        color(Color.WHITE, alphaMult);
        GL11.glVertex2f(dotX, dotY);
        GL11.glEnd();
        if (resultShown) {
            float cx = panelToInfluence.getPosition().getCenterX(), cy = panelToInfluence.getPosition().getCenterY();
            float halfWidth = RESULT_WIDTH / 2f, halfHeight = RESULT_HEIGHT / 2f;
            color(resultColor(), alphaMult);
            GL11.glRectf(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight);
            color(Color.BLACK, alphaMult);
            GL11.glRectf(cx - halfWidth + 1, cy - halfHeight + 1, cx + halfWidth - 1, cy + halfHeight - 1);
        }
        GL11.glPopAttrib();
    }

    public Color resultColor() {
        return denied ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor();
    }

    public static void color(Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha);
    }

    public static void line(float x, float y, float endX, float endY) {
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(endX, endY);
    }
}
