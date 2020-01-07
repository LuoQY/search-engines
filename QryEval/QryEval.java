/*
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.4.
 *
 *  Compatible with Lucene 8.1.1.
 */
import java.io.*;
import java.util.*;

import org.apache.lucene.index.*;

/**
 * @Author Qingyang Luo
 * Last Modified: Nov 18, 2019
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
          "Usage:  java QryEval paramFile\n\n";

  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  The timer is used here to time how long the entire program takes

    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));
    RetrievalModel model = null;
    if (parameters.get("retrievalAlgorithm") != null) {
       model = initializeRetrievalModel(parameters);
    }

    //  Get output parameters

    String outputPath = parameters.get("trecEvalOutputPath");
    int outputLength;
    if (parameters.get("trecEvalOutputLength") != null) {
      outputLength = Integer.parseInt(parameters.get("trecEvalOutputLength"));
    } else {
      outputLength = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
    }

    //  Perform experiments.
    processQueryFile(parameters.get("queryFilePath"), model, outputPath, outputLength, parameters);

    //  Clean up.

    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @param parameters All of the parameters contained in the parameter file
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
          throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

    if (modelString.equals("unrankedboolean")) {

      model = new RetrievalModelUnrankedBoolean();

      //  If this retrieval model had parameters, they would be
      //  initialized here.

    }

    else if(modelString.equals("rankedboolean")) {
      model = new RetrievalModelRankedBoolean();
    }

    else if(modelString.equals("bm25")) {
      model = new RetrievalModelBM25(parameters.get("BM25:k_1"), parameters.get("BM25:b"), parameters.get("BM25:k_3"));
    }

    else if(modelString.equals("indri")) {
      model = new RetrievalModelIndri(parameters.get("Indri:mu"), parameters.get("Indri:lambda"));
    }

    else {

      throw new IllegalArgumentException
              ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }

    return model;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   *
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    //System.out.println("Memory used:  "
    //        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Process one query.
   * @param qryString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qryString, RetrievalModel model)
          throws IOException {

    String defaultOp = model.defaultQrySopName ();
    qryString = defaultOp + "(" + qryString + ")";
    Qry q = QryParser.getQuery (qryString);

    // Show the query that is evaluated

    System.out.println("    --> " + q);

    if (q != null) {

      ScoreList results = new ScoreList ();

      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          results.add (docid, score);
          q.docIteratorAdvancePast (docid);
        }
      }

      return results;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param queryFilePath Path to the query file
   *  @param model A retrieval model that will guide matching and scoring
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath, RetrievalModel model, String outputPath, int outputLength, Map<String, String> parameters)
          throws IOException {

    BufferedReader input = null;

    // diversification
    if(parameters.get("diversity") != null && parameters.get("diversity").equals("true")) {
      int maxRankingsLength = Integer.parseInt(parameters.get("diversity:maxInputRankingsLength"));
      String initialFile = parameters.get("diversity:initialRankingFile");
      // given initial rank file
      if ( initialFile != null) {
        try {
          String qLine = null;
          String pre_qid = null;
          ScoreList docList = new ScoreList();
          input = new BufferedReader(new FileReader(initialFile));
          // <q.intents.id, scoreList> for each query
          Map<String, ScoreList> relevanceList = new HashMap<>();
          while ((qLine = input.readLine()) != null) {
            printMemoryUsage(false);
            String[] files = qLine.split(" ");
            String docId = files[2];
            double score = Double.parseDouble(files[4]);
            if (pre_qid == null) {
              pre_qid = files[0];  // assign the query id
              docList.add(Idx.getInternalDocid(docId), score);
            }
            else if (pre_qid.equals(files[0])) { // same query document
              docList.add(Idx.getInternalDocid(docId), score);
            }
            else if(files[0].contains(pre_qid) || files[0].contains(".")) { // query intent id

              relevanceList.put(pre_qid, docList);
              docList = new ScoreList();
              docList.add(Idx.getInternalDocid(docId), score);
              pre_qid = files[0];
            } else {  // enter next query
              docList.sort();
              docList.truncate(maxRankingsLength);
              relevanceList.put(pre_qid, docList);
              DiversityList divList = normalize(parameters, relevanceList, pre_qid);
              processDiversityList(parameters, divList, pre_qid, outputPath);

              relevanceList = new HashMap<>();
              docList = new ScoreList();
              docList.add(Idx.getInternalDocid(docId), score);
              pre_qid = files[0];

            }
          }

          docList.sort();
          docList.truncate(maxRankingsLength);
          relevanceList.put(pre_qid, docList);
          DiversityList divList = normalize(parameters, relevanceList, pre_qid);
          processDiversityList(parameters, divList, pre_qid, outputPath);

        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          input.close();
        }
      }

      // no diversification initial file, use query
      else {
        try {
          // read intent file
          Map<String, String> intents = new HashMap<>();
          BufferedReader inputIntents = new BufferedReader((new FileReader(parameters.get("diversity:intentsFile"))));
          String intentLine = null;
          while ((intentLine = inputIntents.readLine()) != null) {
            String[] pair = intentLine.split(":");
            if (pair.length != 2) {
              throw new IllegalArgumentException
                      ("Syntax error:  Each line must contain one ':'.");
            }
            String qid = pair[0];
            String query = pair[1];
            intents.put(qid, query);
          }

          // <q.intents.id, scoreList> for each query
          Map<String, ScoreList> relevanceList = new HashMap<>();
          String qLine = null;
          input = new BufferedReader(new FileReader(queryFilePath));
          //  Each pass of the loop processes one query.
          while ((qLine = input.readLine()) != null) {
            printMemoryUsage(false);
            String[] pair = qLine.split(":");
            if (pair.length != 2) {
              throw new IllegalArgumentException
                      ("Syntax error:  Each line must contain one ':'.");
            }
            String qid = pair[0];
            String query = pair[1];
            ScoreList results = processQuery(query, model);
            results.sort();

            results.truncate(maxRankingsLength);
            relevanceList.put(qid, results);

            // get relevant intent file
            Set<String> intentIds = intents.keySet();
            int intentNum = 0;
            for(String qIntentId: intentIds) {
              if(qIntentId.contains(qid)) {
                String intentQuery = intents.get(qIntentId);
                ScoreList intentResult = processQuery(intentQuery, model);
                intentResult.sort();
                intentResult.truncate(maxRankingsLength);
                relevanceList.put(qIntentId, intentResult);
                intentNum++;
              }
            }

            DiversityList divList = normalize(parameters, relevanceList, qid+"."+intentNum);
            processDiversityList(parameters, divList, qid+"."+intentNum, outputPath);

          }
        } catch (Exception ex) {
          ex.printStackTrace();
        } finally {
          input.close();
        }


      }
    }
    // no diversity
    else {
      try {
        String qLine = null;

        input = new BufferedReader(new FileReader(queryFilePath));

        //  Each pass of the loop processes one query.

        while ((qLine = input.readLine()) != null) {

          printMemoryUsage(false);
          System.out.println("Query " + qLine);
          String[] pair = qLine.split(":");

          if (pair.length != 2) {
            throw new IllegalArgumentException
                    ("Syntax error:  Each line must contain one ':'.");
          }

          String qid = pair[0];
          String query = pair[1];
          ScoreList results = processQuery(query, model);

          if (results != null) {
            printResults(qid, results, outputPath, outputLength);
            System.out.println();
          }
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        input.close();
      }
    }
  }


  static DiversityList normalize(Map<String, String> parameters, Map<String, ScoreList> relevanceList, String qid) throws Exception {
    qid = qid.substring(0, qid.indexOf("."));
    ScoreList queryDocList = relevanceList.get(qid);
    int maxLength = Integer.parseInt(parameters.get("diversity:maxInputRankingsLength"));

    HashSet<Integer> queryDocHash = new HashSet<>();
    for(int i = 0; i < Math.min(maxLength, queryDocList.size()); i++) {
      queryDocHash.add(queryDocList.getDocid(i));
    }

    double maxSum = 0;
    DiversityList diversityList = new DiversityList();
    for (Map.Entry mapElement : relevanceList.entrySet()) {
      double sum = 0;
      ScoreList scorelist = (ScoreList) mapElement.getValue();
      String qIntentId = (String) mapElement.getKey();
      for(int i = 0; i < scorelist.size(); i++) {
        if(queryDocHash.contains(scorelist.getDocid(i))) {
          double s = scorelist.getDocidScore(i);
          sum += s;
          diversityList.add(scorelist.getDocid(i), qIntentId, s);
        }
      }
      if (sum > maxSum) {
        maxSum = sum;
      }
    }

    // if maxSum is smaller than 1, don't scale
    if(maxSum < 1)
      maxSum = 1;

    // scaling
    for (int docid: queryDocHash) {
      List<Double> scores = diversityList.getAllScores(docid);
      for(int j = 0; j < scores.size(); j++) {
        double s = scores.get(j);
        scores.set(j, s/maxSum);
      }
    }

    return diversityList;
  }


  private static void processDiversityList(Map<String, String> parameters, DiversityList divList, String qIntentId, String outputPath) throws Exception {
    int intentNum = Integer.parseInt(qIntentId.split("\\.")[1]);
    double weight = 1.0 / intentNum;
    String qid = qIntentId.split("\\.")[0];
    String algorithm = parameters.get("diversity:algorithm");
    int maxOutputLength = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
    // store the final ranking results
    ScoreList results = new ScoreList();
    double lambda = Double.parseDouble(parameters.get("diversity:lambda"));

    // xQuAD
    if(algorithm.equals("xQuAD")) {
      // store the coverage after getting one doc
      Map<Integer, Double> coverage = new HashMap<>();
      for (int i = 1; i <= intentNum; i++)
        coverage.put(i, 1.0);
      int r = 0;
      while(r < maxOutputLength) {
        ScoreList docList = new ScoreList();
        Set<Integer> docs = divList.getKeys();
        for (int docid : docs) {
          double p_d_q = divList.getScore(docid, qid);
          double divSum = 0.0;
          for (int j = 1; j <= intentNum; j++) {
            double score = divList.getScore(docid, qid + "." + j);
            divSum += weight * score * coverage.get(j);
          }
          double divScore = (1 - lambda) * p_d_q + lambda * divSum;
          docList.add(docid, divScore);
        }
        // sort the list and choose the highest doc
        docList.sort();
        int bestDocId = docList.getDocid(0);
        double score = docList.getDocidScore(0);
        // update coverage
        for (int j = 1; j <= intentNum; j++) {
          double cover = divList.getScore(bestDocId, qid + "." + j);
          coverage.put(j, coverage.get(j) * (1 - cover));
        }
        divList.remove(bestDocId);
        results.add(bestDocId, score);
        r++;
      }
      printResults(qid, results, outputPath, maxOutputLength);


    }
    // PM2
    else if(algorithm.equals("PM2")) {
      int maxInputRankingLength = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
      double vote = weight * maxInputRankingLength;
      Map<Integer, Double> slots = new HashMap<>();
      //initialize
      for(int i = 1; i <= intentNum; i++) {
        slots.put(i, 0.0);
      }

      int r = 0;
      while(r < maxOutputLength) {
        ScoreList docList = new ScoreList();
        Set<Integer> docs = divList.getKeys();
        for (int docid : docs) {
          // find the maximum qt
          int max_Qt_index = 1;
          double max_Qt = 0;
          ArrayList<Double> qts = new ArrayList<>();
          for (int i = 1; i <= intentNum; i++) {
            double qt = vote / (2 * slots.get(i) + 1);
            qts.add(qt);
            if (qt > max_Qt) {
              max_Qt_index = i;
              max_Qt = qt;
            }
          }


          // calculate the other intent coverage
          double coverage = 0;
          for (int j = 1; j <= intentNum; j++) {
            if (j == max_Qt_index)
              continue;
            coverage += qts.get(j - 1) * divList.getScore(docid, qid + "." + j);
          }


          double pm2Score = lambda * max_Qt * divList.getScore(docid, qid + "." + max_Qt_index) + (1 - lambda) * coverage;
          docList.add(docid, pm2Score);

        }

        // sort the list and choose the highest doc
        docList.sort();
        int bestDocId = docList.getDocid(0);
        double score = docList.getDocidScore(0);

        // update slots
        double sum_slots = 0;
        for (int k = 1; k <= intentNum; k++)
          sum_slots += divList.getScore(bestDocId, qid + "." + k);
        for (int k = 1; k <= intentNum; k++) {
          double slot = divList.getScore(bestDocId, qid + "." + k) / sum_slots;
          slots.put(k, slots.get(k) + slot);
        }

        divList.remove(bestDocId);
        results.add(bestDocId, score);
        r++;
      }

      printResults(qid, results, outputPath, maxOutputLength);

    } else {
      throw new IllegalArgumentException
              ("The system doesn't support such diversification algorithm");
    }
  }

  /**
   * Print the query results.
   *
   *
   * @param queryName
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryName, ScoreList result, String outputPath, int outputLength) throws IOException {

    if (result.size() < 1) {
      BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true));
      String s = queryName + " Q0 " + "dummyRecord" + " " + "1" + " " + "0" + " run-1\n";
      writer.write(s);
      writer.close();
    }
    else {
      result.sort();
      BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true));
      for (int i = 0; i < outputLength && i<result.size(); i++) {
        String s = queryName + " Q0 " + Idx.getExternalDocid(result.getDocid(i)) + " " + (i+1) + " " + result.getDocidScore(i) + " run-1\n";
        writer.write(s);
      }
      writer.close();
    }
  }

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @param parameterFileName The name of the parameter file
   *  @return The parameters, in &lt;key, value&gt; format.
   *  @throws IllegalArgumentException The parameter file can't be read or doesn't contain required parameters
   *  @throws IOException The parameter file can't be read
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
          throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();
    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
              ("Can't read " + parameterFileName);
    }

    //  Store (all) key/value parameters in a hashmap.

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    //  Confirm that some of the essential parameters are present.
    //  This list is not complete.  It is just intended to catch silly
    //  errors.

    if (! (parameters.containsKey ("indexPath") &&
            parameters.containsKey ("queryFilePath") &&
            parameters.containsKey ("trecEvalOutputPath"))) {
      throw new IllegalArgumentException
              ("Required parameters were missing from the parameter file.");
    }

    if(parameters.get("retrievalAlgorithm") != null &&
       parameters.get("retrievalAlgorithm").equals("bm25") &&
      !(parameters.containsKey("BM25:k_1") &&
        parameters.containsKey("BM25:b") &&
        parameters.containsKey("BM25:k_3"))){
      throw new IllegalArgumentException
              ("Required parameters for BM25 were missing from the parameter file.");
    }

    if(parameters.get("retrievalAlgorithm") != null &&
            parameters.get("retrievalAlgorithm").equals("indri") &&
            !(parameters.containsKey("Indri:mu") &&
                    parameters.containsKey("Indri:lambda"))){
      throw new IllegalArgumentException
              ("Required parameters for Indri were missing from the parameter file.");
    }

    return parameters;
  }

}
