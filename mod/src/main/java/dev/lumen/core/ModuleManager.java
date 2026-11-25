package dev.lumen.core;

import dev.lumen.mods.*;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModuleManager {
    public final List<Module> all = new ArrayList<Module>();
    public final Set<Module> defaultOn = new HashSet<Module>();

    public ModuleManager() {
        // HUD
        on(SimpleHuds.fps()); on(SimpleHuds.cps()); on(new Keystrokes()); on(new Coordinates());
        on(new ArmorStatus()); on(new PotionEffects()); add(new InventoryHud()); add(new PingHud());
        add(SimpleHuds.clock()); add(SimpleHuds.server()); add(SimpleHuds.speed()); add(SimpleHuds.memory());
        add(new ComboCounter()); add(new ReachDisplay()); add(new PotCounter()); on(new ToggleSprint());
        add(new ScoreboardMod()); add(new BossBarMod());
        add(new Cooldowns()); add(new TargetHud()); add(new ToggleSneak()); add(new ItemCounter()); add(new DirectionHud());
        add(new ResourceTimer()); add(new ResourceCount()); add(new SessionStats()); add(new StatsOverlay()); add(new BowCharge()); add(new BlockCount());
        add(new Saturation()); add(new TabEditor()); add(new Titles()); add(new PackDisplay());
        // Hypixel
        on(new AutoGG()); on(new AutoTip()); on(new QuickPlay());
        // Visual
        on(new OldAnimations()); on(new ScrollableTooltips());
        // OptiFine already has its own zoom on the same key, so ours starts disabled when it is installed.
        if (FMLClientHandler.instance().hasOptifine()) add(new Zoom()); else on(new Zoom()); add(new Fullbright());
        add(new TimeChanger()); add(new Crosshair()); add(new BlockOverlay()); add(new Nametags());
        add(new ChatTweaks()); add(new HurtCam()); add(new FovTweaks()); add(new FireOverlay());
        on(new ItemPhysics()); add(new HitColor()); on(new TntTimer()); add(new Hitboxes()); add(new Particles());
        add(new MenuBlur()); add(new MotionBlur()); add(new CleanView()); on(new MainMenu());
        on(new Screenshots()); add(new NickHider()); add(new Vibrance()); add(new GameFont());
        on(new KoreanInput());
        // Cosmetics
        // Performance
        on(new Performance()); add(new Items2D()); on(new MemoryOptimizer()); on(new CrashPatcher());
    }

    private void add(Module m) { all.add(m); }
    private void on(Module m) { all.add(m); defaultOn.add(m); }

    public List<Module> of(Category c) {
        List<Module> out = new ArrayList<Module>();
        for (Module m : all) if (!m.hidden && (c == null || m.category == c)) out.add(m);
        return out;
    }
}
