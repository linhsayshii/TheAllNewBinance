package com.auction.server.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton Platform Thread Pool dedicated to blocking JDBC Database I/O.
 *
 * <p><b>Design Rationale</b>: Virtual Threads (used by the Socket Server) must NOT be used
 * directly for JDBC blocking I/O, for two critical reasons:
 *
 * <ol>
 *   <li><b>JDBC Driver Pinning</b>: MySQL Connector/J and MariaDB Java Client contain
 *       {@code synchronized} blocks in their Socket I/O and Packet Parsing layers. A Virtual Thread
 *       executing through these blocks gets pinned to its Carrier Thread, depleting the JVM's
 *       physical thread pool under high load and causing system-wide freezes.
 *   <li><b>Connection Pool Contention</b>: Creating unlimited Virtual Threads to fetch DB
 *       connections does not increase DB throughput — all threads still compete for a fixed number
 *       of physical connections (e.g. HikariCP maxPoolSize). This only increases contention on
 *       Pool's internal locks, reducing throughput.
 * </ol>
 *
 * <p><b>Solution</b>: A fixed Platform Thread Pool sized to match HikariCP's {@code maxPoolSize}
 * (default 20). This creates exactly one OS thread per available DB connection, achieving maximum
 * DB throughput with zero pinning risk. Socket Virtual Threads delegate DB tasks here via
 * {@code CompletableFuture.supplyAsync(..., DBExecutor.getExecutor())} and safely yield their
 * Carrier Thread while waiting.
 */
public final class DBExecutor {

    /**
     * Platform Thread Pool size must match or be close to the HikariCP maxPoolSize configuration.
     * Change this constant in sync with the DB pool size.
     */
    private static final int DB_POOL_SIZE = 20;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(DB_POOL_SIZE);

    private DBExecutor() {
        // Utility class — no instantiation
    }

    /**
     * Returns the shared Platform Thread Pool executor for all blocking DB I/O operations.
     *
     * @return Bounded Platform Thread Pool sized to DB connection pool capacity.
     */
    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }
}
