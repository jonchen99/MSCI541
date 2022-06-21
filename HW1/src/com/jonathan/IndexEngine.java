package com.jonathan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Month;
import java.util.HashMap;
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
        HashMap<Integer, String> idMap = new HashMap<Integer, String>();
        HashMap<String, String> metadata = new HashMap<String, String>();

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
        FileOutputStream fos = new FileOutputStream(outputPath + "/idMapping.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(idMap);
        oos.close();
        fos.close();
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
        String strippedHeadline = headline.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();

        return strippedHeadline;
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
}
