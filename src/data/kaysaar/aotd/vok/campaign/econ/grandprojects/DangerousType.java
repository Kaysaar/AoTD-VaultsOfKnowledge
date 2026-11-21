package data.kaysaar.aotd.vok.campaign.econ.grandprojects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.awt.Color;

public class DangerousType extends TechnologyType {
    @Override
    public String getId() {
        return "dangerous";
    }

    @Override
    public String getName() {
        return "Dangerous";
    }

    @Override
    public Color getColor() {
        return Global.getSettings().getFactionSpec(Factions.DWELLER).getBaseUIColor();
    }
}
