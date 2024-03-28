package org.codetracker.experiments;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.codetracker.experiment.AbstractExperimentStarter;
import org.codetracker.experiment.MethodExperimentStarter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/* Created by pourya on 2024-03-22*/
public class ExperimentResultTest {
    private static final String generatedFilePathTemplate = "experiments/tracking-accuracy/%s/tracker/final.csv";
    private static final String expectedFilePathTemplate = "src/test/resources/tracking-accuracy/%s/tracker/final.csv";

    public static Stream<Arguments> experimentStartedProvider() {
        return Stream.of(
//                Arguments.of(new AttributeExperimentStarter(), "attribute"),
                Arguments.of(new MethodExperimentStarter(), "method")
//                Arguments.of(new ClassExperimentStarter(), "class"),
//                Arguments.of(new VariableExperimentStarter(), "variable"),
//                Arguments.of(new BlockExperimentStarter(), "block")
        );
    }

    @Test
    public void c(){
        System.out.println("a");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource(value = "experimentStartedProvider")
    public void ExperimentStarterTest(AbstractExperimentStarter abstractExperimentStarter, String folderName) throws IOException, CsvException {
        String expectedFilePath = String.format(expectedFilePathTemplate, folderName);
        String generatedFilePath = String.format(generatedFilePathTemplate, folderName);
        System.out.println(expectedFilePath);
        System.out.println(generatedFilePath);
        abstractExperimentStarter.start();
        try (
                CSVReader expectedCSV = new CSVReader(new FileReader(expectedFilePath));
                CSVReader actualCSV = new CSVReader(new FileReader(generatedFilePath))
        ) {
            List<String[]> expectedLines = expectedCSV.readAll();
            List<String[]> actualLines = actualCSV.readAll();

            for (int i = 0; i < actualLines.size() && i < expectedLines.size(); i++) {
                String[] record1 = actualLines.get(i);
                String[] record2 = expectedLines.get(i);
                String tp1 = record1[5]; String fp1 = record1[6]; String fn1 = record1[7];
                String tp2 = record2[5]; String fp2 = record2[6]; String fn2 = record2[7];
                System.out.println("Comparing " + tp1 + " with " + tp2);
                System.out.println("Comparing " + fp1 + " with " + fp2);
                System.out.println("Comparing " + fn1 + " with " + fn2);
                assertEquals(tp1, tp2);
                assertEquals(fp1, fp2);
                assertEquals(fn1, fn2);
            }
        }
    }
}
