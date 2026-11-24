package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.unique.MeetTheSpy;
import data.kaysaar.aotd.vok.scripts.pcf.unique.SpyVaultKeypad;

import java.util.List;
import java.util.Map;

public class AoTDSpyKeypad extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        MemoryAPI memory = dialog.getInteractionTarget().getMarket().getMemoryWithoutUpdate();
        if (!memory.getBoolean(MeetTheSpy.SITE) || memory.getBoolean("$aotd_fac_explored")) return false;
        int hits = memory.getInt(MeetTheSpy.HITS);
        if ("mash".equals(params.get(0).getString(memoryMap))) {
            if (hits >= 4 || memory.getBoolean(MeetTheSpy.CODE)) return false;
            memory.set(MeetTheSpy.HITS, ++hits);
            dialog.getTextPanel().addPara(hits == 4 ? "CONFIR-" : "DISTORTED BEEP",
                    hits == 4 ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor());
            if (hits < 4) dialog.getTextPanel().addPara("Something ticks faintly behind the panel.");
            return FireBest.fire(ruleId, dialog, memoryMap, hits == 4 ? "AoTDSpyForced" : "AoTDSpyKeys");
        }
        boolean canMash = Global.getSector().getPlayerStats().getStoryPoints() >= 1;
        new SpyVaultKeypad(dialog, canMash);
        if (hits >= 4) return true;
        if (!memory.getBoolean(MeetTheSpy.CODE)) {
            dialog.getOptionPanel().setEnabled("aotdSpyOpen", false);
        } else if (canMash) {
            dialog.getOptionPanel().setEnabled("aotdSpyMash", false);
        }
        return true;
    }
}
