package org.codetracker.experiment;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.graph.EndpointPair;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import org.codetracker.HistoryImpl;
import org.codetracker.api.*;
import org.codetracker.element.Variable;
import org.codetracker.experiment.oracle.ChangeHistory;
import org.codetracker.experiment.oracle.VariableHistoryInfo;
import org.codetracker.experiment.oracle.VariableOracle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.codetracker.util.FileUtil.createDirectory;
import static org.codetracker.util.FileUtil.writeToFile;

public class VariableExperimentStarter extends AbstractExperimentStarter {


    private static final String TOOL_NAME = "tracker";
    private static final String CODE_ELEMENT_NAME = "variable";

    public static void main(String[] args) throws IOException {
        VariableExperimentStarter variableExperimentStarter = new VariableExperimentStarter();
        variableExperimentStarter.findOracleChanges();
        variableExperimentStarter.start();
    }

    @Override
    protected String getCodeElementName() {
        return CODE_ELEMENT_NAME;
    }

    public void start() throws IOException {
        createDirectory(new String[]{"result", "result/variable"});

        List<VariableOracle> oracles = VariableOracle.all();

        for (VariableOracle oracle : oracles) {
            codeTracker(oracle);
            calculateFinalResults(oracle.getName(), TOOL_NAME);
        }

    }

    public void findOracleChanges() throws IOException {
        List<VariableOracle> oracles = VariableOracle.all();
        for (VariableOracle oracle : oracles) {
            System.out.println(oracle.getName());
            System.out.println(oracle.getNumberOfInstancePerChangeKind());
            System.out.println("+++++++++++++++++++++++++++++++++");
        }

    }

    private void codeTracker(VariableOracle variableOracle) throws IOException {
        String oracleName = variableOracle.getName();
        GitService gitService = new GitServiceImpl();
        Set<String> processedFiles = getAllProcessedSamples(oracleName, TOOL_NAME);
        for (Map.Entry<String, VariableHistoryInfo> variableOracleInstance : variableOracle.getOracle().entrySet()) {
            String fileName = variableOracleInstance.getKey();
            if (processedFiles.contains(fileName))
                continue;
            VariableHistoryInfo variableHistoryInfo = variableOracleInstance.getValue();
            String repositoryWebURL = variableHistoryInfo.getRepositoryWebURL();
            String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
            String projectDirectory = FOLDER_TO_CLONE + repositoryName;

            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                HashMap<String, ChangeHistory> oracleChanges = oracle(variableHistoryInfo.getExpectedChanges());

                long startTime = System.nanoTime();

                VariableTracker variableTracker = CodeTracker.variableTracker()
                        .repository(repository)
                        .filePath(variableHistoryInfo.getFilePath())
                        .startCommitId(variableHistoryInfo.getStartCommitId())
                        .methodName(variableHistoryInfo.getFunctionName())
                        .methodDeclarationLineNumber(variableHistoryInfo.getFunctionStartLine())
                        .variableName(variableHistoryInfo.getVariableName())
                        .variableDeclarationLineNumber(variableHistoryInfo.getVariableStartLine())
                        .build();
                History<Variable> variableHistory = variableTracker.track();

                long refactoringMinerProcessingTime = (System.nanoTime() - startTime) / 1000000;

                HashMap<String, ChangeHistory> detectedChanges = new HashMap<>();
                HashMap<String, ChangeHistory> notDetectedChanges = new HashMap<>(oracleChanges);
                HashMap<String, ChangeHistory> falseDetectedChanges = processHistory((HistoryImpl<Variable>) variableHistory);

                //TEMPv
                {
                    ObjectWriter writer = MAPPER.writer(new DefaultPrettyPrinter());
                    VariableHistoryInfo variableHistoryInfo2 = new VariableHistoryInfo();
                    variableHistoryInfo2.setRepositoryName(variableHistoryInfo.getRepositoryName());
                    variableHistoryInfo2.setRepositoryWebURL(variableHistoryInfo.getRepositoryWebURL());
                    variableHistoryInfo2.setFilePath(variableHistoryInfo.getFilePath());
                    variableHistoryInfo2.setFunctionName(variableHistoryInfo.getFunctionName());
                    variableHistoryInfo2.setFunctionKey(variableHistoryInfo.getFunctionKey());
                    variableHistoryInfo2.setFunctionStartLine(variableHistoryInfo.getFunctionStartLine());
                    variableHistoryInfo2.setStartCommitId(variableHistoryInfo.getStartCommitId());
                    variableHistoryInfo2.setVariableName(variableHistoryInfo.getVariableName());
                    variableHistoryInfo2.setVariableKey(variableHistoryInfo.getVariableKey());
                    variableHistoryInfo2.setVariableStartLine(variableHistoryInfo.getVariableStartLine());
                    variableHistoryInfo2.getExpectedChanges().addAll(falseDetectedChanges.values());
                    variableHistoryInfo2.getExpectedChanges().sort(Comparator.comparing(ChangeHistory::getCommitTime).thenComparing(ChangeHistory::getCommitId).thenComparing(ChangeHistory::getChangeType).reversed());
                    File newFile = new File(String.format("E:\\Data\\History\\variable\\oracle\\%s\\%s", oracleName, fileName));
                    writer.writeValue(newFile, variableHistoryInfo2);
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

                History.HistoryReport historyReport = variableHistory.getHistoryReport();
                content.append("\"")
                        .append(variableHistoryInfo.getVariableKey()).append("\",")
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

                writeToFile(String.format(SUMMARY_RESULT_FILE_NAME_FORMAT, CODE_ELEMENT_NAME, TOOL_NAME, oracleName), SUMMARY_RESULT_HEADER, content.toString(), StandardOpenOption.APPEND);

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

                    writeToFile(String.format(DETAILED_RESULT_FILE_NAME_FORMAT, CODE_ELEMENT_NAME, TOOL_NAME, oracleName),
                            DETAILED_RESULT_HEADER,
                            String.format(DETAILED_CONTENT_FORMAT,
                                    variableHistoryInfo.getRepositoryWebURL(),
                                    variableHistoryInfo.getVariableKey(),
                                    changeHistory.getParentCommitId(),
                                    changeHistory.getCommitId(),
                                    changeHistory.getCommitTime(),
                                    changeHistory.getChangeType(),
                                    changeHistory.getElementFileBefore(),
                                    changeHistory.getElementFileAfter(),
                                    changeHistory.getElementNameBefore(),
                                    changeHistory.getElementNameAfter(),
                                    resultType,
                                    changeHistory.getComment()
                            ),
                            StandardOpenOption.APPEND);
                }
                writeToFile(getProcessedFilePath(oracleName, TOOL_NAME), "file_name" + System.lineSeparator(), fileName + System.lineSeparator(), StandardOpenOption.APPEND);
            } catch (Exception exception) {
                try (FileWriter fw = new FileWriter(String.format(ERROR_FILE_NAME_FORMAT, CODE_ELEMENT_NAME, TOOL_NAME, oracleName, fileName), false)) {
                    try (PrintWriter pw = new PrintWriter(fw)) {
                        pw.println(exception.getMessage());
                        pw.println("====================================================================================");
                        exception.printStackTrace(pw);
                    }
                }
            }

        }
    }

    private HashMap<String, ChangeHistory> processHistory(HistoryImpl<Variable> historyImpl) {
        HashMap<String, ChangeHistory> historyChanges = new HashMap<>();
        if (historyImpl.getGraph() == null)
            return historyChanges;

        Set<EndpointPair<Variable>> edges = historyImpl.getGraph().getEdges();

        for (EndpointPair<Variable> edge : edges) {
            Edge edgeValue = historyImpl.getGraph().getEdgeValue(edge).get();
            Set<Change> changeList = edgeValue.getChangeList();
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


}
