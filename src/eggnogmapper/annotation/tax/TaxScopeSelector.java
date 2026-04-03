package eggnogmapper.annotation.tax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaxScopeSelector {
    public static final String BROADEST = "broadest";
    public static final String INNER_BROADEST = "inner_broadest";
    public static final String INNER_NARROWEST = "inner_narrowest";
    public static final String NARROWEST = "narrowest";

    public static class NogEntry {
        public final String ogId;
        public final String taxId;
        public final String nogName;
        public final int depth;

        public NogEntry(String ogId, String taxId, String nogName, int depth) {
            this.ogId = ogId;
            this.taxId = taxId;
            this.nogName = nogName;
            this.depth = depth;
        }

        public String asOgTax() {
            return ogId + "@" + taxId;
        }
    }

    public static class NogSelection {
        public final List<NogEntry> allNogs;
        public final List<String> matchNogNames;
        public final List<NogEntry> narrowestOgs;
        public final List<NogEntry> bestOgs;

        public NogSelection(List<NogEntry> allNogs, List<String> matchNogNames, List<NogEntry> narrowestOgs, List<NogEntry> bestOgs) {
            this.allNogs = allNogs;
            this.matchNogNames = matchNogNames;
            this.narrowestOgs = narrowestOgs;
            this.bestOgs = bestOgs;
        }
    }

    public static NogSelection parseNogs(List<String> matchNogs, String taxScopeMode, Set<String> taxScopeIds, Set<String> taxScopeModeIds, TaxScopeData data) {
        List<NogEntry> full = new ArrayList<NogEntry>();
        for (String nog : matchNogs) {
            String[] p = nog.split("@");
            if (p.length != 2) {
                continue;
            }
            String tax = p[1];
            Integer depth = data.levelDepth.get(tax);
            if (depth == null) {
                depth = 99999;
            }
            String name = data.levelNames.containsKey(tax) ? data.levelNames.get(tax) : tax;
            name = name.replace(",", " ");
            full.add(new NogEntry(p[0], tax, nog + "|" + name, depth));
        }
        Collections.sort(full, (a, b) -> {
            if (a.depth != b.depth) return Integer.compare(a.depth, b.depth);
            return a.nogName.compareTo(b.nogName);
        });
        if (full.isEmpty()) {
            return new NogSelection(full, new ArrayList<String>(), null, null);
        }
        int maxDepth = full.get(full.size() - 1).depth;
        int minDepth = full.get(0).depth;
        List<NogEntry> narrowest = filterByDepth(full, maxDepth);
        List<NogEntry> best = null;

        if (taxScopeIds == null || taxScopeIds.isEmpty()) {
            if (BROADEST.equals(taxScopeMode) || INNER_BROADEST.equals(taxScopeMode)) {
                best = filterByDepth(full, minDepth);
            } else if (INNER_NARROWEST.equals(taxScopeMode) || NARROWEST.equals(taxScopeMode)) {
                best = narrowest;
            } else if (taxScopeModeIds != null && !taxScopeModeIds.isEmpty()) {
                Set<String> inters = intersectTaxIds(full, taxScopeModeIds);
                if (!inters.isEmpty()) {
                    best = filterByDepth(full, maxDepthFromSet(inters, data));
                }
            }
        } else {
            Set<String> inters = intersectTaxIds(full, taxScopeIds);
            if (!inters.isEmpty()) {
                if (BROADEST.equals(taxScopeMode)) {
                    best = filterByDepth(full, minDepth);
                } else if (INNER_BROADEST.equals(taxScopeMode)) {
                    int d = minDepthFromSet(inters, data);
                    best = filterByDepth(full, d);
                } else if (INNER_NARROWEST.equals(taxScopeMode)) {
                    int d = maxDepthFromSet(inters, data);
                    best = filterByDepth(full, d);
                } else if (NARROWEST.equals(taxScopeMode)) {
                    best = narrowest;
                } else if (taxScopeModeIds != null && !taxScopeModeIds.isEmpty()) {
                    Set<String> inters2 = intersectTaxIds(full, taxScopeModeIds);
                    if (!inters2.isEmpty()) {
                        best = filterByDepth(full, maxDepthFromSet(inters2, data));
                    }
                }
            }
        }

        List<String> names = new ArrayList<String>();
        for (NogEntry e : full) {
            names.add(e.nogName);
        }
        return new NogSelection(full, names, narrowest, best);
    }

    public static Set<String> parseTaxScopeIds(String taxScopeArg, TaxScopeData data, File builtInDir) throws IOException {
        if (taxScopeArg == null || taxScopeArg.trim().isEmpty() || "none".equalsIgnoreCase(taxScopeArg)) {
            return null;
        }
        List<String> raw = expandScopeArg(taxScopeArg, builtInDir, new HashSet<String>());
        Set<String> out = new HashSet<String>();
        for (String t : raw) {
            if (data.levelNames.containsKey(t)) {
                out.add(t);
            } else if (data.levelDict.containsKey(t)) {
                out.add(data.levelDict.get(t));
            } else {
                throw new IllegalArgumentException("Unrecognized tax scope token: " + t);
            }
        }
        return out;
    }

    private static List<String> expandScopeArg(String scopeArg, File builtInDir, Set<String> seenBuiltIns) throws IOException {
        File file = new File(scopeArg);
        if (file.exists() && file.isFile()) {
            return expandScopeTokens(readScopeFile(file), builtInDir, seenBuiltIns);
        }
        File builtIn = resolveBuiltInScopeFile(scopeArg, builtInDir);
        if (builtIn != null) {
            return expandBuiltInScopeFile(builtIn, builtInDir, seenBuiltIns);
        }
        return splitScopeCsv(scopeArg);
    }

    private static List<String> expandScopeTokens(List<String> tokens, File builtInDir, Set<String> seenBuiltIns) throws IOException {
        List<String> expanded = new ArrayList<String>();
        for (String token : tokens) {
            String trimmed = token == null ? "" : token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            File nestedBuiltIn = resolveBuiltInScopeFile(trimmed, builtInDir);
            if (nestedBuiltIn != null) {
                expanded.addAll(expandBuiltInScopeFile(nestedBuiltIn, builtInDir, seenBuiltIns));
            } else {
                expanded.add(trimmed);
            }
        }
        return expanded;
    }

    private static List<String> expandBuiltInScopeFile(File builtInFile, File builtInDir, Set<String> seenBuiltIns) throws IOException {
        String key = builtInFile.getCanonicalPath();
        if (!seenBuiltIns.add(key)) {
            throw new IllegalArgumentException("Circular tax scope reference: " + builtInFile.getName());
        }
        try {
            return expandScopeTokens(readScopeFile(builtInFile), builtInDir, seenBuiltIns);
        } finally {
            seenBuiltIns.remove(key);
        }
    }

    private static File resolveBuiltInScopeFile(String scopeArg, File builtInDir) {
        if (builtInDir == null) {
            return null;
        }
        File[] candidates = builtInDir.listFiles();
        if (candidates == null) {
            return null;
        }
        for (File candidate : candidates) {
            if (candidate.isFile() && candidate.getName().equals(scopeArg)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> splitScopeCsv(String scopeArg) {
        List<String> raw = new ArrayList<String>();
        String[] arr = scopeArg.split(",");
        for (String s : arr) {
            if (!s.trim().isEmpty()) {
                raw.add(s.trim());
            }
        }
        return raw;
    }

    public static boolean isNamedMode(String taxScopeMode) {
        return BROADEST.equals(taxScopeMode)
                || INNER_BROADEST.equals(taxScopeMode)
                || INNER_NARROWEST.equals(taxScopeMode)
                || NARROWEST.equals(taxScopeMode);
    }

    private static List<NogEntry> filterByDepth(List<NogEntry> all, int depth) {
        List<NogEntry> out = new ArrayList<NogEntry>();
        for (NogEntry n : all) {
            if (n.depth == depth) out.add(n);
        }
        return out;
    }

    private static int minDepthFromSet(Set<String> ids, TaxScopeData data) {
        int m = Integer.MAX_VALUE;
        for (String id : ids) {
            Integer d = data.levelDepth.get(id);
            if (d != null && d < m) m = d;
        }
        return m == Integer.MAX_VALUE ? 99999 : m;
    }

    private static int maxDepthFromSet(Set<String> ids, TaxScopeData data) {
        int m = Integer.MIN_VALUE;
        for (String id : ids) {
            Integer d = data.levelDepth.get(id);
            if (d != null && d > m) m = d;
        }
        return m == Integer.MIN_VALUE ? -1 : m;
    }

    private static List<String> readScopeFile(File scopeFile) throws IOException {
        List<String> raw = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(scopeFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    raw.add(line.trim());
                }
            }
        } finally {
            br.close();
        }
        return raw;
    }

    private static Set<String> intersectTaxIds(List<NogEntry> full, Set<String> allowed) {
        Set<String> inters = new HashSet<String>();
        for (NogEntry e : full) {
            if (allowed.contains(e.taxId)) {
                inters.add(e.taxId);
            }
        }
        return inters;
    }
}
