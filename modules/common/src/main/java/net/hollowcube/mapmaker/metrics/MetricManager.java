package net.hollowcube.mapmaker.metrics;

import net.hollowcube.world.storage.PostgreSQLManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public class MetricManager {

    private PostgreSQLManager postgreSQLManager;
    private Set<Metric> cachedMetricList;

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
     * @param metric
     * @return
     */
    public boolean addMetric(Metric metric) {
        try {
            return this.postgreSQLManager.execute("INSERT INTO metrics " +
                    "(id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE) " +
                    "VALUES (" + metric.id + ", " + metric.source + ", " + metric.target + ", " + metric.value + ")");
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
            return addMetric(new Metric(id, source, target, value));
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
    public void syncMetrics() {
        for (Metric metric : cachedMetricList) {
            boolean success = updateMetric(metric.id, metric.source, metric.target, metric.value);
            if (!success) {
                System.out.println("Failed to update metric " + metricString(metric));
            }
        }
    }

    /**
     * Returns a string printing the provided id, source, target, and value
     * @param metric
     * @return
     */
    private String metricString(Metric metric) {
        return "ID " + metric.id + ", SRC " + metric.source + ", TGT " + metric.target + ", VAL" + metric.value;
    }

    private String metricString(int id, String source, String target) {
        try {
            ResultSet rs = this.postgreSQLManager.querySync("SELECT * FROM metrics WHERE " +
                    "id = " + id + " AND " +
                    "source = '" + source + "' AND " +
                    "target = '" + target + "'");
            if (rs.first()) {
                return metricString(new Metric(id, source, target, rs.getDouble("value")));
            }
            else {
                return "No metric for provided id, source, and target!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Failure in printing metrics.";
        }
    }

    public Double getValue(int id, String source, String target) {
        try {
            ResultSet rs = this.postgreSQLManager.querySync("SELECT * FROM metrics WHERE " +
                    "id = " + id + " AND " +
                    "source = '" + source + "' AND " +
                    "target = '" + target + "'");
            if (rs.first()) {
                return rs.getDouble("value");
            } else {
                System.out.println("No value for provided metric");
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Double getCachedValue(int id, String source, String target) {
        try {
            Optional<Metric> matchingMetric =
                    cachedMetricList.stream().filter(metric -> (
                            (metric.id == id) && (metric.source == source) && (metric.target == target))).findFirst();
            return matchingMetric.get().value;
        } catch (NullPointerException e) {
            return null;
        }
    }
}
