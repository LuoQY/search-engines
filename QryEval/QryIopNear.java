import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Vector;

public class QryIopNear extends QryIop {

    private int distance = 0;

    public QryIopNear(int distance){
        this.distance = distance;
    }

    public void initialize(RetrievalModel r) throws IOException{
        super.initialize(r);
    }

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
                // System.out.println("Match! " + this.args.get(k).docIteratorGetMatch() + " docid=" + docid);
                if(!((QryIop)this.args.get(k)).docIteratorHasMatch())
                    docMatch=false;
                //else
                //    System.out.println("Match! " + this.args.get(k).docIteratorGetMatch() + " docid=" + docid);
                // Simple Greedy Algorithm
                /**
                if(!((QryIop)this.args.get(k)).docIteratorHasMatch())
                    return;
                else if(this.args.get(k).docIteratorGetMatch()!=docid)
                    docMatch = false;
                System.out.print(this.args.get(k).docIteratorGetMatch()+" ");
                **/
            }

            // Find a pair of match doc ids
            if(docMatch){
                ArrayList<Integer> locations = new ArrayList<Integer>();
                Vector<Integer> iniloc = ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().positions;
                while(!iniloc.isEmpty()){
                    boolean locMatch = true;

                    // Find the ascending locations
                    int firstloc = iniloc.get(0);
                    for(int i=1; i<this.args.size(); i++) {
                        QryIop qry = (QryIop)this.args.get(i);
                        while(qry.locIteratorHasMatch()) {

                            //test
                            //if(docid==520218){
                            //    System.out.println("This is record for doc.520218");
                            //    System.out.println("firstloc="+firstloc+" other loc="+qry.locIteratorGetMatch());
                            //}

                            if(firstloc>=qry.locIteratorGetMatch()) {
                                qry.locIteratorAdvance();
                            }
                            // Find possible pair
                            else {
                                if(qry.locIteratorGetMatch() - firstloc>this.distance)
                                    locMatch = false;
                                else {
                                    firstloc = qry.locIteratorGetMatch();
                                    locMatch = true;
                                }
                                break;
                            }
                        }
                        if(!locMatch || !qry.locIteratorHasMatch()){
                            locMatch = false;
                            break;
                        }
                    }

                    // Find one pair of match positions
                    if(locMatch) {
                        locations.add(firstloc);
                        for(Qry arg: this.args){
                            ((QryIop)arg).locIteratorAdvance();
                            if(!((QryIop) arg).locIteratorHasMatch())
                                iniloc.clear();
                        }
                        if(!iniloc.isEmpty())
                            iniloc.remove(0);
                    }
                    // Didn't match positions, but no position list is empty, the first location is added by 1
                    else {
                        //((QryIop)this.args.get(0)).locIteratorAdvance();
                        iniloc.remove(0);
                    }

                }

                if(!locations.isEmpty()) {
                    this.invertedList.appendPosting(docid, locations);
                    //System.out.println("------------one document done:"+docid);
                    //for(int i: locations)
                    //    System.out.print(i+" ");
                }
                for (Qry arg : this.args)
                    ((QryIop) arg).docIteratorAdvance();


            }

            // Didn't find match, add 1 to the all doc iterators
            else{
                // Simple Greedy Algorithm
                /**
                for(Qry arg: this.args)
                    ((QryIop)arg).docIteratorAdvance();
                **/
                ((QryIop)this.args.get(0)).docIteratorAdvance();
            }
        }

    }
}
