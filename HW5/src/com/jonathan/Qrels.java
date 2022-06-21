package com.jonathan;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//    Adapted from code written by Mark D. Smucker
public class Qrels {

    /// <summary>
    /// the results of reading in the file
    /// </summary>
    public RelevanceJudgements judgements = new RelevanceJudgements();

    /// <summary>
    /// Yo, this will throw IO exceptions if something IO bad happens
    /// </summary>
    /// <param name="fullpath"></param>
    public Qrels(String fullpath) throws Exception {

        FileInputStream fis = null;
        InputStreamReader reader = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(fullpath);
            reader = new InputStreamReader(fis);
            br = new BufferedReader(reader);
        } catch (IOException e) {
            System.out.println("ERROR: Unable to read file");
            e.printStackTrace();
        }

        String line ;
        while ( (line = br.readLine()) != null ) {
            String whitespace = "[ \t]";
            String[] fields = line.split(whitespace) ;
            // should be "query-num unknown doc-id rel-judgment"
            if ( fields.length != 4 ) {
                throw new Exception("input should have 4 columns");
            }
            String queryID = fields[0] ;
            String docID = fields[2] ;
            int relevant = Integer.parseInt(fields[3]) ;
            judgements.addJudgement(queryID, docID, relevant);
        }
        br.close() ;
    }
}
