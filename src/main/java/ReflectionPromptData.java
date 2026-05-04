import java.util.HashMap;
import java.util.Map;

// Stores reflection / prediction prompt data for the learning-theory PoC.
// Currently populated for the Ancient Cyprus collection only.
// Design lens: Richland, Kornell & Kao (2009), pretesting effect;
// Stanton, Sebesta & Dunlosky (2021),  metacognitive prompting.

public final class ReflectionPromptData {

    private ReflectionPromptData() {}

    // Bundle of prompt strings for a single artefact
    public static class Prompts {
        public final String predictionQuestion; // Shown BEFORE play, a prediction / curiosity question.
        public final String[] predictionChoices; // Short choices the player can think about (not graded).
        public final String revealText; // Shown AFTER completion, a brief factual reveal.
        public final String reflectionQuestion; // Shown AFTER reveal, a metacognitive reflection question.

        public Prompts(String predictionQuestion, String[] predictionChoices,
                       String revealText, String reflectionQuestion) {
            this.predictionQuestion = predictionQuestion;
            this.predictionChoices = predictionChoices;
            this.revealText = revealText;
            this.reflectionQuestion = reflectionQuestion;
        }
    }

    // Catalogue (Ancient Cyprus PoC)
    private static final Map<String, Prompts> DATA = new HashMap<>();

    static {
        // All content derived from ArtifactCatalog metadata + museum sources.

        DATA.put("/Ancient Cyprus/Artifacts/jug-1.jpg", new Prompts(
                "What do you think this large jar was used for?",
                new String[]{"Storing water", "Holding oil or perfume", "A funerary offering"},
                "This Early Cypriot jar (c. 2250–2150 BC) has a spherical body with Red Polished slip, a hallmark of handmade Bronze Age pottery from Vounous.",
                "What details in the shape or decoration stood out to you while assembling?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/jug-2.jpg", new Prompts(
                "Why might a jug have a long beak-shaped spout?",
                new String[]{"For pouring liquids carefully", "Purely decorative", "To imitate an animal form"},
                "This Red Polished jug (c. 2075–2000 BC) has a cut-away beak spout and zig-zag incised decoration on its handle, typical of Early Cypriot craftsmanship.",
                "Did you notice the zig-zag patterns on the handle? What might they signify?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/jug-3.jpg", new Prompts(
                "What can the colour of pottery tell us about how it was made?",
                new String[]{"The type of clay used", "The firing temperature", "Both of these"},
                "This jug's rich brown surface comes from well-mixed clay fired to buff tones. The grey core visible in cracks reveals the firing didn't fully oxidise the interior.",
                "What did you notice about the surface colour and condition of this artefact?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/jug-4.jpg", new Prompts(
                "What is a 'potter's mark' and why might it be on a jug?",
                new String[]{"A signature of the maker", "A quality stamp", "A symbol of ownership"},
                "This medium jug (c. 2075–2000 BC) reportedly has a potter's mark on its handle, though much of the red polished slip has fallen off over time.",
                "How might wear and damage over 4,000 years change what we can learn from an artefact?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/jug-5.jpg", new Prompts(
                "This jug is from a different period than the others. Can you guess which era?",
                new String[]{"Early Bronze Age (~2000 BC)", "Iron Age (~800 BC)", "Roman period (~100 AD)"},
                "This Cypro-Geometric jug (c. 850–750 BC) is over 1,000 years newer than most others here. Its Black Slip bucchero decoration with vertical parallel lines marks a distinct artistic tradition.",
                "How does this jug's style differ from the earlier Red Polished pieces you've seen?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/bowl-1.jpg", new Prompts(
                "This bowl may have been imported from southern Cyprus. What might suggest that?",
                new String[]{"Its size", "Its decoration style", "The type of clay"},
                "The mottle-effect Red Polished decoration is more common at southern sites like Psematismenos than at northern Vounous, suggesting this bowl was traded across the island.",
                "What does the presence of imported goods tell us about Bronze Age Cypriot communities?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/bowl-2.jpg", new Prompts(
                "What do you think this tulip-shaped bowl was used for?",
                new String[]{"Everyday dining", "Funerary ceremonies", "Storing grain"},
                "Tulip-shaped bowls like this had significant ritual importance in funerary ceremonies. Their sophisticated designs may reflect communities asserting regional identity through material culture.",
                "What details did you notice in the zig-zag and line patterns decorating this bowl?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/bowl-3.jpg", new Prompts(
                "This bowl has a horn-shaped handle. What purpose might that serve?",
                new String[]{"Easier to grip", "Symbolic or ritual significance", "For hanging the bowl"},
                "The Black-topped Red Polished decoration was achieved by careful planning during the firing process, not by painting. The horn handle and incised semi-circles suggest ritual use.",
                "How is the decoration technique (firing, not painting) different from what you expected?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/bowl-4.jpg", new Prompts(
                "Why might a bowl have a long spout on one side?",
                new String[]{"For pouring offerings", "For feeding infants", "Both could be possible"},
                "This large Red Polished bowl (c. 2250–2150 BC) has a long spout and black interior. The colour variation from red to dark brown near the spout is caused by the firing process.",
                "What did you notice about how the colour changes across the bowl's surface?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/bowl-5.jpg", new Prompts(
                "What can cracks and missing pieces tell us about an artefact?",
                new String[]{"How old it is", "How it was buried or stored", "How it was excavated"},
                "This small Red Polished bowl (c. 2150–2100 BC) has cracks and a small missing section, yet it survived over 4,000 years. Its lug with a two-sided hole may have held a cord.",
                "How do you think this small bowl survived for over 4,000 years?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/Pyxis-Lid.jpg", new Prompts(
                "This ivory disc has a lozenge pattern made of interlocking spirals. What might it be?",
                new String[]{"A mirror", "A lid for a container", "A decorative disc for a wall"},
                "This is the lid of a pyxis (cosmetic box) from c. 1300 BC. Its decoration resembles gold bezel rings found in the same tomb, linking personal adornment to everyday objects.",
                "What surprised you about the level of detail carved into this ivory lid?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/Spindle-Whorl.jpg", new Prompts(
                "What was a spindle whorl used for in the ancient world?",
                new String[]{"Weaving and spinning thread", "As a game piece", "As a decorative bead"},
                "Spindle whorls were essential textile tools, the weight keeps the spindle spinning to twist fibres into thread. This ivory example (c. 1300 BC) is unusually decorated with incised petal patterns.",
                "Why might a functional tool like a spindle whorl be decorated so elaborately?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/Human-Remains-1.jpg", new Prompts(
                "What can skeletal remains tell archaeologists about ancient people?",
                new String[]{"Diet and health", "Age and cause of death", "All of these and more"},
                "Human and animal remains from Late Cypriot tombs (c. 1250–1190 BC) provide evidence about burial practices, diet, health, and the relationship between people and animals in ancient Cyprus.",
                "What did you find yourself thinking about while assembling this particular puzzle?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/Temple-Boy.jpg", new Prompts(
                "This statuette shows a seated boy. Where do you think it was originally placed?",
                new String[]{"In a home", "In a temple as a votive offering", "In a tomb"},
                "Temple boy statuettes were votive offerings placed in sanctuaries. They represent young boys seated cross-legged, possibly depicting worshippers or dedicants to a deity.",
                "What does a votive offering like this tell us about religious practices in ancient Cyprus?"
        ));

        DATA.put("/Ancient Cyprus/Artifacts/VotiveStatueHead.jpg", new Prompts(
                "This is the head of a stone statue. Who do you think it represents?",
                new String[]{"A king or ruler", "A worshipper making an offering", "A deity"},
                "Votive statue heads were offerings left in sanctuaries. They represent worshippers or dedicants, not the gods themselves, a personal act of devotion preserved in stone.",
                "What does leaving a statue of yourself at a temple tell us about ancient Cypriot beliefs?"
        ));
    }

    // Returns the prompts for a given artefact path, or null if none exist
    // (i.e. the artefact is not part of the PoC collection).

    public static Prompts forArtefact(String resourcePath) {
        return DATA.get(resourcePath);
    }

    // Returns true if the given collection has reflection prompt support.
    public static boolean isSupported(String collectionPath) {
        return collectionPath != null && collectionPath.contains("Ancient Cyprus");
    }
}
