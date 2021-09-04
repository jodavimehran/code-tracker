package org.refactoringrefiner.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felixgrund.codeshovel.entities.Yresult;
import com.felixgrund.codeshovel.execution.ShovelExecution;
import com.felixgrund.codeshovel.services.RepositoryService;
import com.felixgrund.codeshovel.services.impl.CachingRepositoryService;
import com.felixgrund.codeshovel.util.Utl;
import com.felixgrund.codeshovel.wrappers.StartEnvironment;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository;
import org.hibernate.SessionFactory;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.api.Change;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

enum Detector {
    MINER,
    MINER_ORACLE,
    SHOVEL,
    SHOVEL_ORACLE,
}

public class Starter {
    public static final String FINAL_RESULT_HEADER = "tool,oracle,level,processing_time_avg,processing_time_median,tp,fp,fn,precision,recall" + System.lineSeparator();
    public static final String FINAL_RESULT_FORMAT = "%s,%s,%s,%f,%d,%d,%d,%d,%f,%f" + System.lineSeparator();
    public static final String DETAILED_RESULT_HEADER = "repository,element_key,parent_commit_id,commit_id,commit_time, change_type,element_file_before,element_file_after,element_name_before,element_name_after,result" + System.lineSeparator();
    public static final String DETAILED_CONTENT_FORMAT = "\"%s\",\"%s\",%s,%s,%d,%s,%s,%s,\"%s\",\"%s\",%s" + System.lineSeparator();
    public static final String SUMMARY_RESULT_FILE_NAME_FORMAT = "result/method/summary-%s-%s.csv";
    public static final String DETAILED_RESULT_FILE_NAME_FORMAT = "result/method/detailed-%s-%s.csv";
    public static final String ERROR_FILE_NAME_FORMAT = "result/method/error-%s-%s-%s.txt";
    public static final String PROCESSED_FILE_NAME_FORMAT = "result/method/processed-%s-%s.csv";
    public static final String RESULT_FINAL_CSV = "result/method/final.csv";
    private final static String FOLDER_TO_CLONE = "H:\\Projects\\";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final static String SUMMARY_RESULT_HEADER;
    private final static String RESULT_HEADER_SHOVEL;
    static SessionFactory sessionFactoryObj;

    static {
        StringBuilder header = new StringBuilder();
        StringBuilder headerShovel = new StringBuilder();
        header.append("instance,processing_time,analysed_commits,git_log_command_calls,step2,step3,step4,step5,");
        headerShovel.append("instance,processing_time,");
        for (Change.Type changeType : Change.Type.values()) {
            if (Change.Type.NO_CHANGE.equals(changeType) || Change.Type.MULTI_CHANGE.equals(changeType) || Change.Type.REMOVED.equals(changeType))
                continue;
            header.append("tp_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
            header.append("fp_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
            header.append("fn_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");

            headerShovel.append("tp_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
            headerShovel.append("fp_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
            headerShovel.append("fn_").append(changeType.getTitle().toLowerCase().replace(" ", "_")).append(",");
        }

        header.append("tp_all,fp_all,fn_all").append(System.lineSeparator());
        headerShovel.append("tp_all,fp_all,fn_all").append(System.lineSeparator());
        SUMMARY_RESULT_HEADER = header.toString();
        RESULT_HEADER_SHOVEL = headerShovel.toString();
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void main(String[] args) throws Exception {
//        new Starter().historyTest("E:\\Data\\History\\processed.csv");


        Starter starter = new Starter();
//        starter.methodHistoryExperiment("E:\\Data\\History\\processed.csv");
//        starter.methodHistoryExperiment();
        //starter.correctDBType();
//        starter.countNumberOfCommit();
//        starter.variableHistoryTest("E:\\Data\\History\\Variable\\processed.csv");
//        starter.createVariableDataset();
        System.exit(0);
    }


    private static void createInput(String repositoryWebURL) throws Exception {
        GitService gitService = new GitServiceImpl();
//        String.format("result/miner_result_%s.csv", oracleName)

//        for (String repositoryWebURL : Files.readAllLines(Paths.get("C:\\Users\\asus\\Desktop\\repositories.csv"))) {
        String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
        String projectDirectory = FOLDER_TO_CLONE + repositoryName;
        String compareFile = "E:\\Data\\Compare\\" + repositoryName.replace("\\", "-").toLowerCase() + ".csv";
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
            try (Git git = new Git(repository)) {
                PullCommand pull = git.pull();
                Set<String> tags = git.tagList().call().stream().map(ref -> ref.getName().replace("refs/tags/", "")).collect(Collectors.toSet());
                GHRepository ghRepository = new GitHistoryRefactoringMinerImpl().getGitHubRepository(repositoryWebURL);
                Path path = Paths.get(compareFile);
                if (!path.toFile().exists()) {
                    Files.createFile(path);
                    writeToFile(compareFile, "repository_web_url,start_tag,end_tag,status,total_commit,ahead_by,behind_by" + System.lineSeparator(), "", StandardOpenOption.APPEND);
                }
                Set<String> processed = Files
                        .readAllLines(path)
                        .stream()
                        .map(s -> s.split(","))
                        .map(strings -> strings[1] + "..." + strings[2])
                        .collect(Collectors.toSet());
                for (String startTag : tags) {
                    for (String endTag : tags) {
                        String join = startTag + "..." + endTag;
                        if (!processed.contains(join)) {
                            System.out.println(join + "," + repositoryWebURL);
                            try {
                                GHCompare ghCompare = ghRepository.getCompare(startTag, endTag);
                                writeToFile(compareFile, String.format("%s,%s,%s,%s,%d,%d,%d" + System.lineSeparator(), repositoryWebURL, startTag, endTag, ghCompare.getStatus(), ghCompare.getTotalCommits(), ghCompare.getAheadBy(), ghCompare.getBehindBy()), StandardOpenOption.APPEND);
                                processed.add(join);
                                String reverse = endTag + "..." + startTag;
                                if (!join.equals(reverse)) {
                                    System.out.println(reverse + "," + repositoryWebURL);
                                    processed.add(reverse);
                                    GHCompare.Status reverseStatus = ghCompare.getStatus();
                                    switch (ghCompare.getStatus()) {
                                        case ahead:
                                            reverseStatus = GHCompare.Status.behind;
                                            break;
                                        case behind:
                                            reverseStatus = GHCompare.Status.ahead;
                                            break;
                                        case diverged:
                                        case identical:
                                    }

                                    writeToFile(compareFile, String.format("%s,%s,%s,%s,%d,%d,%d" + System.lineSeparator(), repositoryWebURL, endTag, startTag, reverseStatus, ghCompare.getBehindBy(), ghCompare.getBehindBy(), ghCompare.getAheadBy()), StandardOpenOption.APPEND);
                                    processed.add(reverse);
                                }
                            } catch (Exception ex) {
                                writeToFile(compareFile, String.format("%s,%s,%s,%s,%d,%d,%d" + System.lineSeparator(), repositoryWebURL, endTag, startTag, "error", -1, -1, -1), StandardOpenOption.APPEND);
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

//            }
        }
    }


    private static void createDirectory(String analysisDirectory) throws IOException {
        Path path = Paths.get(analysisDirectory);
        File directory = path.toFile();

        if (!directory.exists()) {
            Files.createDirectory(path);
        }
    }

//    private void createRawData() throws IOException {
//        StringBuilder result = new StringBuilder();
//        GitService gitService = new GitServiceImpl();
//        GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
//
//        result.append("repository, Tag Range, commit ID, File Name, Status, Lines Changed, Lines Added, Lines Deleted").append(System.lineSeparator());
//        for (Input input : getInputs()) {
//            String projectDirectory = FOLDER_TO_CLONE + input.getRepositoryWebURL().replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
//            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, input.getRepositoryWebURL())) {
//                GHRepository ghRepository = gitHistoryRefactoringMiner.getGhRepository(input.getRepositoryWebURL());
//                Iterable<RevCommit> revsWalkBetweenTags = gitService.createRevsWalkBetweenTags(repository, input.getStartTag(), input.getEndTag());
//                for (RevCommit revCommit : revsWalkBetweenTags) {
//                    String commitId = revCommit.getId().getName();
//                    GHCommit ghCommit = ghRepository.getCommit(commitId);
//                    for (GHCommit.File commitFile : ghCommit.getFiles()) {
//                        if (commitFile.getFileName().endsWith(".java")) {
//                            result.append(input.getRepositoryWebURL());
//                            result.append(",").append(input.getTagRange());
//                            result.append(",").append(commitId);
//                            result.append(",").append(commitFile.getFileName());
//                            result.append(",").append(commitFile.getStatus());
//                            result.append(",").append(commitFile.getLinesChanged());
//                            result.append(",").append(commitFile.getLinesAdded());
//                            result.append(",").append(commitFile.getLinesDeleted());
//                            result.append(System.lineSeparator());
//                        }
//                    }
//                }
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        writeToNewFile("C:\\Users\\asus\\Desktop\\raw_data.csv", result.toString());
//
//    }

//    private void analyse() throws IOException {
//        RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
//        GitService gitService = new GitServiceImpl();
//        for (Input input : getInputs("C:\\Users\\asus\\Desktop\\input.csv")) {
//            String projectDirectory = FOLDER_TO_CLONE + input.getRepositoryWebURL().replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
//
//            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, input.getRepositoryWebURL())) {
//                RefactoringResult refactoringResult = refactoringRefinerImpl.detect(repository, input.getStartTag(), input.getEndTag());
//
//                String analysisDirectory = projectDirectory + "\\analysis";
//                String tagDirectory = analysisDirectory + "\\" + input.getStartTag() + "-" + input.getEndTag();
//
//                createDirectory(analysisDirectory);
//                createDirectory(tagDirectory);
//                writeToNewFile(tagDirectory + "\\result.csv", getResults(refactoringResult));
//                writeToNewFile(tagDirectory + "\\01-refactoring-miner-all-commits.txt", refactoringResult.getRefactoringMinerAllCommits().stream().map(Refactoring::toString).collect(Collectors.joining(System.lineSeparator())));
//                writeToNewFile(tagDirectory + "\\02-refactoring-miner-first-last.txt", refactoringResult.getRefactoringMinerFirstAndLast().stream().map(Refactoring::toString).collect(Collectors.joining(System.lineSeparator())));
//                writeToNewFile(tagDirectory + "\\03-refactoring-refiner.txt", refactoringResult.getRefactoringRefiner().stream().flatMap(aggregatedRefactoring -> aggregatedRefactoring.getRefactorings().stream()).map(Refactoring::toString).collect(Collectors.joining(System.lineSeparator())));
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//    }

//    private List<RawData> getRawData() throws IOException {
//        List<RawData> inputs = Files.readAllLines(Paths.get("C:\\Users\\asus\\Desktop\\raw_data.csv"))
//                .stream()
//                .skip(1)
//                .map(row -> {
//                    String[] split = row.split(",");
//                    return new RawData(split[0], split[1], split[2], split[3], split[4], Integer.valueOf(split[5]), Integer.valueOf(split[6]), Integer.valueOf(split[7]));
//                })
//                .collect(Collectors.toList());
//        return inputs;
//    }

    private static void writeToNewFile(String pathString, String header, String content) throws IOException {
        writeToFile(pathString, header, content, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeToFile(String pathString, String content, StandardOpenOption standardOpenOption) throws IOException {
        writeToFile(pathString, null, content, standardOpenOption);
    }

    private static Path writeToFile(String pathString, String header, String content, StandardOpenOption standardOpenOption) throws IOException {
        Path path = Paths.get(pathString);
        if (!path.toFile().exists()) {
            Files.createFile(path);
            if (header != null)
                Files.write(path, header.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (content != null) {
            Files.write(path, content.getBytes(), standardOpenOption);
        }
        return path;
    }

    public static String getNumber(Map<RefactoringType, Long> map, RefactoringType refactoringType) {
        if (map.containsKey(refactoringType)) {
            return map.get(refactoringType).toString();
        } else {
            return "0";
        }
    }


    private static Yresult runShovelExecution(
            String repositoryPathGit,
            String repositoryName,
            String startCommitName,
            String filepath,
            String methodName,
            String outFilePath,
            int startLine
    ) throws Exception {
        // Unix vs. Windows. Probably there is a better way to do this.
        String pathDelimiter = repositoryPathGit.contains("\\") ? "\\" : "/";
        // Repo paths need to reference the .git directory. We add it to the path if it's not provided.
        String gitPathEnding = "/.git";
        if (!repositoryPathGit.endsWith(gitPathEnding)) {
            repositoryPathGit += gitPathEnding;
        }
        Repository repository = Utl.createRepository(repositoryPathGit);
        Git git = new Git(repository);
        RepositoryService repositoryService = new CachingRepositoryService(git, repository, repositoryName, repositoryPathGit);
        com.felixgrund.codeshovel.wrappers.Commit startCommit = repositoryService.findCommitByName(startCommitName);

        StartEnvironment startEnv = new StartEnvironment(repositoryService);
        startEnv.setRepositoryPath(repositoryPathGit);
        startEnv.setFilePath(filepath);
        startEnv.setFunctionName(methodName);
        startEnv.setFunctionStartLine(startLine);
        startEnv.setStartCommitName(startCommitName);
        startEnv.setStartCommit(startCommit);
        startEnv.setFileName(Utl.getFileName(startEnv.getFilePath()));
        startEnv.setOutputFilePath(outFilePath);

        return ShovelExecution.runSingle(startEnv, startEnv.getFilePath(), true);
    }

    private static Change.Type getChangeType(String ychange) {
        switch (ychange) {
            case "Ybodychange":
                return Change.Type.BODY_CHANGE;
            case "Yparametermetachange":
            case "Yparameterchange":
                return Change.Type.PARAMETER_CHANGE;
            case "Yfilerename":
                return Change.Type.CONTAINER_CHANGE;
            case "Ymodifierchange":
                return Change.Type.MODIFIER_CHANGE;
            case "Yintroduced":
                return Change.Type.INTRODUCED;
            case "Yexceptionschange":
                return Change.Type.EXCEPTION_CHANGE;
            case "Ymovefromfile":
                return Change.Type.METHOD_MOVE;
            case "Yrename":
                return Change.Type.RENAME;
            case "Yreturntypechange":
                return Change.Type.RETURN_TYPE_CHANGE;
        }
        throw new RuntimeException(ychange + " UNKNOWN CHANGE TYPE!!!!!!");
    }

    private static Set<String> getAllProcessedSamples(String processedFilePath) throws IOException {
        Path path = writeToFile(processedFilePath, "file-names" + System.lineSeparator(), null, StandardOpenOption.APPEND);
        return new HashSet<>(Files.readAllLines(path));
    }

    public static Set<String> merge(Set<String> a, Set<String> b) {
        a.addAll(b);
        return a;
    }

    private static Set<Change.Type> shovelChangeToChange(String change) {
        if (change.startsWith("Ymultichange")) {
            return Arrays.stream(change.replace("Ymultichange(", "").replace(")", "").split(",")).map(Starter::getChangeType).collect(Collectors.toSet());
        }
        Set<Change.Type> result = new HashSet<>();
        result.add(getChangeType(change));
        return result;
    }

    private String convertChangeTypeToChangeKind(String input, String desc) {
        switch (input) {
            case "changed body": {
                if (desc != null && desc.startsWith("The documentation"))
                    return "documentation change";
                else
                    return "body change";
            }
            case "changed parameter":
                return "parameter change";
            case "changed modifier":
                return "modifier change";
            case "changed container":
                return "container change";
            case "changed annotation":
                return "annotation change";

            case "added":
                return "introduced";
            case "moved":
                return "method move";
            case "change thrown exception":
                return "exception change";
            case "renamed":
                return "rename";
            case "changed return type":
                return "return type change";
        }
        throw new RuntimeException("invalid " + input);
    }

//    private void methodHistoryExperiment() throws Exception {
//        String[] neededDirectories = new String[]{"result", "result/method", "result/method/shovel", "result/method/shovel/test", "result/method/shovel/training"};
//        for (String directoryName : neededDirectories) {
//            Path directoryPath = Paths.get(directoryName);
//            if (!directoryPath.toFile().exists())
//                Files.createDirectories(directoryPath);
//        }
//        String[] toolNames = new String[]{
//                "tracker",
////                "tracker-naive",
////                "shovel",
////                "shovel-oracle"
//        };
//        List<MethodOracle> oracles = MethodOracle.all();
//        for (String toolName : toolNames) {
//            for (MethodOracle oracle : oracles) {
//                switch (toolName) {
//                    case "tracker":
//                        codeTracker(oracle, false);
//                        calculateFinalResults(oracle.getName(), toolName);
//                        break;
//                    case "tracker-naive":
//                        codeTracker(oracle, true);
//                        calculateFinalResults(oracle.getName(), toolName);
//                        break;
//                    case "shovel":
//                        codeShovel(oracle);
//                        calculateFinalResults(oracle.getName(), toolName);
//                        break;
//                    case "shovel-oracle":
//                        analyseCodeShovelOracle(oracle);
//                        calculateFinalResults(oracle.getName(), toolName);
//                        break;
//                }
//            }
//        }
//    }

    private void calculateFinalResults(String oracleName, String toolName) {
        Map<String, Set<String>> commitLevelExpected = new HashMap<>();
        Map<String, Set<String>> commitLevelActual = new HashMap<>();
        try {
            List<Integer> processingTime = new ArrayList<>();
            List<String[]> summaryResults = readResults(String.format(SUMMARY_RESULT_FILE_NAME_FORMAT, toolName, oracleName));
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
            writeToFile(RESULT_FINAL_CSV, FINAL_RESULT_HEADER, String.format(FINAL_RESULT_FORMAT, toolName, oracleName, "change", processingTimeAverage, processingTimeMedian, tp, fp, fn, ((double) tp / (tp + fp)) * 100, ((double) tp / (tp + fn)) * 100), StandardOpenOption.APPEND);

            List<String[]> detailedResults = readResults(String.format(DETAILED_RESULT_FILE_NAME_FORMAT, toolName, oracleName));

            for (String[] result : detailedResults) {
                String elementKey = result[1];
                String commitId = result[3];
                if (result[result.length - 1].equals("TP")) {
                    commitLevelExpected.get(elementKey).add(commitId);
                    commitLevelActual.get(elementKey).add(commitId);
                }
                if (result[result.length - 1].equals("FN")) {
                    commitLevelExpected.get(elementKey).add(commitId);
                }
                if (result[result.length - 1].equals("FP")) {
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
            writeToFile(RESULT_FINAL_CSV, FINAL_RESULT_HEADER, String.format(FINAL_RESULT_FORMAT, toolName, oracleName, "commit", processingTimeAverage, processingTimeMedian, sumTp, sumFP, sumFn, ((double) sumTp / (sumTp + sumFP)) * 100, ((double) sumTp / (sumTp + sumFn)) * 100), StandardOpenOption.APPEND);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


//                        =============================================================================================
//                    UMLModel startModel = refactoringMiner.getUMLModel(historyInfo.getStartCommitName(), Collections.singletonList(historyInfo.getFilePath()));
//                    Method startMethod = RefactoringMiner.getMethodByName(startModel, refactoringMiner.getVersion(historyInfo.getStartCommitName()), historyInfo.getFunctionKey());
//                    if (startMethod == null)
//                        continue;
//                    for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
//                        if (!variableDeclaration.isParameter())
//                            continue;
//                        HistoryInfo variableHistoryInfo = new HistoryInfo();
//                        variableHistoryInfo.setRepositoryName(repositoryName);
//                        variableHistoryInfo.setRepositoryWebURL(repositoryWebURL);
//                        variableHistoryInfo.setFilePath(startMethod.getFilePath());
//                        variableHistoryInfo.setFunctionName(startMethod.getUmlOperation().getName());
//                        variableHistoryInfo.setFunctionKey(startMethod.getName());
//                        variableHistoryInfo.setFunctionStartLine(historyInfo.getFunctionStartLine());
//
//                        variableHistoryInfo.setVariableName(variableDeclaration.getVariableName());
//                        int startLine = variableDeclaration.getLocationInfo().getStartLine();
//                        variableHistoryInfo.setVariableKey(String.format("%s$%s(%d)", historyInfo.getFunctionKey(), variableDeclaration.getVariableName(), startLine));
//                        variableHistoryInfo.setVariableStartLine(startLine);
//
//                        variableHistoryInfo.setStartCommitName(historyInfo.getStartCommitName());
//
//                        HistoryResult methodIntroducedCommit = getMethodIntroducedCommit(sessionObj, repositoryWebURL, historyInfo.getFunctionKey());
//
//                        boolean addedFound = false;
//                        if (methodIntroducedCommit != null) {
//                            String commitId = methodIntroducedCommit.getElementVersionIdAfter();
//                            UMLModel addedModel = refactoringMiner.getUMLModel(commitId, Collections.singletonList(methodIntroducedCommit.getElementFileAfter()));
//                            Method addedMethod = RefactoringMiner.getMethodByName(addedModel, refactoringMiner.getVersion(commitId), methodIntroducedCommit.getElementNameAfter());
//                            if (addedMethod != null) {
//                                for (VariableDeclaration addedVariableDeclaration : addedMethod.getUmlOperation().getAllVariableDeclarations()) {
//                                    if (addedVariableDeclaration.getVariableName().equals(variableDeclaration.getVariableName())) {
//                                        variableHistoryInfo.getExpectedResult().put(commitId, "added");
//                                        addedFound = true;
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                        if (!addedFound) {
//                            List<HistoryResult> methodChangedCommits = getMethodChangedCommits(sessionObj, repositoryWebURL, historyInfo.getFunctionKey());
//                            for (HistoryResult historyResult : methodChangedCommits) {
//                                if (addedFound)
//                                    break;
//                                UMLModel changedBodyModelRight = refactoringMiner.getUMLModel(historyResult.getElementVersionIdAfter(), Collections.singletonList(historyResult.getElementFileAfter()));
//                                Method changedBodyMethodRight = RefactoringMiner.getMethodByName(changedBodyModelRight, refactoringMiner.getVersion(historyResult.getElementVersionIdAfter()), historyResult.getElementNameAfter());
//
//                                UMLModel changedBodyModelLeft = refactoringMiner.getUMLModel(historyResult.getElementVersionIdBefore(), Collections.singletonList(historyResult.getElementFileBefore()));
//                                Method changedBodyMethodLeft = RefactoringMiner.getMethodByName(changedBodyModelLeft, refactoringMiner.getVersion(historyResult.getElementVersionIdBefore()), historyResult.getElementNameBefore());
//                                if (changedBodyMethodLeft == null || changedBodyMethodRight == null)
//                                    continue;
//                                UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(changedBodyMethodLeft.getUmlOperation(), changedBodyMethodRight.getUmlOperation(), null);
//                                umlOperationBodyMapper.getRefactorings();
//                                for (VariableDeclaration addedVariableDeclaration : umlOperationBodyMapper.getAddedVariables().stream().map(Pair::getLeft).collect(Collectors.toList())) {
//                                    if (addedVariableDeclaration.getVariableName().equals(variableDeclaration.getVariableName())) {
//                                        variableHistoryInfo.getExpectedResult().put(historyResult.getElementVersionIdAfter(), "added");
//                                        addedFound = true;
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//
//                        File newFile = new File(resultFolder.getPath() + "\\" + file.getName().replace(".json", "") + "-" + variableDeclaration.getVariableName() + ".json");
//                        int i = 1;
//                        while (newFile.exists()) {
//                            newFile = new File(resultFolder.getPath() + "\\" + file.getName().replace(".json", "") + "-" + variableDeclaration.getVariableName() + i + ".json");
//                            i++;
//                        }
//                        writer.writeValue(newFile, variableHistoryInfo);
//                        writeToFile(finishedFilePath, newFile.getName() + ", " + (addedFound ? "found" : "not found") + System.lineSeparator(), StandardOpenOption.APPEND);
//                    }


//                    HistoryResult methodIntroducedCommit = getMethodIntroducedCommit(repositoryWebURL, historyInfo.getFunctionKey());
//                    if(methodIntroducedCommit!=null) {
//                        UMLModel addedModel = refactoringMiner.getUMLModel(methodIntroducedCommit.getElementVersionIdAfter(), Collections.singletonList(methodIntroducedCommit.getElementFileAfter()));
//                        Method addedMethod = RefactoringMiner.getMethodByName(addedModel, refactoringMiner.getVersion(methodIntroducedCommit.getElementVersionIdAfter()), methodIntroducedCommit.getElementNameAfter());
//                        UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(addedMethod.getUmlOperation(), startMethod.getUmlOperation(), null);
//                        System.out.println("");
//                    }else {
//                        System.out.println(file.getName());
//                    }
//                }
//            }
//            writeToFile(processedFilePath, file.getName() + System.lineSeparator(), StandardOpenOption.APPEND);
//        }
//    }

//}

    private List<String[]> readResults(String fileName) throws IOException, CsvException {
        FileReader filereader = new FileReader(fileName);
        CSVReader csvReader = new CSVReaderBuilder(filereader).withSkipLines(1).build();
        return csvReader.readAll();
    }

//    private void countNumberOfCommit() throws Exception {
//        File historyFolder = new File("H:\\Projects\\ataraxie\\codeshovel\\src\\test\\resources\\oracles\\java\\test");
//        Map<String, Integer> byType = new HashMap<>();
//        for (File file : historyFolder.listFiles()) {
//            VariableHistoryInfo historyInfo = mapper.readValue(file, VariableHistoryInfo.class);
//            for (Map.Entry<String, String> entry : historyInfo.getExpectedResult().entrySet()) {
//                String key = entry.getValue().startsWith("Ymultichange") ? "Ymultichange" : entry.getValue();
//                byType.merge(key, 1, Integer::sum);
//            }
//        }
//        System.out.println();
//    }

//    private void variableHistoryTest(String processedFilePath) throws Exception {
//        Session sessionObj = null;
//        try {
//            sessionObj = buildSessionFactory().openSession();
//            Set<String> processedFiles = getAllProcessedSamples(processedFilePath);
//            File historyFolder = new File(Starter.class.getClassLoader().getResource("history/variable/training").getFile());
//
//            List<HistoryResult> allVariableHistory = getAllVariableHistory(sessionObj);
//            HashMap<String, HashMap<String, HashMap<String, HistoryResult>>> allResults = getHistoryResultMap(allVariableHistory);
//
//            for (File file : historyFolder.listFiles()) {
//                if (processedFiles.contains(file.getName()))
//                    continue;
//
//                VariableHistoryInfo historyInfo = mapper.readValue(file, VariableHistoryInfo.class);
//
//                String repositoryWebURL = historyInfo.getRepositoryWebURL();
//                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
//                String projectDirectory = FOLDER_TO_CLONE + repositoryName;
//
//                try {
//                    RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
//                    HistoryImpl<CodeElement, Edge> variableHistory = (HistoryImpl<CodeElement, Edge>) refactoringRefinerImpl.findVariableHistory(projectDirectory, repositoryWebURL, historyInfo.getStartCommitName(), historyInfo.getFilePath(), historyInfo.getFunctionKey(), historyInfo.getVariableName(), historyInfo.getVariableStartLine());
//                    HashMap<String, HistoryResult> historyResultHashMap = allResults.getOrDefault(repositoryWebURL, new HashMap<>()).getOrDefault(String.format("%s$%s", historyInfo.getFunctionKey(), historyInfo.getVariableKey()), new HashMap<>());
//                    for (Map.Entry<String, String> entry : historyInfo.getExpectedResult().entrySet()) {
//                        String key = String.format("%s-%s", entry.getKey(), entry.getValue());
//                        if (!historyResultHashMap.containsKey(key)) {
//                            historyResultHashMap.put(key, new HistoryResult());
//                        }
//                        HistoryResult historyResult = historyResultHashMap.get(key);
//                        historyResult.setOracle("refactoring-miner-training");
//                        historyResult.setCodeShovelOracleVote(1);
//                    }
//                    processHistory(variableHistory, historyResultHashMap, repositoryWebURL, String.format("%s$%s(%d)", historyInfo.getFunctionKey(), historyInfo.getVariableName(), historyInfo.getVariableStartLine()), Detector.MINER, "variable");
//                    saveAllResultsToDatabase(historyResultHashMap.values(), sessionObj);
////                    writeToFile(processedFilePath, file.getName() + System.lineSeparator(), StandardOpenOption.APPEND);
//
//                } catch (Exception exception) {
//                    exception.printStackTrace();
//                }
//            }
//        } catch (Exception sqlException) {
//            if (null != sessionObj.getTransaction()) {
//                System.out.println("\n.......Transaction Is Being Rolled Back.......");
//                sessionObj.getTransaction().rollback();
//            }
//            sqlException.printStackTrace();
//        } finally {
//            if (sessionObj != null) {
//                sessionObj.close();
//            }
//        }
//    }


//    private void codeShovelOracle(VariableHistoryInfo historyInfo, String repositoryWebURL, HashMap<String, HistoryResult> historyResultHashMap) {
//        for (Map.Entry<String, String> entry : historyInfo.getExpectedResult().entrySet()) {
//            if (entry.getValue().equals("Ynochange"))
//                continue;
//            List<String> changes = new ArrayList<>();
//            if (entry.getValue().contains("Ymultichange")) {
//                for (String change : entry.getValue().replace("Ymultichange(", "").replace(")", "").split(",")) {
//                    changes.add(change);
//                }
//            } else {
//                changes.add(entry.getValue());
//            }
//            for (String ychange : changes) {
//                Change.Type changeType = getChangeType(ychange);
//                String commitId = entry.getKey();
//
//                addHistoryResult(historyResultHashMap,
//                        changeType,
//                        commitId,
//                        historyInfo.getFunctionKey(),
//                        "method",
//                        Detector.SHOVEL_ORACLE,
//                        ychange,
//                        repositoryWebURL,
//                        null,
//                        null,
//                        null,
//                        null,
//                        null,
//                        commitId,
//                        -1,
//                        -1);
//            }
//        }
//    }

//    private void codeTracker(MethodOracle methodOracle, boolean naive) throws IOException {
//        String oracleName = methodOracle.getName();
//        String toolName = "tracker" + (naive ? "-naive" : "");
//        String processedFilePath = String.format(PROCESSED_FILE_NAME_FORMAT, toolName, oracleName);
//        Set<String> processedFiles = getAllProcessedSamples(processedFilePath);
//        for (Map.Entry<String, MethodHistoryInfo> entry : methodOracle.getOracle().entrySet()) {
//            String fileName = entry.getKey();
//            try {
//                if (processedFiles.contains(fileName))
//                    continue;
//                MethodHistoryInfo methodHistoryInfo = entry.getValue();
//                String repositoryWebURL = methodHistoryInfo.getRepositoryWebURL();
//                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
//                String projectDirectory = FOLDER_TO_CLONE + repositoryName;
//
//                HashMap<String, HistoryResult> historyResultHashMap = new HashMap<>();
//                oracle(methodHistoryInfo, historyResultHashMap, false);
//
//
//                RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
//
//                HistoryImpl<CodeElement, Edge> historyImpl;
//                long startTime = System.nanoTime();
//                if (naive) {
//                    historyImpl = (HistoryImpl<CodeElement, Edge>) refactoringRefinerImpl.findMethodHistory2(projectDirectory, repositoryWebURL, methodHistoryInfo.getStartCommitId(), methodHistoryInfo.getFilePath(), methodHistoryInfo.getFunctionKey());
//                } else {
//                    historyImpl = (HistoryImpl<CodeElement, Edge>) refactoringRefinerImpl.findMethodHistory(projectDirectory, repositoryWebURL, methodHistoryInfo.getStartCommitId(), methodHistoryInfo.getFilePath(), methodHistoryInfo.getFunctionKey());
//                }
//
//                long refactoringMinerProcessingTime = (System.nanoTime() - startTime) / 1000000;
//                processHistory(historyImpl, historyResultHashMap, repositoryWebURL, methodHistoryInfo.getFunctionKey(), Detector.MINER, "method");
//
//
//                //TEMPv
////                ObjectWriter writer = MAPPER.writer(new DefaultPrettyPrinter());
////                methodHistoryInfo.getExpectedChanges().clear();
////                for (HistoryResult documentChangeResult : historyResultHashMap.values().stream().filter(historyResult -> historyResult.getRefactoringMinerVote() == 1).collect(Collectors.toList())) {
////                    documentChangeResult.setRefactoringMinerOracleVote(1);
////                    ChangeHistory changeHistory = new ChangeHistory();
////                    changeHistory.setChangeType(documentChangeResult.getChangeType());
////                    changeHistory.setCommitId(documentChangeResult.getElementVersionIdAfter());
////                    changeHistory.setCommitTime(documentChangeResult.getElementVersionTimeAfter());
////                    changeHistory.setElementFileAfter(documentChangeResult.getElementFileAfter());
////                    changeHistory.setElementFileBefore(documentChangeResult.getElementFileBefore());
////                    changeHistory.setElementNameAfter(documentChangeResult.getElementNameAfter());
////                    changeHistory.setElementNameBefore(documentChangeResult.getElementNameBefore());
////                    changeHistory.setParentCommitId(documentChangeResult.getElementVersionIdBefore());
////
////                    methodHistoryInfo.getExpectedChanges().add(changeHistory);
////                }
////                methodHistoryInfo.setRepositoryName(repositoryWebURL.substring(repositoryWebURL.lastIndexOf("/") + 1).replace(".git", ""));
////                methodHistoryInfo.getExpectedChanges().sort(Comparator.comparing(ChangeHistory::getCommitTime).thenComparing(ChangeHistory::getCommitId).thenComparing(ChangeHistory::getChangeType).reversed());
////                File newFile = new File(String.format("E:\\Data\\History\\method\\oracle\\%s\\%s", oracleName, fileName));
////                writer.writeValue(newFile, methodHistoryInfo);
//                //TEMP^
//
//                StringBuilder content = new StringBuilder();
//
//                History.HistoryReport historyReport = historyImpl.getHistoryReport();
//                content.append("\"")
//                        .append(methodHistoryInfo.getFunctionKey()).append("\",")
//                        .append(refactoringMinerProcessingTime).append(",")
//                        .append(historyReport.getAnalysedCommits()).append(",")
//                        .append(historyReport.getGitLogCommandCalls()).append(",")
//                        .append(historyReport.getStep2()).append(",")
//                        .append(historyReport.getStep3()).append(",")
//                        .append(historyReport.getStep4()).append(",")
//                        .append(historyReport.getStep5()).append(",")
//                ;
//                Set<Map.Entry<String, HistoryResult>> resultEntrySet = historyResultHashMap.entrySet();
//                for (Change.Type changeType : Change.Type.values()) {
//                    if (Change.Type.NO_CHANGE.equals(changeType) || Change.Type.MULTI_CHANGE.equals(changeType) || Change.Type.REMOVED.equals(changeType))
//                        continue;
//                    long tp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle()) && historyResult.getRefactoringMinerVote() == 1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                    long fn = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle()) && historyResult.getRefactoringMinerVote() == -1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                    long fp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle()) && historyResult.getRefactoringMinerVote() == 1 && historyResult.getRefactoringMinerOracleVote() == -1).count();
//
//                    content.append(tp).append(",").append(fp).append(",").append(fn).append(",");
//                }
//
//
//                long tp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getRefactoringMinerVote() == 1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                long fn = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getRefactoringMinerVote() == -1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                long fp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getRefactoringMinerVote() == 1 && historyResult.getRefactoringMinerOracleVote() == -1).count();
//
//                content.append(tp).append(",").append(fp).append(",").append(fn).append(System.lineSeparator());
//
//                writeToFile(String.format(SUMMARY_RESULT_FILE_NAME_FORMAT, toolName, oracleName), SUMMARY_RESULT_HEADER, content.toString(), StandardOpenOption.APPEND);
//
//                List<HistoryResult> historyResults = resultEntrySet.stream().map(Map.Entry::getValue).sorted(Comparator.comparing(HistoryResult::getElementVersionIdAfter).reversed()).collect(Collectors.toList());
//                for (HistoryResult historyResult : historyResults) {
//                    String resultType;
//                    if (historyResult.getRefactoringMinerVote() == 1 && historyResult.getRefactoringMinerOracleVote() == 1)
//                        resultType = "TP";
//                    else if (historyResult.getRefactoringMinerVote() == -1 && historyResult.getRefactoringMinerOracleVote() == 1)
//                        resultType = "FN";
//                    else if (historyResult.getRefactoringMinerVote() == 1 && historyResult.getRefactoringMinerOracleVote() == -1)
//                        resultType = "FP";
//                    else
//                        resultType = "UN!";
//
//                    writeToFile(String.format(DETAILED_RESULT_FILE_NAME_FORMAT, toolName, oracleName),
//                            DETAILED_RESULT_HEADER,
//                            String.format(DETAILED_CONTENT_FORMAT,
//                                    historyResult.getRepository(), historyResult.getElementKey(), historyResult.getElementVersionIdBefore(), historyResult.getElementVersionIdAfter(), historyResult.getElementVersionTimeAfter(), historyResult.getChangeType(),
//                                    historyResult.getElementFileBefore(), historyResult.getElementFileAfter(), historyResult.getElementNameBefore(), historyResult.getElementNameAfter(), resultType
//                            ),
//                            StandardOpenOption.APPEND);
//                }
//                writeToFile(processedFilePath, "file_name" + System.lineSeparator(), fileName + System.lineSeparator(), StandardOpenOption.APPEND);
//            } catch (Exception exception) {
//                try (FileWriter fw = new FileWriter(String.format(ERROR_FILE_NAME_FORMAT, toolName, oracleName, fileName), false)) {
//                    try (PrintWriter pw = new PrintWriter(fw)) {
//                        pw.println(exception.getMessage());
//                        pw.println("====================================================================================");
//                        exception.printStackTrace(pw);
//                    }
//                }
//            }
//        }
//    }

//    private void codeShovel(MethodOracle methodOracle) throws Exception {
//        String oracleName = methodOracle.getName();
//        String toolName = "shovel";
//        String processedFilePath = String.format(PROCESSED_FILE_NAME_FORMAT, toolName, oracleName);
//        Set<String> processedFiles = getAllProcessedSamples(processedFilePath);
//        for (Map.Entry<String, MethodHistoryInfo> oracleEntry : methodOracle.getOracle().entrySet()) {
//            String fileName = oracleEntry.getKey();
//            try {
//                if (processedFiles.contains(fileName))
//                    continue;
//                MethodHistoryInfo methodHistoryInfo = oracleEntry.getValue();
//                String repositoryWebURL = methodHistoryInfo.getRepositoryWebURL();
//                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
//                String projectDirectory = FOLDER_TO_CLONE + repositoryName;
//
//                HashMap<String, HistoryResult> historyResultHashMap = new HashMap<>();
//                oracle(methodHistoryInfo, historyResultHashMap, true);
//
//                long startTime = System.nanoTime();
//
//                Yresult yresult = runShovelExecution(projectDirectory,
//                        methodHistoryInfo.getRepositoryName(),
//                        methodHistoryInfo.getStartCommitId(),
//                        methodHistoryInfo.getFilePath(),
//                        methodHistoryInfo.getFunctionName(),
//                        String.format("result/shovel/%s/%s", oracleName, fileName),
//                        methodHistoryInfo.getFunctionStartLine()
//                );
//                long codeShovelProcessingTime = (System.nanoTime() - startTime) / 1000000;
//                for (Map.Entry<String, Ychange> entry : yresult.entrySet()) {
//                    if (entry.getValue() instanceof Ynochange)
//                        continue;
//                    List<Ychange> changes = new ArrayList<>();
//                    if (entry.getValue() instanceof Ymultichange) {
//                        Ymultichange ymultichange = (Ymultichange) entry.getValue();
//                        changes.addAll(ymultichange.getChanges());
//                    } else {
//                        changes.add(entry.getValue());
//                    }
//                    for (Ychange ychange : changes) {
//                        Change.Type changeType = getChangeType(ychange.getTypeAsString());
//                        String commitId = entry.getKey();
//                        String elementFileBefore = null;
//                        String elementFileAfter = null;
//
//                        String elementNameBefore = null;
//                        String elementNameAfter = null;
//
//                        String elementVersionIdBefore = null;
//                        String elementVersionIdAfter = null;
//
//                        long elementVersionTimeBefore = 0;
//                        long elementVersionTimeAfter = 0;
//
//                        if (ychange instanceof Ycomparefunctionchange) {
//                            Ycomparefunctionchange ycomparefunctionchange = (Ycomparefunctionchange) ychange;
//                            elementFileBefore = ycomparefunctionchange.getOldFunction().getSourceFilePath();
//                            elementFileAfter = ycomparefunctionchange.getNewFunction().getSourceFilePath();
//
//                            elementNameBefore = ycomparefunctionchange.getOldFunction().getId();
//                            elementNameAfter = ycomparefunctionchange.getNewFunction().getId();
//
//                            elementVersionIdBefore = ycomparefunctionchange.getOldFunction().getCommitName();
//                            elementVersionIdAfter = ycomparefunctionchange.getNewFunction().getCommitName();
//
//                            elementVersionTimeBefore = ycomparefunctionchange.getOldFunction().getCommit().getCommitTime();
//                            elementVersionTimeAfter = ycomparefunctionchange.getNewFunction().getCommit().getCommitTime();
//                        } else if (ychange instanceof Yintroduced) {
//                            Yintroduced yintroduced = (Yintroduced) ychange;
//
//                            elementFileBefore = yintroduced.getNewFunction().getSourceFilePath();
//                            elementFileAfter = yintroduced.getNewFunction().getSourceFilePath();
//
//                            elementNameBefore = yintroduced.getNewFunction().getId();
//                            elementNameAfter = yintroduced.getNewFunction().getId();
//
//                            elementVersionIdBefore = yintroduced.getNewFunction().getCommitName();
//                            elementVersionIdAfter = entry.getKey();
//
//                            elementVersionTimeBefore = yintroduced.getNewFunction().getCommit().getCommitTime();
//                            elementVersionTimeAfter = yintroduced.getNewFunction().getCommit().getCommitTime();
//                        }
//
//                        addHistoryResult(historyResultHashMap,
//                                changeType,
//                                commitId,
//                                methodHistoryInfo.getFunctionKey(),
//                                "method",
//                                Detector.SHOVEL,
//                                ychange.toString(),
//                                repositoryWebURL,
//                                elementFileBefore,
//                                elementFileAfter,
//                                elementNameBefore,
//                                elementNameAfter,
//                                elementVersionIdBefore,
//                                elementVersionIdAfter,
//                                elementVersionTimeBefore,
//                                elementVersionTimeAfter);
//                    }
//                }
//
//
//                StringBuilder content = new StringBuilder();
//                content.append("\"")
//                        .append(methodHistoryInfo.getFunctionKey()).append("\",")
//                        .append(codeShovelProcessingTime).append(",")
//                ;
//                Set<Map.Entry<String, HistoryResult>> resultEntrySet = historyResultHashMap.entrySet();
//                for (Change.Type changeType : Change.Type.values()) {
//                    if (Change.Type.NO_CHANGE.equals(changeType) || Change.Type.MULTI_CHANGE.equals(changeType) || Change.Type.REMOVED.equals(changeType))
//                        continue;
//                    long tp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle()) && historyResult.getCodeShovelVote() == 1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                    long fn = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle()) && historyResult.getCodeShovelVote() == -1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                    long fp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getChangeType().equals(changeType.getTitle()) && historyResult.getCodeShovelVote() == 1 && historyResult.getRefactoringMinerOracleVote() == -1).count();
//                    content.append(tp).append(",").append(fp).append(",").append(fn).append(",");
//                }
//
//                long tp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getCodeShovelVote() == 1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                long fn = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getCodeShovelVote() == -1 && historyResult.getRefactoringMinerOracleVote() == 1).count();
//                long fp = resultEntrySet.stream().map(Map.Entry::getValue).filter(historyResult -> historyResult.getCodeShovelVote() == 1 && historyResult.getRefactoringMinerOracleVote() == -1).count();
//
//                content.append(tp).append(",").append(fp).append(",").append(fn).append(System.lineSeparator());
//
//                writeToFile(String.format(SUMMARY_RESULT_FILE_NAME_FORMAT, toolName, oracleName), RESULT_HEADER_SHOVEL, content.toString(), StandardOpenOption.APPEND);
//
//                List<HistoryResult> historyResults = resultEntrySet.stream().map(Map.Entry::getValue).sorted(Comparator.comparing(HistoryResult::getElementVersionIdAfter).reversed()).collect(Collectors.toList());
//                for (HistoryResult historyResult : historyResults) {
//                    String resultType;
//                    if (historyResult.getCodeShovelVote() == 1 && historyResult.getRefactoringMinerOracleVote() == 1)
//                        resultType = "TP";
//                    else if (historyResult.getCodeShovelVote() == -1 && historyResult.getRefactoringMinerOracleVote() == 1)
//                        resultType = "FN";
//                    else if (historyResult.getCodeShovelVote() == 1 && historyResult.getRefactoringMinerOracleVote() == -1)
//                        resultType = "FP";
//                    else
//                        resultType = "UN!";
//
//                    writeToFile(String.format(DETAILED_RESULT_FILE_NAME_FORMAT, toolName, oracleName),
//                            DETAILED_RESULT_HEADER,
//                            String.format(DETAILED_CONTENT_FORMAT,
//                                    historyResult.getRepository(), historyResult.getElementKey(), historyResult.getElementVersionIdBefore(), historyResult.getElementVersionIdAfter(), historyResult.getElementVersionTimeAfter(), historyResult.getChangeType(),
//                                    historyResult.getElementFileBefore(), historyResult.getElementFileAfter(), historyResult.getElementNameBefore(), historyResult.getElementNameAfter(), resultType
//                            ),
//                            StandardOpenOption.APPEND);
//                }
//
//                writeToFile(processedFilePath, "file_name" + System.lineSeparator(), fileName + System.lineSeparator(), StandardOpenOption.APPEND);
//            } catch (Exception exception) {
//                try (FileWriter fw = new FileWriter(String.format(ERROR_FILE_NAME_FORMAT, toolName, oracleName, fileName), false)) {
//                    try (PrintWriter pw = new PrintWriter(fw)) {
//                        exception.printStackTrace(pw);
//                    }
//                }
//            }
//        }
//    }

//    private void processHistory(HistoryImpl<CodeElement, Edge> historyImpl, HashMap<String, HistoryResult> result, String repositoryWebURL, String elementKey, Detector detector, String elementType) {
//        if (historyImpl.getGraph() == null)
//            return;
//        Set<EndpointPair<CodeElement>> edges = historyImpl.getGraph().getEdges();
//
//        for (EndpointPair<CodeElement> edge : edges) {
//            EdgeImpl edgeValue = (EdgeImpl) historyImpl.getGraph().getEdgeValue(edge).get();
//            for (Change change : edgeValue.getChangeList()) {
//                if (Change.Type.NO_CHANGE.equals(change.getType()))
//                    continue;
//                Change.Type changeType = change.getType();
//
//                String commitId = edge.target().getVersion().getId();
//
//                String elementFileBefore = edge.source().getFilePath();
//                String elementFileAfter = edge.target().getFilePath();
//
//                String elementNameBefore = edge.source().getName();
//                String elementNameAfter = edge.target().getName();
//
//                String elementVersionIdBefore = edge.source().getVersion().getId();
//                String elementVersionIdAfter = commitId;
//
//                long elementVersionTimeBefore = edge.source().getVersion().getTime();
//                long elementVersionTimeAfter = edge.target().getVersion().getTime();
//
//                addHistoryResult(result,
//                        changeType,
//                        commitId,
//                        elementKey,
//                        elementType,
//                        detector,
//                        change.toString(),
//                        repositoryWebURL,
//                        elementFileBefore,
//                        elementFileAfter,
//                        elementNameBefore,
//                        elementNameAfter,
//                        elementVersionIdBefore,
//                        elementVersionIdAfter,
//                        elementVersionTimeBefore,
//                        elementVersionTimeAfter);
//
//            }
//        }
//    }

    private String getChangeKey(Change.Type changeType, String commitId) {
        return String.format("%s-%s", commitId, changeType);
    }

//    private ChangeType getChangeType(Ychange ychange) {
//        return getChangeType(ychange.getTypeAsString());
//    }

//    private void addHistoryResult(HashMap<String, HistoryResult> result,
//                                  Change.Type changeType,
//                                  String commitId,
//                                  String elementKey,
//                                  String elementType,
//                                  Detector detector,
//                                  String desc,
//                                  String repositoryWebURL,
//                                  String elementFileBefore,
//                                  String elementFileAfter,
//                                  String elementNameBefore,
//                                  String elementNameAfter,
//                                  String elementVersionIdBefore,
//                                  String elementVersionIdAfter,
//                                  long elementVersionTimeBefore,
//                                  long elementVersionTimeAfter
//    ) {
//        if ((Detector.SHOVEL.equals(detector) || Detector.SHOVEL_ORACLE.equals(detector)) && Change.Type.BODY_CHANGE.equals(changeType)) {
//            String changeDocumentationKey = getChangeKey(Change.Type.DOCUMENTATION_CHANGE, commitId);
//            if (result.containsKey(changeDocumentationKey)) {
//                switch (detector) {
//                    case SHOVEL_ORACLE:
//                        result.get(changeDocumentationKey).setCodeShovelOracleVote(1);
//                        break;
//                    case SHOVEL:
//                        result.get(changeDocumentationKey).setCodeShovelVote(1);
//                        break;
//                }
//
//            }
//        }
//        String changeKey = getChangeKey(changeType, commitId);
//        HistoryResult historyResult;
//        if (result.containsKey(changeKey)) {
//            historyResult = result.get(changeKey);
//        } else {
//            if ((Detector.SHOVEL.equals(detector) || Detector.SHOVEL_ORACLE.equals(detector)) && Change.Type.BODY_CHANGE.equals(changeType)) {
//                String changeDocumentationKey = getChangeKey(Change.Type.DOCUMENTATION_CHANGE, commitId);
//                if (result.containsKey(changeDocumentationKey)) {
//                    return;
//                }
//            }
//            historyResult = new HistoryResult();
//            result.put(changeKey, historyResult);
//        }
//        historyResult.setElementType(elementType);
//        historyResult.setRepository(repositoryWebURL);
//        historyResult.setChangeType(changeType.getTitle());
//        historyResult.setElementKey(elementKey);
//        if (detector != null) {
//            switch (detector) {
//                case MINER:
//                    if (historyResult.getRefactoringMinerVote() != 1) {
//                        historyResult.setRefactoringMinerVote(1);
//                        if (historyResult.getRefactoringMinerDesc() != null)
//                            historyResult.setRefactoringMinerDesc(historyResult.getRefactoringMinerDesc() + ";" + desc);
//                        else
//                            historyResult.setRefactoringMinerDesc(desc);
//
//                        historyResult.setElementFileBefore(elementFileBefore);
//                        historyResult.setElementFileAfter(elementFileAfter);
//                        historyResult.setElementNameBefore(elementNameBefore);
//                        historyResult.setElementNameAfter(elementNameAfter);
//                        historyResult.setElementVersionIdBefore(elementVersionIdBefore);
//                        historyResult.setElementVersionIdAfter(elementVersionIdAfter);
//                        historyResult.setElementVersionTimeBefore(elementVersionTimeBefore);
//                        historyResult.setElementVersionTimeAfter(elementVersionTimeAfter);
//                    }
//                    break;
//                case SHOVEL:
//                    if (historyResult.getCodeShovelVote() != 1) {
//                        historyResult.setCodeShovelVote(1);
//                        if (historyResult.getCodeShovelDesc() != null)
//                            historyResult.setCodeShovelDesc(historyResult.getCodeShovelDesc() + ";" + desc);
//                        else
//                            historyResult.setCodeShovelDesc(desc);
//                    }
//                    break;
//                case SHOVEL_ORACLE:
//                    historyResult.setCodeShovelOracleVote(1);
//                    break;
//                case MINER_ORACLE: {
//                    historyResult.setRefactoringMinerOracleVote(1);
//                    historyResult.setElementFileBefore(elementFileBefore);
//                    historyResult.setElementFileAfter(elementFileAfter);
//                    historyResult.setElementNameBefore(elementNameBefore);
//                    historyResult.setElementNameAfter(elementNameAfter);
//                    historyResult.setElementVersionIdBefore(elementVersionIdBefore);
//                    historyResult.setElementVersionIdAfter(elementVersionIdAfter);
//                    historyResult.setElementVersionTimeBefore(elementVersionTimeBefore);
//                    historyResult.setElementVersionTimeAfter(elementVersionTimeAfter);
//                    break;
//                }
//            }
//        }
//
//        if (historyResult.getElementFileBefore() == null)
//            historyResult.setElementFileBefore(elementFileBefore);
//        if (historyResult.getElementFileAfter() == null)
//            historyResult.setElementFileAfter(elementFileAfter);
//
//        if (historyResult.getElementNameBefore() == null)
//            historyResult.setElementNameBefore(elementNameBefore);
//        if (historyResult.getElementNameAfter() == null)
//            historyResult.setElementNameAfter(elementNameAfter);
//
//        if (historyResult.getElementVersionIdBefore() == null)
//            historyResult.setElementVersionIdBefore(elementVersionIdBefore);
//        if (historyResult.getElementVersionIdAfter() == null)
//            historyResult.setElementVersionIdAfter(elementVersionIdAfter);
//
//        if (historyResult.getElementVersionTimeBefore() <= 0)
//            historyResult.setElementVersionTimeBefore(elementVersionTimeBefore);
//        if (historyResult.getElementVersionTimeAfter() <= 0)
//            historyResult.setElementVersionTimeAfter(elementVersionTimeAfter);
//    }
}

