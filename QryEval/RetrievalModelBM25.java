public class RetrievalModelBM25 extends RetrievalModel {
    private double k_1;
    private double b;
    private double k_3;

    public double getK_1() {
        return k_1;
    }
    public double getB() {
        return b;
    }
    public double getK_3() {
        return k_3;
    }

    public RetrievalModelBM25(String x, String y, String z){
        this.k_1 = Double.parseDouble(x);
        this.b = Double.parseDouble(y);
        this.k_3 = Double.parseDouble(z);
    }

    @Override
    public String defaultQrySopName() {
        return new String("#sum");
    }

}
