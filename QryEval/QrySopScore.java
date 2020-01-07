/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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

    else if(r instanceof RetrievalModelBM25) {
        return this.getScoreBM25(r);
    }

    else if(r instanceof RetrievalModelIndri) {
        return this.getScoreIndri(r);
    }

    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }

    @Override
    public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
      if(r instanceof RetrievalModelIndri){
          double MLE = this.getArg(0).getCtf()/(double)Idx.getSumOfFieldLengths(this.getArg(0).field);
          double mu = ((RetrievalModelIndri) r).getMu();
          double lambda = ((RetrievalModelIndri) r).getLambda();
          double docLength = Idx.getFieldLength(this.getArg(0).field, docid);
          return (1-lambda)*(mu*MLE)/(docLength+mu)+lambda*MLE;
      }
      return 0;
    }

    /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }


  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if(! this.docIteratorHasMatchCache())
      return 0.0;
    else{
      return this.getArg(0).docIteratorGetMatchPosting().tf;
    }
  }

  public double getScoreBM25 (RetrievalModel r) throws IOException {
      if(! this.docIteratorHasMatchCache())
          return 0.0;
      else{
          double N = Idx.getNumDocs();
          double RSJ = Math.max(0.0, Math.log((N - this.getArg(0).getDf() + 0.5) / (this.getArg(0).getDf() + 0.5)));
          double tf = this.getArg(0).docIteratorGetMatchPosting().tf;
          RetrievalModelBM25 rbm25 = (RetrievalModelBM25) r;
          double b = rbm25.getB();
          double docLength = Idx.getFieldLength(this.getArg(0).field, this.getArg(0).docIteratorGetMatch());
          double avgDocLength = (double) Idx.getSumOfFieldLengths(this.getArg(0).field) / (double) Idx.getDocCount(this.getArg(0).field);
          double tfw = tf / (tf + (rbm25.getK_1() * ((1.0 - b) + b * (docLength / avgDocLength))));
          double userweight = (rbm25.getK_3() + 1.0) / (rbm25.getK_3() + 1.0);
          return RSJ * tfw * userweight;
      }
  }

  public double getScoreIndri (RetrievalModel r) throws IOException {
      double MLE = this.getArg(0).getCtf()/(double)Idx.getSumOfFieldLengths(this.getArg(0).field);
      double mu = ((RetrievalModelIndri) r).getMu();
      double lambda = ((RetrievalModelIndri) r).getLambda();
      double tf = this.getArg(0).docIteratorGetMatchPosting().tf;
      double docLength = Idx.getFieldLength(this.getArg(0).field, this.getArg(0).docIteratorGetMatch());
      return (1-lambda)*(tf+mu*MLE)/(docLength+mu)+lambda*MLE;
  }



  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
