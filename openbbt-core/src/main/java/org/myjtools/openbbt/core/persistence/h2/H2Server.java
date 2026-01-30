package org.myjtools.openbbt.core.persistence.h2;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.myjtools.jexten.Version;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.persistence.Session;
import org.myjtools.openbbt.core.util.Log;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class H2Server {

    private static final Log log = Log.of("core.persistence");

    private static final String USER = "sa";
    private static final String PWD = "sa";
    private static final String SCHEMA = "OPENBBT";


    private static final PrintWriter dbLogWriter = new PrintWriter(new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (len == 1 && cbuf[off] == '\n') return;
            log.trace(String.copyValueOf(cbuf,off,len));
        }
        @Override
        public void flush() throws IOException {
            //
        }
        @Override
        public void close() throws IOException {
            //
        }
    });

    private final Server server;
    private final DataSource dataSource;


    public H2Server(Path file) {
        file = file.resolve(SCHEMA);
        log.debug("using database file {file}", file);
        this.server = createServer(file);
        this.dataSource = createDataSource(file);
    }



    public void start() {
        try {
            server.start();
            prepareDatabase();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    public void stop() {
        server.stop();
    }



    public Connection newConnection() {
        try {
            Connection connection = dataSource.getConnection();
            log.debug("using new connection with URL {}", connection.getMetaData().getURL());
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    private Server createServer(Path file) {
        try {
            return Server.createTcpServer("-baseDir",file.toString(),"-ifNotExists");
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    private static DataSource createDataSource(Path file) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUser(USER);
        dataSource.setPassword(PWD);
        dataSource.setURL("jdbc:h2:tcp://localhost/"+SCHEMA);
        dataSource.setLogWriter(dbLogWriter);
        return dataSource;
    }


    private void prepareDatabase() {
        try (var session = new Session(this::newConnection, log)) {
            initDatabase(session);
            List<Version> appliedPatches = findAppliedPatches(session);
            Map<Version,Path> pendingPatches = findPendingPatches(appliedPatches);
            applyPendingPatches(session, pendingPatches);
            session.commit();
        } catch (SQLException e) {
            throw new OpenBBTException(e);
        }
    }


    private void initDatabase(Session session) throws SQLException {
        if (!session.exists("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'DBCHANGELOG'", SCHEMA)) {
            log.info("First time using the data repository, preparing the database...");
            session.execute("CREATE TABLE DBCHANGELOG (VERSION VARCHAR(20), TIMESTAMP TIMESTAMP, PRIMARY KEY (VERSION))");
        }
    }


    private void applyPendingPatches(Session session, Map<Version,Path> pendingPatches)
            throws SQLException {
        try {
            for (var pendingPatch : pendingPatches.entrySet()) {
                Version version = pendingPatch.getKey();
                Path file = pendingPatch.getValue();
                log.info("Applying version {} patch to data repository", version);
                for (String sql : Files.readString(file).split(";")) {
                    session.execute(sql);
                }
                session.execute("INSERT INTO DBCHANGELOG (VERSION) VALUES ('"+version+"')");
            }

        } catch (IOException e) {
            throw new OpenBBTException(e);
        }
    }





    private List<Version> findAppliedPatches(Session session) throws SQLException {
        return session.list(
                resultSet -> Version.of(resultSet.getString(1)),
                "SELECT VERSION FROM DBCHANGELOG"
        );
    }


    private Map<Version,Path> findPendingPatches(List<Version> appliedPatches) {
        try {
            SortedMap<Version,Path> pendingPatches;
            URL sqlFolderURL = H2Server.class.getClassLoader().getResource("sql");
            if (sqlFolderURL == null) {
                return Map.of();
            }
            Path sqlFolder = Path.of(sqlFolderURL.toURI()).toAbsolutePath();
            try (Stream<Path> files = Files.walk(sqlFolder)) {
                pendingPatches = files
                    .filter(Files::isRegularFile)
                    .map(file -> Map.entry(Version.of(filename(file)),file))
                    .filter(entry -> !appliedPatches.contains(entry.getKey()))
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b)->a,
                        TreeMap::new
                    ));
            }
            return pendingPatches;
        } catch (IOException | URISyntaxException e) {
            throw new OpenBBTException(e);
        }
    }




    private static String filename(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.lastIndexOf('.'));
    }


    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new OpenBBTException(e);
        }
    }


}
