public class RetrievalModelLetor extends RetrievalModel  {
    private double k_1;
    private double b;
    private double k_3;

    private double mu;
    private double lambda;

    public RetrievalModelLetor(String k_1, String b, String k_3, String mu, String lambda) {
        this.k_1 = Double.parseDouble(k_1);
        this.b = Double.parseDouble(b);
        this.k_3 = Double.parseDouble(k_3);
        this.mu = Double.parseDouble(mu);
        this.lambda = Double.parseDouble(lambda);
    }

    @Override
    public String defaultQrySopName() {
        return null;
    }
}
