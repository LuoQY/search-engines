import java.io.IOException;
import java.lang.NumberFormatException;
import java.util.ArrayList;
import java.util.List;

public class QrySopWsum extends QrySop {

    private String weightstr;

    public void setWeightStr(String weights) {
        this.weightstr = weights;
    }

    public ArrayList<Double> getDoubleWeights() {
        String[] nums = weightstr.split(" ");
        ArrayList<Double> numbers = new ArrayList<>();
        for(String num: nums)
            numbers.add(Double.parseDouble(num));
        return numbers;
    }

    public double getSumWeight() {
        double sum = 0.0;
        String[] nums = weightstr.split(" ");
        for(String num: nums)
            sum += Double.parseDouble(num);
        return sum;
    }



    @Override
    public double getScore(RetrievalModel r) throws IOException {
        if(r instanceof RetrievalModelIndri)
            return this.getScoreIndri(r);
        else
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WSUM operator.");

    }

    @Override
    public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
        if(r instanceof RetrievalModelIndri) {
            double sum = 0.0;
            ArrayList<Double> weights = getDoubleWeights();
            for (Qry q : this.args) {
                double weight = weights.get(0);
                sum += (weight/getSumWeight()) * ((QrySop) q).getDefaultScore(r, docid);
                weights.remove(0);
            }
            return sum;
        }
        else
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WSUM operator.");
    }

    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }

    public double getScoreIndri(RetrievalModel r) throws IOException {
        if(!this.docIteratorHasMatch(r))
            return 0.0;
        double sum = 0.0;
        int docid = this.docIteratorGetMatch();
        ArrayList<Double> weights = getDoubleWeights();
        for(Qry q: this.args){
            double weight = weights.get(0);


            if(q.docIteratorHasMatch(r) && q.docIteratorGetMatch()==docid)
                sum += (weight/getSumWeight())*((QrySop) q).getScore(r);
            else
                sum += (weight/getSumWeight())*((QrySop) q).getDefaultScore(r, docid);
            weights.remove(0);
        }
        return sum;
    }
}
