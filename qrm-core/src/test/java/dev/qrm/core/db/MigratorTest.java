package dev.qrm.core.db;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.core.QrmStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigratorTest {
    private Database db;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private int scalar(String sql) {
        return db.read(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    private void exec(String sql) {
        db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            }
            return null;
        });
    }

    @Test
    void createsAllTables() {
        Migrator.migrate(db);
        for (String table : List.of("qrm_player", "qrm_character", "qrm_account", "qrm_account_balance",
                "qrm_transaction", "qrm_mod_data", "qrm_character_job", "qrm_permission_override",
                "qrm_organization", "qrm_org_rank", "qrm_org_member", "qrm_interaction_point")) {
            assertEquals(0, scalar("SELECT COUNT(*) FROM " + table), table);
        }
    }

    @Test
    void recordsVersionAndIsIdempotent() {
        Migrator.migrate(db);
        Migrator.migrate(db);
        assertEquals(8, scalar("SELECT COUNT(*) FROM qrm_schema_version"));
        assertEquals(8, scalar("SELECT MAX(version) FROM qrm_schema_version"));
    }

    @Test
    void upgradesFromV1KeepingExistingData() throws IOException {
        // Simula un database creato dalla v0.1: schema V1 + riga di versione 1 + dati.
        exec("CREATE TABLE qrm_schema_version (version INT PRIMARY KEY, applied_at BIGINT NOT NULL)");
        String v1;
        try (InputStream in = Migrator.class.getResourceAsStream("/db/migration/V1__init.sql")) {
            v1 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String stmt : v1.replaceAll("(?m)^\\s*--.*$", "").split(";")) {
            if (!stmt.isBlank()) exec(stmt.strip());
        }
        exec("INSERT INTO qrm_schema_version VALUES (1, 0)");
        exec("INSERT INTO qrm_player (id, first_seen, last_seen, character_slots) VALUES ('p1', 1, 1, 3)");

        Migrator.migrate(db);

        assertEquals(8, scalar("SELECT MAX(version) FROM qrm_schema_version"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM qrm_player"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM qrm_character_job"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM qrm_organization"));
    }

    @Test
    void upgradesFromV2KeepingExistingData() throws IOException {
        // Simula un database v0.2: schema V1 + V2, righe di versione 1 e 2, dati.
        exec("CREATE TABLE qrm_schema_version (version INT PRIMARY KEY, applied_at BIGINT NOT NULL)");
        for (String file : List.of("/db/migration/V1__init.sql", "/db/migration/V2__jobs_permissions.sql")) {
            String sql;
            try (InputStream in = Migrator.class.getResourceAsStream(file)) {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (String stmt : sql.replaceAll("(?m)^\s*--.*$", "").split(";")) {
                if (!stmt.isBlank()) exec(stmt.strip());
            }
        }
        exec("INSERT INTO qrm_schema_version VALUES (1, 0)");
        exec("INSERT INTO qrm_schema_version VALUES (2, 0)");
        exec("INSERT INTO qrm_player (id, first_seen, last_seen, character_slots) VALUES ('p1', 1, 1, 3)");
        exec("INSERT INTO qrm_character (id, player_id, first_name, last_name, name_key, birth_date, status, created_at)"
                + " VALUES ('c1','p1','Mario','Rossi','mario rossi','1990-01-01','ACTIVE',1)");
        exec("INSERT INTO qrm_character_job (character_id, job_id, grade_id, on_duty, since)"
                + " VALUES ('c1','police','cadet',1,1)");

        Migrator.migrate(db);

        assertEquals(8, scalar("SELECT MAX(version) FROM qrm_schema_version"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM qrm_player"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM qrm_character"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM qrm_character_job WHERE character_id='c1' AND job_id='police'"));
        for (String table : List.of("qrm_organization", "qrm_org_rank", "qrm_org_member")) {
            assertEquals(0, scalar("SELECT COUNT(*) FROM " + table), table);
        }
    }

    @Test
    void jobAssignmentAndOverrideRequireAnExistingCharacter() {
        Migrator.migrate(db);
        assertThrows(QrmStorageException.class, () -> exec(
                "INSERT INTO qrm_character_job (character_id, job_id, grade_id, on_duty, since) VALUES ('nope','police','cadet',0,1)"));
        assertThrows(QrmStorageException.class, () -> exec(
                "INSERT INTO qrm_permission_override (character_id, node, effect) VALUES ('nope','a.b','GRANT')"));
    }

    @Test
    void organizationTablesEnforceReferencesAndUniqueLevels() {
        Migrator.migrate(db);
        exec("INSERT INTO qrm_account (id, owner_type, owner_id, status, overdraft_allowed, created_at)"
                + " VALUES ('a1','ORGANIZATION','o1','ACTIVE',0,1)");
        exec("INSERT INTO qrm_organization (id, label, status, account_id, created_at) VALUES ('acme','Acme','ACTIVE','a1',1)");
        exec("INSERT INTO qrm_org_rank (org_id, rank_id, label, level, permissions) VALUES ('acme','boss','Capo',5,'a.b')");
        assertThrows(QrmStorageException.class, () -> exec(
                "INSERT INTO qrm_org_rank (org_id, rank_id, label, level, permissions) VALUES ('acme','other','Altro',5,'')"));
        assertThrows(QrmStorageException.class, () -> exec(
                "INSERT INTO qrm_org_rank (org_id, rank_id, label, level, permissions) VALUES ('nope','boss','Capo',1,'')"));
        assertThrows(QrmStorageException.class, () -> exec(
                "INSERT INTO qrm_org_member (org_id, character_id, rank_id, since) VALUES ('acme','nope','boss',1)"));
        assertThrows(QrmStorageException.class, () -> exec(
                "INSERT INTO qrm_organization (id, label, status, account_id, created_at) VALUES ('x','X','ACTIVE','missing',1)"));
    }
}
