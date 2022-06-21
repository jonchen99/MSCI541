package com.jonathan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

//    Adapted from code written by Mark D. Smucker
public class Results {
    public static class Result implements Comparable {
        public Result(String docID, double score, int rank) {
            this._docID = docID ;
            this._score = score ;
            this._rank = rank ;
        }

        private final String _docID ;
        private final double _score ;
        private final int _rank ;

        public String getDocID() {
            return _docID;
        }
        public double getScore() {
            return _score;
        }
        public int getRank() {
            return _rank ;
        }

        // For Comparable, we'll sort from high to low score,
        // if the scores are the same, then we sort from high docno to low docno
        // This is what TREC eval does.  Checked on trec 9 web to work.
        // as of 10/14/2011, I think sorting from high to low docno may be
        // backwards.
        // OKAy, this is what trec_eval does (as far as I can tell):
        //static int
        //comp_sim_docno (ptr1, ptr2)
        //TEXT_TR *ptr1;
        //TEXT_TR *ptr2;
        //{
        //    if (ptr1->sim > ptr2->sim)
        //        return (-1);
        //    if (ptr1->sim < ptr2->sim)
        //        return (1);
        //    return (strcmp (ptr2->docno, ptr1->docno));
        //}
        //
        // so that is a descending sort on score and docno
        //
        public int compareTo(Object obj) {
            Result rhs = (Result)obj ;
            Result lhs = this ;
            Double lhsScore = lhs.getScore();
            Double rhsScore = rhs.getScore();
            int scoreCompare = -1 * lhsScore.compareTo(rhsScore);
            if ( scoreCompare == 0 ) {
                return -1 * lhs.getDocID().compareTo(rhs.getDocID()) ;
            } else {
                return scoreCompare ;
            }
        }
    }

    /// <summary>
    /// holds keys of queryID and docID to make sure no dupes are added
    /// </summary>
    private final Hashtable<String, String> tupleKeys;

    /// <summary>
    /// keyed by queryID to an ArrayList of the queries' results.
    /// </summary>
    private final Hashtable<String, ArrayList<Result>> query2results ;
    private final Hashtable<String, Boolean> query2isSorted ;

    public Results() {
        this.tupleKeys = new Hashtable<>() ;
        this.query2results = new Hashtable<>() ;
        this.query2isSorted = new Hashtable<>() ;
    }

    public void addResult(String queryID, String docID, double score, int rank) throws Exception {
        // be a bit careful about catching a bad mistake
        String key = this.generateTupleKey(queryID, docID);
        if (this.tupleKeys.containsKey(key))
            throw new Exception("Cannot have duplicate queryID and docID data points");
        this.tupleKeys.put(key, "") ;

        // Add to database
        ArrayList<Result> results = null ;
        if (this.query2results.containsKey(queryID)) {
            results = this.query2results.get(queryID);
        } else {
            results = new ArrayList<>() ;
            this.query2results.put(queryID, results);
            this.query2isSorted.put(queryID, false);
        }
        Result result = new Result(docID, score, rank);
        results.add(result);
    }

    public String generateTupleKey(String queryID, String docID )
    {
        return queryID + "-" + docID ;
    }

    /// <summary>
    /// Returns the results for queryID sorted by score
    /// </summary>
    /// <param name="queryID"></param>
    /// <returns></returns>
    public ArrayList<Result> queryResults(String queryID) throws Exception {
        if (!this.query2results.containsKey(queryID))
            throw new Exception("no such queryID in results");
        ArrayList<Result> results = this.query2results.get(queryID);
        if (!(boolean) this.query2isSorted.get(queryID)) {
            Collections.sort(results);
            this.query2isSorted.replace(queryID, true);
        }
        return results;
    }

    /// <summary>
    /// returns the collection of QueryIDs
    /// </summary>
    /// <returns></returns>
    public Set<String> getQueryIDs(){
        return this.query2results.keySet();
    }

    public boolean queryIDExists(String queryID) {
        return this.query2results.containsKey(queryID);
    }
}
