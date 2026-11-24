package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.cybermaze;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Random;

public class FacilityMaze {
    static final int NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8;
    private static final int MAX_PATROL_TILES = 7, MAX_EXIT_PATROL_TILES = 4;
    final int columns, rows;
    final int startRow, finishRow;
    final FacilityMazeCell[][] cells;
    final ArrayList<FacilityMazeCell> route = new ArrayList<>();
    final ArrayList<FacilityMazeGuard> guards = new ArrayList<>();
    int x, y;

    public FacilityMaze(int columns, int rows, Random random) {
        this.columns = columns;
        this.rows = rows;
        startRow = random.nextInt(rows);
        finishRow = random.nextInt(rows);
        x = 0;
        y = startRow;
        cells = new FacilityMazeCell[columns][rows];
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                cells[x][y] = new FacilityMazeCell(x == 0 && y == startRow, x == columns - 1 && y == finishRow);
            }
        }
        route.add(cells[x][y]);
        boolean hasExitPatrol = columns > 1 && rows >= MAX_EXIT_PATROL_TILES;
        int interiorColumns = hasExitPatrol ? columns - 1 : columns;
        for (int row = 0; row < rows; row++) {
            int runStart = 0;
            for (int col = 0; col < interiorColumns; col++) {
                if (col + 1 < interiorColumns && (row == rows - 1 || random.nextInt(100) < 52)) {
                    cells[col][row].passages |= EAST;
                    cells[col + 1][row].passages |= WEST;
                } else if (row + 1 < rows) {
                    int opening = runStart + random.nextInt(col - runStart + 1);
                    cells[opening][row].passages |= NORTH;
                    cells[opening][row + 1].passages |= SOUTH;
                    runStart = col + 1;
                }
            }
        }
        int exitColumn = columns - 1;
        if (hasExitPatrol) {
            for (int row = 1; row < rows; row++) {
                cells[exitColumn][row - 1].passages |= NORTH;
                cells[exitColumn][row].passages |= SOUTH;
            }
            cells[exitColumn - 1][finishRow].passages |= EAST;
            cells[exitColumn][finishRow].passages |= WEST;
        }
        boolean[][] solution = new boolean[columns][rows];
        traceOverride(x, y, solution, null);
        if (hasExitPatrol) {
            FacilityMazeGuard exitGuard = FacilityMazeGuard.pick(this, solution, exitColumn, exitColumn, MAX_EXIT_PATROL_TILES, random);
            if (exitGuard != null) guards.add(exitGuard);
        }
        for (int half = 0; half < 2; half++) {
            int first = half == 0 ? 3 : columns / 2;
            int last = half == 0 ? columns / 2 - 1 : columns - 4;
            FacilityMazeGuard guard = FacilityMazeGuard.pick(this, solution, first, last, MAX_PATROL_TILES, random);
            if (guard != null) guards.add(guard);
        }
    }

    public boolean canMove(int direction) {
        return (cells[x][y].passages & direction) != 0 && !unlocked();
    }

    public int targetX(int direction) {
        if (direction == EAST) return x + 1;
        if (direction == WEST) return x - 1;
        return x;
    }

    public int targetY(int direction) {
        if (direction == NORTH) return y + 1;
        if (direction == SOUTH) return y - 1;
        return y;
    }

    public boolean isBacktracking(int direction) {
        return canMove(direction) && route.size() > 1 && cells[targetX(direction)][targetY(direction)] == route.get(route.size() - 2);
    }

    public void move(int direction) {
        if (!canMove(direction)) return;
        boolean backtracking = isBacktracking(direction);
        x = targetX(direction);
        y = targetY(direction);
        if (backtracking) route.remove(route.size() - 1);
        else route.add(cells[x][y]);
        cells[x][y].hasBeenVisited = true;
    }

    public ArrayDeque<Integer> planOverride(int startX, int startY) {
        ArrayDeque<Integer> steps = new ArrayDeque<>();
        traceOverride(startX, startY, new boolean[columns][rows], steps);
        return steps;
    }

    public boolean traceOverride(int col, int row, boolean[][] scanned, ArrayDeque<Integer> steps) {
        scanned[col][row] = true;
        if (cells[col][row].isFinishingPoint) return true;
        int towardFinish = row < finishRow ? NORTH : SOUTH;
        for (int direction : new int[]{EAST, towardFinish, towardFinish == NORTH ? SOUTH : NORTH, WEST}) {
            if ((cells[col][row].passages & direction) == 0) continue;
            int nextX = col + (direction == EAST ? 1 : direction == WEST ? -1 : 0);
            int nextY = row + (direction == NORTH ? 1 : direction == SOUTH ? -1 : 0);
            if (scanned[nextX][nextY]) continue;
            if (steps != null) steps.addLast(direction);
            if (traceOverride(nextX, nextY, scanned, steps)) return true;
            if (steps != null) steps.addLast(direction == NORTH ? SOUTH : direction == SOUTH ? NORTH : direction == EAST ? WEST : EAST);
        }
        if (steps == null) scanned[col][row] = false;
        return false;
    }

    public boolean unlocked() {
        return cells[x][y].isFinishingPoint;
    }
}
