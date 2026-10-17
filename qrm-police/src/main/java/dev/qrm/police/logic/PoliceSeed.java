package dev.qrm.police.logic;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

/** Dati di prova per usare l'MDT da soli. Gli id derivano dal nome: rieseguire non duplica nessuno. */
public final class PoliceSeed {
    private static final String OFFICER = "Uff. Ferrari";

    private PoliceSeed() {}

    private static UUID idOf(String first, String last) {
        return UUID.nameUUIDFromBytes(("qrm-police-seed:" + first + last).getBytes(StandardCharsets.UTF_8));
    }

    private static Citizen citizen(PoliceDatabase db, String first, String last, String birth, Danger danger,
            boolean gunLicense, boolean driveOk) {
        Citizen c = db.ensureCitizen(idOf(first, last), first, last, birth);
        c.danger = danger;
        c.weaponLicense = gunLicense;
        c.drivingLicenseValid = driveOk;
        return c;
    }

    private static void record(PoliceDatabase db, Citizen c, long now, int daysAgo, String offense, long fine,
            int months, String notes) {
        db.addRecord(c, OFFICER, offense, fine, months, notes, now - daysAgo * 86_400_000L);
    }

    private static void weapon(PoliceDatabase db, Random rnd, String type, Citizen owner, WeaponStatus status) {
        db.putWeapon(new WeaponEntry(db.newSerial(rnd), type, owner == null ? "" : owner.id.toString(),
                owner == null ? "Polizia di Quorum" : owner.fullName(), status));
    }

    /** Aggiunge 10 cittadini, sei armi, due BOLO e il profilo del giocatore. @return il profilo del giocatore. */
    public static Citizen populate(PoliceDatabase db, UUID selfId, String selfFirst, String selfLast, long now,
            Random rnd) {
        Citizen self = db.ensureCitizen(selfId, selfFirst, selfLast, "1995-06-15");

        citizen(db, "Marco", "Bianchi", "1988-03-02", Danger.LOW, false, true);
        Citizen giulia = citizen(db, "Giulia", "Romano", "1992-11-21", Danger.LOW, true, true);
        citizen(db, "Elena", "Conti", "2000-01-09", Danger.LOW, false, true);
        citizen(db, "Paolo", "Greco", "1979-07-30", Danger.MEDIUM, false, false);

        Citizen luca = citizen(db, "Luca", "Moretti", "1990-05-14", Danger.HIGH, false, true);
        luca.wanted = true;
        luca.wantedReason = "Rapina a mano armata - gioielleria di Via Roma";
        record(db, luca, now, 40, "Rapina", 4000, 24, "Condannato, pena sospesa");
        record(db, luca, now, 12, "Resistenza a pubblico ufficiale", 1200, 3, "Fermato durante un controllo");

        Citizen viktor = citizen(db, "Viktor", "Kozlov", "1983-09-08", Danger.ARMED, false, true);
        viktor.wanted = true;
        viktor.wantedReason = "Tentato omicidio - sparatoria al porto";
        record(db, viktor, now, 90, "Possesso illegale di arma da fuoco", 3000, 12, "Arma mai recuperata");
        record(db, viktor, now, 20, "Aggressione", 1500, 6, "");
        record(db, viktor, now, 3, "Esplosione di colpi in luogo pubblico", 2500, 8, "Testimoni concordi");

        Citizen sara = citizen(db, "Sara", "De Luca", "1996-02-17", Danger.MEDIUM, true, true);
        record(db, sara, now, 30, "Eccesso di velocità", 250, 0, "");
        record(db, sara, now, 8, "Guida pericolosa", 500, 0, "");

        Citizen carlo = citizen(db, "Carlo", "Esposito", "1975-12-25", Danger.HIGH, false, false);
        carlo.wanted = true;
        carlo.wantedReason = "Spaccio di stupefacenti";
        record(db, carlo, now, 60, "Spaccio di stupefacenti", 3500, 18, "");

        Citizen anna = citizen(db, "Anna", "Ricci", "1985-04-04", Danger.LOW, true, true);
        record(db, anna, now, 100, "Disturbo della quiete pubblica", 300, 0, "");

        weapon(db, rnd, "Pistola semiautomatica 9mm", giulia, WeaponStatus.REGULAR);
        weapon(db, rnd, "Revolver .357", anna, WeaponStatus.REGULAR);
        weapon(db, rnd, "Pistola semiautomatica 9mm", sara, WeaponStatus.STOLEN);
        weapon(db, rnd, "Fucile a pompa", viktor, WeaponStatus.STOLEN);
        weapon(db, rnd, "Pistola d'ordinanza", null, WeaponStatus.POLICE_ISSUE);
        weapon(db, rnd, "Pistola semiautomatica 9mm", luca, WeaponStatus.SEIZED);

        db.addBolo("Sospettato in fuga: Viktor Kozlov",
                "Armato e pericoloso. Visto l'ultima volta in direzione del porto. Non avvicinarsi da soli.", 3,
                OFFICER, now);
        db.addBolo("Veicolo ricercato: berlina nera", "Berlina nera senza targa, coinvolta nella rapina di Via Roma.",
                2, OFFICER, now);
        return self;
    }
}
