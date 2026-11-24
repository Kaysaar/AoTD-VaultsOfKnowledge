package data.kaysaar.aotd.vok.scripts.pcf.generic.minigame.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReactorPuzzle {
    private static final int[][] LINK_PATTERN = {{2, 3}, {0, 5}, {4}, {4, 5}, {}, {2, 4}};
    public static final int ROD_COUNT = LINK_PATTERN.length;
    private static final float FLOOD_SECONDS = 120f;
    private static final float FLOOD_PER_DROPPED_ROD = 0.06f;
    private static final float PUMP_RATE = 0.4f;
    private final int[][] links = new int[ROD_COUNT][];
    private int raised;
    private float flood;
    private boolean pumping;

    public ReactorPuzzle() {
        List<Integer> rods = new ArrayList<>(ROD_COUNT);
        for (int i = 0; i < ROD_COUNT; i++) rods.add(i);
        Collections.shuffle(rods);

        for (int i = 0; i < ROD_COUNT; i++) {
            int[] linked = LINK_PATTERN[i].clone();
            for (int j = 0; j < linked.length; j++) linked[j] = rods.get(linked[j]);
            links[rods.get(i)] = linked;
        }
    }

    public boolean isRaised(int rod) {
        return (raised & (1 << rod)) != 0;
    }

    public boolean isLinked(int rod, int target) {
        for (int linked : links[rod]) {
            if (linked == target) return true;
        }
        return false;
    }

    public void pull(int rod) {
        if (pumping || isRaised(rod)) return;
        raised |= 1 << rod;
        for (int linked : links[rod]) {
            if (isRaised(linked)) {
                raised &= ~(1 << linked);
                flood = Math.min(1f, flood + FLOOD_PER_DROPPED_ROD);
            }
        }
    }

    public int getRaisedCount() {
        return Integer.bitCount(raised);
    }

    public void startPumps() {
        if (!isComplete()) pumping = true;
    }

    public boolean isPumping() {
        return pumping;
    }

    public boolean isComplete() {
        return getRaisedCount() == ROD_COUNT;
    }

    public void advance(float amount) {
        if (pumping) {
            flood = Math.max(0f, flood - amount * PUMP_RATE);
            if (flood == 0f) {
                raised = (1 << ROD_COUNT) - 1;
                pumping = false;
            }
        } else if (!isComplete()) {
            flood = Math.min(1f, flood + amount / FLOOD_SECONDS);
        }
    }

    public float getFlood() {
        return flood;
    }

    public float getSalvageFraction() {
        return getSalvageFraction(flood);
    }

    public static float getSalvageFraction(float flood) {
        return 1f - flood * 0.75f;
    }

    public static String getOutcome(float flood) {
        if (flood < 0.25f) return "Good";
        if (flood < 0.75f) return "Okay";
        return "Bad";
    }
}
