package eggnogmapper.annotation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationRecord {
    public String query;
    public String seedOrtholog;
    public double evalue;
    public double score;
    public String eggnogOgs = "-";
    public String maxAnnotLvl = "-";
    public String cogCategory = "-";
    public String description = "-";
    public final Map<String, String> fields = new LinkedHashMap<String, String>();
    public final Map<String, Set<String>> allOrthologies = new LinkedHashMap<String, Set<String>>();
    public List<String> annotOrthologs;
}
