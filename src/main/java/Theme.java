import java.awt.Color;

public enum Theme {
    LIGHT(Palette.light()),
    DARK(Palette.dark());

    public final Palette palette;

    Theme(Palette palette) {
        this.palette = palette;
    }

    public Theme next() {
        Theme[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    // Full palette lives here
    public static final class Palette {
        public final Base base;
        public final Overlay overlay;

        public Palette(Base base, Overlay overlay) {
            this.base = base;
            this.overlay = overlay;
        }

        // Normal app surfaces (main panels, buttons, inputs, lists)
        public static final class Base {
            public final Color appBg;
            public final Color text;
            public final Color mutedText;

            public final Color controlBg;
            public final Color controlBorder;

            public final Color inputBg;
            public final Color inputBorder;

            public final Color listBg;
            public final Color listSelectionBg;

            public Base(Color appBg, Color text, Color mutedText,
                        Color controlBg, Color controlBorder,
                        Color inputBg, Color inputBorder,
                        Color listBg, Color listSelectionBg) {
                this.appBg = appBg;
                this.text = text;
                this.mutedText = mutedText;
                this.controlBg = controlBg;
                this.controlBorder = controlBorder;
                this.inputBg = inputBg;
                this.inputBorder = inputBorder;
                this.listBg = listBg;
                this.listSelectionBg = listSelectionBg;
            }
        }

        // Overlays/popovers/dialog cards (scrim + card + overlay controls)
        public static final class Overlay {
            public final Color scrim;
            public final Color cardFill;
            public final Color cardStroke;

            public final Color text;

            public final Color buttonBg;
            public final Color buttonBorder;

            public final Color inputBg;
            public final Color inputBorder;

            public Overlay(Color scrim, Color cardFill, Color cardStroke,
                           Color text,
                           Color buttonBg, Color buttonBorder,
                           Color inputBg, Color inputBorder) {
                this.scrim = scrim;
                this.cardFill = cardFill;
                this.cardStroke = cardStroke;
                this.text = text;
                this.buttonBg = buttonBg;
                this.buttonBorder = buttonBorder;
                this.inputBg = inputBg;
                this.inputBorder = inputBorder;
            }
        }

        // Concrete palettes
        public static Palette light() {
            Base base = new Base(
                    new Color(245,245,245),  // appBg
                    new Color(20,20,20),     // text
                    new Color(80,80,80),     // mutedText

                    new Color(235,235,235),  // controlBg
                    new Color(0,0,0,70),     // controlBorder

                    new Color(255,255,255),  // inputBg
                    new Color(0,0,0,70),     // inputBorder

                    new Color(255,255,255),  // listBg
                    new Color(210,210,210)   // listSelectionBg
            );

            Overlay overlay = new Overlay(
                    new Color(0,0,0,90),         // scrim
                    new Color(255,255,255,235),  // cardFill
                    new Color(0,0,0,90),         // cardStroke
                    new Color(20,20,20),         // text
                    new Color(235,235,235),      // buttonBg
                    new Color(0,0,0,70),         // buttonBorder
                    new Color(255,255,255),      // inputBg
                    new Color(0,0,0,70)          // inputBorder
            );

            return new Palette(base, overlay);
        }

        public static Palette dark() {
            Base base = new Base(
                    new Color(32,32,32),
                    new Color(235,235,235),
                    new Color(170,170,170),

                    new Color(45,45,45),
                    new Color(120,120,120),

                    new Color(25,25,25),
                    new Color(70,70,70),

                    new Color(22,22,22),
                    new Color(90,90,90)
            );

            Overlay overlay = new Overlay(
                    new Color(0,0,0,120),
                    new Color(0,0,0,220),
                    new Color(220,220,220,160),
                    new Color(235,235,235),
                    new Color(30,30,30),
                    new Color(120,120,120),
                    new Color(25,25,25),
                    new Color(70,70,70)
            );

            return new Palette(base, overlay);
        }
    }
}
