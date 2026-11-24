package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMazeDialog;
import data.kaysaar.aotd.vok.scripts.pcf.unique.MeetTheSpy;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMazeDialog.*;

public class AoTDFacilityCybermaze extends BaseCommandPlugin {
    public static final String SITE = "$aotd_pcf_cybermaze";
    public static final String BATTLE_STARTED = "$aotd_pcf_cybermaze_battle_started";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        SectorEntityToken target = dialog.getInteractionTarget();
        MarketAPI market = target == null ? null : target.getMarket();
        if (market == null) return false;
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        if (!memory.getBoolean(SITE) || memory.getBoolean(MeetTheSpy.SITE)
                || memory.getBoolean("$aotd_fac_explored") || memory.getBoolean("$aotd_failed_pre_collapse")) return false;

        String action = params.get(0).getString(memoryMap);
        int failures = memory.getInt(FAILURES);
        if (memory.getBoolean(ACCESS)) return showPage(ruleId, dialog, memoryMap, "AoTDCybermazeSuccess");
        if (failures >= MAX_FAILURES) {
            if ("defend".equals(action)) return new AoTDPCFEncounter().initCybermazeBattle(dialog, memoryMap);
            return showPage(ruleId, dialog, memoryMap, "AoTDCybermazeFinalFailure");
        }
        if ("play".equals(action)) {
            if (!memory.contains(SEED)) memory.set(SEED, new Random().nextLong());
            FacilityMazeDialog maze = new FacilityMazeDialog(memory.getLong(SEED), memory, () -> {
                if (dialog.getInteractionTarget() != target) return;
                int currentFailures = memory.getInt(FAILURES);
                String page = "AoTDCybermazeOptions";
                if (memory.getBoolean(ACCESS)) page = "AoTDCybermazeSuccess";
                else if (currentFailures >= MAX_FAILURES) page = "AoTDCybermazeFinalFailure";
                else if (currentFailures > failures) page = "AoTDCybermazeFirstFailure";
                else if (currentFailures > 0) page = "AoTDCybermazeRetry";
                showPage(ruleId, dialog, memoryMap, page);
            });
            dialog.getOptionPanel().clearOptions();
            maze.show();
            return true;
        }
        if (failures > 0) return showPage(ruleId, dialog, memoryMap, "AoTDCybermazeRetry");
        if ("proceed".equals(action)) return showPage(ruleId, dialog, memoryMap, "AoTDCybermazeOptions");
        if ("tutorial".equals(action)) return showPage(ruleId, dialog, memoryMap, "AoTDCybermazeWalkthrough");
        if ("enter".equals(action)) return showPage(ruleId, dialog, memoryMap, "AoTDCybermazeEntrance");
        return false;
    }

    public static void reportDefendersDefeated(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        dialog.getInteractionTarget().getMarket().getMemoryWithoutUpdate().set(ACCESS, true);
        showPage(null, dialog, memoryMap, "AoTDCybermazeSuccess");
    }

    private static boolean showPage(String ruleId, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, String trigger) {
        dialog.getOptionPanel().clearOptions();
        return FireBest.fire(ruleId, dialog, memoryMap, trigger);
    }
}
