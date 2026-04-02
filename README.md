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
java -jar eggnog_java.jar -m diamond -i <input.fa> --itype proteins|CDS -o <out_prefix>

Options
  --data_dir <dir>          eggnog-mapper data directory
  --output_dir <dir>        output directory
  --dmnd_db <path>          DIAMOND database path
  --diamond_bin <path>      diamond executable path
  --cpu <n>                 worker threads; 0 means all CPUs
  --temp_dir <dir>          temp directory
  --translate               when --itype CDS, translate input and run blastp
  --trans_table <code>      translation table code
  --sensmode <mode>         default|fast|mid-sensitive|sensitive|more-sensitive|very-sensitive|ultra-sensitive
  --dmnd_iterate <yes|no>   enable or disable diamond --iterate
  --dmnd_algo <mode>        auto|0|1|ctg
  --dmnd_ignore_warnings    pass --ignore-warnings to diamond
  --outfmt_short            use 4-column seed output
  --evalue <v> --score <v> --pident <v> --query_cover <v> --subject_cover <v>
  --seed_ortholog_evalue <v> --seed_ortholog_score <v>
  --tax_scope_mode <mode>   broadest|inner_broadest|inner_narrowest|narrowest
  --tax_scope <ids/file>    comma-separated taxa IDs/names, built-in scope, or file
  --target_orthologs <type> all|one2one|one2many|many2many|many2one
  --target_taxa <ids>       comma-separated taxa IDs or names
  --excluded_taxa <ids>     comma-separated taxa IDs or names
  --go_evidence <mode>      experimental|non-electronic|all
  --report_orthologs        output orthologs file
  --resume                  reuse existing hits and append missing annotations
  --override                overwrite existing outputs
  --no_annot                skip annotation
