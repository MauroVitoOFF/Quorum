package dev.qrm.wardrobe.logic;

import dev.qrm.api.id.CharacterId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Chi indossa quale divisa. Tutte le scelte e i controlli passano da qui; il client non decide nulla. */
public final class WardrobeService {
    private final WardrobeStorage storage;
    private final JobLookup jobs;
    private final Map<CharacterId, String> worn = new HashMap<>();
    private final Set<CharacterId> loaded = new HashSet<>();
    private OutfitCatalog catalog = OutfitCatalog.empty();

    public WardrobeService(WardrobeStorage storage, JobLookup jobs) {
        this.storage = storage;
        this.jobs = jobs;
    }

    public void setCatalog(OutfitCatalog catalog) {
        this.catalog = catalog;
    }

    public OutfitCatalog catalog() {
        return catalog;
    }

    /** {@code outfitId} vuoto = abito civile. */
    public ChooseResult choose(CharacterId character, String outfitId, String pointJob, Optional<Wearer> wearer) {
        if (outfitId.isEmpty()) {
            set(character, null);
            return ChooseResult.OK;
        }
        Optional<OutfitDefinition> outfit = catalog.find(outfitId);
        if (outfit.isEmpty()) return ChooseResult.UNKNOWN_OUTFIT;
        if (!outfit.get().job().equals(pointJob)) return ChooseResult.WRONG_JOB;
        if (wearer.isEmpty() || !WardrobeRules.canWear(outfit.get(), wearer.get(), jobs)) return ChooseResult.NOT_ALLOWED;
        set(character, outfitId);
        return ChooseResult.OK;
    }

    /** Ricontrolla la divisa indossata. Vero se è stata tolta perché non più valida. */
    public boolean revalidate(CharacterId character, Optional<Wearer> wearer) {
        Optional<String> id = worn(character);
        if (id.isEmpty()) return false;
        Optional<OutfitDefinition> outfit = catalog.find(id.get());
        if (outfit.isPresent() && wearer.isPresent() && WardrobeRules.canWear(outfit.get(), wearer.get(), jobs)) {
            return false;
        }
        set(character, null);
        return true;
    }

    public Optional<String> worn(CharacterId character) {
        if (loaded.add(character)) storage.read(character).ifPresent(id -> worn.put(character, id));
        return Optional.ofNullable(worn.get(character));
    }

    public Map<CharacterId, String> allWorn() {
        return Map.copyOf(worn);
    }

    private void set(CharacterId character, String idOrNull) {
        loaded.add(character);
        if (idOrNull == null) worn.remove(character);
        else worn.put(character, idOrNull);
        storage.write(character, idOrNull);
    }
}
