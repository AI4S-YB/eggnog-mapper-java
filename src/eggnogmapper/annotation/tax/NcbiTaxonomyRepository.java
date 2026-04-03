package eggnogmapper.annotation.tax;

import eggnogmapper.common.SqliteDriverSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NcbiTaxonomyRepository implements AutoCloseable {
    private final Connection conn;
    private final boolean hasSynonymTable;

    public NcbiTaxonomyRepository(String sqlitePath) throws SQLException {
        ensureSqliteDriver();
        this.conn = SqliteDriverSupport.open(sqlitePath);
        this.hasSynonymTable = hasTable("synonym");
    }

    public Map<Integer, String> getTaxidTranslator(Set<Integer> taxids) throws SQLException {
        Map<Integer, String> out = new LinkedHashMap<Integer, String>();
        if (taxids == null || taxids.isEmpty()) {
            return out;
        }

        List<Integer> ids = new ArrayList<Integer>(taxids);
        Collections.sort(ids);
        PreparedStatement ps = conn.prepareStatement("SELECT taxid, spname FROM species WHERE taxid IN (" + placeholders(ids.size()) + ")");
        bindInts(ps, ids);
        ResultSet rs = ps.executeQuery();
        Set<Integer> found = new HashSet<Integer>();
        while (rs.next()) {
            int taxid = rs.getInt(1);
            out.put(Integer.valueOf(taxid), rs.getString(2));
            found.add(Integer.valueOf(taxid));
        }
        rs.close();
        ps.close();

        for (Integer rawId : ids) {
            if (found.contains(rawId)) {
                continue;
            }
            int translated = translateMerged(rawId.intValue());
            if (translated != rawId.intValue()) {
                PreparedStatement ps2 = conn.prepareStatement("SELECT spname FROM species WHERE taxid = ?");
                ps2.setInt(1, translated);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    out.put(rawId, rs2.getString(1));
                }
                rs2.close();
                ps2.close();
            }
        }
        return out;
    }

    public int resolveTaxonToken(String token) throws SQLException {
        try {
            return translateMerged(Integer.parseInt(token));
        } catch (NumberFormatException ignored) {
        }

        List<String> names = new ArrayList<String>();
        names.add(token);
        Map<String, List<Integer>> translated = getNameTranslator(names);
        List<Integer> ids = translated.get(token);
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Unrecognized taxon: " + token);
        }
        return translateMerged(ids.get(0).intValue());
    }

    public Set<Integer> getDescendantTaxa(int taxid, boolean intermediateNodes) throws SQLException {
        int translated = translateMerged(taxid);
        Set<Integer> out = new HashSet<Integer>();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT taxid FROM species WHERE track = ? OR track LIKE ? OR track LIKE ? OR track LIKE ?"
        );
        String tid = String.valueOf(translated);
        ps.setString(1, tid);
        ps.setString(2, tid + ",%");
        ps.setString(3, "%," + tid + ",%");
        ps.setString(4, "%," + tid);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            out.add(Integer.valueOf(rs.getInt(1)));
        }
        rs.close();
        ps.close();
        if (out.isEmpty()) {
            out.add(Integer.valueOf(translated));
        }
        return out;
    }

    public Map<String, List<Integer>> getNameTranslator(List<String> names) throws SQLException {
        Map<String, List<Integer>> out = new HashMap<String, List<Integer>>();
        if (names == null || names.isEmpty()) {
            return out;
        }
        Map<String, String> originalByLower = new HashMap<String, String>();
        for (String name : names) {
            originalByLower.put(name.toLowerCase(), name);
        }

        PreparedStatement ps = conn.prepareStatement("SELECT spname, taxid FROM species WHERE spname IN (" + placeholders(names.size()) + ")");
        bindStrings(ps, names);
        ResultSet rs = ps.executeQuery();
        Set<String> matched = new HashSet<String>();
        while (rs.next()) {
            String name = rs.getString(1);
            String original = originalByLower.containsKey(name.toLowerCase()) ? originalByLower.get(name.toLowerCase()) : name;
            int taxid = rs.getInt(2);
            List<Integer> ids = out.get(original);
            if (ids == null) {
                ids = new ArrayList<Integer>();
                out.put(original, ids);
            }
            ids.add(Integer.valueOf(taxid));
            matched.add(original.toLowerCase());
        }
        rs.close();
        ps.close();

        if (hasSynonymTable) {
            List<String> missing = new ArrayList<String>();
            for (String name : names) {
                if (!matched.contains(name.toLowerCase())) {
                    missing.add(name);
                }
            }
            if (!missing.isEmpty()) {
                PreparedStatement ps2 = conn.prepareStatement("SELECT spname, taxid FROM synonym WHERE spname IN (" + placeholders(missing.size()) + ")");
                bindStrings(ps2, missing);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    String name = rs2.getString(1);
                    String original = originalByLower.containsKey(name.toLowerCase()) ? originalByLower.get(name.toLowerCase()) : name;
                    int taxid = rs2.getInt(2);
                    List<Integer> ids = out.get(original);
                    if (ids == null) {
                        ids = new ArrayList<Integer>();
                        out.put(original, ids);
                    }
                    ids.add(Integer.valueOf(taxid));
                }
                rs2.close();
                ps2.close();
            }
        }
        return out;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    private boolean hasTable(String table) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?");
        ps.setString(1, table);
        ResultSet rs = ps.executeQuery();
        boolean found = rs.next();
        rs.close();
        ps.close();
        return found;
    }

    private int translateMerged(int taxid) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT taxid_new FROM merged WHERE taxid_old = ?");
        ps.setInt(1, taxid);
        ResultSet rs = ps.executeQuery();
        int out = taxid;
        if (rs.next()) {
            out = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return out;
    }

    private static void bindInts(PreparedStatement ps, List<Integer> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            ps.setInt(i + 1, values.get(i).intValue());
        }
    }

    private static void bindStrings(PreparedStatement ps, List<String> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            ps.setString(i + 1, values.get(i));
        }
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        return sb.toString();
    }

    private static void ensureSqliteDriver() throws SQLException {
        SqliteDriverSupport.ensureLoaded();
    }
}
