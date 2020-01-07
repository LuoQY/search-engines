import java.io.IOException;
import java.lang.IllegalArgumentException;


public class QrySopSum extends QrySop {

    @Override
    public double getScore(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25 (r);
        }
        else
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
    }

    @Override
    public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
        double sum = 0.0;
        for(Qry q: this.args)
                sum += ((QrySop) q).getDefaultScore(r, docid);
        return sum;
    }

    @Override
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }

    public double getScoreBM25(RetrievalModel r) throws IOException {
        if(!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            double sum = 0.0;
            int docid = this.docIteratorGetMatch();
            for(Qry q: this.args){
                if(q.docIteratorHasMatch(r) && q.docIteratorGetMatch()==docid) {
                    sum += ((QrySop) q).getScore(r);
                }
            }
            return sum;
        }
    }
}
