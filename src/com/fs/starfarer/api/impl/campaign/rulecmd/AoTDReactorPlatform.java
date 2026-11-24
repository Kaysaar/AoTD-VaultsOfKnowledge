package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.campaign.econ.colonydevelopment.impl.HexagonPlatform;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor.ReactorPuzzle;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor.ReactorPuzzleDialog;

import java.util.List;
import java.util.Map;

public class AoTDReactorPlatform extends BaseCommandPlugin {
    public static final String SITE = "$aotd_pcf_reactor";
    public static final String COMPLETE = "$aotd_pcf_reactor_complete";
    public static final String FLOOD = "$aotd_pcf_reactor_flood";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        SectorEntityToken target = dialog.getInteractionTarget();
        if (target == null || target.getMarket() == null) return false;
        MemoryAPI memory = target.getMarket().getMemoryWithoutUpdate();
        if (!memory.getBoolean(SITE) || memory.getBoolean("$aotd_fac_explored")) return false;

        if (memory.getBoolean(COMPLETE)) {
            showResult(ruleId, dialog, memoryMap, memory.getFloat(FLOOD));
            return true;
        }
        if (!"play".equals(params.get(0).getString(memoryMap))) return false;

        ReactorPuzzleDialog puzzle = new ReactorPuzzleDialog(result -> {
            if (dialog.getInteractionTarget() != target) return;
            if (result.isComplete()) {
                float flood = result.getFlood();
                memory.set(FLOOD, flood);
                memory.set(COMPLETE, true);
                showResult(ruleId, dialog, memoryMap, flood);
            } else {
                dialog.getOptionPanel().clearOptions();
                FireBest.fire(ruleId, dialog, memoryMap, "AoTDReactorReady");
            }
        });
        if (!puzzle.show()) return false;
        dialog.getOptionPanel().clearOptions();
        return true;
    }

    private void showResult(String ruleId, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, float flood) {
        dialog.getOptionPanel().clearOptions();
        FireBest.fire(ruleId, dialog, memoryMap, "AoTDReactor" + ReactorPuzzle.getOutcome(flood));
        if (flood >= 1f) return;
        dialog.getTextPanel().addPara("Before the salvage operation begins, your team recovers a databank containing the Domain's plans for settling ocean worlds.");
        MemoryAPI sector = Global.getSector().getMemoryWithoutUpdate();
        if (!sector.getBoolean(HexagonPlatform.UNLOCKED)) {
            sector.set(HexagonPlatform.UNLOCKED, true);
            dialog.getTextPanel().addPara("Unlocked new plan %s.", Misc.getPositiveHighlightColor(), "Hexagon Platform");
        }
    }
}
