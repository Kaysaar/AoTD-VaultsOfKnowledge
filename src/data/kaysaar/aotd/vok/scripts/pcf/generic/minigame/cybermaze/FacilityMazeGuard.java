package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze.FacilityMaze.*;

public class FacilityMazeGuard {
    private final ArrayList<Point> patrol;
    private int step = 1;

    private FacilityMazeGuard(ArrayList<Point> patrol) {
        this.patrol = patrol;
    }

    public static FacilityMazeGuard pick(FacilityMaze maze, boolean[][] solution, int first, int last, int maxPatrolTiles, Random random) {
        ArrayList<Point> picked = null;
        int longest = 3, candidates = 0;
        for (int x = first; x <= last; x++) {
            for (int y = 0; y < maze.rows; y++) {
                if (!solution[x][y]) continue;
                Point start = new Point(x, y);
                if (maze.guards.stream().anyMatch(guard -> guard.isNearCrossing(start))) continue;
                ArrayList<Point> patrol = new ArrayList<>();
                Set<Point> visited = new HashSet<>();
                patrol.add(start);
                visited.add(start);
                tracePatrol(maze, solution, first, last, maxPatrolTiles, start, visited, patrol);
                patrol.remove(patrol.size() - 1);
                if (visited.size() < longest) continue;
                if (visited.stream().noneMatch(tile -> Math.abs(tile.x - start.x) + Math.abs(tile.y - start.y) > 1)) continue;
                if (visited.size() > longest) {
                    longest = visited.size();
                    candidates = 0;
                }
                if (random.nextInt(++candidates) == 0) picked = patrol;
            }
        }
        return picked == null ? null : new FacilityMazeGuard(picked);
    }

    private boolean isNearCrossing(Point crossing) {
        Point start = patrol.get(0);
        return Math.abs(start.x - crossing.x) + Math.abs(start.y - crossing.y) < 4;
    }

    private static void tracePatrol(FacilityMaze maze, boolean[][] solution, int first, int last, int maxPatrolTiles, Point tile,
                                    Set<Point> visited, ArrayList<Point> patrol) {
        for (int direction : new int[]{NORTH, SOUTH, EAST, WEST}) {
            if (visited.size() == maxPatrolTiles) break;
            if ((maze.cells[tile.x][tile.y].passages & direction) == 0) continue;
            int x = tile.x + (direction == EAST ? 1 : direction == WEST ? -1 : 0);
            int y = tile.y + (direction == NORTH ? 1 : direction == SOUTH ? -1 : 0);
            if (x < first || x > last || solution[x][y]) continue;
            Point next = new Point(x, y);
            if (!visited.add(next)) continue;
            patrol.add(next);
            tracePatrol(maze, solution, first, last, maxPatrolTiles, next, visited, patrol);
            patrol.add(tile);
        }
    }

    public int x() { return patrol.get(step).x; }
    public int y() { return patrol.get(step).y; }
    public int dx() { return patrol.get((step + 1) % patrol.size()).x - x(); }
    public int dy() { return patrol.get((step + 1) % patrol.size()).y - y(); }

    public int sight(FacilityMaze maze) {
        int dx = dx(), dy = dy();
        int direction = dx > 0 ? EAST : dx < 0 ? WEST : dy > 0 ? NORTH : SOUTH;
        int distance = 0;
        while (distance < 2 && (maze.cells[x() + distance * dx][y() + distance * dy].passages & direction) != 0) distance++;
        return distance;
    }

    public boolean sees(FacilityMaze maze, int px, int py) {
        int distance = Math.abs(px - x()) + Math.abs(py - y());
        return distance <= sight(maze) && px == x() + distance * dx() && py == y() + distance * dy();
    }

    public boolean advance(FacilityMaze maze, int px, int py) {
        boolean seen = sees(maze, px, py);
        step = (step + 1) % patrol.size();
        return seen || sees(maze, px, py);
    }
}
