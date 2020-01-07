public class RetrievalModelIndri extends RetrievalModel {

    private double mu;
    private double lambda;

    public RetrievalModelIndri(String x, String y){
        this.mu = Double.parseDouble(x);
        this.lambda = Double.parseDouble(y);
    }

    public double getMu() {
        return this.mu;
    }
    public double getLambda() {
        return this.lambda;
    }

    @Override
    public String defaultQrySopName() {
        return new String("#and");
    }
}
