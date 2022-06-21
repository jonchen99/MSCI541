package com.jonathan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class IndexEngine {
    public static void main(String[] args) throws IOException {

        // Input argument checking
        if (args.length != 2) {
            System.out.println(String.format("ERROR: Wrong number of arguments passed in! Expected: 2. Received: %d", args.length));
            System.out.println("First parameter: path to the latimes.gz file.");
            System.out.println("Second parameter: output directory path to save the indexed data");
            System.exit(1);
        }
        String inputFile = args[0];
        String outputPath = args[1];
        Path outputDirectory = Paths.get(outputPath);
        if (Files.exists(outputDirectory)) {
            System.out.println("ERROR: Output directory already exists!");
            System.exit(1);
        }

        Files.createDirectory(outputDirectory);

        // Reading in the gzipped file
        FileInputStream fis = null;
        GZIPInputStream gzis = null;
        InputStreamReader reader = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(inputFile);
            gzis = new GZIPInputStream(fis);
            reader = new InputStreamReader(gzis);
            br = new BufferedReader(reader);
        } catch (IOException e) {
            System.out.println("ERROR: Unable to read latimes file");
            e.printStackTrace();
        }

        String txt = "";
        StringBuffer doc = new StringBuffer();
        int id = 0;
        HashMap<Integer, String> idMap = new HashMap<>();
        HashMap<String, String> metadata = new HashMap<>();

        HashMap<String, Integer> lexicon = new HashMap<>();
        HashMap<Integer, String> lexiconIdToString = new HashMap<>();
        HashMap<Integer, List<Integer>> invIndex = new HashMap<>();
        HashMap<String, Integer> documentLength = new HashMap<>();

        File metadataPath = new File(outputPath + "/metadata.txt");
        FileWriter metadataWriter = new FileWriter(metadataPath, StandardCharsets.UTF_8);

        // Reading file line by line
        while ((txt = br.readLine()) != null) {
            doc.append(txt+"\n");

            // Process the doc when the </DOC> is reached
            if (txt.contains("</DOC>")) {
                // Inserting all metadata into a hashmap
                metadata.put("docno", getDocNo(doc));
                metadata.put("id", Integer.toString(id));
                metadata.put("headline", getHeadline(doc));
                metadata.put("date", getFormattedDate(metadata.get("docno")));

                // Determining tokens
                List<String> tokens = new ArrayList<>();
                tokenize(metadata.get("headline"), tokens);
                tokenize(getText(doc), tokens);
                tokenize(getGraphic(doc), tokens);
                metadata.put("documentLength", Integer.toString(tokens.size()));
                documentLength.put(metadata.get("docno"), tokens.size());

                List<Integer> tokenIDs = convertTokensToIDs(tokens, lexicon, lexiconIdToString);
                HashMap<Integer, Integer> wordCounts = countWords(tokenIDs);

                addToPostings(wordCounts, id, invIndex);

                // Save metadata into its own doc
                saveMetadata(metadataWriter, metadata);

                metadata.put("rawDocument", doc.toString());

                // Save each doc into its own file
                saveDocument(outputPath, metadata);

                // Create the mapping of id to docno
                idMap.put(id, metadata.get("docno"));

                metadata.clear();
                doc.delete(0, doc.length());
                id++;
            }
        }

        br.close();
        metadataWriter.close();
        reader.close();
        fis.close();
        gzis.close();

        // Save the id to docno mapping to a file
        writeMapToFile(idMap, outputPath + "/idMapping.txt");
        writeMapToFile(lexicon, outputPath + "/lexicon.txt");
        writeMapToFile(lexiconIdToString, outputPath + "/lexiconIdToString.txt");
        writeMapToFile(invIndex, outputPath + "/invIndex.txt");
        writeMapToFile(documentLength, outputPath + "/documentLength.txt");
    }

    // General function to save hashmap to a file
    public static void writeMapToFile(HashMap<?,?> map, String outputPath) {
        try {
            FileOutputStream fos = new FileOutputStream(outputPath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
            fos.close();
        }
        catch (Exception e) {
            System.out.println(String.format("ERROR: Unable to write to %s", outputPath));
        }

    }

    // Return the docno from the text
    public static String getDocNo(StringBuffer doc) {
        Pattern docnoRegex = Pattern.compile("<DOCNO>(.+?)</DOCNO>");
        Matcher docnoMatcher = docnoRegex.matcher(doc);
        String docno = "";
        if (docnoMatcher.find()) {
            docno = docnoMatcher.group(1).replaceAll("\\s", "");
        }
        return docno;
    }

    // Return the headline from the text
    public static String getHeadline(StringBuffer doc) {
        Pattern headlineRegex = Pattern.compile("<HEADLINE>([\\s\\S]*?)</HEADLINE>");
        Matcher headlineMatcher = headlineRegex.matcher(doc);
        String headline = "";
        if(headlineMatcher.find()) headline = headlineMatcher.group(1);

        // Remove all inner tags from the headline (ex. <P></P>)
        String strippedHeadline = headline.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ").trim();

        return strippedHeadline;
    }

    // Return the text within the <TEXT></TEXT> tags
    public static String getText(StringBuffer doc) {
        Pattern textRegex = Pattern.compile("<TEXT>([\\s\\S]*?)</TEXT>");
        Matcher textMatcher = textRegex.matcher(doc);
        String text = "";

        if (textMatcher.find()) text = textMatcher.group(1);

        // Remove all inner tags from the text (ex. <P></P>)
        String strippedText = text.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ").trim();
        return strippedText;
    }

    // Return the text within the <GRAPHIC></GRAPHIC> tags
    public static String getGraphic(StringBuffer doc) {
        Pattern graphicRegex = Pattern.compile("<GRAPHIC>([\\s\\S]*?)</GRAPHIC>");
        Matcher graphicMatcher = graphicRegex.matcher(doc);
        String graphic = "";
        if (graphicMatcher.find()) graphic = graphicMatcher.group(1);

        // Remove all inner tags from the text (ex. <P></P>)
        String strippedGraphic = graphic.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ").trim();

        return strippedGraphic;
    }

    // Return a hashmap containing the month, day, and year as keys
    public static HashMap<String, String> getDate(String docno) {
        HashMap<String, String> dateInfo = new HashMap<String, String>();
        String date = docno.substring(2, docno.indexOf("-"));
        dateInfo.put("month", date.substring(0,2));
        dateInfo.put("day", date.substring(2,4));
        dateInfo.put("year", date.substring(4));
        return dateInfo;
    }

    // Return the file folder path of where each indexed file is stored
    public static String getOutputFilePath(String outputPath, String docNo) {
        HashMap<String, String> dateInfo = getDate(docNo);
        return String.format("%s/%s/%s/%s/%s.txt", outputPath, dateInfo.get("year"),
                dateInfo.get("month"), dateInfo.get("day"), docNo);
    }

    // Format the date to me Month DD, YYYY
    public static String getFormattedDate(String docno) {
        HashMap<String, String> dateInfo = getDate(docno);
        String monthName = Month.of(Integer.valueOf(dateInfo.get("month"))).name();
        monthName = monthName.substring(0,1).toUpperCase() + monthName.substring(1).toLowerCase();
        return String.format("%s %s, %s", monthName, dateInfo.get("day"), "19" + dateInfo.get("year"));
    }

    // Save the metadata
    public static void saveMetadata(FileWriter writer, HashMap<String,String> metadata) {
        try {
            for (String value : metadata.values()) {
                writer.write(String.format("%s\n", value));
            }
        } catch (IOException e) {
            System.out.println("Error writing to metadata file");
            e.printStackTrace();
        }
    }

    // Save the metadata and raw document to the indexed data location
    public static void saveDocument(String outputPath, HashMap<String,String> metadata) {
        File output = new File(getOutputFilePath(outputPath, metadata.get("docno")));
        output.getParentFile().mkdirs();
        try {
            FileWriter writer = new FileWriter(output, StandardCharsets.UTF_8);
            writer.write(String.format("docno: %s\n", metadata.get("docno")));
            writer.write(String.format("internal id: %s\n", metadata.get("id")));
            writer.write(String.format("date: %s\n", metadata.get("date")));
            writer.write(String.format("headline: %s\n", metadata.get("headline")));
            writer.write(String.format("raw document: \n%s\n", metadata.get("rawDocument")));
            writer.close();

        } catch (IOException e) {
            System.out.println("Error writing to files");
            e.printStackTrace();
        }
    }

    // Tokenize a string and add tokens to a list
    public static void tokenize(String text, List<String> tokens) {
        String lowerCaseText = text.toLowerCase();
        int start = 0;
        int i;
        for (i=0; i < lowerCaseText.length(); ++i) {
            char c = lowerCaseText.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (start != i) {
                    String token = lowerCaseText.substring(start, i);
                    tokens.add(token);
                }
                start = i+1;
            }
        }
        if (start != i) {
            tokens.add(lowerCaseText.substring(start, i));
        }
    }

    // Get tokenIDs and update lexicon
    public static List<Integer> convertTokensToIDs(List<String> tokens, HashMap<String, Integer> lexicon, HashMap<Integer, String> lexiconIdToString) {
        List<Integer> tokenIDs = new ArrayList<>();
        for (String token:tokens) {
            if (lexicon.containsKey(token)) {
                tokenIDs.add(lexicon.get(token));
            } else {
                int id = lexicon.size();
                lexicon.put(token, id);
                lexiconIdToString.put(id, token);
                tokenIDs.add(id);
            }
        }
        return tokenIDs;
    }

    // Count word occurrences
    public static HashMap<Integer, Integer> countWords(List<Integer> tokenIDs) {
        HashMap<Integer, Integer> wordCounts = new HashMap<>();
        for (int id : tokenIDs) {
            Integer count = wordCounts.get(id);
            if (count == null) {
                wordCounts.put(id, 1);
            } else {
                wordCounts.put(id, count+1);
            }
        }
        return wordCounts;
    }

    // Add docid and word count to postings list and update inverted index
    public static void addToPostings(HashMap<Integer, Integer> wordCounts, int docID, HashMap<Integer, List<Integer>> invIndex) {
        for (int termID : wordCounts.keySet()) {
            int count = wordCounts.get(termID);
            List<Integer> postings = new ArrayList<>();
            if (invIndex.containsKey(termID)) {
                postings = invIndex.get(termID);
            }
            postings.add(docID);
            postings.add(count);
            invIndex.put(termID, postings);
        }
    }
}
