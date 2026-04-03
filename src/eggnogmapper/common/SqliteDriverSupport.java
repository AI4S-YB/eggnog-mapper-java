package eggnogmapper.common;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public final class SqliteDriverSupport {
    private static final String DRIVER_CLASS = "org.sqlite.JDBC";
    private static volatile boolean loaded = false;

    private SqliteDriverSupport() {
    }

    public static synchronized void ensureLoaded() throws SQLException {
        if (loaded) {
            return;
        }
        if (tryLoadFromClasspath()) {
            loaded = true;
            return;
        }

        File driverJar = findBundledDriverJar();
        if (driverJar == null) {
            throw new SQLException(
                    "SQLite JDBC driver not found. Put sqlite-jdbc-<version>.jar in eggnog_java/lib " +
                            "or next to the runnable jar under lib/, or add it to the Java classpath."
            );
        }

        try {
            URL jarUrl = driverJar.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, SqliteDriverSupport.class.getClassLoader());
            Driver driver = (Driver) Class.forName(DRIVER_CLASS, true, loader).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
            loaded = true;
        } catch (Exception ex) {
            throw new SQLException("Failed to load bundled SQLite JDBC driver from " + driverJar.getAbsolutePath(), ex);
        }
    }

    public static Connection open(String sqlitePath) throws SQLException {
        ensureLoaded();
        return DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
    }

    private static boolean tryLoadFromClasspath() {
        try {
            Class.forName(DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static File findBundledDriverJar() {
        File directJar = fileFromProperty("eggnog.sqlite.jdbc.jar");
        if (directJar != null && directJar.isFile()) {
            return directJar;
        }

        Set<File> candidates = new LinkedHashSet<File>();
        addDirFromProperty(candidates, "eggnog.sqlite.jdbc.dir");
        addCodeSourceCandidates(candidates);
        addUserDirCandidates(candidates);

        for (File dir : candidates) {
            File jar = findSqliteJar(dir);
            if (jar != null) {
                return jar;
            }
        }
        return null;
    }

    private static void addUserDirCandidates(Set<File> candidates) {
        String userDir = System.getProperty("user.dir");
        if (userDir == null || userDir.trim().isEmpty()) {
            return;
        }
        File cwd = canonical(new File(userDir));
        if (cwd != null) {
            candidates.add(new File(cwd, "lib"));
            candidates.add(new File(cwd, "eggnog_java/lib"));
        }
        try {
            File repoRoot = CompatUtils.findRepoRoot();
            if (repoRoot != null) {
                candidates.add(new File(repoRoot, "eggnog_java/lib"));
            }
        } catch (IOException ignored) {
        }
    }

    private static void addCodeSourceCandidates(Set<File> candidates) {
        try {
            CodeSource codeSource = SqliteDriverSupport.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return;
            }
            File codeLocation = canonical(new File(codeSource.getLocation().toURI()));
            if (codeLocation == null) {
                return;
            }
            File baseDir = codeLocation.isFile() ? codeLocation.getParentFile() : codeLocation;
            if (baseDir == null) {
                return;
            }
            candidates.add(new File(baseDir, "lib"));
            File parent = baseDir.getParentFile();
            if (parent != null) {
                candidates.add(new File(parent, "lib"));
                File grandParent = parent.getParentFile();
                if (grandParent != null) {
                    candidates.add(new File(grandParent, "lib"));
                }
            }
        } catch (URISyntaxException ignored) {
        }
    }

    private static void addDirFromProperty(Set<File> candidates, String propertyName) {
        File dir = fileFromProperty(propertyName);
        if (dir != null) {
            candidates.add(dir);
        }
    }

    private static File fileFromProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return canonical(new File(value));
    }

    private static File canonical(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return file.getAbsoluteFile();
        }
    }

    private static File findSqliteJar(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        File[] matches = dir.listFiles();
        if (matches == null || matches.length == 0) {
            return null;
        }
        Arrays.sort(matches);
        for (int i = matches.length - 1; i >= 0; i--) {
            File f = matches[i];
            String name = f.getName().toLowerCase();
            if (f.isFile() && name.startsWith("sqlite-jdbc-") && name.endsWith(".jar")) {
                return f;
            }
        }
        return null;
    }

    private static final class DriverShim implements Driver {
        private final Driver delegate;

        private DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
