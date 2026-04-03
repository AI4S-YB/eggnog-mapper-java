package eggnogmapper.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompatUtils {
    private static final Pattern VERSION_RE = Pattern.compile("__VERSION__='([^']+)'");
    private static File repoRoot;
    private static String cachedVersion;

    private CompatUtils() {
    }

    public static File findRepoRoot() throws IOException {
        if (repoRoot != null) {
            return repoRoot;
        }
        File here = new File(System.getProperty("user.dir")).getCanonicalFile();
        File current = here;
        while (current != null) {
            File data = new File(current, "data");
            File eggnogmapper = new File(current, "eggnogmapper");
            File eggnogJava = new File(current, "eggnog_java");
            if (data.isDirectory() && eggnogmapper.isDirectory() && eggnogJava.isDirectory()) {
                repoRoot = current;
                return repoRoot;
            }
            current = current.getParentFile();
        }
        repoRoot = here;
        return repoRoot;
    }

    public static String defaultDataDir() throws IOException {
        String env = System.getenv("EGGNOG_DATA_DIR");
        if (env != null && !env.trim().isEmpty()) {
            return new File(env).getCanonicalPath();
        }
        return new File(findRepoRoot(), "data").getCanonicalPath();
    }

    public static String defaultDiamondBin() throws IOException {
        File local = new File(findRepoRoot(), "eggnogmapper/bin/diamond");
        if (local.isFile()) {
            return local.getCanonicalPath();
        }
        return "diamond";
    }

    public static String defaultTaxScopeVarsFile() throws IOException {
        File repoFile = new File(findRepoRoot(), "eggnogmapper/annotation/tax_scopes/vars.py");
        if (repoFile.isFile()) {
            return repoFile.getCanonicalPath();
        }
        return BundledTaxScopeResources.defaultVarsPath();
    }

    public static String readVersion() {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        try {
            File versionFile = new File(findRepoRoot(), "eggnogmapper/version.py");
            BufferedReader br = new BufferedReader(new FileReader(versionFile));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = VERSION_RE.matcher(line);
                    if (m.find()) {
                        cachedVersion = "emapper-" + m.group(1);
                        return cachedVersion;
                    }
                }
            } finally {
                br.close();
            }
        } catch (IOException ignored) {
        }
        try {
            InputStream in = CompatUtils.class.getResourceAsStream("/eggnogmapper/version.py");
            if (in != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Matcher m = VERSION_RE.matcher(line);
                        if (m.find()) {
                            cachedVersion = "emapper-" + m.group(1);
                            return cachedVersion;
                        }
                    }
                } finally {
                    br.close();
                }
            }
        } catch (IOException ignored) {
        }
        cachedVersion = "emapper-unknown";
        return cachedVersion;
    }

    public static String buildCallInfo(List<String> rawArgs) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(new Date().toString()).append('\n');
        sb.append("## ").append(readVersion()).append('\n');
        sb.append("## java Main");
        for (String arg : rawArgs) {
            sb.append(' ').append(arg);
        }
        sb.append('\n');
        sb.append("##");
        return sb.toString();
    }

    public static String shellQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    public static void deleteIfExists(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}
