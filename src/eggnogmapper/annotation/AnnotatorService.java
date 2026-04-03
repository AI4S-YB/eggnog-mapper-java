package eggnogmapper.annotation;

import eggnogmapper.annotation.db.EggnogSqliteRepository;
import eggnogmapper.annotation.tax.NcbiTaxonomyRepository;
import eggnogmapper.cli.CliArgs;
import eggnogmapper.search.Hit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnnotatorService {
    private final CliArgs args;

    public AnnotatorService(CliArgs args) {
        this.args = args;
    }

    public void annotate(String annotationsPath, String orthologsPath, List<Hit> hits) throws IOException {
        if (args.noAnnot && !args.reportOrthologs) {
            return;
        }
        if (args.eggnogDb == null || args.eggnogDb.trim().isEmpty()) {
            throw new IllegalArgumentException("Annotation requires --eggnog_db <path>");
        }
        try {
            EggnogSqliteRepository db = new EggnogSqliteRepository(args.eggnogDb);
            NcbiTaxonomyRepository ncbi = new NcbiTaxonomyRepository(args.taxDb);
            try {
                EggnogAnnotator annotator = new EggnogAnnotator(
                        db,
                        ncbi,
                        args.taxScopeVarsFile,
                        args.taxScopeMode,
                        args.taxScope,
                        args.targetOrthologs,
                        args.targetTaxa,
                        args.excludedTaxa,
                        args.goEvidence
                );
                List<Hit> toAnnotate = hits;
                if (args.resume) {
                    Set<String> done = args.noAnnot
                            ? OrthologOutputWriter.parseExistingQueries(orthologsPath)
                            : AnnotationOutputWriter.parseExistingQueries(annotationsPath);
                    if (!done.isEmpty()) {
                        List<Hit> filtered = new ArrayList<Hit>();
                        for (Hit h : hits) {
                            if (!done.contains(h.query)) {
                                filtered.add(h);
                            }
                        }
                        toAnnotate = filtered;
                    }
                }

                List<AnnotationRecord> rows = annotator.annotate(toAnnotate, args.seedOrthologEvalue, args.seedOrthologScore);
                if (!args.noAnnot) {
                    AnnotationOutputWriter.writeAnnotations(annotationsPath, rows, args, hits.size());
                }
                if (args.reportOrthologs) {
                    OrthologOutputWriter.writeOrthologs(orthologsPath, rows, ncbi, args, hits.size());
                }
            } finally {
                ncbi.close();
                db.close();
            }
        } catch (SQLException ex) {
            throw new IOException("SQLite annotation failed: " + ex.getMessage(), ex);
        }
    }
}
