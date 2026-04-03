package eggnogmapper.annotation.tax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaxScopeLoader {
    private static final Pattern KV_INT = Pattern.compile("'([^']+)'\\s*:\\s*(\\d+)");
    private static final Pattern KV_STR = Pattern.compile("'([^']+)'\\s*:\\s*'([^']*)'");
    private static final Pattern KV_LIST = Pattern.compile("'([^']+)'\\s*:\\s*\\[([^\\]]*)\\]");

    public static TaxScopeData load(String varsPath) throws IOException {
        TaxScopeData data = new TaxScopeData();
        String txt = readAll(new File(varsPath));

        parseDepth(extractSection(txt, "LEVEL_DEPTH={", "LEVEL_NAMES="), data);
        parseNames(extractSection(txt, "LEVEL_NAMES={", "LEVEL_PARENTS="), data);
        parseParents(extractSection(txt, "LEVEL_PARENTS={", "LEVEL_DICT="), data);
        parseDict(extractSection(txt, "LEVEL_DICT={", null), data);
        return data;
    }

    private static void parseDepth(String sec, TaxScopeData data) {
        if (sec == null) {
            return;
        }
        Matcher m = KV_INT.matcher(sec);
        while (m.find()) {
            data.levelDepth.put(m.group(1), Integer.parseInt(m.group(2)));
        }
    }

    private static void parseNames(String sec, TaxScopeData data) {
        if (sec == null) {
            return;
        }
        Matcher m = KV_STR.matcher(sec);
        while (m.find()) {
            data.levelNames.put(m.group(1), m.group(2));
        }
    }

    private static void parseDict(String sec, TaxScopeData data) {
        if (sec == null) {
            return;
        }
        Matcher m = KV_STR.matcher(sec);
        while (m.find()) {
            data.levelDict.put(m.group(1), m.group(2));
        }
    }

    private static void parseParents(String sec, TaxScopeData data) {
        if (sec == null) return;
        Matcher m = KV_LIST.matcher(sec);
        while (m.find()) {
            String tax = m.group(1);
            String raw = m.group(2);
            List<String> parents = new ArrayList<String>();
            Matcher sm = Pattern.compile("'([^']+)'").matcher(raw);
            while (sm.find()) {
                parents.add(sm.group(1));
            }
            data.levelParents.put(tax, parents);
        }
    }

    private static String extractSection(String txt, String startKey, String endKey) {
        int s = txt.indexOf(startKey);
        if (s < 0) {
            return null;
        }
        s += startKey.length();
        int e = endKey == null ? txt.length() : txt.indexOf(endKey, s);
        if (e < 0) {
            e = txt.length();
        }
        return txt.substring(s, e);
    }

    private static String readAll(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }
}
