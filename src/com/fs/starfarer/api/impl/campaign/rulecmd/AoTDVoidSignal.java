package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.voidsignal.VoidSignalDialog;

import java.util.List;
import java.util.Map;

public class AoTDVoidSignal extends BaseCommandPlugin {
    public static final String SITE = "$aotd_pcf_void_signal";
    public static final String COMPLETE = "$aotd_pcf_void_signal_complete";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty() || !"play".equals(params.get(0).getString(memoryMap))) return false;
        if (!(dialog.getInteractionTarget() instanceof PlanetAPI planet) || planet.getMarket() == null) return false;
        MemoryAPI memory = planet.getMarket().getMemoryWithoutUpdate();
        if (!memory.getBoolean(SITE) || memory.getBoolean("$aotd_fac_explored")
                || memory.getBoolean("$aotd_failed_pre_collapse")) return false;
        if (memory.getBoolean(COMPLETE)) return FireBest.fire(ruleId, dialog, memoryMap, "AoTDVoidSignalResult");

        VoidSignalDialog puzzle = new VoidSignalDialog(Misc.getRandom(Misc.getSalvageSeed(planet), 97), complete -> {
            if (dialog.getInteractionTarget() != planet) return;
            if (complete) memory.set(COMPLETE, true);
            FireBest.fire(ruleId, dialog, memoryMap, complete ? "AoTDVoidSignalResult" : "AoTDVoidSignalReady");
        });
        if (!puzzle.show()) return FireBest.fire(ruleId, dialog, memoryMap, "AoTDVoidSignalReady");
        dialog.getOptionPanel().clearOptions();
        return true;
    }
}
