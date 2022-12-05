package net.hollowcube.mapmaker.metrics;

import net.hollowcube.world.storage.PostgreSQLManager;

import java.sql.SQLException;

public class MetricManager {

    private PostgreSQLManager postgreSQLManager;
    private int id;

    public MetricManager(PostgreSQLManager postgreSQLManager) throws SQLException {
        this.postgreSQLManager = postgreSQLManager;

        // If metric value table is yet to exist, create it
        this.postgreSQLManager.execute("CREATE TABLE IF NOT EXISTS metrics " +
                "(id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE)");
    }

    public boolean addMetric(int id, String source, String target, double value) throws SQLException {
        return this.postgreSQLManager.execute("INSERT INTO metrics" +
                "(id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE)" +
                "VALUES (" + id + ", " + source + ", " + target + ", " + value + ")");
    }
}
