package com.jonathan;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//    Adapted from code written by Mark D. Smucker
public class ResultsFile {
    /// <summary>
    /// the results
    /// </summary>
    public Results results = new Results() ;
    public String runID;

    /// <summary>
    /// This will throw IO exceptions if something IO bad happens
    /// </summary>
    /// <param name="path"></param>
    public ResultsFile(String fullpath) throws Exception {
        String whitespace = "[ \t]";

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

        boolean firstLine = true ;
        String line ;
        while ((line = br.readLine()) != null) {
            String[] fields = line.split(whitespace) ;
            // should be "queryID Q0 doc-id rank score runID"
            if (fields.length != 6 ) {
                throw new Exception( "input should have 6 columns" ) ;
            }

            String queryID = fields[0] ;
            String docID = fields[2] ;
            int rank = Integer.parseInt(fields[3]);
            double score = Double.parseDouble(fields[4]);
            results.addResult(queryID, docID, score, rank);
            if (firstLine) {
                this.runID = fields[5];
                firstLine = false;
            }
            else if (!this.runID.equals(fields[5])) {
                throw new Exception("mismatching runIDs in file" ) ;
            }
        }
        br.close() ;
    }
}

