package dev.qrm.core.db;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseTest {
    private Database db;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE t(x INT)");
            }
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private int count() {
        return db.read(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM t")) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    @Test
    void txCommitsOnSuccess() {
        db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("INSERT INTO t VALUES (1)");
            }
            return null;
        });
        assertEquals(1, count());
    }

    @Test
    void txRollsBackOnException() {
        assertThrows(IllegalStateException.class, () -> db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("INSERT INTO t VALUES (1)");
            }
            throw new IllegalStateException("boom");
        }));
        assertEquals(0, count());
    }

    @Test
    void txRollsBackOnError() {
        assertThrows(StackOverflowError.class, () -> db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("INSERT INTO t VALUES (1)");
            }
            throw new StackOverflowError("boom");
        }));
        assertEquals(0, count());
    }
}
