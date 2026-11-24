package data.kaysaar.aotd.vok.scripts.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

public class AoTDAshPadTooltip implements TooltipMakerAPI.TooltipCreator {
    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return true;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return 500f;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        tooltip.addTitle("Ashpad 2.0", Global.getSettings().getFactionSpec(Factions.PLAYER).getBrightUIColor());
        tooltip.addPara("An even better, more refined version of the TriPad, developed and designed by %s.", 5f, Color.ORANGE, "Sophia Anderson");
        tooltip.addPara("Usage without authorization will result in receiving a lethal dose of alcohol directly into your bloodstream.", 5f);
        tooltip.addPara("This version of the AshPad still contains some notes Sophia left during design and development.", 5f);

        if (!expanded) return;

        tooltip.addPara("TODO FINAL_final ver. 3.0:", Color.ORANGE, 10f);
        tooltip.addPara("- Recruit new %s and %s. %s.", 3f, Color.ORANGE, "R&D directors", "scientists", "TO BE DONE BY END OF CYCLE");
        tooltip.addPara("- Update the %s for %s. Get more %s into testing.", 3f, Color.ORANGE, "Abyssal Research Expansion", "Part II", "abyssal weapons");
        tooltip.addPara("- Get %s off the ground. %s approval pending.", 3f, Color.ORANGE, "Persean Technology Institute", "Wonder");
        tooltip.addPara("- Ask R&D dir. re: %s for %s. Req. #???", 3f, Color.ORANGE, "more funding", "special projects");
        tooltip.addPara("- %s / %s init broken. %s.", 3f, Color.ORANGE, "My ship", "new survey sys.", "FIX ASAP");
        tooltip.addPara("- %s diags -> %s exp. chk uniq. IDs / dupe flags??", 3f, Color.ORANGE, "Fleet R&D", "PCF enc. pool");
        tooltip.addPara("- %s?? wait WHAT??? stat +/-?? %s", 3f, Color.ORANGE, "Balancing", "cfg TBD");
        tooltip.addPara("- %s?? naaah -> %s. %s / shelve.", 3f, Color.ORANGE, "Orion Sector", "next next next major patch", "M+3 MIN");
    }
}
