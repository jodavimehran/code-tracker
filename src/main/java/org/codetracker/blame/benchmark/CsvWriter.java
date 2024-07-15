package org.codetracker.blame.benchmark;

import org.codetracker.api.CodeElement;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* Created by pourya on 2024-07-15*/
public class CsvWriter {
    private final String filePath;
    private final Map<Integer, CodeElement> codeElementMap;
    private final String owner;
    private final String project;
    private final String commitId;
    private final String output_folder = "out/";

    public CsvWriter(String owner, String project, String commitId, String filePath) {
        this(owner, project, commitId, filePath, null);
    }
    public CsvWriter(String owner, String project, String commitId, String filePath, Map<Integer, CodeElement> codeElementMap) {
        this.owner = owner;
        this.project = project;
        this.commitId = commitId;
        this.filePath = filePath;
        this.codeElementMap = codeElementMap;
    }

    public void writeToCSV(Map<Integer, EnumMap<BlamerFactory, String>> table) {

        try (PrintWriter writer = new PrintWriter(makeFile())) {
            Set<BlamerFactory> blamerFactories = table.entrySet().iterator().next().getValue().keySet();
            writer.print("LineNumber,");
            Iterator<BlamerFactory> iterator = blamerFactories.iterator();
            while (iterator.hasNext()){
                BlamerFactory next = iterator.next();
                writer.print(next.getName());
                if (iterator.hasNext()) writer.print(",");
            }
            if (codeElementMap != null)
                writer.print(",CodeElement");
            writer.println();
            // Write the data
            for (Map.Entry<Integer, EnumMap<BlamerFactory, String>> entry : table.entrySet()) {
                EnumMap<BlamerFactory, String> results = entry.getValue();
                Iterator<String> valueIterator = results.values().iterator();
                writer.print(entry.getKey() + ",");
                while (valueIterator.hasNext()){
                    String next = valueIterator.next();
                    writer.print(next);
                    if (valueIterator.hasNext()) writer.print(",");
                }
                if (codeElementMap != null) {
                    CodeElement codeElement = codeElementMap.get(entry.getKey());
                    writer.print("," + getCodeElementName(codeElement));
                }

                writer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String getCodeElementName(CodeElement codeElement) {
        String className = codeElement.getClass().toString();
        String additional = "class org.codetracker.element.";
        return className.replace(additional, "");
    }

    private File makeFile() {
        String finalPath = output_folder + owner + "/" + project + "/" + commitId + "/" + filePath.replace("/",".") + ".csv";
        File file = new File(finalPath);
        file.getParentFile().mkdirs(); // Create the directories if they don't exist
        return file;
    }
}
