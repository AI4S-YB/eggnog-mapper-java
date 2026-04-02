# eggnog-mapper-DiamondOnly (Pure Java)
## English

### Overview

This project is a pure Java implementation designed to reproduce the **DIAMOND-based workflow** of [eggnog-mapper](https://github.com/eggnogdb/eggnog-mapper), with the goal of improving cross-platform usability through Java.

The implemented workflow covers the following chain:

`hits -> seed orthologs -> (optional) annotation -> (optional) orthologs`

### Requirements

Before running this program, make sure the following dependencies are available:

- **Java** must be installed on your computer.
- A working `diamond` executable is required.
  - By default, the program will first try:
    - `eggnogmapper/bin/diamond`
  - If not found, it will try `diamond` from the system `PATH`.
  - You can also explicitly specify the executable path with `--diamond_bin`.
- The same three database files required by the original eggnog-mapper are also required:
  - `eggnog_proteins.dmnd`
  - `eggnog.db`
  - `eggnog.taxa.db`

### Validation

The annotation results of this program were compared with those of the original eggnog-mapper using the same databases.

For 10 test genome protein datasets, the annotated entries were consistent with those produced by the original program.

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

### Usage

```bash
java -jar eggnog_java.jar -m diamond -i <input.fa> --itype proteins|CDS -o <out_prefix>```

Options
--data_dir <dir>
eggnog-mapper data directory
--output_dir <dir>
output directory
--dmnd_db <path>
DIAMOND database path
--diamond_bin <path>
path to the diamond executable
--cpu <n>
number of worker threads; 0 means using all available CPUs
--temp_dir <dir>
temporary directory
--translate
when --itype CDS is used, translate the input sequences and run blastp
--trans_table <code>
translation table code
--sensmode <mode>
sensitivity mode for DIAMOND
available values: default|fast|mid-sensitive|sensitive|more-sensitive|very-sensitive|ultra-sensitive
--dmnd_iterate <yes|no>
enable or disable DIAMOND --iterate
--dmnd_algo <mode>
DIAMOND algorithm mode
available values: auto|0|1|ctg
--dmnd_ignore_warnings
pass --ignore-warnings to DIAMOND
--outfmt_short
use 4-column seed output format
--evalue <v>
e-value threshold
--score <v>
minimum score threshold
--pident <v>
minimum percent identity
--query_cover <v>
minimum query coverage
--subject_cover <v>
minimum subject coverage
--seed_ortholog_evalue <v>
e-value threshold for seed ortholog selection
--seed_ortholog_score <v>
score threshold for seed ortholog selection
--tax_scope_mode <mode>
taxonomic scope mode
available values: broadest|inner_broadest|inner_narrowest|narrowest
--tax_scope <ids/file>
comma-separated taxon IDs or names, built-in scope, or a file
--target_orthologs <type>
ortholog type to report
available values: all|one2one|one2many|many2many|many2one
--target_taxa <ids>
comma-separated target taxon IDs or names
--excluded_taxa <ids>
comma-separated taxon IDs or names to exclude
--go_evidence <mode>
GO evidence filtering mode
available values: experimental|non-electronic|all
--report_orthologs
output orthologs file
--resume
reuse existing hits and append missing annotations
--override
overwrite existing output files
--no_annot
skip annotation step
