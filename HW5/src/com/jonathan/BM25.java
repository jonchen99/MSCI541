package com.jonathan;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BM25 {
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final double K2 = 7;

    // Source code inspired by: https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values
    public class MapUtil {
        public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
            List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
            list.sort(Map.Entry.comparingByValue());
            Collections.reverse(list);

            Map<K, V> result = new LinkedHashMap<>();
            for (Map.Entry<K, V> entry : list) {
                result.put(entry.getKey(), entry.getValue());
            }

            return result;
        }
    }

    public static void main(String[] args) throws Exception {

        // Input argument checking
        if (args.length != 4) {
            System.out.printf("ERROR: Wrong number of arguments passed in! Expected: 4. Received: %d%n", args.length);
            System.out.println("First parameter: path to the indexed data");
            System.out.println("Second parameter: path to the queries file.");
            System.out.println("Third Parameter: use of porter stemmer. (True/False)");
            System.out.println("Fourth parameter: output directory path to save the results");
            System.exit(1);
        }

        String indexedDataPath = args[0];
        String queriesFile = args[1];
        String porterStemmerString = args[2];
        String outputPath = args[3];
        boolean stem = porterStemmerString.equals("True");

        Path outputDirectory = Paths.get(outputPath);
        if (!Files.exists(outputDirectory)) {
            Files.createDirectory(outputDirectory);
        }

        String outputFileName = outputPath + "/hw4-bm25-baseline-jhhchen.txt";
        if (stem) {
            outputFileName = outputPath + "/hw4-bm25-stem-jhhchen.txt";
        }

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

                    // Read in the mapping of docid to document length
                    try (FileInputStream docLengthFIS = new FileInputStream(indexedDataPath + "/documentLength.txt");
                         ObjectInputStream docLengthOIS = new ObjectInputStream(docLengthFIS)) {
                        @SuppressWarnings("unchecked")
                        HashMap<String, Integer> documentLengthMap = (HashMap<String, Integer>) docLengthOIS.readObject();

                        // Read in the queries
                        try (BufferedReader br = new BufferedReader(new FileReader(queriesFile))) {
                            String line;

                            File outFile = new File(outputFileName);
                            FileOutputStream fos = new FileOutputStream(outFile);
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                            int totalDocLength = 0;
                            for (int length : documentLengthMap.values()) {
                                totalDocLength += length;
                            }
                            double avgDocLength = (double) totalDocLength / documentLengthMap.size();

                            // Determining BM25 rankings results
                            while ((line = br.readLine()) != null) {
                                String queryId = line;
                                String queryTerms = br.readLine();
                                List<String> queryTokens = new ArrayList<>();
                                IndexEngine.tokenize(queryTerms, queryTokens, stem);
                                Map<String, Double> bm25Scores = calculateBM25(avgDocLength, queryTokens, lexicon, invIndex, documentLengthMap, idMap);

                                // Writing the results to a file
                                for (int i = 0; i < bm25Scores.size(); i++) {
                                    if (i >= 1000) {
                                        break;
                                    }
                                    String docno = (String) bm25Scores.keySet().toArray()[i];
                                    double score = bm25Scores.get(docno);
                                    if (stem) {
                                        bw.write(String.format("%s Q0 %s %d %f jhhchenBM25-stem\n", queryId, docno, i+1, score));
                                    } else {
                                        bw.write(String.format("%s Q0 %s %d %f jhhchenBM25-baseline\n", queryId, docno, i+1, score));
                                    }
                                }
                            }
                            System.out.println("Finished ranking documents. Results can be found at " + outputFileName);
                            bw.close();

                        } catch (IOException e) {
                            System.out.println("ERROR: Unable to get queries file");
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR: Unable to get document length");
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

    public static Map<String, Double> calculateBM25(double avgDocLength, List<String> queryTokens, HashMap<String, Integer> lexicon,
                                                    HashMap<Integer, List<Integer>> invIndex, HashMap<String, Integer> documentLengthMap,
                                                    HashMap<Integer, String> idMap) {

        // N is the total number of docs in collection
        int N = documentLengthMap.size();
        HashMap<String, Double> docScore = new HashMap<>();

        for (String token : queryTokens) {

            // qf is the frequency of the word in the query
            int qf = Collections.frequency(queryTokens, token);
            double queryTf = (K2+1)*qf / (K2 + qf);
            int tokenId = 0;

            // Skip if the token is not in the lexicon
            if (lexicon.containsKey(token)) {
                tokenId = lexicon.get(token);
            } else {
                continue;
            }

            // Postings is in a list with format [docid, count, docid, count, etc.]
            List<Integer> postings = invIndex.get(tokenId);

            // n_i is the number of docs with the token in them
            int n_i = postings.size() / 2;

            double idf = Math.log((N - n_i + 0.5) / (n_i + 0.5 + 1));

            for (int i = 0; i < postings.size(); i+=2) {
                String docno = idMap.get(postings.get(i));
                int docLength = documentLengthMap.get(docno);
                double K = K1*((1-B) + B*docLength/avgDocLength);

                // f_i is the frequency of the term in the doc
                int f_i = postings.get(i+1);
                double docTf = ((K1 + 1) * f_i) / (K + f_i);
                double score = docTf * queryTf * idf;
                if (docScore.containsKey(docno)) {
                    double tempScore = docScore.get(docno) + score;
                    docScore.replace(docno, tempScore);
                } else {
                    docScore.put(docno, score);
                }
            }

        }

        return MapUtil.sortByValue(docScore);
    }
}
