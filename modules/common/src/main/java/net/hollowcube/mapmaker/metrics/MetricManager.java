package net.hollowcube.mapmaker.metrics;

import net.hollowcube.world.storage.PostgreSQLManager;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetricManager {

    private PostgreSQLManager postgreSQLManager;

    public static void init(PostgreSQLManager postgreSQLManager) {
        postgreSQLManager = postgreSQLManager;

        // If metric value table is yet to exist, create it
        try {
            postgreSQLManager.execute("CREATE TABLE IF NOT EXISTS metrics " +
                    "(id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the provided metric to the metrics database
     * @param id
     * @param source
     * @param target
     * @param value
     * @return
     */
    public boolean addMetric(int id, String source, String target, double value) {
        try {
            return this.postgreSQLManager.execute("INSERT INTO metrics " +
                    "(id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE) " +
                    "VALUES (" + id + ", " + source + ", " + target + ", " + value + ")");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the provided metric in the metrics database, overwriting its previous value for
     * matching source and target, or applies the new value if it doesn't exist
     * @param id
     * @param source
     * @param target
     * @param value
     * @return
     */
    public boolean updateMetric(int id, String source, String target, double value) {
        try {
            ResultSet rs = this.postgreSQLManager.querySync("SELECT * FROM metrics WHERE " +
                    "id = " + id + " AND " +
                    "source = '" + source + "' AND " +
                    "target = '" + target + "'");
            if (rs.first()) {
                rs.deleteRow();
            }
            return addMetric(id, source, target, value);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a new metric locally (suggested for metrics which are called frequently) which will be stored
     * until the next call of syncMetrics
     * @param id
     * @param source
     * @param target
     * @param value
     * @return
     */
    public boolean addMetricLocal(int id, String source, String target, double value) {
        return false;
    }

    /**
     * Updates a metric locally (suggested for metrics which are called frequently) which will be stored
     * until the next call of syncMetrics
     * @param id
     * @param source
     * @param target
     * @param value
     * @return
     */
    public boolean updateMetricLocal(int id, String source, String target, double value) {
        return false;
    }

    /**
     * Takes all locally stored metrics and syncs them with the database
     * @return
     */
    public boolean syncMetrics() {
        return false;
    }

    /**
     * Returns a string printing the provided id, source, target, and value
     * @param id
     * @param source
     * @param target
     * @param value
     * @return
     */
    private String printMetric(int id, String source, String target, double value) {
        return "ID " + id + ", SRC " + source + ", TGT " + target + ", VAL" + value;
    }

    private String printMetric(int id, String source, String target) {
        try {
            ResultSet rs = this.postgreSQLManager.querySync("SELECT * FROM metrics WHERE " +
                    "id = " + id + " AND " +
                    "source = '" + source + "' AND " +
                    "target = '" + target + "'");
            if (rs.first()) {
                return printMetric(id, source, target, rs.getDouble("value"));
            }
            else {
                return "No metric for provided id, source, and target!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Failure in printing metrics.";
        }
    }
}
