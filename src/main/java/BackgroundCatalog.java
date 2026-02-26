public final class BackgroundCatalog {
    private BackgroundCatalog() {}

    public static String backgroundFor(String collectionName, Theme theme) {
        boolean dark = (theme == Theme.DARK);

        // default collection if null
        if (collectionName == null) collectionName = "/Rome";

        return switch (collectionName) {
            case "/Ancient Cyprus" ->
                    dark ? "/Starting/Backgrounds/Ancient Cyprus/Dark.jpg"
                            : "/Starting/Backgrounds/Ancient Cyprus/Light.jpg";
            case "/Ancient Greece" ->
                    dark ? "/Starting/Backgrounds/Ancient Greece/Dark.jpg"
                            : "/Starting/Backgrounds/Ancient Greece/Light.jpg";
            case "/Ancient Egypt" ->
                    dark ? "/Starting/Backgrounds/Ancient Egypt/Dark.jpg"
                            : "/Starting/Backgrounds/Ancient Egypt/Light.jpg";
            case "/Ancient Near East" ->
                    dark ? "/Starting/Backgrounds/Ancient Near East/Dark.jpg"
                            : "/Starting/Backgrounds/Ancient Near East/Light.jpg";
            case "/Rome" ->
                    dark ? "/Starting/Backgrounds/Rome/Dark.jpg"
                            : "/Starting/Backgrounds/Rome/Light.jpg";
            default ->
                    dark ? "/Starting/Backgrounds/Rome/Dark.jpg"
                            : "/Starting/Backgrounds/Rome/Light.jpg";
        };
    }
}
