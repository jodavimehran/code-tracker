package org.codetracker.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.codetracker.api.Change;
import org.codetracker.experiment.oracle.ChangeHistory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.codetracker.util.FileUtil.writeToFile;

public abstract class AbstractExperimentStarter {
    protected final static String FOLDER_TO_CLONE = "H:\\Projects\\";
    protected static final String SUMMARY_RESULT_FILE_NAME_FORMAT = "experiments/tracking-accuracy/%s/%s/summary-%s-%s.csv";
    protected final static String SUMMARY_RESULT_HEADER;
    protected static final String DETAILED_RESULT_HEADER = "file_name, repository,element_key,parent_commit_id,commit_id,commit_time, change_type,element_file_before,element_file_after,element_name_before,element_name_after,result,comment" + System.lineSeparator();
    protected static final String DETAILED_RESULT_FILE_NAME_FORMAT = "experiments/tracking-accuracy/%s/%s/detailed-%s-%s.csv";
    protected static final String FINAL_RESULT_HEADER = "tool,oracle,level,processing_time_avg,processing_time_median,tp,fp,fn,precision,recall" + System.lineSeparator();
    protected static final String DETAILED_CONTENT_FORMAT = "%s,\"%s\",\"%s\",%s,%s,%d,%s,%s,%s,\"%s\",\"%s\",%s,\"%s\"" + System.lineSeparator();
    protected static final String FINAL_RESULT_FORMAT = "%s,%s,%s,%f,%d,%d,%d,%d,%f,%f" + System.lineSeparator();
    protected static final String ERROR_FILE_NAME_FORMAT = "experiments/tracking-accuracy/%s/%s/error-%s-%s-%s.txt";
    protected static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RESULT_FINAL_CSV = "experiments/tracking-accuracy/%s/%s/final.csv";
    private static final String PROCESSED_FILE_NAME_FORMAT = "experiments/tracking-accuracy/%s/%s/processed-%s-%s.csv";

    static {
        StringBuilder header = new StringBuilder();
        header.append("instance,processing_time,analysed_commits,git_log_command_calls,step2,step3,step4,step5,");
        for (Change.Type changeType : Change.Type.values()) {
            if (Change.Type.NO_CHANGE.equals(changeType) || Change.Type.MULTI_CHANGE.equals(changeType) || Change.Type.REMOVED.equals(changeType))
                continue;
            header.append("tp_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
            header.append("fp_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
            header.append("fn_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");

        }

        header.append("tp_all,fp_all,fn_all").append(System.lineSeparator());
        SUMMARY_RESULT_HEADER = header.toString();
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected static List<String[]> readResults(String fileName) throws IOException, CsvException {
        FileReader filereader = new FileReader(fileName);
        CSVReader csvReader = new CSVReaderBuilder(filereader).withSkipLines(1).build();
        return csvReader.readAll();
    }


    protected static String getChangeKey(Change.Type changeType, String commitId) {
        return getChangeKey(changeType.getTitle(), commitId);
    }

    protected static String getChangeKey(String changeType, String commitId) {
        return String.format("%s-%s", commitId, changeType);
    }

    protected static String getChangeKey(ChangeHistory changeHistory) {
        return getChangeKey(changeHistory.getChangeType(), changeHistory.getCommitId());
    }

    protected static HashMap<String, ChangeHistory> oracle(List<ChangeHistory> expectedChanges) {
        HashMap<String, ChangeHistory> oracleChanges = new HashMap<>();
        for (ChangeHistory changeHistory : expectedChanges) {
            Change.Type changeType = Change.Type.get(changeHistory.getChangeType());
            String commitId = changeHistory.getCommitId();
            String changeKey = getChangeKey(changeType, commitId);
            oracleChanges.put(changeKey, changeHistory);
        }
        return oracleChanges;
    }

    protected String getProcessedFilePath(String oracleName, String toolName) {
        return String.format(PROCESSED_FILE_NAME_FORMAT, getCodeElementName(), toolName, toolName, oracleName);
    }

    protected Set<String> getAllProcessedSamples(String oracleName, String toolName) throws IOException {
        String processedFilePath = getProcessedFilePath(oracleName, toolName);
        Path path = writeToFile(processedFilePath, "file-names" + System.lineSeparator(), null, StandardOpenOption.APPEND);
        return new HashSet<>(Files.readAllLines(path));
    }

    protected abstract String getCodeElementName();

    protected void calculateFinalResults(String oracleName, String toolName) {
        Map<String, Set<String>> commitLevelExpected = new HashMap<>();
        Map<String, Set<String>> commitLevelActual = new HashMap<>();
        try {
            List<Integer> processingTime = new ArrayList<>();
            List<String[]> summaryResults = readResults(getSummaryResultFileName(oracleName, toolName));
            int tp = 0, fp = 0, fn = 0;
            for (String[] result : summaryResults) {
                tp += Integer.parseInt(result[result.length - 3]);
                fp += Integer.parseInt(result[result.length - 2]);
                fn += Integer.parseInt(result[result.length - 1]);
                commitLevelExpected.put(result[0], new HashSet<>());
                commitLevelActual.put(result[0], new HashSet<>());
                processingTime.add(Integer.parseInt(result[1]));
            }
            Collections.sort(processingTime);
            int middle = processingTime.size() / 2;
            middle = middle > 0 && middle % 2 == 0 ? middle - 1 : middle;
            int processingTimeMedian = processingTime.get(middle);
            double processingTimeAverage = processingTime.stream().mapToInt(Integer::intValue).average().getAsDouble();
            writeToFile(String.format(RESULT_FINAL_CSV, getCodeElementName(), toolName), FINAL_RESULT_HEADER, String.format(FINAL_RESULT_FORMAT, toolName, oracleName, "change", processingTimeAverage, processingTimeMedian, tp, fp, fn, ((double) tp / (tp + fp)) * 100, ((double) tp / (tp + fn)) * 100), StandardOpenOption.APPEND);

            List<String[]> detailedResults = readResults(getDetailedResultFileName(oracleName, toolName));

            for (String[] result : detailedResults) {
                String elementKey = result[2];
                String commitId = result[4];
                if (result[result.length - 2].equals("TP")) {
                    commitLevelExpected.get(elementKey).add(commitId);
                    commitLevelActual.get(elementKey).add(commitId);
                }
                if (result[result.length - 2].equals("FN")) {
                    commitLevelExpected.get(elementKey).add(commitId);
                }
                if (result[result.length - 2].equals("FP")) {
                    commitLevelActual.get(elementKey).add(commitId);
                }
            }
            int sumTp = 0, sumFP = 0, sumFn = 0;
            for (Map.Entry<String, Set<String>> entry : commitLevelExpected.entrySet()) {
                String instanceName = entry.getKey();
                Set<String> expectedCommitIds = entry.getValue();
                Set<String> actualCommitIds = commitLevelActual.get(instanceName);
                int expectedSize = expectedCommitIds.size();
                int actualSize = actualCommitIds.size();
                HashSet<String> actualCopy = new HashSet<>(actualCommitIds);
                actualCopy.removeAll(expectedCommitIds);
                int commitLevelFp = actualCopy.size();
                sumFP += commitLevelFp;

                HashSet<String> expectedCopy = new HashSet<>(expectedCommitIds);
                expectedCopy.removeAll(actualCommitIds);
                int commitLevelFn = expectedCopy.size();
                sumFn += commitLevelFn;

                int commitLevelTp = actualSize - commitLevelFp;
                sumTp += commitLevelTp;
            }
            writeToFile(String.format(RESULT_FINAL_CSV, getCodeElementName(), toolName), FINAL_RESULT_HEADER, String.format(FINAL_RESULT_FORMAT, toolName, oracleName, "commit", processingTimeAverage, processingTimeMedian, sumTp, sumFP, sumFn, ((double) sumTp / (sumTp + sumFP)) * 100, ((double) sumTp / (sumTp + sumFn)) * 100), StandardOpenOption.APPEND);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private String getSummaryResultFileName(String oracleName, String toolName) {
        return String.format(SUMMARY_RESULT_FILE_NAME_FORMAT, getCodeElementName(), toolName, toolName, oracleName);
    }

    protected void writeToSummaryFile(String oracleName, String toolName, String content) throws IOException {
        writeToFile(getSummaryResultFileName(oracleName, toolName), SUMMARY_RESULT_HEADER, content, StandardOpenOption.APPEND);
    }

    private String getDetailedResultFileName(String oracleName, String toolName) {
        return String.format(DETAILED_RESULT_FILE_NAME_FORMAT, getCodeElementName(), toolName, toolName, oracleName);
    }

    protected void writeToDetailedFile(String oracleName, String toolName, String oracleFileName,
                                       String repositoryWebURL, String elementKey, String parentCommitId, String commitId,
                                       long commitTime, String changeType, String elementFileBefore, String elementFileAfter,
                                       String elementNameBefore, String elementNameAfter, String resultType, String comment) throws IOException {

        writeToFile(getDetailedResultFileName(oracleName, toolName), DETAILED_RESULT_HEADER,
                getDetailedResultContent(oracleFileName, repositoryWebURL, elementKey, parentCommitId, commitId, commitTime, changeType, elementFileBefore, elementFileAfter, elementNameBefore, elementNameAfter, resultType, comment), StandardOpenOption.APPEND);
    }

    private String getDetailedResultContent(String oracleFileName, String repositoryWebURL, String elementKey, String parentCommitId, String commitId, long commitTime, String changeType, String elementFileBefore, String elementFileAfter, String elementNameBefore, String elementNameAfter, String resultType, String comment) {
        return String.format(DETAILED_CONTENT_FORMAT, oracleFileName, repositoryWebURL, elementKey, parentCommitId,
                commitId, commitTime, changeType, elementFileBefore, elementFileAfter, elementNameBefore, elementNameAfter,
                resultType, comment);
    }

}
