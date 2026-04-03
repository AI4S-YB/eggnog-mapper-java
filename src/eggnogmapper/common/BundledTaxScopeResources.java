package eggnogmapper.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class BundledTaxScopeResources {
    private static final String RESOURCE_ROOT = "/eggnogmapper/annotation/tax_scopes/";
    private static final String[] RESOURCE_FILES = {
            "vars.py",
            "all",
            "all_broad",
            "all_narrow",
            "archaea",
            "auto",
            "auto_broad",
            "bacteria",
            "bacteria_broad",
            "eukaryota",
            "eukaryota_broad",
            "prokaryota_broad"
    };

    private static volatile File extractedDir;

    private BundledTaxScopeResources() {
    }

    public static synchronized String defaultVarsPath() throws IOException {
        return new File(ensureExtractedDirectory(), "vars.py").getCanonicalPath();
    }

    private static File ensureExtractedDirectory() throws IOException {
        if (extractedDir != null && extractedDir.isDirectory()) {
            return extractedDir;
        }
        File dir = Files.createTempDirectory("eggnog_java_tax_scopes").toFile();
        dir.deleteOnExit();
        for (String name : RESOURCE_FILES) {
            copyResource(name, new File(dir, name));
        }
        extractedDir = dir.getCanonicalFile();
        return extractedDir;
    }

    private static void copyResource(String name, File target) throws IOException {
        InputStream in = BundledTaxScopeResources.class.getResourceAsStream(RESOURCE_ROOT + name);
        if (in == null) {
            throw new IOException("Bundled tax-scope resource missing from jar: " + name);
        }
        FileOutputStream out = new FileOutputStream(target);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            try {
                in.close();
            } finally {
                out.close();
            }
        }
        target.deleteOnExit();
    }
}
