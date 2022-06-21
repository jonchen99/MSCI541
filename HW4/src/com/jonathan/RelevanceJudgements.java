package com.jonathan;

import java.util.*;

//    Adapted from code written by Mark D. Smucker
public class RelevanceJudgements {
    /// <summary>
    /// Internal class for use by RelevanceJudgments to hold the judgements
    /// </summary>
    private static class Tuple {
        public Tuple(String queryID, String docID, int relevant) {
            this._queryID = queryID ;
            this._docID = docID ;
            this._relevant = relevant ;
        }
        private final String _queryID ;
        private final String _docID ;
        private final int _relevant ;

        public String getQueryID() {
            return _queryID;
        }
        public String getDocID() {
            return _docID;
        }
        public int getRelevant() {
            return _relevant;
        }

        public static String generateKey(String queryID, String docID ) {
            return queryID + "-" + docID ;
        }

        public String getKey() {
            return _queryID + "-" + _docID;
        }
    }

    private final Hashtable<String, Tuple> tuples;
    private final Hashtable<String, ArrayList<String>> query2reldocnos;

    public RelevanceJudgements() {
        this.tuples = new Hashtable<>() ;
        this.query2reldocnos = new Hashtable<>() ;
    }

    public void addJudgement(String queryID, String docID, int relevant) throws Exception {
        Tuple tuple = new Tuple(queryID, docID, relevant);
        if (this.tuples.containsKey(tuple.getKey()))
            throw new Exception( "Cannot have duplicate queryID and docID data points" ) ;
        this.tuples.put(tuple.getKey(), tuple) ;
        if (tuple.getRelevant() != 0 ) {
            // store the reldocnos
            ArrayList<String> tmpRelDocnos = null;
            if (query2reldocnos.containsKey(queryID)) {
                tmpRelDocnos = query2reldocnos.get(queryID);
            }
            else {
                tmpRelDocnos = new ArrayList<>();
                query2reldocnos.put(queryID, tmpRelDocnos);
            }
            if (!tmpRelDocnos.contains(docID))
                tmpRelDocnos.add(docID);
        }
    }

    /// <summary>
    /// Is the document relevant to the query?
    /// </summary>
    /// <param name="queryID"></param>
    /// <param name="docID"></param>
    /// <returns></returns>
    public boolean isRelevant(String queryID, String docID) throws Exception {
        return getJudgment(queryID, docID, true) != 0;
    }

    public int getJudgment(String queryID, String docID) throws Exception {
        return getJudgment(queryID, docID, false) ;
    }

    public int getJudgment(String queryID, String docID, boolean assumeNonRelevant) throws Exception {
        if (!this.query2reldocnos.containsKey( queryID ) )
            throw new Exception( "no relevance judgments for queryID = " + queryID);

        String key = Tuple.generateKey( queryID, docID ) ;
        if (!tuples.containsKey(key)) {
            if (assumeNonRelevant)
                return 0 ;
            else
                throw new Exception( "no relevance judgement for queryID and docID" ) ;
        }
        else {
            Tuple tuple = tuples.get(key);
            return tuple.getRelevant();
        }
    }

    /// <summary>
    /// Number of relevant documents in collection for query
    /// </summary>
    /// <param name="queryID"></param>
    /// <returns></returns>
    public int numRelevant(String queryID) throws Exception {
        if (this.query2reldocnos.containsKey( queryID ) )
            return (this.query2reldocnos.get(queryID)).size();
        else
            throw new Exception("no relevance judgments for queryID = " + queryID);
    }

    /// <summary>
    /// returns the queryID strings
    /// </summary>
    /// <returns></returns>
    public Set<String> getQueryIDs() {
        return this.query2reldocnos.keySet();
    }

    public ArrayList<String> relDocnos(String queryID) throws Exception {
        if (this.query2reldocnos.containsKey(queryID))
            return this.query2reldocnos.get(queryID);
        else
            throw new Exception( "no relevance judgments for queryID = " + queryID ) ;
    }
}



