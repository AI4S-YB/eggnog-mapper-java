package eggnogmapper.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class FastaUtils {
    private static final Map<String, Character> STANDARD_TABLE = new LinkedHashMap<String, Character>();

    static {
        add("TTT", 'F'); add("TTC", 'F'); add("TTA", 'L'); add("TTG", 'L');
        add("TCT", 'S'); add("TCC", 'S'); add("TCA", 'S'); add("TCG", 'S');
        add("TAT", 'Y'); add("TAC", 'Y'); add("TAA", '*'); add("TAG", '*');
        add("TGT", 'C'); add("TGC", 'C'); add("TGA", '*'); add("TGG", 'W');
        add("CTT", 'L'); add("CTC", 'L'); add("CTA", 'L'); add("CTG", 'L');
        add("CCT", 'P'); add("CCC", 'P'); add("CCA", 'P'); add("CCG", 'P');
        add("CAT", 'H'); add("CAC", 'H'); add("CAA", 'Q'); add("CAG", 'Q');
        add("CGT", 'R'); add("CGC", 'R'); add("CGA", 'R'); add("CGG", 'R');
        add("ATT", 'I'); add("ATC", 'I'); add("ATA", 'I'); add("ATG", 'M');
        add("ACT", 'T'); add("ACC", 'T'); add("ACA", 'T'); add("ACG", 'T');
        add("AAT", 'N'); add("AAC", 'N'); add("AAA", 'K'); add("AAG", 'K');
        add("AGT", 'S'); add("AGC", 'S'); add("AGA", 'R'); add("AGG", 'R');
        add("GTT", 'V'); add("GTC", 'V'); add("GTA", 'V'); add("GTG", 'V');
        add("GCT", 'A'); add("GCC", 'A'); add("GCA", 'A'); add("GCG", 'A');
        add("GAT", 'D'); add("GAC", 'D'); add("GAA", 'E'); add("GAG", 'E');
        add("GGT", 'G'); add("GGC", 'G'); add("GGA", 'G'); add("GGG", 'G');
    }

    private FastaUtils() {
    }

    public static LinkedHashMap<String, String> readFasta(String path) throws IOException {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        BufferedReader br = new BufferedReader(new FileReader(path));
        try {
            String line;
            String currentName = null;
            StringBuilder seq = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith(">")) {
                    if (currentName != null) {
                        out.put(currentName, seq.toString());
                    }
                    currentName = line.substring(1).trim().split("\\s+")[0];
                    seq = new StringBuilder();
                } else {
                    if (currentName == null) {
                        throw new IOException("Wrong FASTA format: sequence without header");
                    }
                    seq.append(line.replaceAll("[\\s\\-.]+", "").toUpperCase(Locale.ROOT));
                }
            }
            if (currentName != null) {
                out.put(currentName, seq.toString());
            }
        } finally {
            br.close();
        }
        return out;
    }

    public static void translateCdsFile(String source, String outfile, String tableCode) throws IOException {
        Map<String, String> seqs = readFasta(source);
        BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
        try {
            for (Map.Entry<String, String> e : seqs.entrySet()) {
                bw.write(">");
                bw.write(e.getKey());
                bw.write("\n");
                bw.write(translateToStop(e.getValue(), tableCode));
                bw.write("\n");
            }
        } finally {
            bw.close();
        }
    }

    public static String translateToStop(String dna, String tableCode) {
        String seq = dna == null ? "" : dna.toUpperCase(Locale.ROOT).replace('U', 'T');
        StringBuilder aa = new StringBuilder();
        for (int i = 0; i + 2 < seq.length(); i += 3) {
            String codon = seq.substring(i, i + 3);
            Character c = translateCodon(codon, tableCode);
            if (c == null) {
                aa.append('X');
            } else if (c.charValue() == '*') {
                break;
            } else {
                aa.append(c.charValue());
            }
        }
        return aa.toString();
    }

    private static Character translateCodon(String codon, String tableCode) {
        Character out = STANDARD_TABLE.get(codon);
        String table = tableCode == null ? "1" : tableCode.trim();
        if ("4".equals(table) && "TGA".equals(codon)) {
            return 'W';
        }
        if ("2".equals(table)) {
            if ("AGA".equals(codon) || "AGG".equals(codon) || "TAA".equals(codon) || "TAG".equals(codon)) {
                return '*';
            }
            if ("ATA".equals(codon)) {
                return 'M';
            }
            if ("TGA".equals(codon)) {
                return 'W';
            }
        }
        return out;
    }

    private static void add(String codon, char aa) {
        STANDARD_TABLE.put(codon, Character.valueOf(aa));
    }
}
