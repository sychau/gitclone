package gitlet;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a gitlet staging area.
 *
 * All operations related to the staging area of .gitlet is stored here.
 * The file name map will store staging area file in the following format:
 * <FILE NAME1> <BLOB UID1>
 * <FILE NAME2> <BLOB UID2>
 * <FILE NAME3> <BLOB UID3>
 * ...
 *
 * @author sychau
 */
public class StagingArea {

    /**
     * Read the desired staging area file as String as parse it to map which map file name
     * to blobUID Then return the map
     */
    public static Map<String, String> getFileMapFrom(File target) {
        Map<String, String> m = new HashMap<>();

        String content = Utils.readContentsAsString(target);
        if (content.equals("")) {
            return m;
        }
        String[] kvPairs = content.split("\n");
        for (String kvInString : kvPairs) {
            String[] kvInArr = kvInString.split(" ");
            m.put(kvInArr[0], kvInArr[1]);
        }
        return m;
    }

    /**
     * Rewrite the targeted staging area given a file name map M
     */
    public static void overwriteFromMap(File target, Map<String, String> m) {
        try {
            FileWriter writer = new FileWriter(target, false);
            for (String key : m.keySet()) {
                String line = String.format("%s %s\n", key, m.get(key));
                writer.write(line);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clear all content inside the target file
     */
    public static void clear(File target) {
        try {
            PrintWriter pw = new PrintWriter(target);
            pw.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
        }
    }
}
