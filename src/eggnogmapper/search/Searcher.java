package eggnogmapper.search;

import java.io.IOException;
import java.util.List;

public interface Searcher {
    List<Hit> search(String inputPath, String hitsPath) throws IOException, InterruptedException;

    List<String> getExecutedCommands();

    void clear() throws IOException;
}
