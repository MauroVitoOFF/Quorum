package dev.qrm.police.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void seedHasCitizensAndWeapons() {
        PoliceDatabase db = seeded();
        assertEquals(10, db.citizens().size());
        assertEquals(3, db.wantedCount());
        assertEquals(6, db.weapons().size());
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
        db.takeCall(call.id, new Agente("Rossi", "QRM-000001", "u1"), 6L);
        PoliceDatabase back = PoliceDatabase.fromJson(db.toJson());
        assertEquals(db.citizens().size(), back.citizens().size());
        assertEquals(db.weapons().size(), back.weapons().size());
        assertEquals(1, back.calls().size());
        assertEquals("Colpi", back.calls().get(0).description);
        assertEquals("Rossi", back.calls().get(0).assignee);
        assertEquals(StatoChiamata.ASSIGNED, back.calls().get(0).state);
        assertEquals(db.toJson(), back.toJson());
    }

    @Test
    void boardSurvivesJsonAndClear() {
        PoliceDatabase db = new PoliceDatabase();
        db.setBoard("Turno serale: pattuglia al porto");
        assertEquals("Turno serale: pattuglia al porto", PoliceDatabase.fromJson(db.toJson()).board());
        db.clear();
        assertEquals("", db.board());
    }

    @Test
    void extraFieldsOnlyReachTheClientPacket() {
        PoliceDatabase db = new PoliceDatabase();
        String packet = db.toJson(40, o -> o.addProperty("me", "PATROL"));
        assertTrue(packet.contains("\"me\":\"PATROL\""));
        assertFalse(db.toJson().contains("\"me\""));
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

    private static final Agente ROSSI = new Agente("Rossi", "QRM-000001", "u1");
    private static final Agente BIANCHI = new Agente("Bianchi", "QRM-000002", "u2");

    @Test
    void callsGetSequentialIdsAndTheDefaultPriorityOfTheirType() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.PANIC, "sos", "d", 0, 0, 0, 1, "x", "u1");
        DispatchCall b = db.addCall(CallType.REPORT, "info", "d", 0, 0, 0, 2, "y");
        assertEquals("D-001", a.id);
        assertEquals("D-002", b.id);
        assertEquals(1, a.priority);
        assertEquals(4, b.priority);
        assertEquals("u1", a.callerId);
        assertEquals(b.id, db.calls().get(0).id, "la più recente per prima");
        assertEquals("RECEIVED", a.events.get(0).type());
    }

    @Test
    void onlyOneAgentCanTakeACall() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.ROBBERY, "a", "d", 0, 0, 0, 1, "x");
        assertTrue(db.takeCall(a.id, ROSSI, 10));
        assertFalse(db.takeCall(a.id, ROSSI, 11));
        assertFalse(db.takeCall(a.id, BIANCHI, 12), "già assegnata: nessuna sovrapposizione");
        assertEquals("Rossi", db.call(a.id).orElseThrow().assignee);
        assertEquals("QRM-000001", db.call(a.id).orElseThrow().assigneeNo);
        assertEquals(10, db.call(a.id).orElseThrow().assignedAt);
        assertEquals(a.id, db.activeCallOf("u1").orElseThrow().id);
    }

    @Test
    void stateMovesOnlyAlongValidSteps() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.ASSAULT, "a", "d", 0, 0, 0, 1, "x");
        assertFalse(db.advanceCall(a.id, StatoChiamata.EN_ROUTE, ROSSI, 2), "serve un incaricato");
        assertFalse(db.requestBackup(a.id, ROSSI, 2));
        db.takeCall(a.id, ROSSI, 3);
        assertTrue(db.advanceCall(a.id, StatoChiamata.EN_ROUTE, ROSSI, 4));
        assertFalse(db.advanceCall(a.id, StatoChiamata.EN_ROUTE, ROSSI, 5), "stesso stato");
        assertFalse(db.advanceCall(a.id, StatoChiamata.CLOSED, ROSSI, 5), "si chiude con closeCall");
        assertTrue(db.advanceCall(a.id, StatoChiamata.ON_SCENE, ROSSI, 6));
        assertTrue(db.requestBackup(a.id, ROSSI, 7));
        assertEquals(StatoChiamata.BACKUP_REQUESTED, db.call(a.id).orElseThrow().state);
        assertFalse(db.requestBackup(a.id, ROSSI, 8));
        assertTrue(db.advanceCall(a.id, StatoChiamata.INTERVENING, ROSSI, 9));
        assertEquals(StatoChiamata.INTERVENING, db.call(a.id).orElseThrow().state);
    }

    @Test
    void notesAreLoggedOnOpenCallsOnly() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.THEFT, "a", "d", 0, 0, 0, 1, "x");
        assertFalse(db.addNote(a.id, "   ", ROSSI, 2), "una nota vuota non vale");
        assertTrue(db.addNote(a.id, "Testimone sul posto", ROSSI, 3));
        assertEquals("Nota: Testimone sul posto", a.events.get(a.events.size() - 1).text());
        db.cancelCall(a.id, "", ROSSI, 4);
        assertFalse(db.addNote(a.id, "troppo tardi", ROSSI, 5), "una chiamata chiusa non si modifica");
        assertFalse(db.addNote("D-999", "x", ROSSI, 5));
    }

    @Test
    void releaseAndReassignKeepTheHistory() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.THEFT, "a", "d", 0, 0, 0, 1, "x");
        assertFalse(db.releaseCall(a.id, ROSSI, "", 2), "nessun incaricato");
        db.takeCall(a.id, ROSSI, 3);
        assertTrue(db.releaseCall(a.id, ROSSI, "", 4));
        assertEquals(StatoChiamata.PENDING, a.state);
        assertTrue(a.assignee.isEmpty());
        assertTrue(db.takeCall(a.id, BIANCHI, 5), "di nuovo disponibile");
        assertFalse(db.assignCall(a.id, BIANCHI, ROSSI, 6), "è già l'incaricato");
        assertTrue(db.assignCall(a.id, ROSSI, BIANCHI, 7));
        assertEquals("Rossi", a.assignee);
        assertEquals(StatoChiamata.ASSIGNED, a.state);
        // ricezione, presa in carico, rilascio, presa in carico, riassegnazione
        assertEquals(5, a.events.size());
    }

    @Test
    void priorityChangesAreClampedAndLogged() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.THEFT, "a", "d", 0, 0, 0, 1, "x");
        assertTrue(db.setPriority(a.id, 1, ROSSI, 2));
        assertEquals(1, a.priority);
        assertFalse(db.setPriority(a.id, 1, ROSSI, 3), "nessun cambiamento");
        assertTrue(db.setPriority(a.id, 99, ROSSI, 4));
        assertEquals(4, a.priority);
        assertEquals("PRIORITY", a.events.get(a.events.size() - 1).type());
    }

    @Test
    void closedAndCanceledCallsAreFrozenAndLeaveTheActiveList() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.ROBBERY, "a", "d", 0, 0, 0, 1, "x");
        DispatchCall b = db.addCall(CallType.ROBBERY, "b", "d", 0, 0, 0, 2, "x");
        db.takeCall(a.id, ROSSI, 3);
        assertTrue(db.closeCall(a.id, "Sospettato arrestato", ROSSI, 4));
        assertEquals(StatoChiamata.CLOSED, a.state);
        assertEquals("Sospettato arrestato", a.outcome);
        assertEquals(4, a.closedAt);
        assertFalse(db.closeCall(a.id, "", ROSSI, 5));
        assertFalse(db.takeCall(a.id, BIANCHI, 5));
        assertFalse(db.releaseCall(a.id, ROSSI, "", 5));
        assertTrue(db.cancelCall(b.id, "Falso allarme", ROSSI, 6));
        assertEquals(StatoChiamata.CANCELED, b.state);
        assertTrue(db.activeCalls().isEmpty());
        assertEquals(2, db.calls().size(), "restano nello storico");
        assertTrue(db.activeCallOf("u1").isEmpty());
    }

    @Test
    void tooManyOpenCallsCancelTheOldestOnes() {
        PoliceDatabase db = new PoliceDatabase();
        for (int i = 0; i < 5; i++) db.addCall(CallType.OTHER, "c" + i, "d", 0, 0, 0, i, "x");
        db.trimActive(3, 100);
        assertEquals(3, db.activeCalls().size());
        assertEquals("c4", db.activeCalls().get(0).description);
        assertEquals(StatoChiamata.CANCELED, db.calls().get(4).state);
    }

    @Test
    void callsSurviveJsonButClosedOnesStayOutOfTheClientPacket() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall open = db.addCall(CallType.SHOOTING, "aperta", "minecraft:overworld", 1, 2, 3, 1, "Tizio", "u9");
        db.takeCall(open.id, ROSSI, 2);
        db.advanceCall(open.id, StatoChiamata.ON_SCENE, ROSSI, 3);
        DispatchCall done = db.addCall(CallType.THEFT, "chiusa", "minecraft:overworld", 1, 2, 3, 4, "Caio");
        db.closeCall(done.id, "ok", ROSSI, 5);
        PoliceDatabase back = PoliceDatabase.fromJson(db.toJson());
        assertEquals(2, back.calls().size());
        DispatchCall again = back.call(open.id).orElseThrow();
        assertEquals(StatoChiamata.ON_SCENE, again.state);
        assertEquals("QRM-000001", again.assigneeNo);
        assertEquals("u9", again.callerId);
        assertEquals(3, again.events.size());
        assertEquals(StatoChiamata.CLOSED, back.call(done.id).orElseThrow().state);
        assertEquals(db.toJson(), back.toJson());
        PoliceDatabase forClient = PoliceDatabase.fromJson(db.toJson(40));
        assertEquals(1, forClient.calls().size());
        assertEquals(open.id, forClient.calls().get(0).id);
        // l'id successivo non ripete quelli già dati
        assertEquals("D-003", back.addCall(CallType.OTHER, "x", "d", 0, 0, 0, 9, "y").id);
    }

    @Test
    void oldCallDataIsMigrated() {
        String old = "{\"next\":3,\"citizens\":[],\"weapons\":[],\"calls\":[{\"id\":\"C1\",\"type\":\"SHOOTING\","
                + "\"desc\":\"Colpi\",\"dim\":\"minecraft:overworld\",\"x\":1,\"y\":2,\"z\":3,\"time\":5,"
                + "\"caller\":\"Tizio\",\"units\":[\"Rossi\"]},{\"id\":\"C2\",\"type\":\"POLICE\",\"desc\":\"x\","
                + "\"dim\":\"d\",\"x\":0,\"y\":0,\"z\":0,\"time\":6,\"caller\":\"y\",\"units\":[]}]}";
        PoliceDatabase db = PoliceDatabase.fromJson(old);
        assertEquals(2, db.calls().size());
        DispatchCall taken = db.call("C1").orElseThrow();
        assertEquals("Rossi", taken.assignee);
        assertEquals(StatoChiamata.ASSIGNED, taken.state);
        assertEquals(1, taken.priority);
        assertEquals(StatoChiamata.PENDING, db.call("C2").orElseThrow().state);
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
    void citizensGetStableUniqueIdNumbers() {
        PoliceDatabase db = new PoliceDatabase();
        Citizen a = db.ensureCitizen(UUID.randomUUID(), "Anna", "Verdi", "1990-01-01");
        Citizen b = db.ensureCitizen(UUID.randomUUID(), "Bruno", "Neri", "1991-02-02");
        assertEquals("QRM-000001", a.idNumber);
        assertEquals("QRM-000002", b.idNumber);
        assertSame(a, db.ensureCitizen(a.id, "X", "Y", "-"));
        PoliceDatabase back = PoliceDatabase.fromJson(db.toJson());
        assertEquals("QRM-000002", back.citizen(b.id).orElseThrow().idNumber);
        Citizen c = back.ensureCitizen(UUID.randomUUID(), "Carla", "Blu", "1992-03-03");
        assertEquals("QRM-000003", c.idNumber);
    }

    @Test
    void recordKindsSurviveJsonAndOldDataIsInferred() {
        PoliceDatabase db = new PoliceDatabase();
        Citizen c = db.ensureCitizen(UUID.randomUUID(), "Anna", "Verdi", "1990-01-01");
        db.addRecord(c, RecordKind.NOTE, "Uff. Rossi", "Frequenta il porto", 0, 0, "", 5);
        db.addRecord(c, "Uff. Rossi", "Rapina", 100, 6, "", 6);
        db.addRecord(c, "Uff. Rossi", "Velocità", 50, 0, "", 7);
        var back = PoliceDatabase.fromJson(db.toJson()).citizen(c.id).orElseThrow().records;
        assertEquals(RecordKind.NOTE, back.get(0).kind());
        assertEquals(RecordKind.ARREST, back.get(1).kind());
        assertEquals(RecordKind.FINE, back.get(2).kind());
    }

    @Test
    void finesUseCatalogAmountAndLeaveATraceInTheHistory() {
        PoliceDatabase db = new PoliceDatabase();
        Citizen c = db.ensureCitizen(UUID.randomUUID(), "Anna", "Verdi", "1990-01-01");
        var inf = FineCatalog.find("STR1").orElseThrow();
        Fine f = db.issueFine(c, inf, "Superato di 30 km/h", "Uff. Rossi", 10);
        assertEquals(inf.amount(), f.amount);
        assertEquals(FineStatus.UNPAID, f.status);
        assertEquals(1, c.records.size());
        assertEquals(RecordKind.FINE, c.records.get(0).kind());
        assertEquals("Infrazioni stradali", FineCatalog.categoryOf("STR1"));
        assertTrue(FineCatalog.find("XXX").isEmpty());

        Fine back = PoliceDatabase.fromJson(db.toJson()).fine(f.id).orElseThrow();
        assertEquals("Anna Verdi", back.citizenName);
        assertEquals("Superato di 30 km/h", back.reason);
    }

    @Test
    void finePaymentStatusOnlyMovesOutOfUnpaid() {
        PoliceDatabase db = new PoliceDatabase();
        Citizen c = db.ensureCitizen(UUID.randomUUID(), "Anna", "Verdi", "1990-01-01");
        Fine paid = db.issueFine(c, FineCatalog.find("STR2").orElseThrow(), "", "Uff. Rossi", 1);
        Fine canceled = db.issueFine(c, FineCatalog.find("QUI1").orElseThrow(), "", "Uff. Rossi", 2);
        assertTrue(db.setFineStatus(paid.id, FineStatus.PAID));
        assertFalse(db.setFineStatus(paid.id, FineStatus.CANCELED));
        assertFalse(db.setFineStatus(canceled.id, FineStatus.UNPAID));
        assertFalse(db.setFineStatus("F999", FineStatus.PAID));
        assertEquals(2, c.records.size());
        assertTrue(db.setFineStatus(canceled.id, FineStatus.CANCELED));
        assertEquals(1, c.records.size());
        assertEquals(FineStatus.CANCELED, db.fine(canceled.id).orElseThrow().status);
    }

    @Test
    void archivedRecordsStayButCanBeRestoredAndUnpaidFinesCannotBeArchived() {
        PoliceDatabase db = new PoliceDatabase();
        Citizen c = db.ensureCitizen(UUID.randomUUID(), "Anna", "Verdi", "1990-01-01");
        CriminalRecord arrest = db.addRecord(c, RecordKind.ARREST, "Uff. Rossi", "Rapina", 100, 6, "", 1);
        Fine fine = db.issueFine(c, FineCatalog.find("STR3").orElseThrow(), "", "Uff. Rossi", 2);

        assertFalse(db.archiveRecord(c, fine.recordId, true, "Uff. Bianchi"), "multa non pagata");
        assertTrue(db.setFineStatus(fine.id, FineStatus.PAID));
        assertTrue(db.archiveRecord(c, fine.recordId, true, "Uff. Bianchi"));
        assertTrue(db.archiveRecord(c, arrest.id(), true, "Uff. Bianchi"));
        assertFalse(db.archiveRecord(c, arrest.id(), true, "Uff. Bianchi"), "già archiviata");
        assertFalse(db.archiveRecord(c, "R999", true, "Uff. Bianchi"));
        assertEquals(2, c.records.size());
        assertTrue(c.records.stream().allMatch(CriminalRecord::archived));

        var back = PoliceDatabase.fromJson(db.toJson()).citizen(c.id).orElseThrow().records;
        assertTrue(back.get(0).archived());
        assertEquals("Uff. Bianchi", back.get(0).archivedBy());
        assertTrue(db.archiveRecord(c, arrest.id(), false, "Uff. Bianchi"));
        assertFalse(c.records.get(0).archived());
        assertEquals("", c.records.get(0).archivedBy());
    }

    @Test
    void arrestsSumTheCodeSanctionsAndRefuseUnknownOffenses() {
        PoliceDatabase db = new PoliceDatabase();
        Citizen c = db.ensureCitizen(UUID.randomUUID(), "Anna", "Verdi", "1990-01-01");
        CriminalRecord r = db.registerArrest(c, java.util.List.of("Rapina", "Resistenza a pubblico ufficiale", "Rapina"),
                "Fermato in flagranza", "Uff. Rossi", 1);
        assertEquals(RecordKind.ARREST, r.kind());
        assertEquals(5200, r.fine());
        assertEquals(27, r.jailMonths());
        assertEquals("Rapina, Resistenza a pubblico ufficiale", r.offense());
        assertEquals(1, c.records.size());
        assertNull(db.registerArrest(c, java.util.List.of("Inventato"), "", "Uff. Rossi", 2));
        assertNull(db.registerArrest(c, java.util.List.of(), "", "Uff. Rossi", 3));
        assertEquals(1, c.records.size());
    }

    @Test
    void trimmedJsonKeepsTheNewestRecords() {
        PoliceDatabase db = seeded();
        Citizen c = db.search("Kozlov").get(0);
        PoliceDatabase back = PoliceDatabase.fromJson(db.toJson(1));
        assertEquals(1, back.citizen(c.id).orElseThrow().records.size());
        assertEquals("Esplosione di colpi in luogo pubblico", back.citizen(c.id).orElseThrow().records.get(0).offense());
    }

    @Test
    void historyListsOnlyFinishedCallsAndAppliesTheFilters() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.ROBBERY, "banca centrale", "d", 0, 0, 0, 100, "Tizio", "u9");
        DispatchCall b = db.addCall(CallType.THEFT, "borsa rubata", "d", 0, 0, 0, 200, "Caio");
        DispatchCall c = db.addCall(CallType.REPORT, "info", "d", 0, 0, 0, 300, "Sempronio");
        db.addCall(CallType.SHOOTING, "ancora aperta", "d", 0, 0, 0, 400, "x");
        db.takeCall(a.id, ROSSI, 110);
        db.closeCall(a.id, "arrestato", ROSSI, 120);
        db.takeCall(b.id, BIANCHI, 210);
        db.closeCall(b.id, "recuperata", BIANCHI, 220);
        db.cancelCall(c.id, "falso allarme", ROSSI, 310);

        FiltroStorico all = new FiltroStorico("", "", 0, null, 0, Long.MAX_VALUE, true, "", "");
        assertEquals(3, db.history(all, 40).size(), "le aperte non compaiono");
        assertEquals(c.id, db.history(all, 40).get(0).id, "dalla più recente");
        assertEquals(1, db.history(all, 1).size());

        assertEquals(1, db.history(new FiltroStorico("bianchi", "", 0, null, 0, Long.MAX_VALUE, true, "", ""), 40).size());
        assertEquals(1, db.history(new FiltroStorico("QRM-000001", "", 0, null, 0, Long.MAX_VALUE, true, "", ""), 40).size());
        assertEquals(1, db.history(new FiltroStorico("", "", 1, null, 0, Long.MAX_VALUE, true, "", ""), 40).size());
        assertEquals(1, db.history(new FiltroStorico("", "", 0, StatoChiamata.CANCELED, 0, Long.MAX_VALUE, true, "", ""), 40)
                .size());
        assertEquals(1, db.history(new FiltroStorico("", "", 0, null, 150, 250, true, "", ""), 40).size());
        assertEquals(1, db.history(new FiltroStorico("", CallType.THEFT.category(), 0, StatoChiamata.CLOSED, 0,
                Long.MAX_VALUE, true, "", ""), 40).size());
    }

    @Test
    void anAgentSeesOnlyTheInterventionsHeTookPartIn() {
        PoliceDatabase db = new PoliceDatabase();
        DispatchCall a = db.addCall(CallType.ROBBERY, "uno", "d", 0, 0, 0, 100, "Tizio", "u9");
        DispatchCall b = db.addCall(CallType.THEFT, "due", "d", 0, 0, 0, 200, "Caio");
        db.takeCall(a.id, ROSSI, 110);
        db.closeCall(a.id, "ok", ROSSI, 120);
        db.takeCall(b.id, BIANCHI, 210);
        db.closeCall(b.id, "ok", BIANCHI, 220);
        assertEquals(1, db.history(new FiltroStorico("", "", 0, null, 0, Long.MAX_VALUE, false, "u1", "QRM-000001"), 40)
                .size());
        assertEquals(a.id, db.history(new FiltroStorico("", "", 0, null, 0, Long.MAX_VALUE, false, "u1", "QRM-000001"), 40)
                .get(0).id);
        assertEquals(1, db.history(new FiltroStorico("", "", 0, null, 0, Long.MAX_VALUE, false, "u9", ""), 40).size(),
                "chi ha chiamato vede la sua chiamata");
        assertTrue(db.history(new FiltroStorico("", "", 0, null, 0, Long.MAX_VALUE, false, "u3", "QRM-000003"), 40)
                .isEmpty());
    }

    @Test
    void agentStatesYouPickAreSeparateFromTheOnesDrivenByTheCall() {
        assertTrue(StatoAgente.PATROL.selectable());
        assertTrue(StatoAgente.OFF.selectable());
        assertFalse(StatoAgente.EN_ROUTE.selectable());
        assertFalse(StatoAgente.INTERVENING.selectable());
        assertEquals(StatoAgente.ON_SCENE, StatoAgente.parse("ON_SCENE", StatoAgente.OFF));
    }
}
