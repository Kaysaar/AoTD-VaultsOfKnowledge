package data.kaysaar.aotd.vok.scripts.misc;

import com.fs.graphics.Sprite;
import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import data.kaysaar.aotd.vok.plugins.ReflectionUtilis;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.AshPadMinigameDialog;

import java.util.List;

public class AoTDAshPadUIScript implements EveryFrameScript, CustomUIPanelPlugin {
    private final SpriteAPI compactHolo = Global.getSettings().getSprite("ui_campaign_components", "ashpad_holo_compact");
    private final SpriteAPI fullHolo = Global.getSettings().getSprite("ui_campaign_components", "ashpad_holo_full");
    private UIComponentAPI compactBackground, fullBackground;
    private Fader logisticsFader, compactFader, fullFader;
    private CustomPanelAPI panel;
    private float hoverAlpha;
    private boolean full, hovering, popupOpen;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!isAvailable(ui)) {
            hovering = false;
            hoverAlpha = 0f;
            return;
        }
        if (!(ui.getCore2() instanceof UIPanelAPI core)) return;

        if (panel == null || panel.getParent() != core || compactBackground.getParent() == null
                || compactBackground.getParent().getParent() != core) {
            attachToLogistics(core);
        }
        if (panel == null) return;

        full = fullFader.getBrightness() > compactFader.getBrightness();
        SpriteAPI holo = full ? fullHolo : compactHolo;
        PositionAPI background = (full ? fullBackground : compactBackground).getPosition();
        PositionAPI bounds = panel.getPosition();
        float x = background.getX() + (full ? 205f : 172f);
        float y = background.getY() + (full ? 125f : 130f);
        if (bounds.getWidth() != holo.getWidth() || bounds.getHeight() != holo.getHeight()) {
            bounds.setSize(holo.getWidth(), holo.getHeight());
        }
        if (bounds.getX() != x || bounds.getY() != y) {
            PositionAPI parent = core.getPosition();
            bounds.inBL(x - parent.getX(), y - parent.getY());
        }
    }

    private void attachToLogistics(UIPanelAPI core) {
        if (panel != null && panel.getParent() != null) panel.getParent().removeComponent(panel);
        panel = null;
        hovering = false;
        hoverAlpha = 0f;
        if (!ReflectionUtilis.hasMethodOfName("getLogistics", core)
                || !(ReflectionUtilis.invokeMethod("getLogistics", core) instanceof UIPanelAPI logistics)
                || !(ReflectionUtilis.invokeMethod("getFader", logistics) instanceof Fader fade)) return;

        compactBackground = null;
        fullBackground = null;
        for (UIComponentAPI child : logistics.getChildrenCopy2()) {
            if (!ReflectionUtilis.hasMethodOfName("getSprite", child)
                    || !(ReflectionUtilis.invokeMethod("getSprite", child) instanceof Sprite sprite)
                    || sprite.getImageWidth() != 280) continue;
            if (!(ReflectionUtilis.invokeMethod("getFader", child) instanceof Fader fader)) continue;
            if (sprite.getImageHeight() == 228) {
                compactBackground = child;
                compactFader = fader;
            } else if (sprite.getImageHeight() == 300) {
                fullBackground = child;
                fullFader = fader;
            }
        }
        if (compactBackground == null || fullBackground == null) return;
        logisticsFader = fade;
        panel = Global.getSettings().createCustom(0f, 0f, this);
        TooltipMakerAPI tooltip = panel.createUIElement(1f, 1f, true);
        tooltip.addTooltipTo(new AoTDAshPadTooltip(), panel, TooltipMakerAPI.TooltipLocation.RIGHT);
        core.addComponent(panel);
    }

    private boolean isAvailable(CampaignUIAPI ui) {
        return !popupOpen && !ui.isHideUI() && !ui.isShowingMenu()
                && (!ui.isShowingDialog() || ui.getCurrentCoreTab() != null);
    }

    private boolean isActive(CampaignUIAPI ui) {
        return panel != null && panel.getParent() == ui.getCore2() && isAvailable(ui);
    }

    private float getBrightness() {
        return logisticsFader.getBrightness() * (full ? fullFader : compactFader).getBrightness();
    }

    @Override
    public void render(float alphaMult) {
        if (!isActive(Global.getSector().getCampaignUI())) return;
        PositionAPI bounds = panel.getPosition();
        float x = Global.getSettings().getMouseX();
        float y = Global.getSettings().getMouseY();
        float brightness = getBrightness();
        boolean over = brightness > 0f
                && x >= bounds.getX() && x < bounds.getX() + bounds.getWidth()
                && y >= bounds.getY() && y < bounds.getY() + bounds.getHeight();
        if (over && !hovering) Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 1f);
        hovering = over;
        hoverAlpha = over ? Math.min(1f, hoverAlpha + 0.125f) : Math.max(0f, hoverAlpha - 0.125f);
        if (hoverAlpha == 0f || brightness <= 0f) return;

        SpriteAPI holo = full ? fullHolo : compactHolo;
        holo.setAdditiveBlend();
        holo.setAlphaMult(alphaMult * brightness * hoverAlpha * (full ? 1f : 0.65f));
        holo.render(bounds.getX(), bounds.getY());
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (!Global.getSettings().isDevMode()) return;
        SectorAPI sector = Global.getSector();
        if (!isActive(sector.getCampaignUI()) || getBrightness() <= 0f) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed() || !event.isLMBDownEvent() || !panel.getPosition().containsEvent(event)) continue;
            event.consume();
            Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
            boolean paused = sector.isPaused();
            popupOpen = true;
            hovering = false;
            sector.setPaused(true);
            new AshPadMinigameDialog(() -> {
                popupOpen = false;
                sector.setPaused(paused);
            }).show();
            return;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
