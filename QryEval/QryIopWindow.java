import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

public class QryIopWindow extends QryIop {

    private int distance;

    public QryIopWindow(int x){
        this.distance = x;
    }

    public int getDistance() {
        return distance;
    }

    public void initialize(RetrievalModel r) throws IOException{
        super.initialize(r);
        //(r);
    }

    @Override
    protected void evaluate() throws IOException {
        if(this.args.size()<=1) return;
        this.invertedList = new InvList(this.getField());
        while(((QryIop)this.args.get(0)).docIteratorHasMatch())
        {
            int docid = this.args.get(0).docIteratorGetMatch();
            boolean docMatch = true;
            for(int k=1; k<this.args.size() && docMatch; k++) {
                while(((QryIop)this.args.get(k)).docIteratorHasMatch()) {
                    if(this.args.get(k).docIteratorGetMatch() == docid){
                        docMatch = true;
                        break;
                    }
                    else if(this.args.get(k).docIteratorGetMatch() < docid)
                        ((QryIop) this.args.get(k)).docIteratorAdvance();
                    else if (this.args.get(k).docIteratorGetMatch() > docid){
                        docMatch = false;
                        break;
                    }
                }
                if(!((QryIop)this.args.get(k)).docIteratorHasMatch())
                    docMatch=false;
            }

            // Find a pair of match doc ids
            if(docMatch){
                ArrayList<Integer> locations = new ArrayList<Integer>();
                boolean locMatch = true;
                while(locMatch){
                    // Find the match locations
                    ArrayList<Integer> qryloc = new ArrayList<>();
                    for(Qry q: this.args){
                        QryIop qry = (QryIop) q;
                        if(!qry.locIteratorHasMatch()){
                            locMatch = false;
                            break;
                        }
                        qryloc.add(qry.locIteratorGetMatch());
                    }

                    if(locMatch){
                        int minloc = Collections.min(qryloc);
                        int maxloc = Collections.max(qryloc);
                        if(maxloc - minloc < this.distance){
                            locations.add(maxloc);
                            for(Qry q: this.args) {
                                ((QryIop) q).locIteratorAdvance();
                                if(!((QryIop) q).locIteratorHasMatch()) {
                                    locMatch = false;
                                    break;
                                }
                            }
                        }
                        else{
                            int index = qryloc.indexOf(minloc);
                            ((QryIop)this.args.get(index)).locIteratorAdvance();
                        }
                    }

                }

                if(!locations.isEmpty())
                    this.invertedList.appendPosting(docid, locations);
                for (Qry arg : this.args)
                    ((QryIop) arg).docIteratorAdvance();
            }

            // Didn't find match, add 1 to the all doc iterators
            else
                ((QryIop)this.args.get(0)).docIteratorAdvance();
        }
    }
}
