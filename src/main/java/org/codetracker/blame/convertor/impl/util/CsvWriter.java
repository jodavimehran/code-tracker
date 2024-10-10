package org.codetracker.blame.convertor.impl.util;

import org.codetracker.api.CodeElement;
import org.codetracker.blame.model.CodeElementWithRepr;
import org.codetracker.blame.model.IBlameTool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* Created by pourya on 2024-07-15*/
public class CsvWriter {
    private final String filePath;
    private final Map<Integer, CodeElementWithRepr> codeElementMap;
    private final String owner;
    private final String project;
    private final String commitId;
    private final String output_folder = "out/";

    public CsvWriter(String owner, String project, String commitId, String filePath) {
        this(owner, project, commitId, filePath, null);
    }
    public CsvWriter(String owner, String project, String commitId, String filePath, Map<Integer, CodeElementWithRepr> codeElementMap) {
        this.owner = owner;
        this.project = project;
        this.commitId = commitId;
        this.filePath = filePath;
        this.codeElementMap = codeElementMap;
    }

    public void writeToCSV(Map<Integer, Map<IBlameTool, String>> table) {

        try (PrintWriter writer = new PrintWriter(makeFile())) {
            Set<IBlameTool> blamerFactories = table.entrySet().iterator().next().getValue().keySet();
            writer.print("LineNumber,");
            Iterator<IBlameTool> iterator = blamerFactories.iterator();
            while (iterator.hasNext()){
                IBlameTool next = iterator.next();
                writer.print(next.getToolName());
                if (iterator.hasNext()) writer.print(",");
            }
            if (codeElementMap != null)
                writer.print(",CodeElement,ActualString");
            writer.println();
            // Write the data
            for (Map.Entry<Integer, Map<IBlameTool, String>> entry : table.entrySet()) {
                Map<IBlameTool, String> results = entry.getValue();
                Iterator<String> valueIterator = results.values().iterator();
                writer.print(entry.getKey() + ",");
                while (valueIterator.hasNext()){
                    String next = valueIterator.next();
                    writer.print(next);
                    if (valueIterator.hasNext()) writer.print(",");
                }
                if (codeElementMap != null) {
                    CodeElementWithRepr codeElementWithRepr = codeElementMap.get(entry.getKey());
                    writer.print("," + getCodeElementName(codeElementWithRepr.getCodeElement()));
                    writer.print("," + escapeCsvField(codeElementWithRepr.getRepresentation()));
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
    private static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // Escape double quotes by replacing them with two double quotes
        field = field.replace("\"", "\"\"");
        // Wrap the field in quotes if it contains a comma or double quotes
        if (field.contains(",") || field.contains("\"")) {
            field = "\"" + field + "\"";
        }
        return field;
    }


    private File makeFile() {
        String finalPath = output_folder + owner + "/" + project + "/" + commitId + "/" + filePath.replace("/",".") + ".csv";
        File file = new File(finalPath);
        file.getParentFile().mkdirs(); // Create the directories if they don't exist
        return file;
    }
}
