package org.codetracker.blame.benchmark;

import org.codetracker.api.CodeElement;

import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.codetracker.blame.benchmark.CsvWriter.getCodeElementName;


/* Created by pourya on 2024-07-15*/
public class StatsCollector {
    Map<String, Integer> dist = new HashMap<>();
    int sum_diff_size = 0;
    int sum_legit_size = 0;
    int count = 0;
    public void process(Map<Integer, EnumMap<BlamerFactory, String>> diff, int legitSize, Map<Integer, CodeElement> codeElementMap) {
        sum_legit_size += legitSize;
        sum_diff_size += diff.size();
        count += 1;
        updateElementDistribution(codeElementMap);
    }

    private void updateElementDistribution(Map<Integer, CodeElement> codeElementMap) {
        for (CodeElement value : codeElementMap.values()) {
            String codeElementName = getCodeElementName(value);
            Integer orDefault = dist.getOrDefault(codeElementName, 0);
            dist.put(codeElementName, orDefault + 1);
        }
    }

    public void writeInfo() {
        try {
            FileWriter writer = new FileWriter("out/collected_stats.txt");
            // Write map content
            writer.write("ElementType Distribution: (only the different lines)\n");
            for (Map.Entry<String, Integer> entry : dist.entrySet()) {
                writer.write(entry.getKey() + " = " + entry.getValue() + "\n");
            }
            writer.write("----------------------------\n");
            // Write other variables
            writer.write("NUM_DIFFERENT_LINES = " + sum_diff_size + "\n");
            writer.write("NUM_LEGIT_LINES = " + sum_legit_size + "\n");
            writer.write("NUM_OF_FILES = " + count + "\n");

            writer.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
