package com.jonathan;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class GetDoc {
    public static void main(String[] args) {

        // Input argument checking
        if (args.length != 3) {
            System.out.println(String.format("ERROR: Wrong number of arguments passed in! Expected: 3. Received: %d", args.length));
            System.out.println("First parameter: path to the indexed data");
            System.out.println("Second parameter: \"id\" or \"docno\"");
            System.out.println("Third parameter: internal integer id or docno");
            System.exit(1);
        }

        String indexedDataPath = args[0];
        String lookupType = args[1];
        String lookupValue = args[2];
        String docno = lookupValue;

        if (!(lookupType.equals("id") || lookupType.equals("docno"))) {
            System.out.println("ERROR: Invalid lookup type. Second parameter must be \"id\" or \"docno\"");
            System.exit(1);
        }

        // Read in the id mapping file
        if (lookupType.equals("id")) {
            try (FileInputStream fis = new FileInputStream(indexedDataPath + "/idMapping.txt");
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                @SuppressWarnings("unchecked")
                HashMap<Integer, String> idMap = (HashMap<Integer, String>) ois.readObject();

                docno = idMap.get(Integer.parseInt(lookupValue));
            } catch (Exception e) {
                System.out.println("ERROR: Unable to get id mapping");
                e.printStackTrace();
            }
        }

        String doc = getDoc(indexedDataPath, docno);
        System.out.println(doc);
    }

    public static String getDoc(String indexedDataPath, String docno) {
        // Output the desired file
        try {
            String outputPath = IndexEngine.getOutputFilePath(indexedDataPath, docno);
            return Files.readString(Paths.get(outputPath));
        } catch (IOException e) {
            System.out.println("ERROR: Unable to get indexed data");
            e.printStackTrace();
        }
        return "";
    }
}
