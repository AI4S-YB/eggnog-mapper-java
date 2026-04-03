package eggnogmapper.annotation.tax;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxScopeData {
    public final Map<String, Integer> levelDepth = new HashMap<String, Integer>();
    public final Map<String, String> levelNames = new HashMap<String, String>();
    public final Map<String, String> levelDict = new HashMap<String, String>();
    public final Map<String, List<String>> levelParents = new HashMap<String, List<String>>();
}
