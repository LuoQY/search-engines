/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchMin (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    }

    //  STUDENTS::
    //  Add support for other retrieval models here.

    else if(r instanceof RetrievalModelRankedBoolean) {
      return this.getScoreRankedBoolean(r);
    }

    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    return 0;
  }

  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      int docid = this.docIteratorGetMatch();
      ArrayList<Double> ctfs = new ArrayList<Double>();
      for(Qry arg: this.args){
          if(arg.docIteratorHasMatch(r)) {
              if (arg.docIteratorGetMatch() == docid)
                  ctfs.add(((QrySop) arg).getScore(r));
          }
      }
      return Collections.max(ctfs);
    }
  }

}
