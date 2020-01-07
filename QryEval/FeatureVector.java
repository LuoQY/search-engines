import java.util.Vector;

public class FeatureVector {
    private String docExternalId;
    private int relevance;
    private String qid;
    private Vector<Double> features;

    public FeatureVector(String docExternalId, String qid) {
        this.docExternalId = docExternalId;
        this.qid = qid;
    }

    public FeatureVector(String docid, int relevance, String qid, Vector<Double> features) {
        this.docExternalId = docid;
        this.relevance = relevance;
        this.qid = qid;
        this.features = features;
    }

    public String getQid() {
        return qid;
    }

    public String getDocExternalId() {
        return docExternalId;
    }

    public int getRelevance() {
        return relevance;
    }

    public Vector<Double> getFeatures() {
        return features;
    }
}
