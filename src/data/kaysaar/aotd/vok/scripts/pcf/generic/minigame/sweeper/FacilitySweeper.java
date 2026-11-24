package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.sweeper;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FacilitySweeper {
    static final int COLUMNS = 24, ROWS = 16;
    static final int CLOSED = 0, OPEN = 1, FLAGGED = 2, BLASTED = 3;
    static final int HIDDEN = 0, DISCOVERED = 1, LOST = 2;
    private static final int MINE_COUNT = 56;
    private final boolean[][] mines = new boolean[COLUMNS][ROWS];
    private final boolean[][] detonatedMines = new boolean[COLUMNS][ROWS];
    private final int[][] tiles = new int[COLUMNS][ROWS];
    final List<Rectangle> caches = new ArrayList<>();
    private final List<Integer> cacheSizes;
    private final Random random;
    private final int[] cacheStates;
    private final int guardedCache, databankCache;
    private boolean started, withdrawn, disarmed;

    public FacilitySweeper(List<Integer> cacheSizes, Random random) {
        this(cacheSizes, random, -1, -1);
    }

    FacilitySweeper(List<Integer> cacheSizes, Random random, int guardedCache, int databankCache) {
        this.cacheSizes = List.copyOf(cacheSizes);
        this.random = random;
        this.guardedCache = guardedCache;
        this.databankCache = databankCache;
        cacheStates = new int[cacheSizes.size()];
    }

    public boolean hasStarted() {
        return started;
    }

    public boolean isFinished() {
        return withdrawn || (started && countCaches(HIDDEN) == 0);
    }

    public void cutLosses() {
        if (started && !isFinished()) withdrawn = true;
    }

    void disarmMines() {
        if (isFinished()) return;
        if (!started) initializeSearch(COLUMNS / 2, ROWS / 2);
        disarmed = true;
        for (int[] column : tiles) Arrays.fill(column, OPEN);
        for (int i = 0; i < cacheStates.length; i++) {
            if (cacheStates[i] == HIDDEN) cacheStates[i] = DISCOVERED;
        }
    }

    boolean isDisarmed() {
        return disarmed;
    }

    private void initializeSearch(int startX, int startY) {
        Rectangle entrance = new Rectangle(startX - 1, startY - 1, 3, 3);
        Rectangle guardedArea = guardedCache < 0 ? null
                : placePriorityCache(cacheSizes.get(guardedCache), entrance, startX, startY, true);
        Rectangle databankArea = databankCache < 0 ? null
                : placePriorityCache(cacheSizes.get(databankCache), entrance, startX, startY, false);
        Rectangle databankPerimeter = databankArea == null ? null : new Rectangle(databankArea);
        if (databankPerimeter != null) databankPerimeter.grow(1, 1);
        for (int i = 0; i < cacheSizes.size(); i++) {
            if (i == guardedCache) {
                caches.add(guardedArea);
                continue;
            }
            if (i == databankCache) {
                caches.add(databankArea);
                continue;
            }
            int size = cacheSizes.get(i);
            List<Rectangle> positions = new ArrayList<>();
            for (int x = 0; x <= COLUMNS - size; x++) {
                for (int y = 0; y <= ROWS - size; y++) {
                    Rectangle perimeter = new Rectangle(x - 1, y - 1, size + 2, size + 2);
                    if (!entrance.intersects(perimeter) && (guardedArea == null || !perimeter.intersects(guardedArea))
                            && (databankArea == null || !perimeter.intersects(databankArea))
                            && caches.stream().noneMatch(perimeter::intersects)) {
                        positions.add(new Rectangle(x, y, size, size));
                    }
                }
            }
            caches.add(positions.get(random.nextInt(positions.size())));
        }
        List<Point> positions = new ArrayList<>();
        for (int x = 0; x < COLUMNS; x++) {
            for (int y = 0; y < ROWS; y++) {
                Point tile = new Point(x, y);
                if (entrance.contains(tile)) tiles[x][y] = OPEN;
                else if ((databankPerimeter == null || !databankPerimeter.contains(tile))
                        && caches.stream().noneMatch(area -> area.contains(tile))) positions.add(tile);
            }
        }
        Collections.shuffle(positions, random);
        int remainingMines = MINE_COUNT;
        if (guardedArea != null) remainingMines -= addTraps(positions, guardedArea, 8);
        for (int i = 0; i < caches.size(); i++) {
            if (i != guardedCache && i != databankCache) {
                remainingMines -= addTraps(positions, caches.get(i), Math.min(2, remainingMines));
            }
        }
        for (Point mine : positions.subList(0, remainingMines)) mines[mine.x][mine.y] = true;
        started = true;
    }

    private Rectangle placePriorityCache(int size, Rectangle entrance, int startX, int startY, boolean guarded) {
        List<Rectangle> positions = new ArrayList<>();
        int inset = guarded ? 2 : 0;
        int bestScore = Integer.MAX_VALUE;
        for (int x = inset; x <= COLUMNS - size - inset; x++) {
            for (int y = inset; y <= ROWS - size - inset; y++) {
                Rectangle area = new Rectangle(x, y, size, size);
                if (entrance.intersects(area)) continue;
                int distance = Math.abs(x + size / 2 - startX) + Math.abs(y + size / 2 - startY);
                int score = guarded ? -distance : distance;
                if (score < bestScore) {
                    positions.clear();
                    bestScore = score;
                }
                if (score == bestScore) positions.add(area);
            }
        }
        return positions.get(random.nextInt(positions.size()));
    }

    private int addTraps(List<Point> positions, Rectangle area, int count) {
        Rectangle perimeter = new Rectangle(area);
        perimeter.grow(1, 1);
        List<Point> traps = positions.stream().filter(perimeter::contains).limit(count).toList();
        for (Point mine : traps) mines[mine.x][mine.y] = true;
        positions.removeAll(traps);
        return traps.size();
    }

    public int getTile(int x, int y) {
        return tiles[x][y];
    }

    boolean hasMine(int x, int y) {
        return !disarmed && mines[x][y];
    }

    boolean hasDisarmedMine(int x, int y) {
        return disarmed && mines[x][y];
    }

    boolean hasDetonatedMine(int x, int y) {
        return detonatedMines[x][y];
    }

    public int getCacheState(int index) {
        return cacheStates[index];
    }

    public int countCaches(int state) {
        int count = 0;
        for (int cacheState : cacheStates) {
            if (cacheState == state) count++;
        }
        return count;
    }

    public int nearbyMines(int x, int y) {
        int count = 0;
        for (int nx = Math.max(0, x - 1); nx <= Math.min(COLUMNS - 1, x + 1); nx++) {
            for (int ny = Math.max(0, y - 1); ny <= Math.min(ROWS - 1, y + 1); ny++) {
                if ((nx != x || ny != y) && hasMine(nx, ny)) count++;
            }
        }
        return count;
    }

    public void toggleFlag(int x, int y) {
        if (!started || isFinished()) return;
        if (tiles[x][y] == CLOSED) tiles[x][y] = FLAGGED;
        else if (tiles[x][y] == FLAGGED) tiles[x][y] = CLOSED;
    }

    public boolean open(int x, int y) {
        if (isFinished()) return false;
        if (!started) {
            initializeSearch(x, y);
            return false;
        }
        if (tiles[x][y] != CLOSED) return false;
        if (hasMine(x, y)) return true;
        ArrayDeque<Point> toOpen = new ArrayDeque<>();
        tiles[x][y] = OPEN;
        toOpen.add(new Point(x, y));
        while (!toOpen.isEmpty()) {
            Point tile = toOpen.removeFirst();
            if (nearbyMines(tile.x, tile.y) != 0) continue;
            for (int nx = Math.max(0, tile.x - 1); nx <= Math.min(COLUMNS - 1, tile.x + 1); nx++) {
                for (int ny = Math.max(0, tile.y - 1); ny <= Math.min(ROWS - 1, tile.y + 1); ny++) {
                    if (tiles[nx][ny] != CLOSED) continue;
                    tiles[nx][ny] = OPEN;
                    toOpen.add(new Point(nx, ny));
                }
            }
        }
        for (int i = 0; i < caches.size(); i++) {
            if (cacheStates[i] != HIDDEN) continue;
            Rectangle area = caches.get(i);
            int opened = 0;
            for (int nx = area.x; nx < area.x + area.width; nx++) {
                for (int ny = area.y; ny < area.y + area.height; ny++) {
                    if (tiles[nx][ny] == OPEN) opened++;
                }
            }
            if (opened == area.width * area.height) cacheStates[i] = DISCOVERED;
        }
        return false;
    }

    public void detonate(int x, int y) {
        if (isFinished() || !hasMine(x, y)) return;
        mines[x][y] = false;
        detonatedMines[x][y] = true;
        Rectangle blast = new Rectangle(x - 1, y - 1, 3, 3);
        for (int nx = Math.max(0, blast.x); nx < Math.min(COLUMNS, blast.x + blast.width); nx++) {
            for (int ny = Math.max(0, blast.y); ny < Math.min(ROWS, blast.y + blast.height); ny++) {
                if (!mines[nx][ny] && tiles[nx][ny] != OPEN) tiles[nx][ny] = BLASTED;
            }
        }
        for (int i = 0; i < caches.size(); i++) {
            if (blast.intersects(caches.get(i))) cacheStates[i] = LOST;
        }
    }
}
