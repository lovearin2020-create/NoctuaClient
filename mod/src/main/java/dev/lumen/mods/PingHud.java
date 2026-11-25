package dev.lumen.mods;

import dev.lumen.core.Setting;
import dev.lumen.core.TextHud;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Hypixel and many other networks report every player's tab-list ping as 1 ms, so by default the ping is
 * measured: every few seconds a connection to the server's address is opened and closed again, and the time
 * the handshake takes is your round trip. Nothing is sent inside the game connection. If the address cannot
 * be reached that way, the old method is used instead (asking the server for statistics and timing the reply).
 */
public class PingHud extends TextHud {
    public static PingHud INSTANCE;
    private final Setting.Mode source = mode("Source", "Measured", "Measured", "Tab List");
    private final Setting.Num interval = num("Measure Every (seconds)", 5, 2, 15, 1);
    private final long[] samples = new long[4];
    private volatile int count;
    private int ticks, failures;
    private volatile boolean pending, probing;
    private volatile long sentAt;
    private boolean useStats;

    public PingHud() {
        super("Ping", "Your latency to the server.", START, 4, START, 80);
        INSTANCE = this;
    }

    private void sample(long ms) { samples[count % samples.length] = ms; count++; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !measuring()) { pending = false; return; }
        if (mc.thePlayer == null || mc.getNetHandler() == null) { count = 0; pending = false; return; }
        ticks++;
        if (pending && System.nanoTime() - sentAt > 5000000000L) pending = false; // reply lost
        if (pending || probing || ticks < Math.max(2, interval.i()) * 20) return;
        ticks = 0;
        if (useStats) {
            sentAt = System.nanoTime();
            pending = true;
            mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
            return;
        }
        final java.net.SocketAddress address = mc.getNetHandler().getNetworkManager().getRemoteAddress();
        if (!(address instanceof java.net.InetSocketAddress)) return; // single player
        probing = true;
        Thread t = new Thread(new Runnable() {
            public void run() {
                java.net.Socket socket = new java.net.Socket();
                try {
                    java.net.InetSocketAddress a = (java.net.InetSocketAddress) address;
                    long start = System.nanoTime();
                    socket.connect(new java.net.InetSocketAddress(a.getAddress(), a.getPort()), 3000);
                    sample((System.nanoTime() - start) / 1000000L);
                    failures = 0;
                } catch (Throwable ex) {
                    if (++failures >= 3) useStats = true; // this network does not allow it; fall back for the rest of the session
                } finally {
                    try { socket.close(); } catch (Throwable ignored) {}
                    probing = false;
                }
            }
        }, "Noctua ping");
        t.setDaemon(true);
        t.start();
    }

    /** "Tab List" shows what the server reports and measures nothing. */
    private boolean measuring() { return source.is("Measured"); }

    /** Called from the network thread the moment the statistics reply arrives (fallback method only). */
    public void onStatsReply() {
        if (!pending) return;
        pending = false;
        sample((System.nanoTime() - sentAt) / 1000000L);
    }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { count = 0; ticks = 1000; pending = false; }

    protected String text(boolean editing) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return editing ? "0 ms" : null;
        if (measuring()) {
            if (mc.isSingleplayer()) return "0 ms";
            int n = count;
            if (n == 0) return "... ms";
            // The lowest recent sample is the closest to the real ping; the others caught a busy moment.
            long best = Long.MAX_VALUE;
            for (int i = 0; i < Math.min(n, samples.length); i++) best = Math.min(best, samples[i]);
            return best + " ms";
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return (info == null ? 0 : info.getResponseTime()) + " ms";
    }
}
