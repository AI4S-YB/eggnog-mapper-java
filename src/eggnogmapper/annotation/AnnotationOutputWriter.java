package eggnogmapper.annotation;

import eggnogmapper.cli.CliArgs;
import eggnogmapper.common.CompatUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AnnotationOutputWriter {
    public static final List<String> ANNOTATION_FIELDS = Arrays.asList(
            "Preferred_name", "GOs", "EC", "KEGG_ko", "KEGG_Pathway",
            "KEGG_Module", "KEGG_Reaction", "KEGG_rclass", "BRITE", "KEGG_TC",
            "CAZy", "BiGG_Reaction", "PFAMs"
    );

    public static void writeAnnotations(String annotationsPath, List<AnnotationRecord> records, CliArgs args, int processedQueries) throws IOException {
        File f = new File(annotationsPath);
        boolean append = args.resume;
        boolean writeHeader = !append || !f.exists() || f.length() == 0;
        long start = System.currentTimeMillis();

        BufferedWriter bw = new BufferedWriter(new FileWriter(annotationsPath, append));
        try {
            if (!args.noFileComments) {
                bw.write(CompatUtils.buildCallInfo(args.rawArgs));
                bw.write("\n");
            }
            if (writeHeader) {
                bw.write("#query\tseed_ortholog\tevalue\tscore\teggNOG_OGs\tmax_annot_lvl\tCOG_category\tDescription\t");
                bw.write(String.join("\t", ANNOTATION_FIELDS));
                bw.write("\n");
            }
            for (AnnotationRecord r : records) {
                StringBuilder line = new StringBuilder();
                line.append(val(r.query)).append("\t");
                line.append(val(r.seedOrtholog)).append("\t");
                line.append(r.evalue).append("\t");
                line.append(r.score).append("\t");
                line.append(val(r.eggnogOgs)).append("\t");
                line.append(val(r.maxAnnotLvl)).append("\t");
                line.append(val(r.cogCategory)).append("\t");
                line.append(val(r.description));
                for (String k : ANNOTATION_FIELDS) {
                    line.append("\t").append(val(r.fields.get(k)));
                }
                bw.write(line.toString());
                bw.write("\n");
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

    public static Set<String> parseExistingQueries(String annotationsPath) throws IOException {
        Set<String> out = new HashSet<String>();
        File f = new File(annotationsPath);
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

    private static String val(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "-";
        }
        return s.trim();
    }
}
