package dev.qrm.wardrobe.logic;

import java.util.List;
import java.util.OptionalInt;

/** Chi può indossare una divisa: stesso lavoro, in servizio, grado almeno pari al minimo. */
public final class WardrobeRules {
    private WardrobeRules() {}

    public static boolean canWear(OutfitDefinition outfit, Wearer wearer, JobLookup jobs) {
        if (!wearer.onDuty() || !outfit.job().equals(wearer.job())) return false;
        if (outfit.minGrade() == null) return true;
        OptionalInt need = jobs.gradeLevel(outfit.job(), outfit.minGrade());
        OptionalInt have = jobs.gradeLevel(wearer.job(), wearer.grade());
        return need.isPresent() && have.isPresent() && have.getAsInt() >= need.getAsInt();
    }

    /** Le divise del lavoro del punto che il Character può indossare adesso. */
    public static List<OutfitDefinition> available(OutfitCatalog catalog, String pointJob, Wearer wearer, JobLookup jobs) {
        return catalog.forJob(pointJob).stream().filter(o -> canWear(o, wearer, jobs)).toList();
    }
}
