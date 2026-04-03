package eggnogmapper.emapper;

import eggnogmapper.annotation.AnnotatorService;
import eggnogmapper.cli.CliArgs;
import eggnogmapper.common.CompatUtils;
import eggnogmapper.search.Hit;
import eggnogmapper.search.Searcher;
import eggnogmapper.search.hits_io.SeedOrthologWriter;
import eggnogmapper.search.search_modes.SearcherFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmapperPipeline {
    private final CliArgs args;

    public EmapperPipeline(CliArgs args) {
        this.args = args;
    }

    public void run() throws Exception {
        File outputDir = new File(args.outputDir);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
        }

        String hitsPath = new File(outputDir, args.outputPrefix + ".emapper.hits").getPath();
        String seedsPath = new File(outputDir, args.outputPrefix + ".emapper.seed_orthologs").getPath();
        String annPath = new File(outputDir, args.outputPrefix + ".emapper.annotations").getPath();
        String orthPath = new File(outputDir, args.outputPrefix + ".emapper.orthologs").getPath();

        List<File> expectedOutputs = new ArrayList<File>();
        expectedOutputs.add(new File(hitsPath));
        expectedOutputs.add(new File(seedsPath));
        if (!args.noAnnot) {
            expectedOutputs.add(new File(annPath));
        }
        if (args.reportOrthologs) {
            expectedOutputs.add(new File(orthPath));
        }
        guardOutputs(expectedOutputs);

        System.out.println("# " + CompatUtils.readVersion());
        System.out.println("# java Main " + String.join(" ", args.rawArgs));

        Searcher searcher = SearcherFactory.fromArgs(args);
        try {
            List<Hit> hits = searcher.search(args.inputPath, hitsPath);
            SeedOrthologWriter.write(seedsPath, hits, args, searcher.getExecutedCommands());

            if (!args.noAnnot || args.reportOrthologs) {
                AnnotatorService annotator = new AnnotatorService(args);
                annotator.annotate(annPath, orthPath, hits);
            }

            System.out.println("Done");
            for (File f : expectedOutputs) {
                if (f.isFile()) {
                    System.out.println("  " + f.getAbsolutePath());
                }
            }
            System.out.println("  total_queries_with_hit: " + hits.size());
        } finally {
            searcher.clear();
        }
    }

    private void guardOutputs(List<File> outputs) {
        if (args.override) {
            for (File f : outputs) {
                CompatUtils.deleteIfExists(f);
            }
            return;
        }
        if (args.resume) {
            return;
        }
        for (File f : outputs) {
            if (f.exists()) {
                throw new IllegalArgumentException("Output files detected in disk. Use --resume or --override to continue");
            }
        }
    }
}
