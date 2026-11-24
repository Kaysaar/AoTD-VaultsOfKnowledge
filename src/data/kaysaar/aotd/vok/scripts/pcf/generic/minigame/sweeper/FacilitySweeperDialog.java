package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper;

import ashlib.data.plugins.ui.models.BaseStoryPointPopUP;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.MinigameDialog;
import data.kaysaar.aotd.vok.ui.onhover.ButtonOnHoverInfo;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper.FacilitySweeper.*;

public class FacilitySweeperDialog extends MinigameDialog {
    private static final float TILE_SIZE = 44f;
    private static final float BOARD_LEFT = 24f, BOARD_TOP = 81f;
    private static final float FOOTER_HEIGHT = 70f, DECODE_WIDTH = 265f;
    private static final int DISARM_COST = 1;
    private static final float PING_INTERVAL = 0.32f, FUSE = PING_INTERVAL * 3f, BLAST_DURATION = 0.45f;
    private static final Color CLOSED_TILE = new Color(32, 56, 71);
    private static final Color OPEN_TILE = new Color(15, 30, 41);
    private static final Color BLASTED_TILE = new Color(75, 37, 35);
    private static final Color DISCOVERED_CACHE = new Color(77, 173, 144);
    private static final Color LOST_CACHE = new Color(173, 78, 62);
    private static final Color ABANDONED_CACHE = new Color(198, 146, 78);
    private static final Color[] CLUES = {Color.WHITE, new Color(132, 216, 240), new Color(146, 222, 169),
            new Color(255, 195, 111), new Color(255, 137, 113)};
    private final FacilitySweeper sweeper;
    private final List<CargoStackAPI> loot;
    private final FacilitySweeperLootDisplay[] lootPanels;
    private final Consumer<CargoAPI> onClose;
    private final LabelAPI[][] clues = new LabelAPI[COLUMNS][ROWS];
    private final TooltipMakerAPI[][] flags = new TooltipMakerAPI[COLUMNS][ROWS];
    private final SpriteAPI mine = Global.getSettings().getSprite("graphics/missiles/heavy_mine2.png");
    private final SpriteAPI glow = Global.getSettings().getSprite("graphics/missiles/heavy_mine2_glow.png");
    private final SpriteAPI explosion = Global.getSettings().getSprite("graphics/fx/explosion0.png");
    private CustomPanelAPI board, markings;
    private LabelAPI readout, discoveredReadout, lostReadout;
    private ButtonAPI cutLossesButton, decodeButton;
    private BaseStoryPointPopUP disarmConfirmation;
    private float tileSize, effectTime;
    private int mineX = -1, mineY, pings;
    private boolean closed;

    public FacilitySweeperDialog(Random random) {
        this(createPreviewLoot(random), null, random, null);
    }

    public FacilitySweeperDialog(CargoAPI cargo, String colonyItem, Random random, Consumer<CargoAPI> onClose) {
        super(onClose == null ? "FacilitySweeper" : "Mined Facility");
        this.onClose = onClose;
        loot = cargo.getStacksCopy();
        loot.sort(Comparator.comparingInt(FacilitySweeperDialog::getCacheSize).reversed());
        lootPanels = new FacilitySweeperLootDisplay[loot.size()];
        int guardedCache = -1, databankCache = -1;
        for (int i = 0; i < loot.size(); i++) {
            CargoStackAPI stack = loot.get(i);
            if (colonyItem != null && stack.isSpecialStack() && colonyItem.equals(stack.getSpecialDataIfSpecial().getId())) {
                guardedCache = i;
            } else if (stack.isCommodityStack() && "research_databank".equals(stack.getCommodityId())) {
                databankCache = i;
            }
        }
        sweeper = new FacilitySweeper(loot.stream().map(FacilitySweeperDialog::getCacheSize).toList(), random,
                guardedCache, databankCache);
    }

    private static CargoAPI createPreviewLoot(Random random) {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        cargo.addCommodity(Commodities.SUPPLIES, 25 + random.nextInt(61));
        cargo.addCommodity(Commodities.PARTS, 15 + random.nextInt(46));
        cargo.addCommodity(Commodities.FUEL, 50 + random.nextInt(76));
        cargo.addWeapons("heavymg", 1 + random.nextInt(3));
        cargo.addWeapons("heavymauler", 2 + random.nextInt(5));
        cargo.addSpecial(new SpecialItemData(Items.WEAPON_BP, "hellbore"), 1);
        List<String> colonyItems = List.of(Items.DRONE_REPLICATOR, Items.CATALYTIC_CORE, Items.CORRUPTED_NANOFORGE);
        cargo.addSpecial(new SpecialItemData(colonyItems.get(random.nextInt(colonyItems.size())), null), 1 + random.nextInt(2));
        return cargo;
    }

    static int getCacheSize(CargoStackAPI stack) {
        float amount = stack.getSize();
        if (stack.isSpecialStack()) {
            if (stack.isBlueprintStack()) return 2;
            return amount > 1f ? 4 : 3;
        }
        if (stack.isWeaponStack()) return amount <= 2f ? 2 : 3;
        if (amount <= 25f) return 2;
        return amount <= 100f ? 3 : 4;
    }

    public CustomPanelAPI createPreviewPanel(CustomPanelAPI parent, ButtonAPI cutLossesButton) {
        this.cutLossesButton = cutLossesButton;
        fitBoard(parent.getWidth(), parent.getHeight());
        goalSizeX = tileSize * COLUMNS + 2f * BOARD_LEFT;
        goalSizeY = tileSize * ROWS + BOARD_TOP + getFooterHeight();
        panelToInfluence = parent.createCustomPanel(goalSizeX, goalSizeY, this);
        reachedMaxHeight = true;
        createUI(panelToInfluence);
        return panelToInfluence;
    }

    public boolean show() {
        fitBoard(WIDTH, HEIGHT);
        return showInInteraction();
    }

    private void fitBoard(float width, float height) {
        tileSize = Math.min(TILE_SIZE, Math.min((width - 2f * BOARD_LEFT) / COLUMNS,
                (height - BOARD_TOP - getFooterHeight()) / ROWS));
    }

    private float getFooterHeight() {
        return FOOTER_HEIGHT + (onClose == null ? 0f : 40f);
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        float boardWidth = tileSize * COLUMNS, boardHeight = tileSize * ROWS;
        float boardLeft = (panel.getWidth() - boardWidth) / 2f;
        TooltipMakerAPI instructions = panel.createUIElement(boardWidth, 45f, false);
        instructions.addPara("Left-click to explore. Right-click to flag a mine. Numbers show adjacent mines.", 0f)
                .setAlignment(Alignment.MID);
        panel.addUIElement(instructions).inTL(boardLeft, 44f);

        board = panel.createCustomPanel(boardWidth, boardHeight, null);
        panel.addComponent(board).inTL(boardLeft, BOARD_TOP);
        markings = panel.createCustomPanel(boardWidth, boardHeight, null);
        panel.addComponent(markings).inTL(boardLeft, BOARD_TOP);
        TooltipMakerAPI numbers = markings.createUIElement(boardWidth, boardHeight, false);
        numbers.setParaFont(Fonts.DEFAULT_SMALL);
        for (int x = 0; x < COLUMNS; x++) {
            for (int y = 0; y < ROWS; y++) {
                LabelAPI label = numbers.addPara(" ", 0f);
                label.getPosition().inTL(x * tileSize + 4f, y * tileSize + 2f);
                clues[x][y] = label;
            }
        }
        markings.addUIElement(numbers).inTL(0f, 0f);

        TooltipMakerAPI status = panel.createUIElement(boardWidth, FOOTER_HEIGHT - 10f, false);
        status.setParaFont(Fonts.ORBITRON_20AA);
        readout = status.addPara(" ", 0f);
        readout.getPosition().setWidth(boardWidth);
        readout.getPosition().inTL(0f, 30f);
        readout.setAlignment(Alignment.MID);

        discoveredReadout = status.addPara(" ", 0f);
        discoveredReadout.getPosition().setWidth(boardWidth);
        discoveredReadout.getPosition().inTL(0f, 0f);
        lostReadout = status.addPara(" ", Misc.getNegativeHighlightColor(), 0f);
        lostReadout.getPosition().setWidth(boardWidth);
        lostReadout.getPosition().inTL(0f, 0f);
        lostReadout.setAlignment(Alignment.RMID);

        panel.addUIElement(status).inTL(boardLeft, BOARD_TOP + boardHeight + 10f);
        if (onClose != null) {
            setConfirmText("Return");
            createConfirmSection(panel);
            float buttonSpacing = buttonConfirmWidth + FOOTER_BUTTON_GAP;
            TooltipMakerAPI footer = addFooter(panel, buttonSpacing + DECODE_WIDTH, buttonSpacing);
            cutLossesButton = footer.addButton("Cut Losses", "cut_losses", Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, buttonConfirmWidth, FOOTER_BUTTON_HEIGHT, 0f);
            cutLossesButton.getPosition().inTL(0f, 0f);
            decodeButton = footer.addButton("Decode Frequency (" + DISARM_COST + " SP)", "decode_frequency",
                    Misc.getStoryBrightColor(), Misc.getStoryDarkColor(), Alignment.MID, CutStyle.TL_BR,
                    DECODE_WIDTH, FOOTER_BUTTON_HEIGHT, 0f);
            decodeButton.getPosition().inTL(buttonSpacing, 0f);
            decodeButton.setShowTooltipWhileInactive(true);
            footer.addTooltipTo(new ButtonOnHoverInfo(300, false, null,
                    "Decode the disarming frequency to disable every mine and recover all surviving loot.",
                    null, null, null, "Decode Frequency"), decodeButton, TooltipMakerAPI.TooltipLocation.BELOW, false);
        }
        updateBoard();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (disarmConfirmation != null && !disarmConfirmation.closed) return;
        if (!reachedMaxHeight || pressedConfirmCancel || board == null) {
            if (onClose != null) super.processInput(events);
            return;
        }
        for (InputEventAPI event : events) {
            if (onClose != null && !canReturn() && event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                event.consume();
                continue;
            }
            if (event.isConsumed() || (!event.isLMBDownEvent() && !event.isRMBDownEvent())) continue;
            PositionAPI bounds = board.getPosition();
            int x = (int) Math.floor((event.getX() - bounds.getX()) / tileSize);
            int y = (int) Math.floor((bounds.getY() + bounds.getHeight() - event.getY()) / tileSize);
            if (x < 0 || x >= COLUMNS || y < 0 || y >= ROWS) continue;
            event.consume();
            if (sweeper.isFinished() || mineX >= 0) continue;
            if (event.isRMBDownEvent()) sweeper.toggleFlag(x, y);
            else if (sweeper.open(x, y)) {
                mineX = x;
                mineY = y;
                effectTime = 0f;
                pings = 1;
                Global.getSoundPlayer().playUISound("mine_spawn", 1f, 0.8f);
            }
            updateBoard();
        }
        if (onClose != null) super.processInput(events);
    }

    @Override
    public void advance(float amount) {
        if (disarmConfirmation != null && !disarmConfirmation.closed) return;
        disarmConfirmation = null;
        if (onClose != null) super.advance(amount);
        if (closed || !reachedMaxHeight || pressedConfirmCancel || board == null) return;
        if (decodeButton != null) {
            boolean canDisarm = canDisarm();
            if (decodeButton.isEnabled() != canDisarm) decodeButton.setEnabled(canDisarm);
            if (decodeButton.isChecked()) {
                decodeButton.setChecked(false);
                if (canDisarm) {
                    disarmConfirmation = new BaseStoryPointPopUP(DISARM_COST, () -> {
                        sweeper.disarmMines();
                        updateBoard();
                    });
                    if (!showInInteraction(disarmConfirmation, 590f, 135f)) disarmConfirmation = null;
                    return;
                }
            }
        }
        if (cutLossesButton.isChecked()) {
            cutLossesButton.setChecked(false);
            if (cutLossesButton.isEnabled()) {
                sweeper.cutLosses();
                updateBoard();
            }
        }
        if (mineX < 0) return;
        float previous = effectTime;
        effectTime += amount;
        while (pings < 3 && effectTime >= pings * PING_INTERVAL) {
            Global.getSoundPlayer().playUISound("mine_spawn", 1f + pings * 0.12f, 0.8f);
            pings++;
        }
        if (previous < FUSE && effectTime >= FUSE) {
            sweeper.detonate(mineX, mineY);
            Global.getSoundPlayer().playUISound("mine_explosion", 1f, 0.8f);
            updateBoard();
        }
        if (effectTime >= FUSE + BLAST_DURATION) {
            mineX = -1;
            updateStatus();
        }
    }

    private void updateBoard() {
        if (!sweeper.hasStarted()) {
            updateStatus();
            return;
        }
        for (int x = 0; x < COLUMNS; x++) {
            for (int y = 0; y < ROWS; y++) {
                int tile = sweeper.getTile(x, y);
                int count = tile == OPEN ? sweeper.nearbyMines(x, y) : 0;
                LabelAPI label = clues[x][y];
                String clue = count > 0 ? Integer.toString(count) : " ";
                if (!clue.equals(label.getText())) {
                    label.setText(clue);
                    label.setColor(CLUES[Math.min(count, CLUES.length - 1)]);
                }
                updateFlag(x, y, tile == FLAGGED);
            }
        }
        boolean finished = sweeper.isFinished();
        for (int i = 0; i < loot.size(); i++) {
            int state = sweeper.getCacheState(i);
            if (state != DISCOVERED && !finished) {
                if (lootPanels[i] != null) {
                    board.removeComponent(lootPanels[i].getPanel());
                    lootPanels[i] = null;
                }
                continue;
            }
            if (lootPanels[i] == null) {
                Rectangle area = sweeper.caches.get(i);
                lootPanels[i] = new FacilitySweeperLootDisplay(board, loot.get(i), area.width * tileSize);
                board.addComponent(lootPanels[i].getPanel()).inTL(area.x * tileSize, area.y * tileSize);
            }
            lootPanels[i].setCacheState(state);
        }
        updateStatus();
    }

    private void updateStatus() {
        boolean mineTriggered = mineX >= 0 && effectTime < FUSE;
        boolean finished = sweeper.isFinished();
        boolean canCutLosses = sweeper.hasStarted() && !finished && !mineTriggered;
        if (cutLossesButton.isEnabled() != canCutLosses) cutLossesButton.setEnabled(canCutLosses);
        if (decodeButton != null) {
            boolean canDisarm = canDisarm();
            if (decodeButton.isEnabled() != canDisarm) decodeButton.setEnabled(canDisarm);
        }
        if (confirmButton != null) {
            boolean canReturn = canReturn();
            if (confirmButton.isEnabled() != canReturn) confirmButton.setEnabled(canReturn);
            if (finished) confirmButton.setText("Continue");
        }
        int discovered = sweeper.countCaches(DISCOVERED);
        if (sweeper.hasStarted()) {
            discoveredReadout.setText("Caches discovered " + discovered + "/" + loot.size());
            lostReadout.setText("Lost " + sweeper.countCaches(LOST));
        }
        String status = " ";
        if (!sweeper.hasStarted()) status = "Left-click any tile to start searching.";
        else if (sweeper.isDisarmed()) status = "Minefield disarmed";
        else if (finished) {
            int abandoned = sweeper.countCaches(HIDDEN);
            status = abandoned > 0 ? "Recovered " + discovered + "   Abandoned " + abandoned : "Sweep complete";
        } else if (mineTriggered) status = "Mine triggered";
        readout.setText(status);
        readout.setColor(mineTriggered ? Misc.getNegativeHighlightColor() : Misc.getTextColor());
    }

    private boolean canDisarm() {
        return !sweeper.isFinished() && mineX < 0 && Global.getSector().getPlayerStats().getStoryPoints() >= DISARM_COST;
    }

    private boolean canReturn() {
        return !sweeper.hasStarted() || (sweeper.isFinished() && mineX < 0);
    }

    @Override
    public void onExit() {
        if (closed || onClose == null) return;
        closed = true;
        if (!sweeper.isFinished()) {
            onClose.accept(null);
            return;
        }
        CargoAPI recovered = Global.getFactory().createCargo(true);
        for (int i = 0; i < loot.size(); i++) {
            if (sweeper.getCacheState(i) == DISCOVERED) recovered.addFromStack(loot.get(i));
        }
        onClose.accept(recovered);
    }

    private void updateFlag(int x, int y, boolean flagged) {
        TooltipMakerAPI flag = flags[x][y];
        if (flagged && flag == null) {
            flag = markings.createUIElement(tileSize, tileSize, false);
            flag.setParaFont(Fonts.ORBITRON_20AA);
            LabelAPI label = flag.addPara("!", Misc.getNegativeHighlightColor(), 0f);
            float height = label.getPosition().getHeight();
            label.getPosition().setWidth(tileSize);
            label.getPosition().inTL(0f, (tileSize - height) / 2f);
            label.setAlignment(Alignment.MID);
            markings.addUIElement(flag).inTL(x * tileSize, y * tileSize);
            flags[x][y] = flag;
        } else if (!flagged && flag != null) {
            markings.removeComponent(flag);
            flags[x][y] = null;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (onClose != null) super.renderBelow(alphaMult);
        if (board == null || !reachedMaxHeight) return;
        PositionAPI bounds = board.getPosition();
        float left = bounds.getX(), top = bounds.getY() + bounds.getHeight();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_QUADS);
        for (int x = 0; x < COLUMNS; x++) {
            for (int y = 0; y < ROWS; y++) {
                Color color = switch (sweeper.getTile(x, y)) {
                    case OPEN -> OPEN_TILE;
                    case BLASTED -> BLASTED_TILE;
                    default -> CLOSED_TILE;
                };
                color(color, alphaMult);
                float tileLeft = left + x * tileSize, tileTop = top - y * tileSize;
                GL11.glVertex2f(tileLeft + 1f, tileTop - 1f);
                GL11.glVertex2f(tileLeft + 1f, tileTop - tileSize + 1f);
                GL11.glVertex2f(tileLeft + tileSize - 1f, tileTop - tileSize + 1f);
                GL11.glVertex2f(tileLeft + tileSize - 1f, tileTop - 1f);
            }
        }
        GL11.glEnd();
        GL11.glLineWidth(2f);
        for (int i = 0; i < loot.size(); i++) {
            int state = sweeper.getCacheState(i);
            if (state == HIDDEN && !sweeper.isFinished()) continue;
            Rectangle area = sweeper.caches.get(i);
            Color border = switch (state) {
                case DISCOVERED -> DISCOVERED_CACHE;
                case LOST -> LOST_CACHE;
                default -> ABANDONED_CACHE;
            };
            color(border, alphaMult * (sweeper.isFinished() ? 0.75f : 0.45f));
            float x = left + area.x * tileSize, y = top - area.y * tileSize;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(x + 1f, y - 1f);
            GL11.glVertex2f(x + area.width * tileSize - 1f, y - 1f);
            GL11.glVertex2f(x + area.width * tileSize - 1f, y - area.height * tileSize + 1f);
            GL11.glVertex2f(x + 1f, y - area.height * tileSize + 1f);
            GL11.glEnd();
        }
        GL11.glPopAttrib();
    }

    private void color(Color color, float alphaMult) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (onClose != null) super.render(alphaMult);
        if (board == null || !reachedMaxHeight) return;
        PositionAPI bounds = board.getPosition();
        if (sweeper.isFinished()) renderRevealedMines(bounds, alphaMult);
        if (mineX < 0) return;
        float x = bounds.getX() + (mineX + 0.5f) * tileSize;
        float y = bounds.getY() + bounds.getHeight() - (mineY + 0.5f) * tileSize;
        if (effectTime < FUSE) {
            mine.setNormalBlend();
            drawEffect(mine, x, y, tileSize * 0.72f, alphaMult);
            float pulse = Math.max(0f, 1f - (effectTime % PING_INTERVAL) / (PING_INTERVAL * 0.65f));
            glow.setAdditiveBlend();
            drawEffect(glow, x, y, tileSize * 0.72f, pulse * alphaMult);
        } else {
            float progress = (effectTime - FUSE) / BLAST_DURATION;
            explosion.setAdditiveBlend();
            drawEffect(explosion, x, y, tileSize * (0.75f + 1.35f * progress), (1f - progress) * alphaMult);
        }
    }

    private void renderRevealedMines(PositionAPI bounds, float alphaMult) {
        mine.setNormalBlend();
        float top = bounds.getY() + bounds.getHeight();
        for (int x = 0; x < COLUMNS; x++) {
            for (int y = 0; y < ROWS; y++) {
                boolean detonated = sweeper.hasDetonatedMine(x, y);
                boolean disarmed = sweeper.hasDisarmedMine(x, y);
                if (!detonated && !disarmed && !sweeper.hasMine(x, y)) continue;
                mine.setColor(detonated ? Misc.getNegativeHighlightColor() : disarmed ? Misc.getPositiveHighlightColor() : Color.WHITE);
                drawEffect(mine, bounds.getX() + (x + 0.5f) * tileSize, top - (y + 0.5f) * tileSize,
                        tileSize * (detonated ? 0.48f : 0.62f), alphaMult);
            }
        }
        mine.setColor(Color.WHITE);
    }

    private void drawEffect(SpriteAPI sprite, float x, float y, float size, float alphaMult) {
        sprite.setSize(size, size);
        sprite.setAlphaMult(alphaMult);
        sprite.renderAtCenter(x, y);
    }
}
