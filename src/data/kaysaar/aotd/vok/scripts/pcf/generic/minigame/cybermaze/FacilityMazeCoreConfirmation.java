package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.MinigameDialog;

public class FacilityMazeCoreConfirmation extends BasePopUpDialog {
    private final String commodityId;
    private final String description;
    private final Runnable onConfirm;
    private boolean spent;
    boolean closed;

    public FacilityMazeCoreConfirmation(String commodityId, String description, Runnable onConfirm) {
        super("Use " + Global.getSettings().getCommoditySpec(commodityId).getName() + "?");
        this.commodityId = commodityId;
        this.description = description;
        this.onConfirm = onConfirm;
    }

    public void show() {
        MinigameDialog.showInInteraction(this, 480, 155);
    }

    @Override
    public void createContentForDialog(TooltipMakerAPI tooltip, float width) {
        CommoditySpecAPI core = Global.getSettings().getCommoditySpec(commodityId);
        TooltipMakerAPI item = tooltip.beginImageWithText(core.getIconName(), 32f);
        item.addPara(description, 0f);
        item.addPara("This consumes %s.", 4f, Misc.getHighlightColor(), "one " + core.getName());
        tooltip.addImageWithText(0f);
    }

    @Override
    public void advance(float amount) {
        if (confirmButton != null && !pressedConfirmCancel) {
            boolean enabled = !spent && getCargo().getCommodityQuantity(commodityId) >= 1f;
            if (confirmButton.isEnabled() != enabled) confirmButton.setEnabled(enabled);
            if (!enabled && confirmButton.isChecked()) confirmButton.setChecked(false);
        }
        super.advance(amount);
    }

    @Override
    public void applyConfirmScript() {
        if (closed || spent) return;
        CargoAPI cargo = getCargo();
        if (cargo.getCommodityQuantity(commodityId) < 1f) return;
        spent = true;
        cargo.removeCommodity(commodityId, 1f);
        onConfirm.run();
    }

    private CargoAPI getCargo() {
        return Global.getSector().getPlayerFleet().getCargo();
    }

    @Override
    public void onExit() {
        closed = true;
    }
}
