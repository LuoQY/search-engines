import java.util.*;

public class DiversityList {
    private class queryIntents {
        private List<String> qIntents = new ArrayList<>();
        private List<Double> scores = new ArrayList<>();

        public queryIntents(String intent, double score) {
            qIntents.add(intent);
            scores.add(score);
        }

        public void add(String intent, double score) {
            qIntents.add(intent);
            scores.add(score);
        }

        public double getScore(String intent) {
            if (getIntent(intent) == -1)
                return 0.0;
            return scores.get(getIntent(intent));
        }

        public int getIntent(String intent) {
            return qIntents.indexOf(intent);
        }

        public List<String> getqIntents() {
            return qIntents;
        }

        public List<Double> getScores() {
            return scores;
        }
    }

    /**
     * Internal doc index
     */
    private HashMap<Integer, queryIntents> diversityList = new HashMap<>();

    public void add(int docId, String intent, double score) {
        if(diversityList.containsKey(docId)) {
            queryIntents intents = diversityList.get(docId);
            intents.add(intent, score);
        }
        else {
            queryIntents qt = new queryIntents(intent, score);
            diversityList.put(docId, qt);
        }
    }

    public double getScore(int docId, String intent) {
        queryIntents intents = diversityList.get(docId);
        return intents.getScore(intent);
    }

    public queryIntents getQueryIntents(int docId) {
        return diversityList.get(docId);
    }

    public int size() {
        return diversityList.size();
    }

    public void printOut (int docId) {
        System.out.println(getQueryIntents(docId).getqIntents());
        System.out.println(getQueryIntents(docId).getScores());
    }

    public List<Double> getAllScores(int docid) {
        return diversityList.get(docid).getScores();
    }

    public List<String> getAllIntents(int docid) {
        return diversityList.get(docid).getqIntents();
    }

    public Set<Integer> getKeys() {
        return diversityList.keySet();
    }

    public void remove(int docid) {
        diversityList.remove(docid);
    }

}
