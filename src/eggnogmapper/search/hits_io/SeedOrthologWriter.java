package eggnogmapper.search.hits_io;

import eggnogmapper.cli.CliArgs;
import eggnogmapper.common.CompatUtils;
import eggnogmapper.search.Hit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SeedOrthologWriter {
    public static void write(String path, List<Hit> hits, CliArgs args, List<String> commands) throws IOException {
        long start = System.currentTimeMillis();
        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        try {
            boolean shortFormat = args.outfmtShort || (!hits.isEmpty() && hits.get(0).isShort());
            if (!args.noFileComments) {
                bw.write(CompatUtils.buildCallInfo(args.rawArgs));
                bw.write("\n");
                if (commands != null) {
                    for (String cmd : commands) {
                        bw.write("##");
                        bw.write(cmd);
                        bw.write("\n");
                    }
                }
            }
            if (shortFormat) {
                bw.write("#qseqid\tsseqid\tevalue\tbitscore\n");
            } else {
                bw.write("#qseqid\tsseqid\tevalue\tbitscore\tqstart\tqend\tsstart\tsend\tpident\tqcov\tscov\n");
            }
            int qn = 0;
            for (Hit h : hits) {
                if (shortFormat || h.isShort()) {
                    bw.write(h.query + "\t" + h.target + "\t" + h.evalue + "\t" + h.score + "\n");
                } else {
                    bw.write(h.query + "\t" + h.target + "\t" + h.evalue + "\t" + h.score + "\t"
                            + h.qstart + "\t" + h.qend + "\t" + h.sstart + "\t" + h.send + "\t"
                            + h.pident + "\t" + h.qcov + "\t" + h.scov + "\n");
                }
                qn += 1;
            }
            if (!args.noFileComments) {
                double elapsed = (System.currentTimeMillis() - start) / 1000.0d;
                bw.write("## " + qn + " queries scanned\n");
                bw.write("## Total time (seconds): " + elapsed + "\n");
                bw.write("## Rate: " + (elapsed > 0.0d ? String.format(Locale.ROOT, "%.2f q/s", qn / elapsed) : "0.00 q/s") + "\n");
            }
        } finally {
            bw.close();
        }
    }
}
