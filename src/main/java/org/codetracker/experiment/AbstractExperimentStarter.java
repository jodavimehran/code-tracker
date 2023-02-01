package org.codetracker.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.graph.EndpointPair;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.codetracker.HistoryImpl;
import org.codetracker.change.Change;
import org.codetracker.api.CodeElement;
import org.codetracker.api.Edge;
import org.codetracker.api.History;
import org.codetracker.experiment.oracle.history.AbstractHistoryInfo;
import org.codetracker.experiment.oracle.history.AttributeHistoryInfo;
import org.codetracker.experiment.oracle.history.ChangeHistory;
import org.codetracker.experiment.oracle.AbstractOracle;
import org.codetracker.experiment.oracle.history.ClassHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.codetracker.util.FileUtil.writeToFile;

public abstract class AbstractExperimentStarter {
    protected final static String FOLDER_TO_CLONE = "tmp/";
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

    protected static <T extends CodeElement> HashMap<String, ChangeHistory> processHistory(HistoryImpl<T> historyImpl) {
        HashMap<String, ChangeHistory> historyChanges = new HashMap<>();
        if (historyImpl.getGraph() == null)
            return historyChanges;

        Set<EndpointPair<T>> edges = historyImpl.getGraph().getEdges();

        for (EndpointPair<T> edge : edges) {
            Edge edgeValue = historyImpl.getGraph().getEdgeValue(edge).get();
            LinkedHashSet<Change> changeList = edgeValue.getChangeList();
            for (Change change : changeList) {
                if (Change.Type.NO_CHANGE.equals(change.getType()))
                    continue;
                ChangeHistory changeHistory = new ChangeHistory();

                String commitId = edge.target().getVersion().getId();
                changeHistory.setCommitId(commitId);
                changeHistory.setParentCommitId(edge.source().getVersion().getId());
                changeHistory.setCommitTime(edge.target().getVersion().getTime());

                String changeType = change.getType().getTitle();
                changeHistory.setChangeType(changeType);

                String leftFile = edge.source().getFilePath();
                changeHistory.setElementFileBefore(leftFile);
                String leftName = edge.source().getName();
                changeHistory.setElementNameBefore(leftName);

                String rightFile = edge.target().getFilePath();
                changeHistory.setElementFileAfter(rightFile);
                String rightName = edge.target().getName();
                changeHistory.setElementNameAfter(rightName);

                changeHistory.setComment(change.toString().replace("\t", " "));
                historyChanges.put(getChangeKey(change.getType(), commitId), changeHistory);
            }
        }
        return historyChanges;
    }

    private String getProcessedFilePath(String oracleName) {
        return String.format(PROCESSED_FILE_NAME_FORMAT, getCodeElementName(), getToolName(), getToolName(), oracleName);
    }

    protected Set<String> getAllProcessedSamples(String oracleName) throws IOException {
        String processedFilePath = getProcessedFilePath(oracleName);
        Path path = writeToFile(processedFilePath, "file-names" + System.lineSeparator(), null, StandardOpenOption.APPEND);
        return new HashSet<>(Files.readAllLines(path));
    }

    protected abstract String getCodeElementName();

    protected abstract String getToolName();

    protected void calculateFinalResults(String oracleName) {
        Map<String, Set<String>> commitLevelExpected = new HashMap<>();
        Map<String, Set<String>> commitLevelActual = new HashMap<>();
        try {
            List<Integer> processingTime = new ArrayList<>();
            List<String[]> summaryResults = readResults(getSummaryResultFileName(oracleName));
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
            writeToFile(String.format(RESULT_FINAL_CSV, getCodeElementName(), getToolName()), FINAL_RESULT_HEADER, String.format(FINAL_RESULT_FORMAT, getToolName(), oracleName, "change", processingTimeAverage, processingTimeMedian, tp, fp, fn, ((double) tp / (tp + fp)) * 100, ((double) tp / (tp + fn)) * 100), StandardOpenOption.APPEND);

            List<String[]> detailedResults = readResults(getDetailedResultFileName(oracleName));

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
            writeToFile(String.format(RESULT_FINAL_CSV, getCodeElementName(), getToolName()), FINAL_RESULT_HEADER, String.format(FINAL_RESULT_FORMAT, getToolName(), oracleName, "commit", processingTimeAverage, processingTimeMedian, sumTp, sumFP, sumFn, ((double) sumTp / (sumTp + sumFP)) * 100, ((double) sumTp / (sumTp + sumFn)) * 100), StandardOpenOption.APPEND);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private String getSummaryResultFileName(String oracleName) {
        return String.format(SUMMARY_RESULT_FILE_NAME_FORMAT, getCodeElementName(), getToolName(), getToolName(), oracleName);
    }

    protected void writeToSummaryFile(String oracleName, String content) throws IOException {
        writeToFile(getSummaryResultFileName(oracleName), SUMMARY_RESULT_HEADER, content, StandardOpenOption.APPEND);
    }

    private String getDetailedResultFileName(String oracleName) {
        return String.format(DETAILED_RESULT_FILE_NAME_FORMAT, getCodeElementName(), getToolName(), getToolName(), oracleName);
    }

    protected void writeToDetailedFile(String oracleName, String oracleFileName,
                                       String repositoryWebURL, String elementKey, String parentCommitId, String commitId,
                                       long commitTime, String changeType, String elementFileBefore, String elementFileAfter,
                                       String elementNameBefore, String elementNameAfter, String resultType, String comment) throws IOException {

        writeToFile(getDetailedResultFileName(oracleName), DETAILED_RESULT_HEADER,
                getDetailedResultContent(oracleFileName, repositoryWebURL, elementKey, parentCommitId, commitId, commitTime, changeType, elementFileBefore, elementFileAfter, elementNameBefore, elementNameAfter, resultType, comment), StandardOpenOption.APPEND);
    }

    private String getDetailedResultContent(String oracleFileName, String repositoryWebURL, String elementKey, String parentCommitId, String commitId, long commitTime, String changeType, String elementFileBefore, String elementFileAfter, String elementNameBefore, String elementNameAfter, String resultType, String comment) {
        return String.format(DETAILED_CONTENT_FORMAT, oracleFileName, repositoryWebURL, elementKey, parentCommitId,
                commitId, commitTime, changeType, elementFileBefore, elementFileAfter, elementNameBefore, elementNameAfter,
                resultType, comment);
    }

    protected <H extends AbstractHistoryInfo, E extends CodeElement> void codeTracker(AbstractOracle<H> oracle, CheckedBiFunction<H, Repository, History<E>> tracker) throws IOException {
        String oracleName = oracle.getName();
        GitService gitService = new GitServiceImpl();
        Set<String> processedFiles = getAllProcessedSamples(oracleName);
        for (Map.Entry<String, H> oracleInstance : oracle.getOracle().entrySet()) {
            String fileName = oracleInstance.getKey();
            if (processedFiles.contains(fileName))
                continue;
            H historyInfo = oracleInstance.getValue();
            String repositoryWebURL = historyInfo.getRepositoryWebURL();
            String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
            String projectDirectory = FOLDER_TO_CLONE + repositoryName;

            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                HashMap<String, ChangeHistory> oracleChanges = oracle(historyInfo.getExpectedChanges());

                long startTime = System.nanoTime();

                History<E> history = tracker.apply(historyInfo, repository);

                long refactoringMinerProcessingTime = (System.nanoTime() - startTime) / 1000000;

                HashMap<String, ChangeHistory> detectedChanges = new HashMap<>();
                HashMap<String, ChangeHistory> notDetectedChanges = new HashMap<>(oracleChanges);
                HashMap<String, ChangeHistory> falseDetectedChanges = processHistory((HistoryImpl<E>) history);

                for (Map.Entry<String, ChangeHistory> oracleChangeEntry : oracleChanges.entrySet()) {
                    String changeKey = oracleChangeEntry.getKey();
                    if (falseDetectedChanges.containsKey(changeKey)) {
                        detectedChanges.put(changeKey, falseDetectedChanges.get(changeKey));
                        notDetectedChanges.remove(changeKey);
                        falseDetectedChanges.remove(changeKey);
                    }
                }

                HashMap<String, ChangeHistory> allChanges = new HashMap<>();
                allChanges.putAll(detectedChanges);
                allChanges.putAll(notDetectedChanges);
                allChanges.putAll(falseDetectedChanges);
                StringBuilder content = new StringBuilder();

                History.HistoryReport historyReport = history.getHistoryReport();
                content.append("\"").append(historyInfo.getElementKey()).append("\",")
                        .append(refactoringMinerProcessingTime).append(",")
                        .append(historyReport.getAnalysedCommits()).append(",")
                        .append(historyReport.getGitLogCommandCalls()).append(",")
                        .append(historyReport.getStep2()).append(",")
                        .append(historyReport.getStep3()).append(",")
                        .append(historyReport.getStep4()).append(",")
                        .append(historyReport.getStep5()).append(",")
                ;

                for (Change.Type changeType : Change.Type.values()) {
                    if (Change.Type.NO_CHANGE.equals(changeType) || Change.Type.MULTI_CHANGE.equals(changeType) || Change.Type.REMOVED.equals(changeType))
                        continue;
                    long tp = detectedChanges.values().stream().filter(changeHistory -> changeHistory.getChangeType().equals(changeType.getTitle())).count();
                    long fn = notDetectedChanges.values().stream().filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle())).count();
                    long fp = falseDetectedChanges.values().stream().filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle())).count();

                    content.append(tp).append(",").append(fp).append(",").append(fn).append(",");
                }


                long tp = detectedChanges.size();
                long fn = notDetectedChanges.size();
                long fp = falseDetectedChanges.size();

                content.append(tp).append(",").append(fp).append(",").append(fn).append(System.lineSeparator());

                writeToSummaryFile(oracleName, content.toString());

                List<ChangeHistory> historyResults = allChanges.values().stream().sorted(Comparator.comparing(ChangeHistory::getCommitTime).reversed().thenComparing(ChangeHistory::getCommitId)).collect(Collectors.toList());
                for (ChangeHistory changeHistory : historyResults) {
                    String changeKey = getChangeKey(changeHistory);
                    String resultType;
                    if (detectedChanges.containsKey(changeKey))
                        resultType = "TP";
                    else if (notDetectedChanges.containsKey(changeKey))
                        resultType = "FN";
                    else if (falseDetectedChanges.containsKey(changeKey))
                        resultType = "FP";
                    else
                        resultType = "UN!";

                    writeToDetailedFile(oracleName, fileName, historyInfo.getRepositoryWebURL(),
                            historyInfo.getElementKey(), changeHistory.getParentCommitId(), changeHistory.getCommitId(),
                            changeHistory.getCommitTime(), changeHistory.getChangeType(), changeHistory.getElementFileBefore(),
                            changeHistory.getElementFileAfter(), changeHistory.getElementNameBefore(), changeHistory.getElementNameAfter()
                            , resultType, changeHistory.getComment()
                    );
                }
                writeToFile(getProcessedFilePath(oracleName), "file_name" + System.lineSeparator(), fileName + System.lineSeparator(), StandardOpenOption.APPEND);
            } catch (Exception exception) {
                try (FileWriter fw = new FileWriter(String.format(ERROR_FILE_NAME_FORMAT, getCodeElementName(), getToolName(), getToolName(), oracleName, fileName), false)) {
                    try (PrintWriter pw = new PrintWriter(fw)) {
                        pw.println(exception.getMessage());
                        pw.println("====================================================================================");
                        exception.printStackTrace(pw);
                    }
                }
            }

        }
    }

    @FunctionalInterface
    public interface CheckedBiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }
}
