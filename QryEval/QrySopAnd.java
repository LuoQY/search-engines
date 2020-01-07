import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class QrySopAnd extends QrySop {

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean (r);
        }

        //  STUDENTS::
        //  Add support for other retrieval models here.

        else if(r instanceof  RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean(r);
        }
        else if(r instanceof RetrievalModelBM25) {
            return this.getScoreBM25(r);
        }
        else if(r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        }
        else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
        }
    }

    @Override
    public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
        if(r instanceof RetrievalModelIndri) {
            double defaultScore = 1.0;
            for (Qry q : this.args) {
                defaultScore *= ((QrySop) q).getDefaultScore(r, docid);
            }
            return Math.pow(defaultScore, 1.0/this.args.size());
        }
        return 0;
    }

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch(RetrievalModel r) {
        if(r instanceof RetrievalModelIndri) {
            return this.docIteratorHasMatchMin(r);
        }
        return this.docIteratorHasMatchAll (r);
    }

    /**
     *  getScore for the UnrankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            return 1.0;
        }
    }

    private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
        if(!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            ArrayList<Double> ctfs = new ArrayList<Double>();
            for(Qry arg: this.args){
                ctfs.add(((QrySop) arg).getScore(r));
            }
            return Collections.min(ctfs);
        }
    }

    private double getScoreBM25(RetrievalModel r) throws IOException {
        if(!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            ArrayList<Double> ctfs = new ArrayList<Double>();
            for(Qry arg: this.args){
                ctfs.add(((QrySop) arg).getScore(r));
            }
            return Collections.min(ctfs);
        }
    }

    private double getScoreIndri(RetrievalModel r) throws IOException {
        if(!this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {
            double produce = 1.0;
            int docid = this.docIteratorGetMatch();

            for(Qry q: this.args) {
                if(q.docIteratorHasMatch(r) && q.docIteratorGetMatch()==docid)
                    produce *= ((QrySop) q).getScore(r);
                else {
                    produce *= ((QrySop) q).getDefaultScore(r, docid);
                }

            }
            return Math.pow(produce, 1.0/this.args.size());
        }
    }
}
