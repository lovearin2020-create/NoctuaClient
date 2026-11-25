package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.Setting;
import dev.lumen.fabric.core.TextHud;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Many networks report every player's tab-list ping as 0 or 1 ms, so by default the ping is measured: every few
 * seconds a connection to the server's address is opened and closed again, and the time the handshake takes is
 * your round trip. Nothing is sent inside the game connection.
 */
public class Ping extends TextHud {
    private final Setting.Mode source = mode("Source", "Measured", "Measured", "Tab List");
    private final Setting.Num interval = num("Measure Every (seconds)", 5, 2, 15, 1);
    private final long[] samples = new long[4];
    private volatile int count;
    private volatile boolean probing;
    private int ticks = 1000, failures;
    private Object world;

    public Ping() { super("Ping", "Your latency to the server.", START, 124, START, 4); }

    private void sample(long ms) { samples[count % samples.length] = ms; count++; }

    @Override
    public void onTick() {
        if (mc.level != world) { world = mc.level; count = 0; ticks = 1000; }
        if (!source.is("Measured") || mc.player == null || mc.getConnection() == null || mc.hasSingleplayerServer()) return;
        if (probing || failures >= 3 || ++ticks < Math.max(2, interval.i()) * 20) return;
        ticks = 0;
        final java.net.SocketAddress address = mc.getConnection().getConnection().getRemoteAddress();
        if (!(address instanceof java.net.InetSocketAddress a)) return;
        probing = true;
        Thread t = new Thread(() -> {
            try (java.net.Socket socket = new java.net.Socket()) {
                long start = System.nanoTime();
                socket.connect(new java.net.InetSocketAddress(a.getAddress(), a.getPort()), 3000);
                sample((System.nanoTime() - start) / 1000000L);
                failures = 0;
            } catch (Throwable ex) {
                failures++; // after three in a row this network does not allow it; the tab-list value is shown instead
            } finally {
                probing = false;
            }
        }, "Noctua ping");
        t.setDaemon(true);
        t.start();
    }

    private int tabList() {
        try {
            if (mc.getConnection() != null && mc.player != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) return info.getLatency();
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    protected String text(boolean editing) {
        if (mc.player == null || mc.getConnection() == null) return editing ? "0 ms" : null;
        if (!source.is("Measured") || failures >= 3) return tabList() + " ms";
        if (mc.hasSingleplayerServer()) return "0 ms";
        int n = count;
        if (n == 0) return "... ms";
        // The lowest recent sample is the closest to the real ping; the others caught a busy moment.
        long best = Long.MAX_VALUE;
        for (int i = 0; i < Math.min(n, samples.length); i++) best = Math.min(best, samples[i]);
        return best + " ms";
    }
}
