public final class BackgroundCatalog {
    private BackgroundCatalog() {}
    
    public static String backgroundFor(String collectionName, Theme theme) {
        boolean dark = (theme == Theme.DARK);

        // default collection if null
        if (collectionName == null) collectionName = "/Rome";

        return switch (collectionName) {
            case "/Ancient Cyprus" ->
                    dark ? "/Ancient Cyprus/Backgrounds/Dark.jpg"
                            : "/Ancient Cyprus/Backgrounds//Light.jpg";
            case "/Ancient Greece" ->
                    dark ? "/Ancient Greece/Backgrounds/Dark.jpg"
                            : "/Ancient Greece/Backgrounds/Light.jpg";
            case "/Ancient Egypt" ->
                    dark ? "/Ancient Egypt/Backgrounds/Dark.jpg"
                            : "/Ancient Egypt/Backgrounds/Light.jpg";
            case "/Ancient Near East" ->
                    dark ? "/Ancient Near East/Backgrounds/Dark.jpg"
                            : "/Ancient Near East/Backgrounds/Light.jpg";
            case "/Rome" ->
                    dark ? "/Rome/Backgrounds/Dark.jpg"
                            : "/Rome/Backgrounds/Light.jpg";
            default ->
                    dark ? "/Rome/Backgrounds/Dark.jpg"
                            : "/Rome/Backgrounds/Light.jpg";
        };
    }
}
