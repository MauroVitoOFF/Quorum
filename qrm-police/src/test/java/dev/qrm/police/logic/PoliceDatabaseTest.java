package dev.qrm.police.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PoliceDatabaseTest {
    private static PoliceDatabase seeded() {
        PoliceDatabase db = new PoliceDatabase();
        PoliceSeed.populate(db, UUID.randomUUID(), "Mario", "Rossi", 1_000_000_000_000L, new Random(1));
        return db;
    }

    @Test
    void seedHasCitizensWeaponsAndBolos() {
        PoliceDatabase db = seeded();
        assertEquals(10, db.citizens().size());
        assertEquals(3, db.wantedCount());
        assertEquals(6, db.weapons().size());
        assertEquals(2, db.bolos().size());
    }

    @Test
    void seedingTwiceDoesNotDuplicateCitizens() {
        PoliceDatabase db = new PoliceDatabase();
        UUID self = UUID.randomUUID();
        PoliceSeed.populate(db, self, "Mario", "Rossi", 0, new Random(1));
        PoliceSeed.populate(db, self, "Mario", "Rossi", 0, new Random(2));
        assertEquals(10, db.citizens().size());
    }

    @Test
    void jsonRoundTripKeepsEverything() {
        PoliceDatabase db = seeded();
        DispatchCall call = db.addCall(CallType.SHOOTING, "Colpi", "minecraft:overworld", 10, 64, -20, 5L, "Tizio");
        db.takeCall(call.id, "Rossi");
        PoliceDatabase back = PoliceDatabase.fromJson(db.toJson());
        assertEquals(db.citizens().size(), back.citizens().size());
        assertEquals(db.weapons().size(), back.weapons().size());
        assertEquals(1, back.calls().size());
        assertEquals("Colpi", back.calls().get(0).description);
        assertEquals(java.util.List.of("Rossi"), back.calls().get(0).units);
        assertEquals(db.toJson(), back.toJson());
    }

    @Test
    void brokenJsonGivesAnEmptyDatabase() {
        assertEquals(0, PoliceDatabase.fromJson("{non json").citizens().size());
    }

    @Test
    void searchMatchesNameInEitherOrder() {
        PoliceDatabase db = seeded();
        assertEquals(1, db.search("kozlov viktor").size());
        assertEquals(1, db.search("viktor kozlov").size());
        assertEquals(1, db.search("Kozlov").size());
    }

    @Test
    void callsAreNewestFirstAndUnitsJoinOnce() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.POLICE, "a", "d", 0, 0, 0, 1, "x");
        DispatchCall b = db.addCall(CallType.MEDICAL, "b", "d", 0, 0, 0, 2, "y");
        assertEquals(b.id, db.calls().get(0).id);
        assertTrue(db.takeCall(a.id, "Rossi"));
        assertFalse(db.takeCall(a.id, "Rossi"));
        assertTrue(db.closeCall(a.id));
        assertFalse(db.takeCall(a.id, "Rossi"));
    }

    @Test
    void weaponStatusChangesAndSerialsAreUnique() {
        PoliceDatabase db = seeded();
        WeaponEntry w = db.weapons().iterator().next();
        assertTrue(db.setWeaponStatus(w.serial().toLowerCase(), WeaponStatus.STOLEN));
        assertEquals(WeaponStatus.STOLEN, db.weapon(w.serial()).orElseThrow().status());
        assertFalse(db.setWeaponStatus("QRM-00000", WeaponStatus.SEIZED));
    }

    @Test
    void trimmedJsonKeepsTheNewestRecords() {
        PoliceDatabase db = seeded();
        Citizen c = db.search("Kozlov").get(0);
        PoliceDatabase back = PoliceDatabase.fromJson(db.toJson(1));
        assertEquals(1, back.citizen(c.id).orElseThrow().records.size());
        assertEquals("Esplosione di colpi in luogo pubblico", back.citizen(c.id).orElseThrow().records.get(0).offense());
    }
}
