import java.io.IOException;
import java.util.ArrayList;

public class QrySopWand extends QrySop {

    private String weightstr;

    public void setWeightStr(String weights) {
        this.weightstr = weights;
    }

    public double getSumWeight() {
        double sum = 0.0;
        String[] nums = weightstr.split(" ");
        for(String num: nums)
            sum += Double.parseDouble(num);
        return sum;
    }

    public ArrayList<Double> getDoubleWeights() {
        String[] nums = weightstr.split(" ");
        ArrayList<Double> numbers = new ArrayList<>();
        for(String num: nums)
            numbers.add(Double.parseDouble(num));
        return numbers;
    }


    @Override
    public double getScore(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri (r);
        }
        else
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WAND operator.");
    }

    @Override
    public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
        if(r instanceof RetrievalModelIndri) {
            double produce = 1.0;
            ArrayList<Double> weights = getDoubleWeights();
            for (Qry q : this.args) {
                double weight = weights.get(0);
                produce *= Math.pow(((QrySop) q).getDefaultScore(r, docid), (weight/getSumWeight()));
                weights.remove(0);
            }
            return produce;
        }
        else
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WAND operator.");
    }

    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        if(r instanceof RetrievalModelIndri) {
            return this.docIteratorHasMatchMin(r);
        }
        return this.docIteratorHasMatchAll (r);
    }

    private double getScoreIndri(RetrievalModel r) throws IOException {
        if(!this.docIteratorHasMatchCache())
            return 0.0;
        double produce = 1.0;
        int docid = this.docIteratorGetMatch();
        ArrayList<Double> weights = getDoubleWeights();
        for (Qry q : this.args) {
            double weight = weights.get(0);
            if(q.docIteratorHasMatch(r)&&q.docIteratorGetMatch()==docid)
                produce *= Math.pow(((QrySop) q).getScore(r), (weight/getSumWeight()));
            else
                produce *= Math.pow(((QrySop) q).getDefaultScore(r, docid), (weight/getSumWeight()));
            weights.remove(0);
        }
        return produce;
    }
}
