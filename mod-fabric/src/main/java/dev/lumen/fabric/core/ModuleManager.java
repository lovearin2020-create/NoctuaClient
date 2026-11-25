package dev.lumen.fabric.core;

import dev.lumen.fabric.mods.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModuleManager {
    public final List<Module> all = new ArrayList<>();
    public final Set<Module> defaultOn = new HashSet<>();

    public ModuleManager() {
        // HUD
        add(new Fps(), true); add(new Cps(), true); add(new Keystrokes(), true); add(new Coordinates(), true);
        add(new ArmorStatus(), true); add(new PotionEffects(), true); add(new InventoryHud(), false); add(new Ping(), false);
        add(new Clock(), false); add(new ServerAddress(), false); add(new Speedometer(), false); add(new Memory(), false);
        add(new Combo(), false); add(new Reach(), false); add(new PotCounter(), false); add(new ToggleSprint(), true);
        add(new ScoreboardMod(), false); add(new BossBarMod(), false);
        add(new Cooldowns(), false); add(new TargetHud(), false); add(new Vibrance(), false); add(new GameFont(), false);
        add(new ToggleSneak(), false); add(new ItemCounter(), false); add(new DirectionHud(), false);
        add(new ResourceTimer(), false); add(new ResourceCount(), false); add(new SessionStats(), false); add(new BowCharge(), false); add(new BlockCount(), false);
        add(new Saturation(), false); add(new TabEditor(), false); add(new Titles(), false); add(new PackDisplay(), false);
        // Hypixel
        add(new AutoGG(), true); add(new AutoTip(), true); add(new QuickPlay(), true);
        // Visual
        add(new OldAnimations(), true); add(new ScrollableTooltips(), true);
        add(new Zoom(), true); add(new Fullbright(), false);
        add(new TimeChanger(), false); add(new Crosshair(), false); add(new BlockOverlay(), false); add(new Nametags(), false);
        add(new ChatTweaks(), false); add(new HurtCam(), false); add(new FovTweaks(), false); add(new FireOverlay(), false);
        add(new ItemPhysics(), true); add(new HitColor(), false); add(new TntTimer(), true); add(new Hitboxes(), false); add(new Particles(), false);
        add(new MenuBlur(), true); add(new MotionBlur(), false); add(new CleanView(), false); add(new MainMenu(), true);
        add(new Screenshots(), true); add(new NickHider(), false); add(new KoreanInput(), true);
        // Cosmetics
        // Performance
        add(new Performance(), true); add(new Items2D(), false); add(new MemoryOptimizer(), true); add(new CrashPatcher(), true);
    }

    private void add(Module m, boolean on) { all.add(m); if (on) defaultOn.add(m); }

    public List<Module> of(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (!m.hidden && (c == null || m.category == c)) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : all) if (m.getClass() == type) return (T) m;
        return null;
    }
}
