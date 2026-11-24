package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import ashlib.data.plugins.ui.models.InstantPopUpUI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class FacilityMazeOptionsPopup extends InstantPopUpUI {
    private static final float WIDTH = 330f;
    private static final float BUTTON_HEIGHT = 24f, BUTTON_PAD = 1f, PANEL_PAD = 6f;
    private static final float HEIGHT = PANEL_PAD * 2f + BUTTON_HEIGHT * 3f + BUTTON_PAD * 2f;
    private static final String GAMMA_EFFECT = "Reveal all firewalls and track active security measures for the rest of the maze.";
    private static final String BETA_EFFECT = "Solve the maze automatically.";
    private final FacilityMazeDialog maze;
    private ButtonAPI gammaButton, betaButton, overrideButton;
    boolean closed;

    public FacilityMazeOptionsPopup(FacilityMazeDialog maze) {
        this.maze = maze;
    }

    public void show(ButtonAPI anchor) {
        PositionAPI parent = FacilityMazeDialog.getInteractionPosition();
        PositionAPI button = anchor.getPosition();
        float x = Math.max(0f, Math.min(button.getX() - parent.getX(), parent.getWidth() - WIDTH));
        float y = Math.max(HEIGHT, Math.min(button.getY() - parent.getY() + button.getHeight() + HEIGHT + 5f, parent.getHeight()));
        CustomPanelAPI panel = Global.getSettings().createCustom(WIDTH, HEIGHT, this);
        initForDialog(panel, x, y, false);
    }

    @Override
    public float createUIMockup(CustomPanelAPI panel) {
        return HEIGHT;
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        TooltipMakerAPI tooltip = panel.createUIElement(WIDTH, HEIGHT, false);
        gammaButton = addOption(tooltip, "Use Gamma Core to Reveal Firewalls", Commodities.GAMMA_CORE, PANEL_PAD,
                new FacilityMazeOptionTooltip(GAMMA_EFFECT, Commodities.GAMMA_CORE, 1));
        betaButton = addOption(tooltip, "Use Beta Core to Solve Maze", Commodities.BETA_CORE, PANEL_PAD + BUTTON_HEIGHT + BUTTON_PAD,
                new FacilityMazeOptionTooltip(BETA_EFFECT, Commodities.BETA_CORE, 1));
        overrideButton = addOption(tooltip, "Override Entire System (" + FacilityMazeDialog.OVERRIDE_COST + " Story Point)",
                "override", PANEL_PAD + (BUTTON_HEIGHT + BUTTON_PAD) * 2f,
                new FacilityMazeOptionTooltip("Confirm the override to solve the maze automatically.", null, FacilityMazeDialog.OVERRIDE_COST));
        panel.addUIElement(tooltip).inTL(0, 0);
    }

    private ButtonAPI addOption(TooltipMakerAPI tooltip, String text, String id, float y, FacilityMazeOptionTooltip hover) {
        boolean story = "override".equals(id);
        ButtonAPI button = tooltip.addAreaCheckbox(text, id,
                story ? Misc.getStoryBrightColor() : Misc.getBasePlayerColor(),
                story ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(),
                story ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor(),
                WIDTH - PANEL_PAD * 2f, BUTTON_HEIGHT, 0f);
        button.getPosition().inTL(PANEL_PAD, y);
        button.setShowTooltipWhileInactive(true);
        tooltip.addTooltipTo(hover, button, TooltipMakerAPI.TooltipLocation.RIGHT, false);
        return button;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (!reachedMaxHeight || closed) return;

        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (isSelected(gammaButton, !maze.gammaCoreUsed && cargo.getCommodityQuantity(Commodities.GAMMA_CORE) >= 1f)) {
            forceDismiss();
            maze.confirmCoreUse(Commodities.GAMMA_CORE, GAMMA_EFFECT, maze::revealSecurity);
        } else if (isSelected(betaButton, cargo.getCommodityQuantity(Commodities.BETA_CORE) >= 1f)) {
            forceDismiss();
            maze.confirmCoreUse(Commodities.BETA_CORE, BETA_EFFECT, maze::overrideSystem);
        } else if (isSelected(overrideButton, Global.getSector().getPlayerStats().getStoryPoints() >= FacilityMazeDialog.OVERRIDE_COST)) {
            forceDismiss();
            maze.confirmOverride();
        }
    }

    private boolean isSelected(ButtonAPI button, boolean enabled) {
        if (button.isEnabled() != enabled) {
            button.setEnabled(enabled);
            button.setClickable(enabled);
        }
        if (!enabled && button.isChecked()) button.setChecked(false);
        return enabled && button.isChecked();
    }

    @Override
    public void onExit() {
        closed = true;
    }
}
