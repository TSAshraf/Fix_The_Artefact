import java.util.*;

// Structured metadata for each artefact: display name, date string, sort year.
// Used by the timeline strip to show artefacts in chronological order.

public final class TimelineData {
    private TimelineData() {}

    public static class Entry {
        public final String imagePath;
        public final String displayName;
        public final String dateLabel; // e.g. "2250–2150 BC"
        public final int sortYear; // negative = BC, for sorting
        public final String microNarrative; // short contextual sentence for meaning-making

        // Legacy 4-arg constructor (no narrative). 
        public Entry(String imagePath, String displayName, String dateLabel, int sortYear) {
            this(imagePath, displayName, dateLabel, sortYear, "");
        }

        public Entry(String imagePath, String displayName, String dateLabel, int sortYear, String microNarrative) {
            this.imagePath = imagePath;
            this.displayName = displayName;
            this.dateLabel = dateLabel;
            this.sortYear = sortYear;
            this.microNarrative = microNarrative;
        }
    }

    private static final Map<String, List<Entry>> TIMELINES = new LinkedHashMap<>();

    static {
        // Ancient Cyprus (sorted oldest to newest)
        List<Entry> cyprus = new ArrayList<>();
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/jug-1.jpg",          "Jug (Red Polished I)",      "2250–2150 BC", -2250,
                "Its red-black colouring came from how it was fired — not painted."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/bowl-2.jpg",         "Tulip Bowl",                "2250–2150 BC", -2245,
                "A delicate vessel shaped like an opening flower, made entirely by hand."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/bowl-3.jpg",         "Horn-handle Bowl",          "2250–2150 BC", -2240,
                "The horn-shaped handle suggests this bowl had a ritual purpose beyond everyday use."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/bowl-4.jpg",         "Spouted Bowl",              "2250–2150 BC", -2235,
                "A spout for pouring — someone used this to serve others over 4,000 years ago."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/bowl-1.jpg",         "Hemispherical Bowl",        "2150–2100 BC", -2150,
                "Simple in form but rich in surface colour — the hallmark of Early Cypriot pottery."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/bowl-5.jpg",         "Small Red Bowl",            "2150–2100 BC", -2140,
                "Small enough to hold in one hand, yet it survived four millennia."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/jug-2.jpg",          "Jug (Beak Spout)",          "2075–2000 BC", -2075,
                "The beak-shaped spout is a distinctly Cypriot design — not found on the mainland."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/jug-3.jpg",          "Jug (Swollen Body)",        "2075–2000 BC", -2070,
                "Cracks tell us this jug was well-used before it was buried with its owner."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/jug-4.jpg",          "Jug (Ovoid)",               "2075–2000 BC", -2065,
                "A potter's mark on the handle — one of the earliest signatures in Cypriot craft."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/Pyxis-Lid.jpg",      "Pyxis Lid (Ivory)",         "c. 1300 BC",   -1300,
                "Carved ivory from the Late Bronze Age, when Cyprus traded with Egypt and the Levant."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/Spindle-Whorl.jpg",  "Spindle Whorl",             "c. 1300 BC",   -1295,
                "A weight for spinning thread — evidence of the textile craft that sustained daily life."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/Human-Remains-1.jpg","Human & Animal Remains",    "1250–1190 BC", -1250,
                "Buried together — the relationship between humans and animals in ancient ritual."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/jug-5.jpg",          "Jug (Cypro-Geometric)",     "850–750 BC",   -850,
                "New geometric patterns emerged as Cyprus rebuilt after the Bronze Age collapse."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/VotiveStatueHead.jpg","Votive Statue Head",       "600–500 BC",   -600,
                "Left at a sanctuary as a prayer in stone — a personal offering to the gods."));
        cyprus.add(new Entry("/Ancient Cyprus/Artifacts/Temple-Boy.jpg",     "Temple Boy",                "400–300 BC",   -400,
                "A child seated cross-legged — a common votive type found across Cypriot sanctuaries."));
        TIMELINES.put("/Ancient Cyprus/Artifacts/", cyprus);

        // Ancient Greece (sorted oldest to newest)
        List<Entry> greece = new ArrayList<>();
        greece.add(new Entry("/Ancient Greece/Artifacts/Wine Flask 2.jpg",               "Oinochoe (Geometric)",        "c. 730 BC",            -730,
                "Geometric patterns replaced earlier abstract forms — a new visual language was emerging."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Aryballos.jpg",                  "Aryballos",                   "665–620 BC",           -665,
                "Small enough to hang from your wrist — athletes carried oil in these to the gymnasium."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Aryballos 2.jpg",                "Aryballos II",                "626–600 BC",           -626,
                "Corinthian potters mass-produced these for export across the Mediterranean."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Aryballos 3.jpg",                "Aryballos III",               "4th Century BC",       -350,
                "By this period, the aryballos form had been refined over three centuries of use."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Amour Helmet.jpg",               "Corinthian Helmet",           "550–450 BC",           -550,
                "Hammered from a single bronze sheet — worn by hoplites in the phalanx."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Kylix.jpg",                      "Kylix",                       "c. 530 BC",            -530,
                "A wide drinking cup — the image inside was revealed as you drained your wine."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Rhyton.jpg",                     "Rhyton",                      "c. 550 BC",            -545,
                "A ceremonial pouring vessel shaped like an animal — used in ritual libations."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Horse votive offering.jpg",      "Horse Votive",                "6th Century BC",       -500,
                "A bronze horse left at a sanctuary — horses symbolised wealth and status."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Fragments of deer figurine.jpg", "Deer Figurine",               "6th–4th Century BC",   -490,
                "Only fragments survive, but the craftsmanship in the antlers is still visible."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Wreath Shaped votive offering.jpg","Wreath Votive",             "6th–4th Century BC",   -480,
                "A wreath offering — victory, honour, and devotion rendered in metal."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Loutrophoros.jpg",                "Loutrophoros",               "Late 4th Century BC",  -320,
                "Used to carry water for wedding or funeral rituals — a vessel for life's turning points."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Drinking Vessel.jpg",            "Skyphos",                     "Late 5th–Early 4th C", -400,
                "An everyday drinking cup — the ancient Greek equivalent of a mug."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Wine Flask.jpg",                 "Oinochoe (Red-figure)",       "360–330 BC",           -360,
                "Red-figure technique let painters add detail with a brush rather than scratching through slip."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Amphora.jpg",                    "Neck Amphora",                "4th Century BC",       -345,
                "Two-handled jars like this stored and transported wine, oil, and grain."));
        greece.add(new Entry("/Ancient Greece/Artifacts/Krater.jpg",                     "Bell Krater",                 "4th Century BC",       -340,
                "Wine was mixed with water in kraters — drinking it neat was considered barbaric."));
        greece.sort((a, b) -> Integer.compare(a.sortYear, b.sortYear));
        TIMELINES.put("/Ancient Greece/Artifacts/", greece);

        // Ancient Egypt (from ArtifactCatalog dates)
        List<Entry> egypt = new ArrayList<>();
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Figurine of a Pygmy Dance Leader.jpeg", "Pygmy Dance Leader",    "1950–1885 BC",   -1950,
                "A rare ivory figure — pygmy dancers were prized performers in the Egyptian court."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Shabti of Djedkhonsuefankh.jpeg",       "Shabti",                "1070–945 BC",    -1070,
                "A servant for the afterlife — shabtis did the hard labour so the dead wouldn't have to."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Schist Statuette Fragment.jpg",         "Statuette Fragment",    "1550–1069 BC",   -1550,
                "Only a fragment survives, but the smooth stone was carved with remarkable precision."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Offering Table.jpg",                    "Offering Table",        "200 BC",         -200,
                "Food and drink were placed here for the dead — sustenance for eternity."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Amulet of Jackal Headed Deity.jpg",     "Jackal-headed Amulet",  "664–525 BC",     -664,
                "Anubis guided souls to the afterlife — this amulet offered protection on that journey."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Composite Papyrus Capital.jpeg",        "Papyrus Capital",       "380–343 BC",     -380,
                "Architecture imitating nature — the papyrus plant shaped Egypt's monumental columns."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Chariots with Court Ladies.jpeg",       "Chariots & Ladies",     "1353–1336 BC",   -1353,
                "A scene from Amarna — Akhenaten's revolutionary court captured in carved relief."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Wedjat Eye Amulet.jpeg",                "Wedjat Eye Amulet",     "1070–664 BC",    -1065,
                "The Eye of Horus — one of the most powerful protective symbols in ancient Egypt."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Artist's Sketch of a Sparrow.jpeg",     "Sparrow Sketch",        "1479–1458 BC",   -1479,
                "An artist's practice sketch on limestone — a fleeting, personal moment preserved."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Ring, signet.jpeg",                     "Signet Ring",           "1353–1323 BC",   -1348,
                "A seal of identity and authority — pressed into clay to authenticate royal documents."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Ring with Cat and Kittens.jpeg",        "Cat & Kittens Ring",    "1295–664 BC",    -1295,
                "Cats were sacred to Bastet — even jewellery celebrated the bond between feline and human."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Sarcophagus of Harkhebit.jpeg",         "Sarcophagus",           "595–526 BC",     -595,
                "Carved from basalt, this massive coffin was meant to protect the body for eternity."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Kneeling statue of Hatshepsut.jpeg",    "Kneeling Hatshepsut",   "1479–1458 BC",   -1475,
                "One of Egypt's few female pharaohs — she ruled for over 20 years as king, not queen."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Inner Coffin Box of Taenty.jpg",        "Coffin of Taenty",      "1069–664 BC",    -1069,
                "Painted eyes on the coffin let the deceased 'see' the offerings left for them."));
        egypt.add(new Entry("/Ancient Egypt/Artifacts/Game of Hounds and Jackals.jpeg",       "Hounds & Jackals Game", "1814–1805 BC",   -1814,
                "A board game buried with its owner — entertainment that mattered enough for the afterlife."));
        egypt.sort((a, b) -> Integer.compare(a.sortYear, b.sortYear));
        TIMELINES.put("/Ancient Egypt/Artifacts/", egypt);

        // Ancient Near East (from ArtifactCatalog dates)
        List<Entry> nearEast = new ArrayList<>();
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Kneeling Bull.jpeg",              "Kneeling Bull",             "3100–2900 BC",             -3100,
                "A silver bull kneeling in submission — a symbol of divine power in early Mesopotamia."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Master of Animals Standard.jpg",  "Master of Animals",         "2900–2600 BC",             -2900,
                "A figure controlling wild beasts — the oldest known depiction of human dominion over nature."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Headdress.jpeg",                  "Headdress",                 "2600–2500 BC",             -2600,
                "Gold leaves and lapis lazuli from the Royal Tombs of Ur — buried with a queen."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Standing Male Worshiper.jpeg",    "Male Worshipper",           "2300–2200 BC",             -2300,
                "Wide eyes fixed open — this statue prayed on its owner's behalf, day and night."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Statue of Gudea.jpeg",            "Statue of Gudea",           "c. 2090 BC",               -2090,
                "Gudea rebuilt Lagash's temples — his statues were placed inside as eternal worshippers."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/South Arabian Statue.jpg",        "South Arabian Statue",      "1000–600 BC",              -1000,
                "From the incense kingdoms of southern Arabia — a culture built on trade routes."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Seated Goddess with a Child .jpeg","Seated Goddess & Child",   "Late 3rd–Early 2nd mill.", -2100,
                "A mother and child — one of humanity's oldest and most enduring artistic themes."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Plaque with horned lion-griffins.jpeg","Lion-griffins Plaque",  "14th–13th Century BC",     -1350,
                "Mythical lion-griffins guarded sacred spaces — power made visible through art."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Panel with Lion.jpeg",            "Panel with Lion",           "604–562 BC",               -604,
                "Glazed brick from Babylon's Processional Way — hundreds of lions lined the route."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Head of a Ruler.jpeg",            "Head of a Ruler",           "6th–4th Century BC",       -500,
                "The identity is lost, but the authority in the expression remains unmistakable."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Helmet with Divine figures.jpeg",  "Helmet with Deities",      "1500–1100 BC",             -1500,
                "Gods engraved on a warrior's helmet — divine protection carried into battle."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Enthroned Deity.jpeg",            "Enthroned Deity",           "100 BC–1 BC",              -100,
                "A late Mesopotamian deity still seated on a throne — old traditions persisting."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Shaft-hole axe head.jpeg",        "Shaft-hole Axe",            "14th–13th Century BC",     -1345,
                "Too ornate for combat — this axe was a symbol of rank, not a tool of war."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Openwork furniture plaque.jpeg",  "Furniture Plaque",          "9th–8th Century BC",       -850,
                "Ivory inlay from palatial furniture — luxury items from the Assyrian elite."));
        nearEast.add(new Entry("/Ancient Near East/Artifacts/Stag Vessel.jpeg",                "Stag Vessel",               "14th–13th Century BC",     -1340,
                "A silver vessel shaped like a stag — Hittite craftsmanship at its finest."));
        nearEast.sort((a, b) -> Integer.compare(a.sortYear, b.sortYear));
        TIMELINES.put("/Ancient Near East/Artifacts/", nearEast);

        // Rome (from ArtifactCatalog dates)
        List<Entry> rome = new ArrayList<>();
        rome.add(new Entry("/Rome/Artifacts/Serapis with Cerberus.jpg",     "Serapis & Cerberus",   "150 BC–70 AD",        -150,
                "An Egyptian god adopted by Rome — religion crossed borders as freely as trade."));
        rome.add(new Entry("/Rome/Artifacts/Ash Chest.jpg",                 "Ash Chest",            "140 BC–50 AD",        -140,
                "Cremated remains were placed in decorated stone chests — death was a public affair."));
        rome.add(new Entry("/Rome/Artifacts/Ash Chest 2.jpg",              "Ash Chest II",          "100 BC–100 AD",       -100,
                "The carved scenes on ash chests told stories about how the deceased wanted to be remembered."));
        rome.add(new Entry("/Rome/Artifacts/Sculpture of Cybele .jpg",     "Sculpture of Cybele",   "1–100 AD",             1,
                "An Anatolian mother goddess worshipped in Rome — the empire absorbed the gods it conquered."));
        rome.add(new Entry("/Rome/Artifacts/Statue of Anchirroe .jpg",     "Statue of Anchirroe",   "100–125 AD",          100,
                "A water nymph from Greek myth — Roman sculptors kept Greek stories alive in stone."));
        rome.add(new Entry("/Rome/Artifacts/Bust of Boy .jpg",             "Bust of Boy",           "1–100 AD",             5,
                "A child's portrait in marble — someone wanted this face remembered forever."));
        rome.add(new Entry("/Rome/Artifacts/Bust of Trajan.jpg",           "Bust of Trajan",        "Mid 1st Century AD",  50,
                "One of Rome's 'good emperors' — his conquests expanded the empire to its largest extent."));
        rome.add(new Entry("/Rome/Artifacts/Bowl 1 .jpg",                  "Bowl I",                "1–100 AD",             10,
                "Roman pottery spread across the empire — a shared material culture from Britain to Syria."));
        rome.add(new Entry("/Rome/Artifacts/Bowl 2.jpg",                   "Bowl II",               "100 BC–100 AD",       -95,
                "Everyday tableware that reveals what ordinary Romans ate and drank."));
        rome.add(new Entry("/Rome/Artifacts/Bowl 3.jpg",                   "Bowl III",              "100–200 AD",          105,
                "Mass-produced yet distinctive — Roman workshops had recognisable house styles."));
        rome.add(new Entry("/Rome/Artifacts/Bust of a Priest of Isis.jpg", "Priest of Isis",        "100–200 AD",          110,
                "A shaved head marked devotion to Isis — Egyptian cults thrived in Roman cities."));
        rome.add(new Entry("/Rome/Artifacts/Statue of Apollo.jpg",         "Statue of Apollo",      "1–300 AD",            15,
                "God of music, prophecy, and the sun — Apollo was among the most beloved Roman deities."));
        rome.add(new Entry("/Rome/Artifacts/Statuette of Hermes.jpg",      "Statuette of Hermes",   "0–200 AD",            20,
                "Messenger of the gods — small bronzes like this were kept in household shrines."));
        rome.add(new Entry("/Rome/Artifacts/Statue of Bacchus.jpg",        "Statue of Bacchus",     "Late 2nd Century AD", 175,
                "The god of wine and ecstasy — his cult promised followers a joyful afterlife."));
        rome.add(new Entry("/Rome/Artifacts/Sarcophagus.jpg",              "Sarcophagus",           "100–200 AD",          115,
                "Burial replaced cremation in the 2nd century — a shift in how Romans thought about the body."));
        rome.sort((a, b) -> Integer.compare(a.sortYear, b.sortYear));
        TIMELINES.put("/Rome/Artifacts/", rome);
    }

    // Get the timeline entries for a collection (already sorted oldest to newest).
    public static List<Entry> forCollection(String collectionPath) {
        return TIMELINES.getOrDefault(collectionPath, Collections.emptyList());
    }

    // Check if timeline data exists for this collection.
    public static boolean hasTimeline(String collectionPath) {
        return TIMELINES.containsKey(collectionPath);
    }

    // Find an entry by image path within a collection's timeline.
    public static Entry forImage(String collectionPath, String imagePath) {
        for (Entry e : forCollection(collectionPath)) {
            if (e.imagePath.equals(imagePath)) return e;
        }
        return null;
    }
}
