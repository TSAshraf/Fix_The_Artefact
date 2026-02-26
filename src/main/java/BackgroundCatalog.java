public final class BackgroundCatalog {
    private BackgroundCatalog() {}

    public static String backgroundFor(String collectionName, Theme theme) {
        boolean dark = (theme == Theme.DARK);

        // default collection if null
        if (collectionName == null) collectionName = "/Rome";

        return switch (collectionName) {
            case "/Ancient Cyprus/Artifacts/" ->
                    dark ? "/Ancient Cyprus/Backgrounds/Dark.png"
                            : "/Ancient Cyprus/Backgrounds/Light.jpg";
            case "/Ancient Greece/Artifacts/" ->
                    dark ? "/Ancient Greece/Backgrounds/Dark.jpeg"
                            : "/Ancient Greece/Backgrounds/Light.jpg";
            case "/Ancient Egypt/Artifacts/" ->
                    dark ? "/Ancient Egypt/Backgrounds/Dark.jpeg"
                            : "/Ancient Egypt/Backgrounds/Light.jpg";
            case "/Ancient Near East/Artifacts/" ->
                    dark ? "/Ancient Near East/Backgrounds/Dark.jpeg"
                            : "/Ancient Near East/Backgrounds/Light.jpg";
            case "/Rome/Artifacts/" ->
                    dark ? "/Rome/Backgrounds/Dark.jpeg"
                            : "/Rome/Backgrounds/Light.jpg";
            default ->
                    dark ? "/Rome/Backgrounds/Dark.jpeg"
                            : "/Rome/Backgrounds/Light.jpg";
        };
    }
}
