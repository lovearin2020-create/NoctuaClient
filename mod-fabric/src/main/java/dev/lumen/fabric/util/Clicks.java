package dev.lumen.fabric.util;

import java.util.ArrayDeque;

/** Rolling one-second click counters for the CPS and Keystrokes mods. */
public final class Clicks {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();

    public static void press(int button) {
        if (button == 0) LEFT.add(System.currentTimeMillis());
        else if (button == 1) RIGHT.add(System.currentTimeMillis());
    }

    private static int count(ArrayDeque<Long> q) {
        long limit = System.currentTimeMillis() - 1000L;
        while (!q.isEmpty() && q.peekFirst() < limit) q.pollFirst();
        return q.size();
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }
}
