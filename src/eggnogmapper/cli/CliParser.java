package eggnogmapper.cli;

import eggnogmapper.common.CompatUtils;

import java.io.File;
import java.io.IOException;

public class CliParser {
    public static CliArgs parse(String[] args) {
        CliArgs out = new CliArgs();
        for (String arg : args) {
            out.rawArgs.add(arg);
        }
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("-h".equals(a) || "--help".equals(a)) {
                out.help = true;
                return out;
            } else if ("-v".equals(a) || "--version".equals(a)) {
                out.version = true;
                return out;
            } else if ("-m".equals(a) || "--mode".equals(a)) {
                out.mode = requireValue(args, ++i, a);
            } else if ("-i".equals(a) || "--input".equals(a)) {
                out.inputPath = requireValue(args, ++i, a);
            } else if ("--itype".equals(a)) {
                out.inputType = requireValue(args, ++i, a);
            } else if ("--translate".equals(a)) {
                out.translate = true;
            } else if ("--trans_table".equals(a)) {
                out.transTable = requireValue(args, ++i, a);
            } else if ("-o".equals(a) || "--output".equals(a)) {
                out.outputPrefix = requireValue(args, ++i, a);
            } else if ("--output_dir".equals(a)) {
                out.outputDir = requireValue(args, ++i, a);
            } else if ("--data_dir".equals(a)) {
                out.dataDir = requireValue(args, ++i, a);
            } else if ("--dmnd_db".equals(a)) {
                out.dmndDb = requireValue(args, ++i, a);
            } else if ("--eggnog_db".equals(a)) {
                out.eggnogDb = requireValue(args, ++i, a);
            } else if ("--tax_db".equals(a)) {
                out.taxDb = requireValue(args, ++i, a);
            } else if ("--diamond_bin".equals(a)) {
                out.diamondBin = requireValue(args, ++i, a);
            } else if ("--cpu".equals(a)) {
                out.cpu = Integer.parseInt(requireValue(args, ++i, a));
            } else if ("--temp_dir".equals(a)) {
                out.tempDir = requireValue(args, ++i, a);
            } else if ("--sensmode".equals(a)) {
                out.sensmode = requireValue(args, ++i, a);
            } else if ("--dmnd_iterate".equals(a)) {
                out.dmndIterate = requireValue(args, ++i, a);
            } else if ("--dmnd_algo".equals(a)) {
                out.dmndAlgo = requireValue(args, ++i, a);
            } else if ("--dmnd_ignore_warnings".equals(a)) {
                out.dmndIgnoreWarnings = true;
            } else if ("--matrix".equals(a)) {
                out.matrix = requireValue(args, ++i, a);
            } else if ("--dmnd_frameshift".equals(a)) {
                out.dmndFrameshift = Integer.valueOf(requireValue(args, ++i, a));
            } else if ("--gapopen".equals(a)) {
                out.gapopen = Integer.valueOf(requireValue(args, ++i, a));
            } else if ("--gapextend".equals(a)) {
                out.gapextend = Integer.valueOf(requireValue(args, ++i, a));
            } else if ("--block_size".equals(a)) {
                out.dmndBlockSize = Double.valueOf(requireValue(args, ++i, a));
            } else if ("--index_chunks".equals(a)) {
                out.dmndIndexChunks = Integer.valueOf(requireValue(args, ++i, a));
            } else if ("--outfmt_short".equals(a)) {
                out.outfmtShort = true;
            } else if ("--evalue".equals(a)) {
                out.evalue = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--score".equals(a)) {
                out.score = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--pident".equals(a)) {
                out.pident = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--query_cover".equals(a)) {
                out.queryCover = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--subject_cover".equals(a)) {
                out.subjectCover = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--seed_ortholog_evalue".equals(a)) {
                out.seedOrthologEvalue = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--seed_ortholog_score".equals(a)) {
                out.seedOrthologScore = Double.parseDouble(requireValue(args, ++i, a));
            } else if ("--tax_scope_mode".equals(a)) {
                out.taxScopeMode = requireValue(args, ++i, a);
            } else if ("--tax_scope".equals(a)) {
                out.taxScope = requireValue(args, ++i, a);
            } else if ("--target_orthologs".equals(a)) {
                out.targetOrthologs = requireValue(args, ++i, a);
            } else if ("--target_taxa".equals(a)) {
                out.targetTaxa = requireValue(args, ++i, a);
            } else if ("--excluded_taxa".equals(a)) {
                out.excludedTaxa = requireValue(args, ++i, a);
            } else if ("--tax_scope_vars".equals(a)) {
                out.taxScopeVarsFile = requireValue(args, ++i, a);
            } else if ("--go_evidence".equals(a)) {
                out.goEvidence = requireValue(args, ++i, a);
            } else if ("--resume".equals(a)) {
                out.resume = true;
            } else if ("--override".equals(a)) {
                out.override = true;
            } else if ("--report_orthologs".equals(a)) {
                out.reportOrthologs = true;
            } else if ("--no_file_comments".equals(a)) {
                out.noFileComments = true;
            } else if ("--annot".equals(a)) {
                out.noAnnot = false;
            } else if ("--no_annot".equals(a)) {
                out.noAnnot = true;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + a);
            }
        }

        try {
            if (out.dataDir == null || out.dataDir.trim().isEmpty()) {
                out.dataDir = CompatUtils.defaultDataDir();
            }
            if (out.diamondBin == null || out.diamondBin.trim().isEmpty()) {
                out.diamondBin = CompatUtils.defaultDiamondBin();
            }
            if (out.taxScopeVarsFile == null || out.taxScopeVarsFile.trim().isEmpty()) {
                out.taxScopeVarsFile = CompatUtils.defaultTaxScopeVarsFile();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to resolve repository defaults: " + ex.getMessage(), ex);
        }
        if (out.dmndDb == null || out.dmndDb.trim().isEmpty()) {
            out.dmndDb = new File(out.dataDir, "eggnog_proteins.dmnd").getPath();
        }
        if (out.eggnogDb == null || out.eggnogDb.trim().isEmpty()) {
            out.eggnogDb = new File(out.dataDir, "eggnog.db").getPath();
        }
        if (out.taxDb == null || out.taxDb.trim().isEmpty()) {
            out.taxDb = new File(out.dataDir, "eggnog.taxa.db").getPath();
        }
        if (out.cpu == 0) {
            out.cpu = Runtime.getRuntime().availableProcessors();
        }
        if (out.inputPath == null || out.inputPath.isEmpty()) {
            throw new IllegalArgumentException("Missing -i/--input");
        }
        if (out.outputPrefix == null || out.outputPrefix.isEmpty()) {
            throw new IllegalArgumentException("Missing -o/--output");
        }
        if (!"diamond".equalsIgnoreCase(out.mode)) {
            throw new IllegalArgumentException("Only -m diamond is supported in eggnog_java");
        }
        if (out.resume && out.override) {
            throw new IllegalArgumentException("Only one of --resume or --override is allowed.");
        }
        String inputType = out.inputType == null ? "" : out.inputType.toLowerCase();
        if (!"proteins".equals(inputType) && !"cds".equals(inputType)) {
            throw new IllegalArgumentException("eggnog_java currently supports --itype proteins|CDS for diamond mode");
        }
        if (!new File(out.dmndDb).isFile()) {
            throw new IllegalArgumentException("DIAMOND database not found: " + out.dmndDb);
        }
        if ((!out.noAnnot || out.reportOrthologs) && !new File(out.eggnogDb).isFile()) {
            throw new IllegalArgumentException("Annotation DB not found: " + out.eggnogDb);
        }
        if ((!out.noAnnot || out.reportOrthologs) && !new File(out.taxDb).isFile()) {
            throw new IllegalArgumentException("Taxonomy DB not found: " + out.taxDb);
        }
        return out;
    }

    private static String requireValue(String[] args, int idx, String flag) {
        if (idx >= args.length) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        return args[idx];
    }

    public static void printHelp() {
        System.out.println("eggnog_java (diamond-compatible subset)");
        System.out.println("Usage:");
        System.out.println("  java Main -m diamond -i <input.fa> --itype proteins|CDS -o <out_prefix>");
        System.out.println("Options:");
        System.out.println("  --data_dir <dir>          eggnog-mapper data directory");
        System.out.println("  --output_dir <dir>        output directory");
        System.out.println("  --dmnd_db <path>          DIAMOND database path");
        System.out.println("  --diamond_bin <path>      diamond executable path");
        System.out.println("  --cpu <n>                 worker threads; 0 means all CPUs");
        System.out.println("  --temp_dir <dir>          temp directory");
        System.out.println("  --translate               when --itype CDS, translate input and run blastp");
        System.out.println("  --trans_table <code>      translation table code");
        System.out.println("  --sensmode <mode>         default|fast|mid-sensitive|sensitive|more-sensitive|very-sensitive|ultra-sensitive");
        System.out.println("  --dmnd_iterate <yes|no>   enable or disable diamond --iterate");
        System.out.println("  --dmnd_algo <mode>        auto|0|1|ctg");
        System.out.println("  --dmnd_ignore_warnings    pass --ignore-warnings to diamond");
        System.out.println("  --outfmt_short            use 4-column seed output");
        System.out.println("  --evalue <v> --score <v> --pident <v> --query_cover <v> --subject_cover <v>");
        System.out.println("  --seed_ortholog_evalue <v> --seed_ortholog_score <v>");
        System.out.println("  --tax_scope_mode <mode>   broadest|inner_broadest|inner_narrowest|narrowest");
        System.out.println("  --tax_scope <ids/file>    comma-separated taxa IDs/names, built-in scope, or file");
        System.out.println("  --target_orthologs <type> all|one2one|one2many|many2many|many2one");
        System.out.println("  --target_taxa <ids>       comma-separated taxa IDs or names");
        System.out.println("  --excluded_taxa <ids>     comma-separated taxa IDs or names");
        System.out.println("  --go_evidence <mode>      experimental|non-electronic|all");
        System.out.println("  --report_orthologs        output orthologs file");
        System.out.println("  --resume                  reuse existing hits and append missing annotations");
        System.out.println("  --override                overwrite existing outputs");
        System.out.println("  --no_annot                skip annotation");
    }
}
