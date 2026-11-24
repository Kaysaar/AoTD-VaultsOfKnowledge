package data.kaysaar.aotd.vok.scripts.pcf.unique;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import java.util.List;

public class SpyVaultKeypad implements CustomUIPanelPlugin {
    private final InteractionDialogAPI dialog;
    private final MemoryAPI memory;
    private final CustomPanelAPI panel;
    private final LabelAPI display;
    private final ButtonAPI[] buttons = new ButtonAPI[9];
    private final boolean canMash;
    private String digits = "";
    private float deniedSeconds;

    public SpyVaultKeypad(InteractionDialogAPI dialog, boolean canMash) {
        this.dialog = dialog;
        this.canMash = canMash;
        memory = dialog.getInteractionTarget().getMarket().getMemoryWithoutUpdate();
        panel = dialog.getVisualPanel().showCustomPanel(200, 280, this);
        TooltipMakerAPI keys = panel.createUIElement(200, 280, false);
        keys.setParaFont(Fonts.ORBITRON_20AA);
        int hits = memory.getInt(MeetTheSpy.HITS);
        String readout = switch (hits) {
            case 1 -> "P155";
            case 2 -> "P41n15";
            case 3 -> "N0P3";
            case 4 -> "93n713m3n";
            default -> "----";
        };
        display = keys.addPara(memory.getBoolean(MeetTheSpy.CODE) ? "UNLOCKED" : readout, 0f);
        display.getPosition().setSize(140, 25);
        display.getPosition().inTL(30, 54);
        display.setAlignment(Alignment.MID);
        boolean enabled = hits < 4;
        for (int i = 1; i <= 9; i++) {
            ButtonAPI button = keys.addButton(String.valueOf(i), i, 40, 39, 0f);
            if (button.isEnabled() != enabled) button.setEnabled(enabled);
            buttons[i - 1] = button;
            button.getPosition().inTL(30 + (i - 1) % 3 * 50, 92 + (i - 1) / 3 * 50);
        }
        panel.addUIElement(keys).inTL(0, 0);
    }

    private void enterDigit(int digit) {
        if (memory.getBoolean(MeetTheSpy.CODE) || memory.getInt(MeetTheSpy.HITS) >= 4) return;
        digits += digit;
        display.setColor(Misc.getTextColor());
        display.setText(digits);
        if (digits.length() < 4) return;
        if ("1111".equals(digits)) {
            memory.set(MeetTheSpy.CODE, true);
            display.setText("UNLOCKED");
            display.setColor(Misc.getPositiveHighlightColor());
            dialog.getOptionPanel().setEnabled("aotdSpyOpen", true);
            if (canMash) dialog.getOptionPanel().setEnabled("aotdSpyMash", false);
        } else {
            deniedSeconds = 1.5f;
            for (ButtonAPI button : buttons) {
                button.setChecked(false);
                if (button.isEnabled()) button.setEnabled(false);
            }
            display.setText(switch (memory.getInt(MeetTheSpy.HITS)) {
                case 1 -> "DEN1ED";
                case 2 -> "D3N1_D";
                case 3 -> "D_N1--";
                default -> "DENIED";
            });
            display.setColor(Misc.getNegativeHighlightColor());
            display.blink(4f, deniedSeconds);
        }
        digits = "";
    }

    @Override
    public void renderBelow(float alphaMult) {
        SpriteAPI sprite = Global.getSettings().getSprite("signalis_ui", "keypad_bg");
        sprite.setSize(200, 280);
        sprite.setAlphaMult(alphaMult);
        sprite.renderAtCenter(panel.getPosition().getCenterX(), panel.getPosition().getCenterY());
    }

    @Override
    public void advance(float amount) {
        if (deniedSeconds > 0f) {
            deniedSeconds -= amount;
            if (deniedSeconds <= 0f) {
                display.stopBlinking();
                display.setText("----");
                display.setColor(Misc.getTextColor());
                for (ButtonAPI button : buttons) {
                    if (!button.isEnabled()) button.setEnabled(true);
                }
            }
            return;
        }
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isChecked()) {
                buttons[i].setChecked(false);
                enterDigit(i + 1);
            }
        }
    }
    @Override public void buttonPressed(Object buttonId) {}
    @Override public void render(float alphaMult) {}
    @Override public void positionChanged(PositionAPI position) {}
    @Override public void processInput(List<InputEventAPI> events) {}
}
