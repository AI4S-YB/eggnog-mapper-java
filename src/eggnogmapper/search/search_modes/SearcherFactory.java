package eggnogmapper.search.search_modes;

import eggnogmapper.cli.CliArgs;
import eggnogmapper.search.Searcher;
import eggnogmapper.search.diamond.DiamondSearcher;

public class SearcherFactory {
    public static Searcher fromArgs(CliArgs args) {
        if ("diamond".equalsIgnoreCase(args.mode)) {
            return new DiamondSearcher(args);
        }
        throw new IllegalArgumentException("Unsupported mode: " + args.mode);
    }
}
