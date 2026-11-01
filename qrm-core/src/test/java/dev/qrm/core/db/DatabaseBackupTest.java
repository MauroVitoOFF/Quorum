package dev.qrm.core.db;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseBackupTest {
    @TempDir
    Path dir;

    private static int scalar(Database db, String sql) {
        return db.read(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    private static void exec(Database db, String sql) {
        db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            }
            return null;
        });
    }

    private String url() {
        return "jdbc:sqlite:" + dir.resolve("qrm.db");
    }

    @Test
    void newDatabaseNeedsNoBackup() {
        try (Database db = Database.open(url())) {
            assertNull(DatabaseBackup.beforeMigration(db, url()));
        }
    }

    @Test
    void inMemoryAndUpToDateDatabasesNeedNoBackup() {
        try (Database db = Database.open("jdbc:sqlite::memory:")) {
            Migrator.migrate(db);
            assertNull(DatabaseBackup.beforeMigration(db, "jdbc:sqlite::memory:"));
        }
        try (Database db = Database.open(url())) {
            Migrator.migrate(db);
            assertNull(DatabaseBackup.beforeMigration(db, url()));
        }
        assertFalse(Files.exists(dir.resolve("qrm.db.bak-v" + Migrator.latestVersion())));
    }

    @Test
    void outdatedDatabaseIsCopiedWithItsData() {
        try (Database db = Database.open(url())) {
            Migrator.migrate(db);
            exec(db, "INSERT INTO qrm_player (id, first_seen, last_seen, character_slots) VALUES ('p1', 1, 1, 3)");
            // simula un database rimasto a una versione precedente
            exec(db, "DELETE FROM qrm_schema_version WHERE version > 3");

            Path backup = DatabaseBackup.beforeMigration(db, url());

            assertEquals(dir.resolve("qrm.db.bak-v3"), backup);
            assertTrue(Files.isRegularFile(backup));
            try (Database copy = Database.open("jdbc:sqlite:" + backup)) {
                assertEquals(1, scalar(copy, "SELECT COUNT(*) FROM qrm_player"));
                assertEquals(3, scalar(copy, "SELECT MAX(version) FROM qrm_schema_version"));
            }
            // una seconda chiamata non sovrascrive la copia
            assertNull(DatabaseBackup.beforeMigration(db, url()));
        }
    }

    @Test
    void fileDatabaseUsesWalAndBusyTimeout() {
        try (Database db = Database.open(url())) {
            db.read(c -> {
                try (Statement s = c.createStatement()) {
                    try (ResultSet rs = s.executeQuery("PRAGMA journal_mode")) {
                        rs.next();
                        assertEquals("wal", rs.getString(1));
                    }
                    try (ResultSet rs = s.executeQuery("PRAGMA busy_timeout")) {
                        rs.next();
                        assertEquals(5000, rs.getInt(1));
                    }
                }
                return null;
            });
        }
    }
}
