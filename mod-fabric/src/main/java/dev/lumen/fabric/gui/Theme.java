package dev.lumen.fabric.gui;

import dev.lumen.fabric.core.Client;

/** Colours of the mod menu. "Dark" is solid; "Glass" lets the (blurred) game show through rounded bubbles. */
public final class Theme {
    public static boolean glass;
    public static int bg, side, card, cardHover, line, outline, ctl, ctlHover, track, bar, barHover, dim, edge;
    public static float radius;

    static { apply(); }

    private Theme() {}

    public static void apply() {
        glass = Client.theme.is("Glass");
        if (glass) {
            bg = 0xA60C0E13; side = 0x30000000; card = 0x22FFFFFF; cardHover = 0x3AFFFFFF; line = 0x22FFFFFF; outline = 0x40FFFFFF;
            ctl = 0x24FFFFFF; ctlHover = 0x40FFFFFF; track = 0x40FFFFFF; bar = 0x1CFFFFFF; barHover = 0x36FFFFFF; dim = 0x38000000; edge = 0x2EFFFFFF;
            radius = 11;
        } else {
            bg = 0xF20F1116; side = 0xFF14171D; card = 0xFF1A1D25; cardHover = 0xFF222733; line = 0xFF20242D; outline = 0xFF2B313D;
            ctl = 0xFF262B36; ctlHover = 0xFF303644; track = 0xFF343A47; bar = 0xFF2A2F3A; barHover = 0xFF3A4150; dim = 0x99000000; edge = 0;
            radius = 6;
        }
    }
}
