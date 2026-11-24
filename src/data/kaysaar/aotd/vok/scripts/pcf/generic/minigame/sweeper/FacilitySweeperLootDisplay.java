package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper;

import ashlib.data.plugins.info.WeaponInfoGenerator;
import ashlib.data.plugins.rendering.WeaponSpriteRenderer;
import ashlib.data.plugins.ui.models.resizable.ImageViewer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper.FacilitySweeper.HIDDEN;
import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper.FacilitySweeper.LOST;

public class FacilitySweeperLootDisplay implements TooltipMakerAPI.TooltipCreator {
    private final CargoStackAPI stack;
    private final CustomPanelAPI panel;
    private int cacheState;

    public FacilitySweeperLootDisplay(CustomPanelAPI parent, CargoStackAPI stack, float size) {
        this.stack = stack;
        panel = parent.createCustomPanel(size, size, null);
        float iconSize = size * 0.72f;
        CustomPanelAPI icon;
        if (stack.isWeaponStack()) {
            WeaponSpecAPI weapon = stack.getWeaponSpecIfWeapon();
            SpriteAPI sprite = Global.getSettings().getSprite(weapon.getTurretSpriteName());
            float height = iconSize / Math.max(1f, sprite.getWidth() / sprite.getHeight());
            WeaponSpriteRenderer renderer = new WeaponSpriteRenderer(weapon, height, 0f);
            icon = panel.createCustomPanel(iconSize, iconSize, renderer);
            renderer.setAnchor(icon);
        } else {
            String path = stack.isSpecialStack() ? stack.getSpecialItemSpecIfSpecial().getIconName()
                    : Global.getSettings().getCommoditySpec(stack.getCommodityId()).getIconName();
            SpriteAPI sprite = Global.getSettings().getSprite(path);
            float scale = iconSize / Math.max(sprite.getWidth(), sprite.getHeight());
            icon = new ImageViewer(sprite.getWidth() * scale, sprite.getHeight() * scale, path).getComponentPanel();
        }
        panel.addComponent(icon).inTL((size - icon.getWidth()) / 2f, (size - icon.getHeight()) / 2f);

        TooltipMakerAPI hover = panel.createUIElement(size, size, false);
        hover.addTooltipTo(this, panel, TooltipMakerAPI.TooltipLocation.BELOW);
        panel.addUIElement(hover).inTL(0f, 0f);
    }

    public CustomPanelAPI getPanel() {
        return panel;
    }

    void setCacheState(int cacheState) {
        this.cacheState = cacheState;
    }

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return stack.isSpecialStack() && stack.getPlugin().isTooltipExpandable();
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        if (stack.isSpecialStack()) return stack.getPlugin().getTooltipWidth();
        return stack.isWeaponStack() ? 500f : 400f;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        if (stack.isSpecialStack()) {
            stack.getPlugin().createTooltip(tooltip, expanded, null, null);
        } else if (stack.isWeaponStack()) {
            WeaponInfoGenerator.generate(tooltip, stack.getWeaponSpecIfWeapon(), getTooltipWidth(tooltipParam));
        } else {
            tooltip.addTitle(stack.getDisplayName());
            Description description = Global.getSettings().getDescription(stack.getCommodityId(), Description.Type.RESOURCE);
            if (description.hasText1()) tooltip.addPara(description.getText1FirstPara(), 10f);
        }
        tooltip.addSectionHeading("Loot data", Alignment.MID, 10f);
        String amount = Misc.getWithDGS(Math.round(stack.getSize())) + " × " + stack.getDisplayName();
        if (cacheState == LOST) {
            tooltip.addPara("A mine destroyed %s.", 5f, Misc.getNegativeHighlightColor(), amount);
        } else if (cacheState == HIDDEN) {
            tooltip.addPara("The team left behind %s when it withdrew.", 5f, Misc.getNegativeHighlightColor(), amount);
        } else {
            tooltip.addPara("We have found %s in this facility.", 5f, Misc.getHighlightColor(), amount);
        }
    }
}
