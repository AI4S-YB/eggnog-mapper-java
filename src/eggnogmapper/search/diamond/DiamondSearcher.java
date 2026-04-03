package eggnogmapper.search.diamond;

import eggnogmapper.cli.CliArgs;
import eggnogmapper.common.CompatUtils;
import eggnogmapper.common.FastaUtils;
import eggnogmapper.search.Hit;
import eggnogmapper.search.Searcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiamondSearcher implements Searcher {
    private final CliArgs args;
    private final List<String> executedCommands = new ArrayList<String>();
    private File workDir;
    private File translatedQueryFile;

    public DiamondSearcher(CliArgs args) {
        this.args = args;
    }

    @Override
    public List<Hit> search(String inputPath, String hitsPath) throws IOException, InterruptedException {
        File hitFile = new File(hitsPath);
        executedCommands.clear();
        if (args.resume) {
            if (!hitFile.isFile()) {
                throw new IOException("Couldn't find hits file " + hitFile.getAbsolutePath() + " to resume.");
            }
        } else {
            runDiamond(inputPath, hitsPath);
        }
        return parseHits(hitsPath);
    }

    @Override
    public List<String> getExecutedCommands() {
        return new ArrayList<String>(executedCommands);
    }

    @Override
    public void clear() {
        deleteRecursively(workDir);
        workDir = null;
        translatedQueryFile = null;
    }

    private void runDiamond(String inputPath, String hitsPath) throws IOException, InterruptedException {
        String itype = args.inputType.toLowerCase();
        String tool;
        String queryPath = inputPath;
        prepareWorkDir();

        if ("proteins".equals(itype)) {
            tool = "blastp";
        } else if ("cds".equals(itype)) {
            if (args.translate) {
                tool = "blastp";
                translatedQueryFile = new File(workDir, "translated_query.faa");
                FastaUtils.translateCdsFile(inputPath, translatedQueryFile.getAbsolutePath(), args.transTable);
                queryPath = translatedQueryFile.getAbsolutePath();
            } else {
                tool = "blastx";
            }
        } else {
            throw new IllegalArgumentException("eggnog_java currently supports --itype proteins|CDS for diamond mode");
        }

        List<String> cmd = new ArrayList<String>();
        cmd.add(args.diamondBin);
        cmd.add(tool);
        cmd.add("-d");
        cmd.add(args.dmndDb);
        cmd.add("-q");
        cmd.add(queryPath);
        cmd.add("--threads");
        cmd.add(String.valueOf(args.cpu));
        cmd.add("-o");
        cmd.add(hitsPath);
        cmd.add("--tmpdir");
        cmd.add(workDir.getAbsolutePath());

        if (args.sensmode != null && !"default".equalsIgnoreCase(args.sensmode)) {
            cmd.add("--" + args.sensmode);
        }
        if ("yes".equalsIgnoreCase(args.dmndIterate)) {
            cmd.add("--iterate");
        }
        if (args.dmndIgnoreWarnings) {
            cmd.add("--ignore-warnings");
        }
        if (args.dmndAlgo != null && !"auto".equalsIgnoreCase(args.dmndAlgo)) {
            cmd.add("--algo");
            cmd.add(args.dmndAlgo);
        }
        if (args.evalue != null) {
            cmd.add("-e");
            cmd.add(String.valueOf(args.evalue));
        }
        if (args.score != null) {
            cmd.add("--min-score");
            cmd.add(String.valueOf(args.score));
        }
        if (args.pident != null) {
            cmd.add("--id");
            cmd.add(String.valueOf(args.pident));
        }
        if (args.queryCover != null) {
            cmd.add("--query-cover");
            cmd.add(String.valueOf(args.queryCover));
        }
        if (args.subjectCover != null) {
            cmd.add("--subject-cover");
            cmd.add(String.valueOf(args.subjectCover));
        }
        if (args.transTable != null && !args.transTable.trim().isEmpty()) {
            cmd.add("--query-gencode");
            cmd.add(args.transTable);
        }
        if (args.matrix != null && !args.matrix.trim().isEmpty()) {
            cmd.add("--matrix");
            cmd.add(args.matrix);
        }
        if (args.dmndFrameshift != null) {
            cmd.add("--frameshift");
            cmd.add(String.valueOf(args.dmndFrameshift));
        }
        if (args.gapopen != null) {
            cmd.add("--gapopen");
            cmd.add(String.valueOf(args.gapopen));
        }
        if (args.gapextend != null) {
            cmd.add("--gapextend");
            cmd.add(String.valueOf(args.gapextend));
        }
        if (args.dmndBlockSize != null) {
            cmd.add("--block-size");
            cmd.add(String.valueOf(args.dmndBlockSize));
        }
        if (args.dmndIndexChunks != null) {
            cmd.add("-c");
            cmd.add(String.valueOf(args.dmndIndexChunks));
        }

        cmd.add("--top");
        cmd.add("3");
        if (args.outfmtShort) {
            Collections.addAll(cmd, "--outfmt", "6", "qseqid", "sseqid", "evalue", "bitscore");
        } else {
            Collections.addAll(cmd, "--outfmt", "6",
                    "qseqid", "sseqid", "pident", "length", "mismatch", "gapopen",
                    "qstart", "qend", "sstart", "send", "evalue", "bitscore", "qcovhsp", "scovhsp");
        }
        executedCommands.add(buildCommandComment(tool, queryPath, hitsPath));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        File logFile = new File(hitsPath + ".diamond.log");
        pb.redirectOutput(logFile);
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("diamond execution failed, exit code=" + code + ", log: " + logFile.getAbsolutePath());
        }
    }

    private List<Hit> parseHits(String hitsPath) throws IOException {
        List<Hit> out = new ArrayList<Hit>();
        Set<String> seenQuery = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(hitsPath));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split("\t");
                if (f.length >= 4 && f.length < 14) {
                    String query = f[0].trim();
                    if (seenQuery.contains(query)) {
                        continue;
                    }
                    seenQuery.add(query);
                    out.add(new Hit(query, f[1].trim(), parseDoubleSafe(f[2]), parseDoubleSafe(f[3]),
                            -1, -1, -1, -1, -1.0d, -1.0d, -1.0d));
                    continue;
                }
                if (f.length < 14) {
                    continue;
                }
                String query = f[0].trim();
                if (seenQuery.contains(query)) {
                    continue;
                }
                seenQuery.add(query);

                String target = f[1].trim();
                double pident = parseDoubleSafe(f[2]);
                int qstart = parseIntSafe(f[6]);
                int qend = parseIntSafe(f[7]);
                int sstart = parseIntSafe(f[8]);
                int send = parseIntSafe(f[9]);
                double evalue = parseDoubleSafe(f[10]);
                double score = parseDoubleSafe(f[11]);
                double qcov = parseDoubleSafe(f[12]);
                double scov = parseDoubleSafe(f[13]);
                out.add(new Hit(query, target, evalue, score, qstart, qend, sstart, send, pident, qcov, scov));
            }
        } finally {
            br.close();
        }
        return out;
    }

    private void prepareWorkDir() throws IOException {
        File base = new File(args.tempDir);
        if (!base.isDirectory() && !base.mkdirs()) {
            throw new IOException("Could not create temp directory: " + base.getAbsolutePath());
        }
        workDir = Files.createTempDirectory(base.toPath(), "emappertmp_dmnd_").toFile();
    }

    private String buildCommandComment(String tool, String queryPath, String hitsPath) {
        StringBuilder cmd = new StringBuilder();
        cmd.append(args.diamondBin).append(' ').append(tool);
        cmd.append(" -d ").append(CompatUtils.shellQuote(args.dmndDb));
        cmd.append(" -q ").append(CompatUtils.shellQuote(queryPath));
        cmd.append(" --threads ").append(args.cpu);
        cmd.append(" -o ").append(CompatUtils.shellQuote(hitsPath));
        cmd.append(" --tmpdir ").append(CompatUtils.shellQuote(workDir.getAbsolutePath()));
        if (args.sensmode != null && !"default".equalsIgnoreCase(args.sensmode)) {
            cmd.append(" --").append(args.sensmode);
        }
        if ("yes".equalsIgnoreCase(args.dmndIterate)) {
            cmd.append(" --iterate");
        }
        if (args.dmndIgnoreWarnings) {
            cmd.append(" --ignore-warnings");
        }
        if (args.dmndAlgo != null && !"auto".equalsIgnoreCase(args.dmndAlgo)) {
            cmd.append(" --algo ").append(args.dmndAlgo);
        }
        if (args.evalue != null) {
            cmd.append(" -e ").append(args.evalue);
        }
        if (args.score != null) {
            cmd.append(" --min-score ").append(args.score);
        }
        if (args.pident != null) {
            cmd.append(" --id ").append(args.pident);
        }
        if (args.queryCover != null) {
            cmd.append(" --query-cover ").append(args.queryCover);
        }
        if (args.subjectCover != null) {
            cmd.append(" --subject-cover ").append(args.subjectCover);
        }
        if (args.transTable != null && !args.transTable.trim().isEmpty()) {
            cmd.append(" --query-gencode ").append(args.transTable);
        }
        if (args.matrix != null && !args.matrix.trim().isEmpty()) {
            cmd.append(" --matrix ").append(args.matrix);
        }
        if (args.dmndFrameshift != null) {
            cmd.append(" --frameshift ").append(args.dmndFrameshift);
        }
        if (args.gapopen != null) {
            cmd.append(" --gapopen ").append(args.gapopen);
        }
        if (args.gapextend != null) {
            cmd.append(" --gapextend ").append(args.gapextend);
        }
        if (args.dmndBlockSize != null) {
            cmd.append(" --block-size ").append(args.dmndBlockSize);
        }
        if (args.dmndIndexChunks != null) {
            cmd.append(" -c ").append(args.dmndIndexChunks);
        }
        cmd.append(" --top 3");
        if (args.outfmtShort) {
            cmd.append(" --outfmt 6 qseqid sseqid evalue bitscore");
        } else {
            cmd.append(" --outfmt 6 qseqid sseqid pident length mismatch gapopen qstart qend sstart send evalue bitscore qcovhsp scovhsp");
        }
        return cmd.toString();
    }

    private static double parseDoubleSafe(String s) {
        return Double.parseDouble(s.trim());
    }

    private static int parseIntSafe(String s) {
        return Integer.parseInt(s.trim());
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
