package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame;

import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

public abstract class MinigameDialog extends BasePopUpDialog {
    public static final int WIDTH = 1150, HEIGHT = 700;
    protected static final float FOOTER_BUTTON_HEIGHT = 25f, FOOTER_BUTTON_GAP = 5f;

    protected MinigameDialog(String title) {
        super(title);
    }

    protected boolean showInInteraction() {
        return showInInteraction(this, WIDTH, HEIGHT);
    }

    protected TooltipMakerAPI addFooter(CustomPanelAPI panel, float width, float rightOffset) {
        TooltipMakerAPI footer = panel.createUIElement(width, FOOTER_BUTTON_HEIGHT, false);
        footer.setButtonFontOrbitron20();
        panel.addUIElement(footer).inTR(20f + rightOffset, goalSizeY - 40f);
        addTooltip(footer);
        return footer;
    }

    public static boolean showInInteraction(BasePopUpDialog popup, float width, float height) {
        UIPanelAPI parent = ProductionUtil.getCoreUIForDialog();
        if (parent == null) return false;
        PositionAPI bounds = parent.getPosition();
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, popup);
        popup.initForDialog(panel, (bounds.getWidth() - width) / 2f, (bounds.getHeight() + height) / 2f, true);
        return true;
    }
}
