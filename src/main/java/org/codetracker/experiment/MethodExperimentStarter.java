package org.codetracker.experiment;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.graph.EndpointPair;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import org.codetracker.api.*;
import org.codetracker.element.Method;
import org.codetracker.experiment.oracle.ChangeHistory;
import org.codetracker.experiment.oracle.MethodHistoryInfo;
import org.codetracker.experiment.oracle.MethodOracle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.codetracker.util.FileUtil.createDirectory;
import static org.codetracker.util.FileUtil.writeToFile;

public class MethodExperimentStarter extends AbstractExperimentStarter {
    private static final String CODE_ELEMENT_NAME = "method";

    public static void main(String[] args) throws IOException {
        MethodExperimentStarter methodExperimentStarter = new MethodExperimentStarter();
        methodExperimentStarter.start();
    }

    public void start() throws IOException {
//        createDirectory(new String[]{"result", "result/method", "result/method/shovel", "result/method/shovel/test", "result/method/shovel/training"});
        String[] toolNames = new String[]{"tracker", "shovel", "shovel-oracle"};
        List<MethodOracle> oracles = MethodOracle.all();
        for (String toolName : toolNames) {
            for (MethodOracle oracle : oracles) {
                switch (toolName) {
                    case "tracker":
                        codeTracker(oracle, toolName);
                        calculateFinalResults(oracle.getName(), toolName);
                        break;
//                    case "shovel":
//                        codeShovel(oracle);
//                        calculateFinalResults(oracle.getName(), toolName);
//                        break;
//                    case "shovel-oracle":
//                        analyseCodeShovelOracle(oracle);
//                        break;
                }
            }
        }
    }

    private void codeTracker(MethodOracle methodOracle, String toolName) throws IOException {
        String oracleName = methodOracle.getName();
        GitService gitService = new GitServiceImpl();
        Set<String> processedFiles = getAllProcessedSamples(oracleName, toolName);
        for (Map.Entry<String, MethodHistoryInfo> entry : methodOracle.getOracle().entrySet()) {
            String fileName = entry.getKey();
            if (processedFiles.contains(fileName))
                continue;
            MethodHistoryInfo methodHistoryInfo = entry.getValue();
            String repositoryWebURL = methodHistoryInfo.getRepositoryWebURL();
            String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
            String projectDirectory = FOLDER_TO_CLONE + repositoryName;

            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                HashMap<String, ChangeHistory> oracleChanges = oracle(methodHistoryInfo.getExpectedChanges());


                long startTime = System.nanoTime();

                MethodTracker methodTracker = CodeTracker.methodTracker()
                        .repository(repository)
                        .filePath(methodHistoryInfo.getFilePath())
                        .startCommitId(methodHistoryInfo.getStartCommitId())
                        .methodName(methodHistoryInfo.getFunctionName())
                        .methodDeclarationLineNumber(methodHistoryInfo.getFunctionStartLine())
                        .build();
                History<Method> methodHistory = methodTracker.track();

                long refactoringMinerProcessingTime = (System.nanoTime() - startTime) / 1000000;

                HashMap<String, ChangeHistory> detectedChanges = new HashMap<>();
                HashMap<String, ChangeHistory> notDetectedChanges = new HashMap<>(oracleChanges);
                HashMap<String, ChangeHistory> falseDetectedChanges = processHistory(methodHistory);


                //TEMPv
                {
                    ObjectWriter writer = MAPPER.writer(new DefaultPrettyPrinter());
                    MethodHistoryInfo methodHistoryInfo2 = new MethodHistoryInfo();
                    methodHistoryInfo2.setRepositoryName(methodHistoryInfo.getRepositoryName());
                    methodHistoryInfo2.setRepositoryWebURL(methodHistoryInfo.getRepositoryWebURL());
                    methodHistoryInfo2.setFilePath(methodHistoryInfo.getFilePath());
                    methodHistoryInfo2.setFunctionName(methodHistoryInfo.getFunctionName());
                    methodHistoryInfo2.setFunctionKey(methodHistoryInfo.getFunctionKey());
                    methodHistoryInfo2.setFunctionStartLine(methodHistoryInfo.getFunctionStartLine());
                    methodHistoryInfo2.setStartCommitId(methodHistoryInfo.getStartCommitId());
                    methodHistoryInfo2.getExpectedChanges().addAll(falseDetectedChanges.values());
                    methodHistoryInfo2.getExpectedChanges().sort(Comparator.comparing(ChangeHistory::getCommitTime).reversed().thenComparing(ChangeHistory::getCommitId).thenComparing(ChangeHistory::getChangeType));
                    File newFile = new File(String.format("E:\\Data\\History\\method\\oracle\\%s\\%s", oracleName, fileName));
                    writer.writeValue(newFile, methodHistoryInfo2);
                }
                //TEMP^

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

                History.HistoryReport historyReport = methodHistory.getHistoryReport();
                content.append("\"")
                        .append(methodHistoryInfo.getFunctionKey()).append("\",")
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

                writeToSummaryFile(oracleName, toolName, content.toString());

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

                    writeToDetailedFile(oracleName, toolName, fileName, methodHistoryInfo.getRepositoryWebURL(),
                            methodHistoryInfo.getFunctionKey(), changeHistory.getParentCommitId(), changeHistory.getCommitId(),
                            changeHistory.getCommitTime(), changeHistory.getChangeType(), changeHistory.getElementFileBefore(),
                            changeHistory.getElementFileAfter(), changeHistory.getElementNameBefore(), changeHistory.getElementNameAfter()
                            , resultType, changeHistory.getComment()
                    );
                }
                writeToFile(getProcessedFilePath(oracleName, toolName), "file_name" + System.lineSeparator(), fileName + System.lineSeparator(), StandardOpenOption.APPEND);

            } catch (Exception exception) {
                try (FileWriter fw = new FileWriter(String.format(ERROR_FILE_NAME_FORMAT, CODE_ELEMENT_NAME, toolName, toolName, oracleName, fileName), false)) {
                    try (PrintWriter pw = new PrintWriter(fw)) {
                        pw.println(exception.getMessage());
                        pw.println("====================================================================================");
                        exception.printStackTrace(pw);
                    }
                }
            }
        }
    }

    private HashMap<String, ChangeHistory> processHistory(History<Method> history) {
        HashMap<String, ChangeHistory> historyChanges = new HashMap<>();
        if (history.getGraph() == null)
            return historyChanges;

        for (EndpointPair<Method> edge : history.getGraph().getEdges()) {
            Edge edgeValue = history.getGraph().getEdgeValue(edge).get();
            for (Change change : edgeValue.getChangeList()) {
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

    @Override
    protected String getCodeElementName() {
        return CODE_ELEMENT_NAME;
    }
}
