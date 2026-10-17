package dev.qrm.police.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Scheda anagrafica e casellario di un cittadino. */
public final class Citizen {
    public final UUID id;
    public String firstName;
    public String lastName;
    public String birthDate;
    public boolean wanted;
    public String wantedReason = "";
    public Danger danger = Danger.LOW;
    public boolean weaponLicense;
    public boolean drivingLicenseValid = true;
    public final List<CriminalRecord> records = new ArrayList<>();

    public Citizen(UUID id, String firstName, String lastName, String birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public boolean matches(String query) {
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isEmpty()) return true;
        String name = fullName().toLowerCase(java.util.Locale.ROOT);
        String reversed = (lastName + " " + firstName).toLowerCase(java.util.Locale.ROOT);
        return name.contains(q) || reversed.contains(q);
    }
}
