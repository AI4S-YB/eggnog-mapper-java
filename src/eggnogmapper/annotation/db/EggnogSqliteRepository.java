package eggnogmapper.annotation.db;

import eggnogmapper.common.SqliteDriverSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EggnogSqliteRepository implements AutoCloseable {
    private final Connection conn;

    public EggnogSqliteRepository(String sqlitePath) throws SQLException {
        ensureSqliteDriver();
        this.conn = SqliteDriverSupport.open(sqlitePath);
        applyPragma("PRAGMA synchronous=OFF;");
        applyPragma("PRAGMA journal_mode=OFF;");
        applyPragma("PRAGMA cache_size=2000;");
    }

    private void applyPragma(String sql) throws SQLException {
        Statement st = conn.createStatement();
        try {
            st.execute(sql);
        } finally {
            st.close();
        }
    }

    private static void ensureSqliteDriver() throws SQLException {
        SqliteDriverSupport.ensureLoaded();
    }

    public String getMemberOgs(String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT ogs FROM prots WHERE name == ?;");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        String out = null;
        if (rs.next()) {
            out = rs.getString(1);
        }
        rs.close();
        ps.close();
        return out;
    }

    public OgDescription getOgDescription(String og, String level) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT nm, description, COG_categories FROM og WHERE og.og == ? AND og.level == ?;"
        );
        ps.setString(1, og);
        ps.setString(2, level);
        ResultSet rs = ps.executeQuery();
        OgDescription best = null;
        while (rs.next()) {
            String nm = rs.getString(1);
            String desc = rs.getString(2);
            String cat = rs.getString(3);
            if (desc != null) {
                String d = desc.trim();
                if (!d.isEmpty() && !"N/A".equalsIgnoreCase(d) && !"NA".equalsIgnoreCase(d)) {
                    best = new OgDescription(nm, d, cat == null ? "-" : cat);
                    break;
                }
            }
        }
        rs.close();
        ps.close();
        return best;
    }

    public ProtAnnotations getProtAnnotations(String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT pname, gos, kegg_ec, kegg_ko, kegg_pathway, kegg_module, kegg_reaction, " +
                        "kegg_rclass, kegg_brite, kegg_tc, kegg_cazy, bigg_reaction, pfam " +
                        "FROM prots WHERE name == ?;"
        );
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        ProtAnnotations out = null;
        if (rs.next()) {
            out = new ProtAnnotations(
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                    rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12),
                    rs.getString(13)
            );
        }
        rs.close();
        ps.close();
        return out;
    }

    public List<ProtAnnotations> getAnnotationsForNames(List<String> names) throws SQLException {
        List<ProtAnnotations> out = new ArrayList<ProtAnnotations>();
        for (String n : names) {
            ProtAnnotations p = getProtAnnotations(n);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    public List<MemberEvent> getMemberEvents(String member, Set<String> targetLevels) throws SQLException {
        List<MemberEvent> out = new ArrayList<MemberEvent>();
        PreparedStatement ps1 = conn.prepareStatement("SELECT orthoindex FROM prots WHERE name == ?;");
        ps1.setString(1, member.trim());
        ResultSet rs1 = ps1.executeQuery();
        String idxs = null;
        if (rs1.next()) {
            idxs = rs1.getString(1);
        }
        rs1.close();
        ps1.close();
        if (idxs == null || idxs.trim().isEmpty() || targetLevels == null || targetLevels.isEmpty()) {
            return out;
        }

        String[] idxArr = idxs.split(",");
        PreparedStatement ps2 = conn.prepareStatement("SELECT level, side1, side2 FROM event WHERE i == ? AND level == ?;");
        for (String iRaw : idxArr) {
            String i = iRaw.trim();
            if (i.isEmpty()) continue;
            int eventId;
            try {
                eventId = Integer.parseInt(i);
            } catch (NumberFormatException ex) {
                continue;
            }
            for (String lvl : targetLevels) {
                ps2.setInt(1, eventId);
                ps2.setString(2, lvl);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    out.add(new MemberEvent(rs2.getString(1), rs2.getString(2), rs2.getString(3)));
                }
                rs2.close();
            }
        }
        ps2.close();
        return out;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    public static class OgDescription {
        public final String name;
        public final String description;
        public final String cogCategory;

        public OgDescription(String name, String description, String cogCategory) {
            this.name = name;
            this.description = description;
            this.cogCategory = cogCategory;
        }
    }

    public static class ProtAnnotations {
        public final String preferredName;
        public final String gos;
        public final String ec;
        public final String keggKo;
        public final String keggPathway;
        public final String keggModule;
        public final String keggReaction;
        public final String keggRclass;
        public final String brite;
        public final String keggTc;
        public final String cazy;
        public final String biggReaction;
        public final String pfams;

        public ProtAnnotations(String preferredName, String gos, String ec, String keggKo,
                               String keggPathway, String keggModule, String keggReaction,
                               String keggRclass, String brite, String keggTc, String cazy,
                               String biggReaction, String pfams) {
            this.preferredName = preferredName;
            this.gos = gos;
            this.ec = ec;
            this.keggKo = keggKo;
            this.keggPathway = keggPathway;
            this.keggModule = keggModule;
            this.keggReaction = keggReaction;
            this.keggRclass = keggRclass;
            this.brite = brite;
            this.keggTc = keggTc;
            this.cazy = cazy;
            this.biggReaction = biggReaction;
            this.pfams = pfams;
        }
    }

    public static class MemberEvent {
        public final String level;
        public final String side1;
        public final String side2;

        public MemberEvent(String level, String side1, String side2) {
            this.level = level;
            this.side1 = side1;
            this.side2 = side2;
        }
    }
}
