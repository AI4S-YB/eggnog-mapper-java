# eggnog-mapper-java Technical Overview

## 1. Purpose

`eggnog-mapper-java` is a pure Java implementation of the DIAMOND-based subset of `eggnog-mapper`.

The implemented workflow is:

`DIAMOND search -> hits -> seed orthologs -> annotation -> optional ortholog report`

This repository is not a full reimplementation of every mode in upstream `eggnog-mapper`. It currently focuses on the practical command-line path needed to:

- search proteins or CDS against a DIAMOND database
- select one seed ortholog per query
- annotate hits from SQLite eggNOG databases
- optionally emit ortholog group members

## 2. Entry Points and Module Layout

### Runtime entry

- `Main` parses CLI arguments and starts the pipeline.
- `Main-Class` is declared in `src/META-INF/MANIFEST.MF`, so the packaged jar can be launched with `java -jar eggnog_java.jar`.

### Main modules

- `src/Main.java`
  - top-level process entry
- `src/eggnogmapper/cli/`
  - `CliArgs`: runtime configuration holder
  - `CliParser`: command-line parsing and validation
- `src/eggnogmapper/emapper/`
  - `EmapperPipeline`: orchestrates search, seed writing, annotation, and output guard behavior
- `src/eggnogmapper/search/`
  - `Searcher` abstraction
  - `DiamondSearcher`: DIAMOND invocation, temp directory handling, hit parsing
  - `SearcherFactory`: currently resolves the DIAMOND implementation
- `src/eggnogmapper/annotation/`
  - `AnnotatorService`: bridges pipeline and annotation layer
  - `EggnogAnnotator`: OG selection, ortholog filtering, functional annotation aggregation
  - `AnnotationOutputWriter` / `OrthologOutputWriter`: final report writers
- `src/eggnogmapper/annotation/db/`
  - `EggnogSqliteRepository`: queries `eggnog.db`
- `src/eggnogmapper/annotation/tax/`
  - `NcbiTaxonomyRepository`: queries taxonomy SQLite DB
  - `TaxScopeLoader` / `TaxScopeSelector`: taxonomic scope parsing and OG selection
- `src/eggnogmapper/common/`
  - repository defaults, bundled resource extraction, FASTA translation helpers, SQLite driver loading

## 3. Pipeline Behavior

### 3.1 CLI parsing and validation

The CLI layer enforces several project-level constraints:

- only `-m diamond` is supported
- `--itype` must be `proteins` or `CDS`
- `--resume` and `--override` are mutually exclusive
- annotation DBs are required unless `--no_annot` is active and ortholog reporting is disabled

Important defaults from `CliArgs`:

- `mode=diamond`
- `inputType=proteins`
- `outputDir=current working directory`
- `tempDir=<outputDir>/.tmp` unless `--temp_dir` is provided
- `cpu=1`, but `--cpu 0` expands to all available processors
- `evalue=0.001`
- `sensmode=sensitive`
- `dmndIterate=yes`
- `taxScopeMode=inner_narrowest`
- `taxScope=auto`
- `targetOrthologs=all`
- `goEvidence=non-electronic`

### 3.2 Search phase

`DiamondSearcher` is responsible for:

- choosing `blastp` for protein input
- choosing `blastx` for CDS input unless `--translate` is enabled
- rewriting input FASTA queries so each ID keeps only the first whitespace-delimited token from the header
- optionally translating CDS into protein FASTA before search
- creating a per-run random temp work directory under the configured temp parent directory
- writing DIAMOND output to `<prefix>.emapper.hits`
- writing DIAMOND stdout/stderr to `<prefix>.emapper.hits.diamond.log`

The search output is parsed into `Hit` objects. For each query, only the first observed hit is kept for downstream annotation. When the long output format is used, the retained fields are:

- query id
- target id
- e-value
- bit score
- query and subject coordinates
- identity
- query coverage
- subject coverage

This means a header such as `>prot1 description text` is emitted downstream as query id `prot1` in `.hits`, `.seed_orthologs`, `.annotations`, and `.orthologs`.

By default, the temp parent directory is `<outputDir>/.tmp`. Each run creates a random working directory inside it, passes that path to DIAMOND `--tmpdir`, and deletes the per-run working directory during cleanup.

### 3.3 Seed ortholog report

`SeedOrthologWriter` writes `<prefix>.emapper.seed_orthologs`.

This file contains:

- invocation metadata comments
- the exact DIAMOND command recorded as comment lines
- one selected seed hit per query

### 3.4 Annotation phase

`AnnotatorService` opens:

- the functional eggNOG SQLite DB through `EggnogSqliteRepository`
- the taxonomy SQLite DB through `NcbiTaxonomyRepository`

`EggnogAnnotator` then performs:

- seed ortholog threshold filtering
- eggNOG OG membership lookup from the `prots` table
- tax-scope-aware OG selection
- optional co-ortholog inference through event data
- target ortholog filtering by relationship type and taxa include/exclude sets
- functional field aggregation across selected orthologs

Current aggregated fields include:

- preferred gene name
- GO
- EC
- KEGG KO / pathway / module / reaction / rclass / BRITE / TC
- CAZy
- BiGG reaction
- PFAM

PFAMs are filtered more aggressively than the other fields: a PFAM is emitted only if it appears in more than one ortholog and in more than 5% of the annotation ortholog set.

### 3.5 Ortholog report

When `--report_orthologs` is enabled, `OrthologOutputWriter` writes `<prefix>.emapper.orthologs`.

The output groups orthologs by:

- orthology type
- species

The seed ortholog is emitted as a dedicated `seed` row, and orthologs that also participate in annotation are marked with `*`.

## 4. External Dependencies and Data Requirements

### Java

- A Java runtime is required.
- The packaged jar is runnable directly through `java -jar`.

### DIAMOND

- A working `diamond` executable is required.
- Resolution order:
  - explicit `--diamond_bin`
  - repository-local `eggnogmapper/bin/diamond`
  - `diamond` from `PATH`

### SQLite JDBC

SQLite access is implemented through `org.sqlite.JDBC`.

The project can load the driver from:

- the runtime classpath
- the packaged fat jar
- `lib/` directories near the jar or working directory
- JVM system properties pointing to a jar or directory

The packaged `eggnog_java.jar` already includes `sqlite-jdbc`, so normal `java -jar eggnog_java.jar ...` execution works without adding an extra dependency.

### eggNOG databases

The runtime expects three data assets:

- DIAMOND database (`*.dmnd`)
- functional SQLite DB (`eggnog.db` or a clade-specific variant)
- taxonomy SQLite DB (`eggnog.taxa.db`)

Important practical note:

- the code defaults assume the canonical upstream file names `eggnog_proteins.dmnd`, `eggnog.db`, and `eggnog.taxa.db`
- if you maintain clade-specific filenames such as `Viridiplantae.dmnd` or `eggnog.Streptophyta.db`, you must pass them explicitly with `--dmnd_db`, `--eggnog_db`, and `--tax_db`

## 5. Tax Scope and Annotation Semantics

Tax-scope resources are loaded either:

- from an upstream-style repository layout detected by `CompatUtils.findRepoRoot()`, or
- from the resources bundled inside the jar

This means packaged execution does not require a separate external `vars.py` as long as the bundled resources are present.

The default scope behavior is:

- `tax_scope=auto`
- `tax_scope_mode=inner_narrowest`

This generally pushes annotation toward the narrowest valid OG within the allowed lineage rather than the broadest OG available.

GO evidence filtering defaults to `non-electronic`, which excludes `ND` and `IEA`. Other supported modes are:

- `experimental`
- `all`

## 6. Output Files

For an output prefix `<prefix>`, the pipeline may create:

- `<prefix>.emapper.hits`
- `<prefix>.emapper.hits.diamond.log`
- `<prefix>.emapper.seed_orthologs`
- `<prefix>.emapper.annotations`
- `<prefix>.emapper.orthologs` when `--report_orthologs` is enabled

Writers prepend metadata comment lines by default, including:

- timestamp
- emapper version string
- exact CLI invocation

## 7. Output Guard and Resume Rules

`EmapperPipeline` guards expected outputs before starting:

- `--override` deletes existing target outputs first
- `--resume` reuses existing hits and appends missing annotations
- with neither flag, pre-existing outputs cause execution to fail fast

Resume behavior is stage-aware:

- search resumes from an existing `.hits` file
- annotation resumes by parsing already written query ids from annotation or ortholog output

## 8. Tested Local Validation

The project was validated locally against the databases stored in the sibling `eggnog-databases` directory and sample FASTA files in `demo data`.

Validated combinations:

- `Oryza_sativa.genome.pep` with the `Viridiplantae` database set
- `Pea.genome.pep.fa` with the `Viridiplantae` database set

Observed successful runs:

- smoke test: 2 Oryza protein sequences, complete `hits -> seed_orthologs -> annotations`
- functional test: 100 Oryza protein sequences, complete `hits -> seed_orthologs -> annotations -> orthologs`, 83 queries with hits
- full pilot: complete `Oryza_sativa.genome.pep`, `--cpu 12`, successful generation of `hits`, `seed_orthologs`, and `annotations`
- cross-check sample: 20 Pea protein sequences, 20 queries with hits

In the local full Oryza pilot:

- input records: 55,986 protein sequences
- DIAMOND aligned queries: 46,352
- end-to-end wall time: about 11 minutes on the tested machine

These numbers are an environment-specific validation result, not a guaranteed benchmark.

## 9. Recommended Command Patterns

### Full annotation run

```bash
java -jar eggnog_java.jar \
  -m diamond \
  -i /path/to/input.fa \
  --itype proteins \
  -o sample \
  --output_dir /path/to/out \
  --dmnd_db /path/to/Viridiplantae.dmnd \
  --eggnog_db /path/to/eggnog.db \
  --tax_db /path/to/eggnog.taxa.db \
  --diamond_bin /path/to/diamond \
  --cpu 12
```

### Include ortholog report

```bash
java -jar eggnog_java.jar \
  -m diamond \
  -i /path/to/input.fa \
  --itype proteins \
  -o sample \
  --output_dir /path/to/out \
  --dmnd_db /path/to/Viridiplantae.dmnd \
  --eggnog_db /path/to/eggnog.db \
  --tax_db /path/to/eggnog.taxa.db \
  --diamond_bin /path/to/diamond \
  --cpu 12 \
  --report_orthologs
```

## 10. Current Limitations

- only DIAMOND mode is implemented
- the project is centered on the DIAMOND subset of eggnog-mapper rather than the full upstream feature set
- the code assumes a SQLite schema compatible with the queries used in `EggnogSqliteRepository` and `NcbiTaxonomyRepository`
- there is no built-in automated test suite in the repository at the time of writing

## 11. Suggested Next Maintenance Steps

- add a lightweight regression test harness around small FASTA subsets and fixed database fixtures
- document database naming conventions and required explicit flags more prominently in `README.md`
- add performance notes for full-scale runs and recommended output directory layout
