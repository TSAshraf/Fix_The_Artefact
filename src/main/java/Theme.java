import java.awt.Color;
import java.awt.Font;

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

    public static final class Palette {
        public final Base base;
        public final Overlay overlay;
        public final Fonts fonts;

        public Palette(Base base, Overlay overlay, Fonts fonts) {
            this.base = base;
            this.overlay = overlay;
            this.fonts = fonts;
        }

        public static final class Fonts {
            public final Font title;
            public final Font heading;
            public final Font body;
            public final Font bodyBold;
            public final Font caption;
            public final Font button;

            public Fonts(Font title, Font heading, Font body, Font bodyBold, Font caption, Font button) {
                this.title = title;
                this.heading = heading;
                this.body = body;
                this.bodyBold = bodyBold;
                this.caption = caption;
                this.button = button;
            }
        }

        public static final class Base {
            public final Color appBg;
            public final Color text;
            public final Color mutedText;

            public final Color controlBg;
            public final Color controlBorder;
            public final Color controlHoverBg;

            public final Color inputBg;
            public final Color inputBorder;

            public final Color listBg;
            public final Color listSelectionBg;

            public final Color separatorColor;

            // Semantic accents
            public final Color successGreen;
            public final Color narrativeText; // warm colour for micro-narrative / contextual text

            public Base(Color appBg, Color text, Color mutedText,
                        Color controlBg, Color controlBorder, Color controlHoverBg,
                        Color inputBg, Color inputBorder,
                        Color listBg, Color listSelectionBg,
                        Color separatorColor,
                        Color successGreen, Color narrativeText) {
                this.appBg = appBg;
                this.text = text;
                this.mutedText = mutedText;
                this.controlBg = controlBg;
                this.controlBorder = controlBorder;
                this.controlHoverBg = controlHoverBg;
                this.inputBg = inputBg;
                this.inputBorder = inputBorder;
                this.listBg = listBg;
                this.listSelectionBg = listSelectionBg;
                this.separatorColor = separatorColor;
                this.successGreen = successGreen;
                this.narrativeText = narrativeText;
            }
        }

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

        private static Fonts defaultFonts() {
            return new Fonts(
                    new Font("Serif", Font.BOLD, 26), // title
                    new Font("Serif", Font.BOLD, 20), // heading
                    new Font("Serif", Font.PLAIN, 17), // body
                    new Font("Serif", Font.BOLD, 17), // bodyBold
                    new Font("Serif", Font.PLAIN, 14), // caption
                    new Font("Serif", Font.BOLD, 16) // button
            );
        }

        public static Palette light() {
            Base base = new Base(
                    new Color(245, 245, 245), // appBg
                    new Color(20, 20, 20), // text
                    new Color(60, 60, 60), // mutedText
                    new Color(235, 235, 235), // controlBg
                    new Color(180, 180, 180), // controlBorder
                    new Color(220, 220, 220), // controlHoverBg
                    new Color(255, 255, 255), // inputBg
                    new Color(180, 180, 180), // inputBorder
                    new Color(255, 255, 255), // listBg
                    new Color(210, 210, 210), // listSelectionBg
                    new Color(200, 200, 200), // separatorColor
                    new Color(34, 139, 34), // successGreen
                    new Color(100, 80, 50) // narrativeText (warm brown)
            );

            Overlay overlay = new Overlay(
                    new Color(0, 0, 0, 90),
                    new Color(255, 255, 255, 235),
                    new Color(0, 0, 0, 90),
                    new Color(20, 20, 20),
                    new Color(235, 235, 235),
                    new Color(180, 180, 180),
                    new Color(255, 255, 255),
                    new Color(180, 180, 180)
            );

            return new Palette(base, overlay, defaultFonts());
        }

        public static Palette dark() {
            Base base = new Base(
                    new Color(32, 32, 32), // appBg
                    new Color(235, 235, 235), // text
                    new Color(170, 170, 170), // mutedText
                    new Color(55, 55, 55), // controlBg
                    new Color(100, 100, 100), // controlBorder
                    new Color(80, 80, 80), // controlHoverBg
                    new Color(25, 25, 25), // inputBg
                    new Color(70, 70, 70), // inputBorder
                    new Color(22, 22, 22), // listBg
                    new Color(90, 90, 90), // listSelectionBg
                    new Color(80, 80, 80), // separatorColor
                    new Color(34, 139, 34), // successGreen
                    new Color(200, 190, 160) // narrativeText (warm gold)
            );

            Overlay overlay = new Overlay(
                    new Color(0, 0, 0, 120),
                    new Color(0, 0, 0, 220),
                    new Color(220, 220, 220, 160),
                    new Color(235, 235, 235),
                    new Color(30, 30, 30),
                    new Color(120, 120, 120),
                    new Color(25, 25, 25),
                    new Color(70, 70, 70)
            );
            return new Palette(base, overlay, defaultFonts());
        }
    }
}
