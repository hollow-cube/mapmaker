package net.hollowcube.world.storage;

import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

// ==============
// UNUSED FOR NOW
// ==============
public class PostgreSQLManager {
    private static PostgreSQLManager postgreSQLManager;
    private String address;
    private int port;
    private String database;
    private String username;
    private String password;

    /**
     * Initializes a default SQL connection
     */
    public static void init() {
        postgreSQLManager = new PostgreSQLManager();
        String address = "localhost";
        int port = 5432;
        String database = "postgres";
        String username = "postgres";
        String password = "starlight";
        postgreSQLManager.connectToPostgreSQL(address, port, username, password, database);
    }

    public static PostgreSQLManager get() {
        return postgreSQLManager;
    }

    /**
     * Initializes a specific SQL connection
     * @param address
     * @param port
     * @param username
     * @param password
     * @param database
     */
    public static void init(String address, int port, String username, String password, String database) {
        postgreSQLManager = new PostgreSQLManager();
        postgreSQLManager.connectToPostgreSQL(address, port, username, password, database);
    }

    public Executor executor;
    public PostgreSQL postgreSQL;
    public boolean isConnected = false;

    public void connectToPostgreSQL(String address, int port, String username, String password, String database) {
        this.executor = Executors.newCachedThreadPool();
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        try {
            this.postgreSQL = new PostgreSQL(address, port, username, password, database);
            if(postgreSQL.isConnected()) {
                isConnected = true;
            }
        } catch (SQLException s) {
            s.printStackTrace();
        }
    }

    public void insertAndGetPrimaryKey(String str, Consumer<Integer> consumer){
        MinecraftServer.getSchedulerManager().buildTask(() ->
        {
            try {
                postgreSQL.getConnection().prepareStatement(str).execute();
                ResultSet rs = postgreSQL.getConnection().prepareStatement("SELECT LAST_INSERT_ID();").executeQuery();
                if(rs.next()) consumer.accept(rs.getInt("LAST_INSERT_ID()"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).executionType(ExecutionType.ASYNC).schedule();
    }

    public void queryUpdate(String str) {
        MinecraftServer.getSchedulerManager().buildTask(() ->
        {
            try {
                postgreSQL.getConnection().prepareStatement(str).execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).executionType(ExecutionType.ASYNC).schedule();
    }

    public void queryUpdateSync(String str) throws SQLException {
        postgreSQL.getConnection().prepareStatement(str).execute();
    }

    public void query(String str, Consumer<ResultSet> consumer){
        MinecraftServer.getSchedulerManager().buildTask(() ->
        {
            try {
                consumer.accept(querySync(str));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).executionType(ExecutionType.ASYNC).schedule();
    }

    public ResultSet querySync(String str) throws SQLException {
        return postgreSQL.getConnection().prepareStatement(str).executeQuery();
    }

    public boolean execute(String str) throws SQLException {
        return postgreSQL.getConnection().prepareStatement(str).execute();
    }

    public static class PostgreSQL {

        private final String host;
        private final String user;
        private final String password;
        private final String database;
        private final int port;
        private Connection conn;

        public PostgreSQL(String host, int port, String user, String password, String database) throws SQLException {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.database = database;
            conn = this.openConnection();
        }

        /**
         * Called when PostgreSQLManager object is created. Establishes PostgreSQL connection
         * using the given login credentials.
         * @return connection object
         */
        public Connection openConnection() throws SQLException {
            try {
                Class.forName("org.postgresql.Driver");
                var uri = String.format("jdbc:postgresql://%s:%d/%s", "150.136.14.28", 5432, "mapmaker");
                var props = new Properties();
                props.setProperty("user", "postgres");
                props.setProperty("password", "7aBQb6tYAbu5FUYffYstGg8G");
                props.setProperty("ssl", "false");
                return this.conn = DriverManager.getConnection(uri, props);
//                var uri = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
//                var props = new Properties();
//                props.setProperty("user", this.user);
//                props.setProperty("password", this.password);
//                props.setProperty("ssl", "false");
//                return this.conn = DriverManager.getConnection(uri, props);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Call this function when the plugin gets disabled.
         */
        public void closeConnection() {
            try {
                this.conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                this.conn = null;
            }
        }

        /**
         * Checks if connection is established.
         * @return true if connected to PostgreSQL server
         */
        public boolean isConnected() {
            try {
                return (this.conn != null) && (this.conn.isValid(10)) && (!this.conn.isClosed());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * Getter for Connection object
         * @return connection object
         */
        public Connection getConnection() {
            return this.conn;
        }

    }
}
