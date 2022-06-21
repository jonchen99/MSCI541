package com.jonathan;

import java.io.*;
import java.util.*;

public class Search {

    // Create class for each entry in the priority queue, containing a sentence and a score
    public static class PriorityQueueEntry implements Comparable<PriorityQueueEntry> {

        public PriorityQueueEntry(int score, String sentence) {
            this._score = score;
            this._sentence = sentence;
        }

        private final int _score;
        private final String _sentence;


        public int getScore() {
            return _score;
        }
        public String getSentence() {
            return _sentence;
        }

        // Sort the priority queue in descending order
        @Override
        public int compareTo(PriorityQueueEntry obj) {
            return this.getScore() < obj.getScore() ? 1 : -1;
        }
    }

    public static void main(String[] args) throws Exception {

        // Input argument checking
        if (args.length != 1) {
            System.out.printf("ERROR: Wrong number of arguments passed in! Expected: 2. Received: %d%n", args.length);
            System.out.println("First parameter: path to the indexed data");
            System.exit(1);
        }

        String indexedDataPath = args[0];

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

                        int totalDocLength = 0;
                        for (int length : documentLengthMap.values()) {
                            totalDocLength += length;
                        }
                        double avgDocLength = (double) totalDocLength / documentLengthMap.size();
                        Scanner reader = new Scanner(System.in);

                        List<String> foundDocnos = searchQuery(indexedDataPath, reader, avgDocLength, lexicon, invIndex, documentLengthMap, idMap);
                        while (true) {
                            System.out.println("Enter [#] rank to view more info. [n] for new search. [q] to quit. ");
                            String input = reader.nextLine();

                            // Quit the program when q or Q is entered
                            if (input.toLowerCase().equals("q")) {
                                System.out.println("Quitting");
                                reader.close();
                                System.exit(0);
                            }

                            // Search for a new query when n or N is entered
                            if (input.toLowerCase().equals("n")) {
                                foundDocnos = searchQuery(indexedDataPath, reader, avgDocLength, lexicon, invIndex, documentLengthMap, idMap);
                                continue;
                            }

                            // Otherwise try retrieving the desired document
                            try {
                                int desiredRank = Integer.parseInt(input);
                                if (foundDocnos.size() >= desiredRank && desiredRank > 0) {
                                    String desiredDocno = foundDocnos.get(desiredRank-1);
                                    String fullDocument = GetDoc.getDoc(indexedDataPath, desiredDocno);
                                    System.out.println(fullDocument);
                                } else {
                                    System.out.println("Invalid number. Try again.");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid value. Try again.");
                            }
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

    // Gets the top 10 results for a query
    public static List<String> searchQuery(String indexedDataPath, Scanner reader, double avgDocLength, HashMap<String, Integer> lexicon,
                                           HashMap<Integer, List<Integer>> invIndex, HashMap<String, Integer> documentLengthMap,
                                           HashMap<Integer, String> idMap) {

        System.out.println("Enter your query: ");
        String queryTerms = reader.nextLine();
        long startTime = System.currentTimeMillis();

        // Tokenizing the query
        List<String> queryTokens = new ArrayList<>();
        IndexEngine.tokenize(queryTerms, queryTokens, false);

        // Calculate BM25 scores
        Map<String, Double> bm25Scores = BM25.calculateBM25(avgDocLength, queryTokens, lexicon, invIndex, documentLengthMap, idMap);

        // Printing the top 10 results
        int stopResultNumber = Math.min(bm25Scores.size(), 10);
        List<String> foundDocnos = new ArrayList<>();
        for (int i = 0; i < stopResultNumber; i++) {
            String docno = (String) bm25Scores.keySet().toArray()[i];
            String doc = GetDoc.getDoc(indexedDataPath, docno);
            StringBuffer docSB = new StringBuffer(doc);
            String headline = IndexEngine.getHeadline(docSB);
            String summary = determineQueryBiasedSummary(queryTokens, IndexEngine.getText(docSB));
            if (headline.equals("")) {
                headline = summary.substring(0, Math.min(summary.length(), 50)) + " ...";
            }
            String date = IndexEngine.getFormattedDate(docno);
            System.out.printf("%s. %s; (%s)\n%s(%s)\n", i+1, headline, date, summary, docno);
            foundDocnos.add(docno);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Retrieval took " + (endTime-startTime)/1000.0 + " seconds");

        return foundDocnos;
    }

    // Creates the query biased summary for an input text
    public static String determineQueryBiasedSummary(List<String> queryTokens, String text) {
        String[] sentences = text.split("(?<=[.!?])");
        PriorityQueue<PriorityQueueEntry> sentencePriority = new PriorityQueue<>();

        int skippedSentences = 0;
        for (int i = 0; i < sentences.length; i++) {
            sentences[i] = sentences[i].trim();

            // Skip sentences that have less than 5 words
            if (sentences[i].split("\\s+").length < 5) {
                skippedSentences++;
                continue;
            }
            List<String> sentenceTokens = new ArrayList<>();
            IndexEngine.tokenize(sentences[i], sentenceTokens, false);
            int l = 0, c = 0, d = 0, k = 0, temp = 0;
            boolean contiguous = false;

            // Assign scores for first and second non-skipped sentences
            if ((i-skippedSentences) == 0) {
                l = 2;
            } else if ((i-skippedSentences) == 1) {
                l = 1;
            }

            // Increment c for the number of query words that are in the sentence
            for (String queryToken : queryTokens) {
                c += Collections.frequency(sentenceTokens, queryToken);
            }

            // Increment d for each unique query term in the sentence
            Set<String> uniqueQueryTokens = new HashSet<>(queryTokens);
            for (String uniqueQueryToken : uniqueQueryTokens) {
                if (sentenceTokens.contains(uniqueQueryToken)) {
                    d += 1;
                }
            }

            // Determine longest contiguous run of query terms in the sentence
            for (String sentenceToken : sentenceTokens) {
                if (queryTokens.contains(sentenceToken)) {
                    temp +=1;
                    if (!contiguous) {
                        contiguous = true;
                    }
                    k = Math.max(temp, k);
                } else {
                    contiguous = false;
                    temp = 0;
                }
            }

            // The score of a sentence is the combination of all the parameters
            int v = c + d + k + l;
            sentencePriority.add(new PriorityQueueEntry(v, sentences[i]));
        }

        // Form summary from the two highest scored sentences
        int i = 0;
        StringBuilder summary = new StringBuilder();
        while (!sentencePriority.isEmpty() && i < 2) {
            String sentence = sentencePriority.poll().getSentence();
            summary.append(sentence);
            summary.append(" ");
            i++;
        }
        return summary.toString();
    }
}
