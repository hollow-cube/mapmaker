package net.hollowcube.mapmaker.metrics;

import net.hollowcube.mapmaker.storage.PostgreSQLManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public class MetricManager {

    private static PostgreSQLManager postgreSQLManager;
    private static Set<Metric> cachedMetricList;

    public static void init(PostgreSQLManager postgreSQLManager) {
        postgreSQLManager = postgreSQLManager;

        // If metric value table is yet to exist, create it
        try {
            postgreSQLManager.execute("CREATE TABLE IF NOT EXISTS metrics " +
                    "(id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Register callback to sync metrics
        Runnable syncMetrics = () -> syncMetrics();
        TaskSchedule delay = TaskSchedule.minutes(5);
        TaskSchedule repeat = TaskSchedule.minutes(10);
        MinecraftServer.getSchedulerManager().scheduleTask(syncMetrics, delay, repeat);
    }

    /**
     * Adds the provided metric to the cached metric list. Updates the metric if it already exists.
     * @param metric
     */
    public void addMetric(Metric metric) {
        cachedMetricList.add(metric);
    }

    /**
     * Adds the provided metric to the metrics database. Updates the metric if it already exists.
     * @param metric
     * @return
     */
    private static boolean uploadMetric(Metric metric) {
        try {
            Connection conn = postgreSQLManager.postgreSQL.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO metrics (id INTEGER, source VARCHAR(100), target VARCHAR(100), value DOUBLE) " +
                            "VALUES (?, ?, ?, ?)"
            );
            stmt.setInt(1, metric.id);
            stmt.setString(2, metric.source);
            stmt.setString(3, metric.target);
            stmt.setDouble(4, metric.value);
            return postgreSQLManager.execute(stmt);
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
    private static boolean updateMetricDB(int id, String source, String target, double value) {
        try {
            Connection conn = postgreSQLManager.postgreSQL.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM metrics WHERE id = ? AND source = '?' AND target = '?'"
            );
            stmt.setInt(1, id);
            stmt.setString(2, source);
            stmt.setString(3, target);
            ResultSet rs = postgreSQLManager.querySync(stmt);
            if (rs.first()) {
                rs.deleteRow();
            }
            return uploadMetric(new Metric(id, source, target, value));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Takes all locally stored metrics and syncs them with the database
     * @return
     */
    public static void syncMetrics() {
        for (Metric metric : cachedMetricList) {
            boolean success = updateMetricDB(metric.id, metric.source, metric.target, metric.value);
            if (!success) {
                System.out.println("Failed to update metric " + metricString(metric));
            } else {
                cachedMetricList.remove(metric);
            }
        }
    }

    /**
     * Returns a string printing the provided id, source, target, and value
     * @param metric
     * @return
     */
    private static String metricString(Metric metric) {
        return "ID " + metric.id + ", SRC " + metric.source + ", TGT " + metric.target + ", VAL" + metric.value;
    }

    public Double getValue(int id, String source, String target) {
        Double value;
        if ((value = getCachedValue(id, source, target)) != null) {
            return value;
        }
        try {
            Connection conn = postgreSQLManager.postgreSQL.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM metrics WHERE id = ? AND source = '?' AND target = '?'"
            );
            stmt.setInt(1, id);
            stmt.setString(2, source);
            stmt.setString(3, target);
            ResultSet rs = postgreSQLManager.querySync(stmt);
            if (rs.first()) {
                return rs.getDouble("value");
            } else {
                System.out.println("No value for provided metric (cached or in database).");
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Double getCachedValue(int id, String source, String target) {
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
