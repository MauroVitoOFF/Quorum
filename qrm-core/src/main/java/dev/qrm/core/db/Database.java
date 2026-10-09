package dev.qrm.core.db;

import dev.qrm.core.QrmStorageException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

/** Una connessione JDBC condivisa e serializzata da un lock. Non annidare tx() dentro tx(). */
public final class Database implements AutoCloseable {
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final Connection connection;
    private final ReentrantLock lock = new ReentrantLock();

    private Database(Connection connection) {
        this.connection = connection;
    }

    public static Database open(String jdbcUrl) {
        return open(jdbcUrl, null, null);
    }

    public static Database open(String jdbcUrl, String user, String password) {
        try {
            Connection c;
            if (jdbcUrl.startsWith("jdbc:sqlite:")) {
                // Istanzia il driver direttamente: la scoperta via ServiceLoader non è affidabile nei layer dei mod.
                Properties props = new Properties();
                c = new org.sqlite.JDBC().connect(jdbcUrl, props);
                try (Statement s = c.createStatement()) {
                    s.execute("PRAGMA foreign_keys=ON");
                } catch (SQLException e) {
                    try {
                        c.close();
                    } catch (SQLException closeFailure) {
                        e.addSuppressed(closeFailure);
                    }
                    throw e;
                }
            } else {
                c = DriverManager.getConnection(jdbcUrl, user, password);
            }
            c.setAutoCommit(true);
            return new Database(c);
        } catch (SQLException e) {
            throw new QrmStorageException("Cannot open database", e);
        }
    }

    public <T> T tx(SqlWork<T> work) {
        lock.lock();
        try {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Throwable t) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    t.addSuppressed(rollbackFailure);
                }
                throw t;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new QrmStorageException("Database error", e);
        } finally {
            lock.unlock();
        }
    }

    public <T> T read(SqlWork<T> work) {
        lock.lock();
        try {
            return work.run(connection);
        } catch (SQLException e) {
            throw new QrmStorageException("Database error", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            connection.close();
        } catch (SQLException e) {
            throw new QrmStorageException("Cannot close database", e);
        } finally {
            lock.unlock();
        }
    }
}
