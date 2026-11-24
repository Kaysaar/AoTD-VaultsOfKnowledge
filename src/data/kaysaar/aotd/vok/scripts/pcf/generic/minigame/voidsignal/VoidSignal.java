package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.voidsignal;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class VoidSignal {
    static final int ANTENNA_ROW = -2;
    private final int gridSize;
    private final Point[] aim, targets;
    private final double[] maxDistances;

    public VoidSignal(Random random) {
        aim = new Point[3 + random.nextInt(3)];
        gridSize = 17 + 2 * (aim.length - 3);
        targets = new Point[aim.length];
        maxDistances = new double[aim.length];
        int crossingSpread = aim.length == 3 ? 2 : 4;
        Point crossing = new Point(gridSize / 2 - crossingSpread + 2 * random.nextInt(crossingSpread + 1),
                4 + 2 * random.nextInt(3));
        Set<Point> usedCells = new HashSet<>();
        Set<Integer> usedProximities = new HashSet<>();
        for (int i = 0; i < aim.length; i++) {
            Point target = pickTarget(i, crossing, random);
            targets[i] = target;
            maxDistances[i] = Math.hypot(Math.max(target.x, gridSize - 1 - target.x),
                    Math.max(target.y, gridSize - 1 - target.y));
            aim[i] = pickStartingAim(i, usedCells, usedProximities, random);
        }
    }

    private Point pickTarget(int antenna, Point crossing, Random random) {
        int stepColumn = (crossing.x - getAntennaColumn(antenna)) / 2;
        int stepRow = (crossing.y - ANTENNA_ROW) / 2;
        int maxSteps = (gridSize - 1 - crossing.y) / stepRow;
        if (stepColumn < 0) maxSteps = Math.min(maxSteps, crossing.x / -stepColumn);
        else if (stepColumn > 0) maxSteps = Math.min(maxSteps, (gridSize - 1 - crossing.x) / stepColumn);
        int steps = 1 + random.nextInt(maxSteps);
        return new Point(crossing.x + stepColumn * steps, crossing.y + stepRow * steps);
    }

    private Point pickStartingAim(int antenna, Set<Point> usedCells, Set<Integer> usedProximities, Random random) {
        Map<Integer, List<Point>> cellsByProximity = new TreeMap<>();
        for (int column = 0; column < gridSize; column++) {
            for (int row = 0; row < gridSize; row++) {
                Point cell = new Point(column, row);
                int proximity = (int) (getSignalProximity(antenna, column, row) * 100f);
                if (proximity < 100 && !usedCells.contains(cell) && !usedProximities.contains(proximity)) {
                    cellsByProximity.computeIfAbsent(proximity, value -> new ArrayList<>()).add(cell);
                }
            }
        }
        List<Map.Entry<Integer, List<Point>>> distances = new ArrayList<>(cellsByProximity.entrySet());
        int first = antenna * distances.size() / aim.length;
        int limit = (antenna + 1) * distances.size() / aim.length;
        Map.Entry<Integer, List<Point>> group = distances.get(first + random.nextInt(limit - first));
        List<Point> cells = group.getValue();
        Point cell = cells.get(random.nextInt(cells.size()));
        usedCells.add(cell);
        usedProximities.add(group.getKey());
        return cell;
    }

    public int getAntennaCount() {
        return aim.length;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getAntennaColumn(int antenna) {
        int halfSpacing = aim.length == 3 ? 3 : 2;
        return gridSize / 2 + (2 * antenna - aim.length + 1) * halfSpacing;
    }

    public int getAimColumn(int antenna) {
        return aim[antenna].x;
    }

    public int getAimRow(int antenna) {
        return aim[antenna].y;
    }

    public void moveAim(int antenna, int dx, int dy) {
        Point point = aim[antenna];
        point.setLocation(Math.max(0, Math.min(gridSize - 1, point.x + dx)),
                Math.max(0, Math.min(gridSize - 1, point.y + dy)));
    }

    public float getSignalProximity(int antenna, float column, float row) {
        return 1f - (float) (targets[antenna].distance(column, row) / maxDistances[antenna]);
    }

    public boolean isAligned(int antenna) {
        return aim[antenna].equals(targets[antenna]);
    }

    public int getAlignedCount() {
        int aligned = 0;
        for (int i = 0; i < aim.length; i++) {
            if (isAligned(i)) aligned++;
        }
        return aligned;
    }
}
