# eggnog-mapper-java

Pure Java reimplementation of the `DIAMOND`-based command-line path from the upstream [eggnog-mapper](https://github.com/eggnogdb/eggnog-mapper) project.

This project focuses on the practical workflow most users need:

`DIAMOND search -> seed ortholog selection -> optional functional annotation -> optional ortholog report`

It is not a full reimplementation of every upstream mode. The current codebase is intentionally scoped to the `-m diamond` pipeline.

For implementation details and architecture notes, see [TECHNICAL_OVERVIEW.md](TECHNICAL_OVERVIEW.md).

## Highlights

- Pure Java entry point and pipeline orchestration.
- Reuses the same `eggnog_proteins.dmnd`, `eggnog.db`, and `eggnog.taxa.db` databases as upstream `eggnog-mapper`.
- Supports both `--itype proteins` and `--itype CDS`.
- Supports CDS search in two modes:
  - default `blastx` mode
  - `--translate` mode, which translates CDS to protein first and then runs `blastp`
- Produces the familiar `.emapper.hits`, `.emapper.seed_orthologs`, `.emapper.annotations`, and optional `.emapper.orthologs` outputs.
- Supports `--resume` and `--override` execution semantics for incremental reruns.
- Bundles tax scope resources so the project can still run even when the upstream Python package layout is not present on disk.

## Current Scope

Implemented and tested:

- `-m diamond`
- protein and CDS input
- seed ortholog extraction
- annotation from `eggnog.db`
- taxonomy-aware ortholog filtering from `eggnog.taxa.db`
- optional ortholog reporting

Not in scope in this repository:

- non-DIAMOND search modes
- upstream Python runtime and its broader CLI surface
- a claim of feature parity with every option or workflow from upstream `eggnog-mapper`

## Requirements

Before running this project, make sure the following are available:

- Java 11 or newer
- a working `diamond` executable
- eggNOG data files compatible with the upstream project:
  - `eggnog_proteins.dmnd`
  - `eggnog.db`
  - `eggnog.taxa.db`

Executable and data lookup rules:

- `diamond` is resolved in this order:
  - `--diamond_bin <path>`
  - repository-local `eggnogmapper/bin/diamond` if present
  - `diamond` from system `PATH`
- database paths are resolved in this order:
  - explicit `--dmnd_db`, `--eggnog_db`, `--tax_db`
  - `--data_dir <dir>`
  - `EGGNOG_DATA_DIR` environment variable
  - fallback `data/` under the detected repository root or current working directory

## Build

This repository includes a NetBeans-compatible Ant project.

- Source root: `src`
- Main class: `Main`
- Build system: Ant

Common commands:

```bash
ant compile
ant jar
ant fat-jar
```

Artifacts:

- `dist/eggnog-mapper-java.jar`
- `dist/eggnog-mapper-java-fat.jar`

Note:

- The SQLite JDBC driver is not vendored in this repository.
- NetBeans `project.properties` currently points to a shared local copy at `D:\NetbeansProject2\TBtools\lib\sqlite-jdbc-3.20.1.jar`.
- If you build on another machine, update that path or provide an equivalent JAR.

## Quick Start

Protein FASTA input:

```bash
java -jar dist/eggnog-mapper-java-fat.jar \
  -m diamond \
  -i proteins.fa \
  --itype proteins \
  -o sample \
  --data_dir /path/to/eggnog-data \
  --output_dir results
```

CDS input using `blastx`:

```bash
java -jar dist/eggnog-mapper-java-fat.jar \
  -m diamond \
  -i cds.fa \
  --itype CDS \
  -o sample_cds \
  --data_dir /path/to/eggnog-data \
  --output_dir results
```

CDS input translated to proteins first, then searched with `blastp`:

```bash
java -jar dist/eggnog-mapper-java-fat.jar \
  -m diamond \
  -i cds.fa \
  --itype CDS \
  --translate \
  --trans_table 11 \
  -o sample_cds_translated \
  --data_dir /path/to/eggnog-data \
  --output_dir results
```

Search only, skip annotation:

```bash
java -jar dist/eggnog-mapper-java-fat.jar \
  -m diamond \
  -i proteins.fa \
  --itype proteins \
  -o sample_hits_only \
  --data_dir /path/to/eggnog-data \
  --output_dir results \
  --no_annot
```

## Output Files

For output prefix `sample`, the pipeline writes:

- `sample.emapper.hits`
  - raw DIAMOND tabular output
- `sample.emapper.hits.diamond.log`
  - DIAMOND stdout and stderr
- `sample.emapper.seed_orthologs`
  - one retained seed ortholog per query
- `sample.emapper.annotations`
  - functional annotation output unless `--no_annot` is used
- `sample.emapper.orthologs`
  - ortholog report only when `--report_orthologs` is enabled

By default, output files contain metadata comment lines starting with `##`. Use `--no_file_comments` to suppress those comments.

## Important Runtime Behavior

- Only `-m diamond` is accepted.
- `--itype` must be `proteins` or `CDS`.
- `--resume` and `--override` are mutually exclusive.
- Existing output files cause execution to stop unless `--resume` or `--override` is used.
- When `--resume` is enabled:
  - the existing `.emapper.hits` file must already exist
  - the search step is skipped
  - annotation outputs are appended only for queries not already present in the target files
- When `--override` is enabled, existing target outputs are deleted before rerun.
- Query IDs are normalized to the first whitespace-delimited token in each FASTA header.
  - Example: `>prot1 description text` becomes query ID `prot1` in downstream `.emapper.*` files.
- If `--temp_dir` is not provided, temporary files are created under `<output_dir>/.tmp`.
  - each run creates its own random working directory there
  - the per-run working directory is deleted after completion
  - the `.tmp` parent directory is kept

## CLI Summary

Minimal usage:

```bash
java -jar dist/eggnog-mapper-java-fat.jar -m diamond -i <input.fa> --itype proteins|CDS -o <out_prefix>
```

General:

- `-h`, `--help` print usage
- `-v`, `--version` print version
- `-m`, `--mode` currently only `diamond`
- `-i`, `--input` input FASTA path
- `--itype <proteins|CDS>` input sequence type
- `-o`, `--output` output prefix
- `--output_dir <dir>` output directory, default is current working directory

Data and executables:

- `--data_dir <dir>` eggNOG data directory
- `--dmnd_db <path>` DIAMOND database path
- `--eggnog_db <path>` eggNOG annotation SQLite DB path
- `--tax_db <path>` taxonomy SQLite DB path
- `--diamond_bin <path>` DIAMOND executable path
- `--tax_scope_vars <path>` override tax scope vars file

Search and performance:

- `--cpu <n>` worker threads, `0` means all CPUs
- `--temp_dir <dir>` temp parent directory
- `--sensmode <mode>` `default|fast|mid-sensitive|sensitive|more-sensitive|very-sensitive|ultra-sensitive`
- `--dmnd_iterate <yes|no>` enable or disable `diamond --iterate`
- `--dmnd_algo <mode>` `auto|0|1|ctg`
- `--dmnd_ignore_warnings` pass `--ignore-warnings` to DIAMOND
- `--matrix <name>` DIAMOND substitution matrix
- `--dmnd_frameshift <int>` pass DIAMOND `--frameshift`
- `--gapopen <int>` pass DIAMOND `--gapopen`
- `--gapextend <int>` pass DIAMOND `--gapextend`
- `--block_size <float>` pass DIAMOND `--block-size`
- `--index_chunks <int>` pass DIAMOND `-c`

Search filtering and seed ortholog selection:

- `--outfmt_short` emit 4-column seed ortholog output
- `--evalue <v>` default `0.001`
- `--score <v>` minimum score
- `--pident <v>` minimum percent identity
- `--query_cover <v>` minimum query coverage
- `--subject_cover <v>` minimum subject coverage
- `--seed_ortholog_evalue <v>` default `0.001`
- `--seed_ortholog_score <v>` seed ortholog score filter

CDS translation:

- `--translate` translate CDS and run `blastp` instead of `blastx`
- `--trans_table <code>` translation table code; also forwarded to DIAMOND as `--query-gencode`

Annotation and ortholog filtering:

- `--tax_scope_mode <mode>` `broadest|inner_broadest|inner_narrowest|narrowest`
- `--tax_scope <ids/file>` comma-separated tax IDs or names, built-in scope name, or file
- `--target_orthologs <type>` `all|one2one|one2many|many2many|many2one`
- `--target_taxa <ids>` comma-separated taxa IDs or names
- `--excluded_taxa <ids>` comma-separated taxa IDs or names
- `--go_evidence <mode>` `experimental|non-electronic|all`
- `--report_orthologs` write `.emapper.orthologs`
- `--annot` explicitly enable annotation
- `--no_annot` skip annotation output

Output and rerun control:

- `--no_file_comments` suppress `##` metadata comments in output files
- `--resume` reuse existing hits and continue missing annotations
- `--override` delete and recreate target outputs

## Validation

The annotation results of this implementation were compared against upstream `eggnog-mapper` using the same databases.

For 10 test genome protein datasets, annotated entries were consistent with the original program.

Tested species:

- *Acanthaster planci*
- *Ananas comosus*
- *Aphanomyces astaci*
- *Arabidopsis thaliana*
- *Armillaria ostoyae*
- *Caenorhabditis elegans*
- Human
- Mouse
- *Oryza sativa*
- *Sus scrofa*

## License

This repository follows the upstream `eggnog-mapper` licensing model and is distributed under the GNU Affero General Public License v3.0.

- Current project license: [LICENSE.txt](LICENSE.txt)
- Upstream project: <https://github.com/eggnogdb/eggnog-mapper>
- Upstream license text: <https://raw.githubusercontent.com/eggnogdb/eggnog-mapper/master/LICENSE.txt>
