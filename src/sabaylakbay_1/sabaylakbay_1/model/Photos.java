package sabaylakbay_1.model;

/**
 * Real landscape photos (no people) from Wikimedia Commons, free to use with credit.
 * The app downloads each one once and keeps it in the "cache/photos" folder, so it works offline after that.
 * Trips without their own cover photo get a photo of their destination (or of their kind of trip).
 * Credits: the "Photo credits" tab in Terms and Privacy, and PHOTO CREDITS.txt.
 */
public final class Photos {

    private Photos() { }

    private static final String BASE = "https://commons.wikimedia.org/wiki/Special:FilePath/";

    /** Commons file names. */
    public static final String MAYON = "Perfect_Mayon.jpg";          // 3600 x 2400, sharp
    public static final String CHOCOLATE_HILLS = "Chocolate_Hills_overview.JPG";
    public static final String EL_NIDO = "Island_lagoon_in_Bacuit_Bay,_El_Nido,_Palawan,_Philippines.jpg";
    public static final String CORON = "Coron_-_Sleeping_Giant.jpg";
    public static final String BATANES = "Sabtang_Batanes.jpg";
    public static final String BANGUI = "Bangui_windmills.jpg";
    public static final String SIARGAO = "Siargao_54.jpg";
    public static final String DARANAK = "Daranak_Falls_in_Tanay,_Rizal.jpg";
    public static final String CARAMOAN = "Caramoan_Peninsula.jpg";
    public static final String BAGUIO = "Baguio_view_5-19-23.jpg";
    public static final String BATAD = "Banaue_Philippines_Batad-Rice-Terraces-02.jpg";

    /** Big banner on Home (wide, sharp on Windows display scaling). */
    public static final String HERO = url(EL_NIDO, 1920);
    /** Background of the log in and sign up screens: lush green rice terraces, very large so it stays sharp. */
    public static final String LOGIN = url(MAYON, 1920);
    public static final String LOGIN_PLACE = "Mayon Volcano, Albay";
    public static final String LOGIN_CREDIT = "Photo: \"Perfect Mayon\" / Wikimedia Commons";

    /** Every photo the app uses, for the credits list. */
    public static final String[][] CREDITS = {
            {"Mayon Volcano, Albay (log in screen)", MAYON},
            {"Batad Rice Terraces, Ifugao (by CEphoto, Uwe Aranas)", BATAD},
            {"Chocolate Hills, Bohol", CHOCOLATE_HILLS},
            {"Bacuit Bay, El Nido, Palawan", EL_NIDO},
            {"Sleeping Giant, Coron", CORON},
            {"Sabtang, Batanes", BATANES},
            {"Bangui Windmills, Ilocos Norte", BANGUI},
            {"Siargao Island", SIARGAO},
            {"Daranak Falls, Tanay, Rizal", DARANAK},
            {"Caramoan Peninsula, Camarines Sur", CARAMOAN},
            {"Baguio City, Benguet", BAGUIO}};

    /** Download link for trip cards (960 px wide is plenty for a card, and quick to download). */
    public static String url(String file) {
        return url(file, 960);
    }

    /**
     * Download link resized by Wikimedia to this width. Use a standard Wikimedia size
     * (500, 960, 1280, 1920): other sizes are refused. If it still fails, the app downloads the original.
     */
    public static String url(String file, int width) {
        return BASE + file + "?width=" + width;
    }

    /** The page on Commons that shows the photographer and the license. */
    public static String page(String file) {
        return "https://commons.wikimedia.org/wiki/File:" + file;
    }

    /** True for one of these downloaded photos (not a photo the organizer chose). */
    public static boolean isStockPhoto(String path) {
        return path != null && path.startsWith(BASE);
    }

    /**
     * A photo for a trip without its own cover: matched by the trip name and destination first,
     * then by the kind of trip. null = no match (the app draws its simple mountain picture).
     */
    public static String forTrip(String title, String destination, String category) {
        String t = ((title == null ? "" : title) + " " + (destination == null ? "" : destination)).toLowerCase();
        if (has(t, "coron", "busuanga")) return url(CORON);
        if (has(t, "el nido", "palawan", "puerto princesa")) return url(EL_NIDO);
        if (has(t, "mayon", "albay", "legazpi")) return url(MAYON);
        if (has(t, "bohol", "chocolate hills", "panglao")) return url(CHOCOLATE_HILLS);
        if (has(t, "batanes", "basco", "sabtang")) return url(BATANES);
        if (has(t, "pagudpud", "bangui", "ilocos", "laoag")) return url(BANGUI);
        if (has(t, "siargao", "surigao")) return url(SIARGAO);
        if (has(t, "tanay", "rizal", "daranak")) return url(DARANAK);
        if (has(t, "caramoan", "camarines")) return url(CARAMOAN);
        if (has(t, "baguio", "benguet")) return url(BAGUIO);
        if (has(t, "banaue", "batad", "ifugao", "sagada", "rice terrace")) return url(BATAD);
        String c = category == null ? "" : category.toLowerCase();
        if (has(c, "hik", "mountain", "trek")) return url(MAYON);
        if (has(c, "island")) return url(CORON);
        if (has(c, "beach", "surf", "swim", "dive", "snorkel")) return url(EL_NIDO);
        if (has(c, "road")) return url(BANGUI);
        if (has(c, "waterfall", "falls")) return url(DARANAK);
        if (has(c, "sightseeing", "nature", "camp")) return url(BATANES);
        if (has(c, "food", "city", "culture")) return url(BAGUIO);
        return null;
    }

    private static boolean has(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }
}
