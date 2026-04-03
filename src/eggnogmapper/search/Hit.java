package eggnogmapper.search;

public class Hit {
    public final String query;
    public final String target;
    public final double evalue;
    public final double score;
    public final int qstart;
    public final int qend;
    public final int sstart;
    public final int send;
    public final double pident;
    public final double qcov;
    public final double scov;

    public Hit(String query, String target, double evalue, double score,
               int qstart, int qend, int sstart, int send,
               double pident, double qcov, double scov) {
        this.query = query;
        this.target = target;
        this.evalue = evalue;
        this.score = score;
        this.qstart = qstart;
        this.qend = qend;
        this.sstart = sstart;
        this.send = send;
        this.pident = pident;
        this.qcov = qcov;
        this.scov = scov;
    }

    public boolean isShort() {
        return qstart < 0 || qend < 0 || sstart < 0 || send < 0;
    }
}
