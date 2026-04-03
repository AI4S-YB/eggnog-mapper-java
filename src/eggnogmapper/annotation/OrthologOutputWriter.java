package eggnogmapper.annotation;

import eggnogmapper.annotation.tax.NcbiTaxonomyRepository;
import eggnogmapper.cli.CliArgs;
import eggnogmapper.common.CompatUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OrthologOutputWriter {
    public static void writeOrthologs(String orthologPath,
                                      List<AnnotationRecord> records,
                                      NcbiTaxonomyRepository ncbi,
                                      CliArgs args,
                                      int processedQueries) throws IOException, SQLException {
        File f = new File(orthologPath);
        boolean append = args.resume;
        boolean writeHeader = !append || !f.exists() || f.length() == 0;
        long start = System.currentTimeMillis();

        BufferedWriter bw = new BufferedWriter(new FileWriter(orthologPath, append));
        try {
            if (!args.noFileComments) {
                bw.write(CompatUtils.buildCallInfo(args.rawArgs));
                bw.write("\n");
            }
            if (writeHeader) {
                bw.write("#query\torth_type\tspecies\torthologs\n");
            }
            for (AnnotationRecord r : records) {
                outputRecord(bw, r, ncbi);
            }
            if (!args.noFileComments) {
                double elapsed = (System.currentTimeMillis() - start) / 1000.0d;
                bw.write("## " + processedQueries + " queries scanned\n");
                bw.write("## Total time (seconds): " + elapsed + "\n");
                bw.write("## Rate: " + (elapsed > 0.0d ? String.format(Locale.ROOT, "%.2f q/s", processedQueries / elapsed) : "0.00 q/s") + "\n");
            }
        } finally {
            bw.close();
        }
    }

    public static Set<String> parseExistingQueries(String orthologPath) throws IOException {
        Set<String> out = new HashSet<String>();
        File f = new File(orthologPath);
        if (!f.exists()) return out;
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", 2);
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    out.add(parts[0].trim());
                }
            }
        } finally {
            br.close();
        }
        return out;
    }

    private static void outputRecord(BufferedWriter bw, AnnotationRecord r, NcbiTaxonomyRepository ncbi) throws IOException, SQLException {
        if (r.allOrthologies == null || r.allOrthologies.isEmpty()) {
            return;
        }
        String seedId = r.seedOrtholog == null ? "-" : r.seedOrtholog;
        String[] sp = seedId.split("\\.", 2);
        String seedShort = sp.length == 2 ? sp[1] : seedId;
        boolean seedShown = false;

        for (Map.Entry<String, Set<String>> e : r.allOrthologies.entrySet()) {
            String target = e.getKey();
            if ("all".equals(target) || "annot_orthologs".equals(target)) {
                continue;
            }
            Set<String> set = e.getValue();
            if (set == null || set.isEmpty()) {
                continue;
            }

            List<String> all = new ArrayList<String>(set);
            Collections.sort(all);
            Map<Integer, List<String>> byTax = new HashMap<Integer, List<String>>();
            Set<Integer> taxIds = new HashSet<Integer>();
            for (String orth : all) {
                String[] p = orth.split("\\.", 2);
                if (p.length != 2) {
                    continue;
                }
                int tax;
                try {
                    tax = Integer.parseInt(p[0]);
                } catch (NumberFormatException ex) {
                    continue;
                }
                taxIds.add(Integer.valueOf(tax));
                List<String> names = byTax.get(Integer.valueOf(tax));
                if (names == null) {
                    names = new ArrayList<String>();
                    byTax.put(Integer.valueOf(tax), names);
                }
                String name = p[1];
                if (r.annotOrthologs != null && r.annotOrthologs.contains(orth)) {
                    name = "*" + name;
                }
                names.add(name);
            }

            final Map<Integer, String> translated = ncbi.getTaxidTranslator(taxIds);
            List<Integer> taxa = new ArrayList<Integer>(byTax.keySet());
            Collections.sort(taxa, new Comparator<Integer>() {
                @Override
                public int compare(Integer left, Integer right) {
                    String l = translated.containsKey(left) ? translated.get(left) : String.valueOf(left);
                    String r = translated.containsKey(right) ? translated.get(right) : String.valueOf(right);
                    int cmp = l.compareTo(r);
                    if (cmp != 0) {
                        return cmp;
                    }
                    return left.compareTo(right);
                }
            });

            for (Integer tax : taxa) {
                List<String> orthNames = byTax.get(tax);
                List<String> rest = new ArrayList<String>();
                for (String n : orthNames) {
                    String plain = n.startsWith("*") ? n.substring(1) : n;
                    if (plain.equals(seedShort)) {
                        if (!seedShown) {
                            bw.write(r.query + "\tseed\t" + speciesLabel(tax, translated) + "\t" + n + "\n");
                            seedShown = true;
                        }
                    } else {
                        rest.add(n);
                    }
                }
                if (!rest.isEmpty()) {
                    Collections.sort(rest);
                    bw.write(r.query + "\t" + target + "\t" + speciesLabel(tax, translated) + "\t" + String.join(",", rest) + "\n");
                }
            }
        }
    }

    private static String speciesLabel(Integer tax, Map<Integer, String> translated) {
        String name = translated.get(tax);
        if (name == null || name.trim().isEmpty()) {
            name = String.valueOf(tax);
        }
        return name + "(" + tax + ")";
    }
}
