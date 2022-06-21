package com.jonathan;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BooleanAND {
    public static void main(String[] args) {

        // Input argument checking
        if (args.length != 3) {
            System.out.println(String.format("ERROR: Wrong number of arguments passed in! Expected: 3. Received: %d", args.length));
            System.out.println("First parameter: path to the indexed data");
            System.out.println("Second parameter: the queries file");
            System.out.println("Third parameter: output file name");
            System.exit(1);
        }

        String indexedDataPath = args[0];
        String queriesFile = args[1];
        String outputFile = args[2];

        Path outputFilePath = Paths.get(outputFile);
        if (Files.exists(outputFilePath)) {
            System.out.println("ERROR: Output file already exists!");
            System.exit(1);
        }

        // Reading in the lexicon
        try (FileInputStream lexiconFIS = new FileInputStream(indexedDataPath + "/lexicon.txt");
             ObjectInputStream lexiconOIS = new ObjectInputStream(lexiconFIS)) {
            @SuppressWarnings("unchecked")
            HashMap<String, Integer> lexicon = (HashMap<String, Integer>) lexiconOIS.readObject();

            // Reading in the idMapping
            try (FileInputStream mappingFIS = new FileInputStream(indexedDataPath + "/idMapping.txt");
                 ObjectInputStream mappingOIS = new ObjectInputStream(mappingFIS)) {
                @SuppressWarnings("unchecked")
                HashMap<Integer, String> idMap = (HashMap<Integer, String>) mappingOIS.readObject();

                // Reading in the inverted index
                try (FileInputStream invIndexFIS = new FileInputStream(indexedDataPath + "/invIndex.txt");
                     BufferedInputStream invIndexBIS = new BufferedInputStream(invIndexFIS);
                     ObjectInputStream invIndexOIS = new ObjectInputStream(invIndexBIS)) {
                    @SuppressWarnings("unchecked")
                    HashMap<Integer, List<Integer>> invIndex = (HashMap<Integer, List<Integer>>) invIndexOIS.readObject();

                    // Reading in the queries
                    try (BufferedReader br = new BufferedReader(new FileReader(queriesFile))) {
                        String line;

                        File outFile = new File(outputFile);
                        FileOutputStream fos = new FileOutputStream(outFile);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                        // Determining booleanAND results
                        while ((line = br.readLine()) != null) {
                            String queryId = line;
                            String queryTerms = br.readLine();
                            List<String> queryTokens = new ArrayList<>();
                            IndexEngine.tokenize(queryTerms, queryTokens);
                            List<Integer> booleanANDResults = booleanANDResults(queryTokens, lexicon, invIndex);
                            int rank = 1;
                            for (int i = 0; i < booleanANDResults.size(); i++) {
                                String docno = idMap.get(booleanANDResults.get(i));
                                int score = booleanANDResults.size() - rank;
                                bw.write(String.format("%s Q0 %s %d %d jhhchenAND\n", queryId, docno, rank, score));
                                rank++;
                            }
                        }
                        bw.close();

                    } catch (IOException e) {
                        System.out.println("ERROR: Unable to get queries file");
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: Unable to get inverted index");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("ERROR: Unable to get docid mapping");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("ERROR: Unable to get lexicon");
            e.printStackTrace();
        }
    }

    // Determining booleanAND results of a query
    public static List<Integer> booleanANDResults(List<String> queryTerms, HashMap<String, Integer> lexicon,
                                                  HashMap<Integer, List<Integer>> invIndex) {
        HashMap<Integer, Integer> docCount = new HashMap<>();
        for (String term : queryTerms) {
            int termId;
            if (lexicon.containsKey(term)) {
                termId = lexicon.get(term);
            } else {
                continue;
            }
            List<Integer> postings = invIndex.get(termId);
            for (int i = 0; i < postings.size(); i+=2) {
                int docid = postings.get(i);
                if (docCount.containsKey(docid)) {
                    int count = docCount.get(docid);
                    docCount.put(docid, count + 1);
                } else {
                    docCount.put(docid, 1);
                }
            }
        }
        List<Integer> resultSet = new ArrayList<>();
        for (int docid : docCount.keySet()) {
            if (docCount.get(docid) == queryTerms.size()) {
                resultSet.add(docid);
            }
        }
        return resultSet;
    }
}
