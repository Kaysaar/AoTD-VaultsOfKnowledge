package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.util.Misc;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMaze.*;
import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMazeDialog.*;

public class FacilityMazeCell {
    final boolean isStartingPoint, isFinishingPoint;
    boolean hasBeenVisited;
    int passages;
    CustomPanelAPI panel;

    public FacilityMazeCell(boolean isStartingPoint, boolean isFinishingPoint) {
        this.isStartingPoint = isStartingPoint;
        this.isFinishingPoint = isFinishingPoint;
        hasBeenVisited = isStartingPoint;
    }

    public void renderLines(float tile, float alphaMult, boolean firewallsRevealed) {
        float l = panel.getPosition().getX(), b = panel.getPosition().getY();
        float r = l + tile, t = b + tile;
        if (!hasBeenVisited && !firewallsRevealed) return;

        color(Misc.getNegativeHighlightColor(), alphaMult);
        if ((passages & NORTH) == 0) line(l, t, r, t);
        if ((passages & SOUTH) == 0) line(l, b, r, b);
        if ((passages & EAST) == 0) line(r, b, r, t);
        if ((passages & WEST) == 0) line(l, b, l, t);
    }
}
