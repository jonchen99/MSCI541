package com.jonathan;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class Evaluate {
    private static final double HALF_LIFE = 224;
    private static final double P_CLICK_RELEVANT = 0.64;
    private static final double P_CLICK_NON_RELEVANT = 0.39;
    private static final double P_SAVE_RELEVANT = 0.77;
    private static final double T_S = 4.4;

    public static void main(String[] args) throws Exception {

        // Input argument checking
        if (args.length != 5) {
            System.out.printf("ERROR: Wrong number of arguments passed in! Expected: 4. Received: %d%n", args.length);
            System.out.println("First parameter: path to the indexed data");
            System.out.println("Second parameter: path to the qrels file.");
            System.out.println("Third parameter: path to the results file.");
            System.out.println("Fourth parameter: output directory path to save the results");
            System.out.println("Fifth parameter: output csv file name");
            System.exit(1);
        }
        String indexedDataPath = args[0];
        String qrelsPath = args[1];
        String resultsPath = args[2];
        String outputPath = args[3];
        String outputFileName = args[4];

        Path outputDirectory = Paths.get(outputPath);
        if (!Files.exists(outputDirectory)) {
            Files.createDirectory(outputDirectory);
        }

        FileWriter outputAveragesWriter;
        BufferedWriter averagesBw;
        Path outputAveragesFilePath = Paths.get(outputPath + "/" + outputFileName);

        // Create the output csv if it doesn't already exist, containing the headers for the csv
        if (!Files.exists(outputAveragesFilePath)) {
            outputAveragesWriter = new FileWriter(outputPath + "/" + outputFileName);
            averagesBw = new BufferedWriter(outputAveragesWriter);
            StringBuilder averagesSb = new StringBuilder();

            String[] averagesHeaders = {"Run Name", "Mean Average Precision", "Mean P@10", "Mean NDCG@10", "MeanNDCG@1000", "Mean TBG"};

            for (String word : averagesHeaders) {
                averagesSb.append(word);
                averagesSb.append(",");
            }
            averagesSb.append("\n");
            averagesBw.write(averagesSb.toString());

            // Otherwise append to the existing csv
        } else {
            outputAveragesWriter = new FileWriter(outputPath + "/" + outputFileName, true);
            averagesBw = new BufferedWriter(outputAveragesWriter);
        }

        // Getting the name of the run (i.e. student1.results -> student1)
        String resultsFileName = resultsPath.substring(resultsPath.lastIndexOf("/")+1);
        String runName = resultsFileName.split("\\.")[0];

        try {
            // Read in the query judgements and the results file
            Qrels qrel = new Qrels(qrelsPath);
            ResultsFile resultsFile = new ResultsFile(resultsPath);

            try {
                LinkedHashMap<String, TreeMap<String, Double>> evaluationMeasures = new LinkedHashMap<>();

                // Calculate the effective measures
                evaluationMeasures.put("Average Precision", calculateAveragePrecision(qrel.judgements, resultsFile.results));
                evaluationMeasures.put("Precision @10", calculatePrecisionAt10(qrel.judgements, resultsFile.results));
                evaluationMeasures.put("NDCG @10", calculateNDCG(qrel.judgements, resultsFile.results, 10));
                evaluationMeasures.put("NDCG @1000", calculateNDCG(qrel.judgements, resultsFile.results, 1000));
                evaluationMeasures.put("TBG", calculateTBG(indexedDataPath, qrel.judgements, resultsFile.results));

                // Write the averaged effective measures to a csv
                writeAverageMeasuresToCSV(evaluationMeasures, averagesBw, runName);

                // Write all the effective measures for each query to a csv
                writeMeasuresToCSV(evaluationMeasures, outputPath, runName);

                System.out.printf("Successfully calculated the effectiveness measures for %s\n", runName);
                System.out.println("Mean Average Precision: " + calculateAverageMeasure(evaluationMeasures.get("Average Precision")));
                System.out.println("Mean Precision @10: " + calculateAverageMeasure(evaluationMeasures.get("Precision @10")));
                System.out.println("Mean NDCG @10: " + calculateAverageMeasure(evaluationMeasures.get("NDCG @10")));
                System.out.println("Mean NDCG @1000: " + calculateAverageMeasure(evaluationMeasures.get("NDCG @1000")));
                System.out.println("Mean TBG: " + calculateAverageMeasure(evaluationMeasures.get("TBG")));
            } catch (Exception e) {
                System.out.println("ERROR: unable to calculate measurement averages");
                writeBadFormatttedRunToCSV(averagesBw, runName);
            }
        } catch (Exception e) {
            System.out.println("Unable to parse results file");
            writeBadFormatttedRunToCSV(averagesBw, runName);
        }

        averagesBw.close();
        outputAveragesWriter.close();
    }

    // Print "bad format" for each column in the averages csv
    public static void writeBadFormatttedRunToCSV(BufferedWriter bw, String runName) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(runName).append(",");
        for (int i = 0; i < 5; i++) {
            sb.append("bad format");
            sb.append(",");
        }
        sb.append("\n");
        bw.write(sb.toString());
    }

    // Write all the averaged effective measures to a csv
    public static void writeAverageMeasuresToCSV(LinkedHashMap<String, TreeMap<String, Double>> measures, BufferedWriter bw, String runName) throws IOException {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.000");
        sb.append(runName).append(",");
        for (TreeMap<String, Double> measure : measures.values()) {
            sb.append(df.format(calculateAverageMeasure(measure)));
            sb.append(",");
        }

        sb.append("\n");
        bw.write(sb.toString());
    }

    // Calculate the average precision
    public static TreeMap<String, Double> calculateAveragePrecision(RelevanceJudgements judgements, Results results) throws Exception {
        TreeMap<String, Double> averagePrecision = new TreeMap<>();

        for (String queryId : judgements.getQueryIDs()) {
            int numRelevant = 0;

            // Get the query results if they exist
            ArrayList<Results.Result> resultList;
            if (results.queryIDExists(queryId)) {
                resultList = results.queryResults(queryId);
            } else {
                averagePrecision.put(queryId, 0.0);
                continue;
            }

            ArrayList<Double> precisionAtRank = new ArrayList<>();

            // Calculate precision
            if (!resultList.isEmpty()) {
                for (int i = 0; i < resultList.size(); i++) {
                    if (judgements.isRelevant(queryId, resultList.get(i).getDocID())) {
                        numRelevant++;
                        precisionAtRank.add((double) numRelevant / (i+1));
                    }
                }
                Double precisionSum = 0.0;
                for (Double precision : precisionAtRank) {
                    precisionSum += precision;
                }

                averagePrecision.put(queryId, precisionSum / (double) judgements.numRelevant(queryId));
            }
            else {
                averagePrecision.put(queryId, 0.0);
            }
        }

        return averagePrecision;
    }

    // Calculate the precision at rank 10
    public static TreeMap<String, Double> calculatePrecisionAt10(RelevanceJudgements judgements, Results results) throws Exception {
        TreeMap<String, Double> precisionAt10 = new TreeMap<>();
        for (String queryId : judgements.getQueryIDs()) {
            int numRelevant = 0;
            ArrayList<Results.Result> resultList;
            if (results.queryIDExists(queryId)) {
                resultList = results.queryResults(queryId);
            } else {
                precisionAt10.put(queryId, 0.0);
                continue;
            }

            if (!resultList.isEmpty()) {
                int actualStopRank = Math.min(resultList.size(), 10);
                for (int i = 0; i < actualStopRank; i++) {
                    if (judgements.isRelevant(queryId, (resultList.get(i).getDocID()))) {
                        numRelevant++;
                    }
                }

                precisionAt10.put(queryId, (double) numRelevant/10);
            } else {
                precisionAt10.put(queryId, 0.0);
            }
        }

        return precisionAt10;
    }

    // Calculate the ndcg at the stopRank passed in (10 or 1000)
    public static TreeMap<String, Double> calculateNDCG(RelevanceJudgements judgements, Results results, int stopRank) throws Exception {
        TreeMap<String, Double> ndcgOutput = new TreeMap<>();
        for (String queryId : judgements.getQueryIDs()) {
            double dcgSum = 0;
            ArrayList<Results.Result> resultList;
            if (results.queryIDExists(queryId)) {
                resultList = results.queryResults(queryId);
            } else {
                ndcgOutput.put(queryId, 0.0);
                continue;
            }
            if (!resultList.isEmpty()) {
                int actualStopRank = Math.min(stopRank, resultList.size());
                for (int i = 0; i < actualStopRank; i++) {
                    if (judgements.isRelevant(queryId, (resultList.get(i).getDocID()))) {
                        // i+2 because the equation is (i+1)+1 since index of for loop starts from 0 instead of 1
                        dcgSum += (1 / (Math.log(i + 2) / Math.log(2)));
                    }
                }

                Double idcg = calculateIDCG(actualStopRank, judgements.numRelevant(queryId));
                ndcgOutput.put(queryId, dcgSum / idcg);
            } else {
                ndcgOutput.put(queryId, 0.0);
            }
        }

        return ndcgOutput;
    }

    // Calculate the time biased gain
    public static TreeMap<String, Double> calculateTBG(String indexedDataPath, RelevanceJudgements judgements, Results results) throws Exception {
        TreeMap<String, Double> tbg = new TreeMap<>();

        HashMap<String, Integer> documentLengthMap = null;
        // Read in the mapping of docid to document length
        try (FileInputStream fis = new FileInputStream(indexedDataPath + "/documentLength.txt");
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            documentLengthMap = (HashMap<String, Integer>) ois.readObject();
        } catch (Exception e) {
            System.out.println("ERROR: Unable to get document length");
            e.printStackTrace();
        }

        for (String queryId : judgements.getQueryIDs()) {
            double tbgSum = 0.0;
            double t_kSum = 0.0;

            ArrayList<Results.Result> resultList;
            if (results.queryIDExists(queryId)) {
                resultList = results.queryResults(queryId);
            } else {
                tbg.put(queryId, 0.0);
                continue;
            }
            for (Results.Result result : resultList) {
                String docID = result.getDocID();
                int length = 0;

                length = documentLengthMap.get(docID);

                if (judgements.isRelevant(queryId, (result.getDocID()))) {
                    t_kSum += T_S + (0.018 * length + 7.8) * P_CLICK_RELEVANT;
                    double decay = Math.exp(-t_kSum * Math.log(2) / HALF_LIFE);
                    tbgSum += P_CLICK_RELEVANT * P_SAVE_RELEVANT * decay;
                } else {
                    t_kSum += T_S + (0.018 * length + 7.8) * P_CLICK_NON_RELEVANT;
                }
            }

            tbg.put(queryId, tbgSum);
        }

        return tbg;
    }

    // Calculate the IDCG
    public static Double calculateIDCG(int stopRank, int numRelevant) {
        double idcg = 0;
        if (numRelevant > 0) {
            for (int i = 1; i <= Math.min(stopRank, numRelevant); i++) {
                idcg += (1/(Math.log(i+1)/Math.log(2)));
            }
        } else {
            idcg = 1.0;
        }

        return idcg;
    }

    // Calculate the average of the effective measure
    public static Double calculateAverageMeasure(TreeMap<String, Double> evaluationMeasure) {
        double sum = 0.0;
        for (String docid : evaluationMeasure.keySet()) {
            sum += evaluationMeasure.get(docid);
        }
        return sum / evaluationMeasure.size();
    }

    // Write all effective measures for each query to a csv
    public static void writeMeasuresToCSV(LinkedHashMap<String, TreeMap<String, Double>> measureMap, String outputPath, String runName) {
        StringBuilder sb = new StringBuilder();
        try {
            FileWriter writer = new FileWriter(String.format("%s/%s.csv", outputPath, runName));
            BufferedWriter bw = new BufferedWriter(writer);

            for (Map.Entry<String, TreeMap<String, Double>> outerMap : measureMap.entrySet()) {
                String measureName = outerMap.getKey();
                for (Map.Entry<String, Double> measure : outerMap.getValue().entrySet()) {
                    sb.append(measureName);
                    sb.append(",");
                    sb.append(measure.getKey());
                    sb.append(",");
                    sb.append(measure.getValue());
                    sb.append("\n");
                }
            }
            bw.write(sb.toString());

            bw.close();
            writer.close();
        } catch (IOException e){
            System.out.printf("ERROR: Unable to write %s to CSV", runName);
            e.printStackTrace();
        }
    }
}