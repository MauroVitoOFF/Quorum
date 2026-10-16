package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class WardrobeRulesTest {
    static final JobLookup JOBS = new JobLookup() {
        @Override
        public boolean hasJob(String job) {
            return job.equals("police") || job.equals("taxi");
        }

        @Override
        public OptionalInt gradeLevel(String job, String grade) {
            if (!job.equals("police")) return OptionalInt.empty();
            return switch (grade) {
                case "cadet" -> OptionalInt.of(0);
                case "officer" -> OptionalInt.of(1);
                case "captain" -> OptionalInt.of(2);
                default -> OptionalInt.empty();
            };
        }
    };

    static OutfitDefinition outfit(String id, String job, String minGrade) {
        return new OutfitDefinition(id, id, job, minGrade, OutfitModel.CLASSIC, new byte[0]);
    }

    @Test
    void differentJobCannotWear() {
        assertFalse(WardrobeRules.canWear(outfit("a", "police", null), new Wearer("taxi", "x", true), JOBS));
    }

    @Test
    void offDutyCannotWear() {
        assertFalse(WardrobeRules.canWear(outfit("a", "police", null), new Wearer("police", "officer", false), JOBS));
    }

    @Test
    void gradeBelowAtAndAboveMinimum() {
        OutfitDefinition o = outfit("a", "police", "officer");
        assertFalse(WardrobeRules.canWear(o, new Wearer("police", "cadet", true), JOBS));
        assertTrue(WardrobeRules.canWear(o, new Wearer("police", "officer", true), JOBS));
        assertTrue(WardrobeRules.canWear(o, new Wearer("police", "captain", true), JOBS));
    }

    @Test
    void missingMinGradeMeansAnyGrade() {
        assertTrue(WardrobeRules.canWear(outfit("a", "police", null), new Wearer("police", "cadet", true), JOBS));
    }

    @Test
    void unknownWearerGradeCannotWearAGradedOutfit() {
        assertFalse(WardrobeRules.canWear(outfit("a", "police", "officer"), new Wearer("police", "ghost", true), JOBS));
    }

    @Test
    void availableKeepsOnlyThePointJobOutfitsTheGradeAllows() {
        OutfitDefinition patrol = outfit("patrol", "police", "officer");
        OutfitDefinition gala = outfit("gala", "police", "captain");
        OutfitDefinition cab = outfit("cab", "taxi", null);
        OutfitCatalog catalog = OutfitCatalog.of(List.of(patrol, gala, cab));
        List<OutfitDefinition> got = WardrobeRules.available(catalog, "police", new Wearer("police", "officer", true), JOBS);
        assertEquals(List.of(patrol), got);
    }
}
