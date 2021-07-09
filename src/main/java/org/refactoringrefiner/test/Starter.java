package org.refactoringrefiner.test;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.graph.EndpointPair;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.HistoryImpl;
import org.refactoringrefiner.RefactoringMiner;
import org.refactoringrefiner.RefactoringRefinerImpl;
import org.refactoringrefiner.api.Change;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.RefactoringRefiner;
import org.refactoringrefiner.edge.EdgeImpl;
import org.refactoringrefiner.edge.Refactored;
import org.refactoringrefiner.element.Attribute;
import org.refactoringrefiner.element.Method;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Starter {
    private final static String FOLDER_TO_CLONE = "H:\\Projects\\";
    private static final ObjectMapper mapper = new ObjectMapper();
    static SessionFactory sessionFactoryObj;

    public static void main(String[] args) throws Exception {
//        new Starter().historyTest("E:\\Data\\History\\processed.csv");
        //new Starter().correctDBType();
        new Starter().variableHistoryTest("E:\\Data\\History\\Variable\\processed.csv");
        //new Starter().createVariableDataset();
        System.exit(0);
    }


    private static void historyTestAttribute() throws Exception {

        Set<String> processedFiles = new HashSet<>(Files.readAllLines(Paths.get("E:\\Data\\History\\Attribute\\processed.csv")));
        ClassLoader classLoader = Starter.class.getClassLoader();
        File historyFolder = new File(classLoader.getResource("history/attribute").getFile());
        RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
        for (File file : historyFolder.listFiles()) {
            if (processedFiles.contains(file.getName()))
                continue;
            AttributeHistoryInfo attributeHistoryInfo = mapper.readValue(file, AttributeHistoryInfo.class);
            String repositoryWebURL = attributeHistoryInfo.getRepositoryWebURL();
            String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
            String projectDirectory = FOLDER_TO_CLONE + repositoryName;
            String filePath = attributeHistoryInfo.getFilePath();
            String elementKey = attributeHistoryInfo.getAttributeKey();
            String startCommit = attributeHistoryInfo.getStartCommitName();

            System.out.printf("Start processing [%s,%s,%s,%s]%n", repositoryWebURL, filePath, elementKey, startCommit);

            HistoryImpl historyImpl = (HistoryImpl) refactoringRefinerImpl.findHistory(projectDirectory, repositoryWebURL, startCommit, filePath, elementKey, RefactoringRefiner.CodeElementType.ATTRIBUTE, false);
            Set<Attribute> nodeList = historyImpl.getGraph().getNodeList();
            System.out.println(nodeList);

        }
    }

    private static void createInput(String repositoryWebURL) throws Exception {
        GitService gitService = new GitServiceImpl();

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
                    writeToFile(compareFile, "repository_web_url,start_tag,end_tag,status,total_commit,ahead_by,behind_by" + System.lineSeparator(), StandardOpenOption.APPEND);
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

    private static RawData createRawData(Input input) {
        System.out.println(input.toString());
        GitService gitService = new GitServiceImpl();

        String projectDirectory = FOLDER_TO_CLONE + input.getRepositoryWebURL().replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, input.getRepositoryWebURL())) {
            try (Git git = new Git(repository)) {
                git.pull().call();
            }
            RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
            RefactoringResult refactoringResult = refactoringRefinerImpl.detect(repository, input.getStartTag(), input.getEndTag());
//            saveAllResultsToDatabase(refactoringResult.getResults(input));

            String analysisDirectory = projectDirectory + "\\analysis";
            String tagDirectory = analysisDirectory + "\\" + input.getStartTag() + "-" + input.getEndTag();

            createDirectory(analysisDirectory);
            createDirectory(tagDirectory);
            writeToNewFile(tagDirectory + "\\result.csv", getResults(refactoringResult));
            writeToNewFile(tagDirectory + "\\01-refactoring-miner-all-commits.json", "{ \"refactorings\": [" + refactoringResult.getRefactoringMinerAllCommits().stream().map(Refactoring::toString).collect(Collectors.joining(",")) + "]}");
            writeToNewFile(tagDirectory + "\\02-refactoring-miner-first-last.json", "{ \"refactorings\": [" + refactoringResult.getRefactoringMinerFirstAndLast().stream().map(Refactoring::toString).collect(Collectors.joining(",")) + "]}");
            writeToNewFile(tagDirectory + "\\03-refactoring-refiner.json", "{ \"refactorings\": [" + refactoringResult.getRefactoringRefiner().stream().map(Refactoring::toJSON).collect(Collectors.joining(",")) + "]}");

            RawData rawData = new RawData(input.getRepositoryWebURL(), input.getStartTag(), input.getEndTag(), refactoringResult);
            return rawData;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static AnalyzeResult processRawData(RawData rawData) {
        return new AnalyzeResult(rawData.getRepository(), rawData.getStartTag(), rawData.getEndTag(), rawData.getRefactoringResult().getNumberOfCommits(), rawData.getRefactoringResult().getSameCodeElementChangeRate(),
                rawData.getRefactoringResult().getDistanceRMACRR(), rawData.getRefactoringResult().getDistanceRMFLCRR(), rawData.getRefactoringResult().getDistanceRMFLCRMAC(),
                rawData.getRefactoringResult().getRefactoringMinerAllCommitProcessTime(), rawData.getRefactoringResult().getRefactoringMinerFirstLastCommitProcessTime(), rawData.getRefactoringResult().getRefactoringRefinerProcessTime());
    }

    private static Set<Input> getInputs(String path) {
        try {
            Set<Input> inputs = Files
                    .readAllLines(Paths.get(path))
                    .stream()
                    .skip(1)
                    .map(row -> {
                        String[] split = row.split(",");
                        return new Input(split[0], split[1], split[2]);
                    })
                    .collect(Collectors.toSet());
            return inputs;
        } catch (IOException inputs) {
            return Collections.EMPTY_SET;
        }
    }

    private static void createDirectory(String analysisDirectory) throws IOException {
        Path path = Paths.get(analysisDirectory);
        File directory = path.toFile();

        if (!directory.exists()) {
            Files.createDirectory(path);
        }
    }

    private static void writeToNewFile(String pathString, String content) throws IOException {
        writeToFile(pathString, content, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeToFile(String pathString, String content, StandardOpenOption standardOpenOption) throws IOException {
        Path path = Paths.get(pathString);
        if (!path.toFile().exists())
            Files.createFile(path);
        Files.write(path, content.getBytes(), standardOpenOption);
    }

    public static String getNumber(Map<RefactoringType, Long> map, RefactoringType refactoringType) {
        if (map.containsKey(refactoringType)) {
            return map.get(refactoringType).toString();
        } else {
            return "0";
        }
    }

    public static String getResults(RefactoringResult refactoringResult) {
        Map<RefactoringType, Long> refactoringRefiner = refactoringResult.getRefactoringRefiner().stream()
                .collect(Collectors.groupingBy(Refactoring::getRefactoringType, Collectors.counting()));

        Map<RefactoringType, Long> refactoringRefinerUsedToAggregate = refactoringResult.getRefactoringRefiner().stream()
                .collect(Collectors.groupingBy(Refactoring::getRefactoringType, Collectors.counting()));


        Map<RefactoringType, Long> refactoringMinerAllCommits = refactoringResult.getRefactoringMinerAllCommits().stream()
                .collect(Collectors.groupingBy(Refactoring::getRefactoringType, Collectors.counting()));

        Map<RefactoringType, Long> refactoringMinerFirstAndLast = refactoringResult.getRefactoringMinerFirstAndLast().stream()
                .collect(Collectors.groupingBy(Refactoring::getRefactoringType, Collectors.counting()));

        StringBuilder result = new StringBuilder();
        result.append("refactoringType, refactoringMiner(all Commits), refactoringMiner(First And Last), refactoringRefiner, refactoringRefiner (used to aggregate)").append(System.lineSeparator());
        for (RefactoringType refactoringType : RefactoringType.values()) {
            result.append(refactoringType.getDisplayName()).append(",");
            result.append(getNumber(refactoringMinerAllCommits, refactoringType)).append(",");
            result.append(getNumber(refactoringMinerFirstAndLast, refactoringType)).append(",");
            result.append(getNumber(refactoringRefiner, refactoringType)).append(",");
            result.append(getNumber(refactoringRefinerUsedToAggregate, refactoringType)).append(System.lineSeparator());
        }
        return result.toString();
    }

    private static void compareRefactorings(String leftSide, String rightSide) throws IOException {
        Map<String, List<String>> leftSideMap = Files.readAllLines(Paths.get(leftSide)).stream().collect(Collectors.groupingBy(String::toString));
        Map<String, List<String>> rightSideMap = Files.readAllLines(Paths.get(rightSide)).stream().collect(Collectors.groupingBy(String::toString));

        Set<String> leftSideNotMapped = leftSideMap
                .entrySet()
                .stream()
                .filter(stringListEntry -> !(rightSideMap.containsKey(stringListEntry.getKey()) && rightSideMap.get(stringListEntry.getKey()).size() == stringListEntry.getValue().size()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<String> rightSideNotMapped = rightSideMap
                .entrySet()
                .stream()
                .filter(stringListEntry -> !(leftSideMap.containsKey(stringListEntry.getKey()) && leftSideMap.get(stringListEntry.getKey()).size() == stringListEntry.getValue().size()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

    }

    private static SessionFactory buildSessionFactory() {
        // Creating Configuration Instance & Passing Hibernate Configuration File
        Configuration configObj = new Configuration();
        configObj.configure("hibernate.cfg.xml");
        configObj.addAnnotatedClass(Result.class);
        configObj.addAnnotatedClass(HistoryResult.class);

        // Since Hibernate Version 4.x, ServiceRegistry Is Being Used
        ServiceRegistry serviceRegistryObj = new StandardServiceRegistryBuilder().applySettings(configObj.getProperties()).build();

        // Creating Hibernate SessionFactory Instance
        sessionFactoryObj = configObj.buildSessionFactory(serviceRegistryObj);
        return sessionFactoryObj;
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

    private static <T> void saveAllResultsToDatabase(Collection<HistoryResult> results, Session sessionObj) {
        System.out.println(".......Start Saving Data to Database.......\n");
        try {

            sessionObj.beginTransaction();

            for (HistoryResult result : results) {
                if (result.getId() == 0)
                    sessionObj.save(result);
                sessionObj.update(result);
            }
            System.out.println("\n.......Records Saved Successfully To The Database.......\n");

            // Committing The Transactions To The Database
            sessionObj.getTransaction().commit();
        } catch (Exception sqlException) {
            if (null != sessionObj.getTransaction()) {
                System.out.println("\n.......Transaction Is Being Rolled Back.......");
                sessionObj.getTransaction().rollback();
            }
            sqlException.printStackTrace();
        }
    }

    private static List<HistoryResult> getAllMethodHistory(Session sessionObj) {
        return getAllHistory(sessionObj, "SELECT hr FROM HistoryResult hr WHERE hr.elementType = 'method'");
    }

    private static List<HistoryResult> getAllVariableHistory(Session sessionObj) {
        return getAllHistory(sessionObj, "SELECT hr FROM HistoryResult hr WHERE hr.elementType = 'variable'");
    }

    private static HistoryResult getMethodIntroducedCommit(Session sessionObj, String repository, String elementKey) {
        List<HistoryResult> allHistory = getAllHistory(sessionObj, String.format("SELECT hr" +
                "            FROM HistoryResult hr" +
                "            WHERE hr.repository = '%s' and hr.elementType = 'method' and hr.elementKey = '%s' and hr.changeType = 'added' and hr.finalDecision = 1", repository, elementKey));
        if (allHistory.isEmpty())
            return null;

        return allHistory.get(0);
    }

    private static List<HistoryResult> getMethodChangedBodyCommits(Session sessionObj, String repository, String elementKey) {
        List<HistoryResult> allHistory = getAllHistory(sessionObj,
                String.format("SELECT hr" +
                        "            FROM HistoryResult hr" +
                        "            WHERE hr.repository = '%s' and hr.elementType = 'method' and hr.elementKey = '%s' and hr.changeType = 'changed body' and hr.finalDecision = 1" +
                        "            ORDER BY hr.elementVersionTimeAfter", repository, elementKey));

        return allHistory;
    }

    private static List<HistoryResult> getAllHistory(Session sessionObj, String query) {

        sessionObj = buildSessionFactory().openSession();
        List<HistoryResult> results = null;
        results = sessionObj.createQuery(query).list();

        if (results == null)
            return Collections.emptyList();
        return results;

    }

    private static ChangeType getChangeTypeRefDiff(Change change) {
        if (change.getDescription() == null)
            throw new RuntimeException(change.getType().getTitle() + " UNKNOWN CHANGE TYPE!!!!!!");
        if (Change.Type.ADDED.equals(change.getType())) {
            return ChangeType.ADDED;
        }
        if (Change.Type.REMOVED.equals(change.getType())) {
            return ChangeType.REMOVED;
        }
        String refDiffChange = change.getDescription().split(";")[0];
        switch (refDiffChange) {
            case "move":
            case "pull_up":
            case "push_down":
                return ChangeType.MOVED;
            case "rename":
                return ChangeType.RENAMED;
            case "change_signature":
                return ChangeType.CHANGED_PARAMETER;
        }
        throw new RuntimeException(change.getType().getTitle() + " UNKNOWN CHANGE TYPE!!!!!!");
    }

//    private static Yresult runShovelExecution(
//            String repositoryPathGit,
//            String repositoryName,
//            String startCommitName,
//            String filepath,
//            String methodName,
//            String outFilePath,
//            int startLine
//    ) throws Exception {
//        // Unix vs. Windows. Probably there is a better way to do this.
//        String pathDelimiter = repositoryPathGit.contains("\\") ? "\\" : "/";
//        // Repo paths need to reference the .git directory. We add it to the path if it's not provided.
//        String gitPathEnding = pathDelimiter + ".git";
//        if (!repositoryPathGit.endsWith(gitPathEnding)) {
//            repositoryPathGit += gitPathEnding;
//        }
//        Repository repository = Utl.createRepository(repositoryPathGit);
//        Git git = new Git(repository);
//        RepositoryService repositoryService = new CachingRepositoryService(git, repository, repositoryName, repositoryPathGit);
//        Commit startCommit = repositoryService.findCommitByName(startCommitName);
//
//        StartEnvironment startEnv = new StartEnvironment(repositoryService);
//        startEnv.setRepositoryPath(repositoryPathGit);
//        startEnv.setFilePath(filepath);
//        startEnv.setFunctionName(methodName);
//        startEnv.setFunctionStartLine(startLine);
//        startEnv.setStartCommitName(startCommitName);
//        startEnv.setStartCommit(startCommit);
//        startEnv.setFileName(Utl.getFileName(startEnv.getFilePath()));
//        startEnv.setOutputFilePath(outFilePath);
//
//        return ShovelExecution.runSingle(startEnv, startEnv.getFilePath(), true);
//    }

    private static ChangeType getChangeTypeAPIDiff(Change change) {
        if (change.getDescription() == null)
            throw new RuntimeException(change.getType().getTitle() + " UNKNOWN CHANGE TYPE!!!!!!");
        if (Change.Type.CONTAINER_CHANGE.equals(change.getType())) {
            return ChangeType.CHANGED_CONTAINER;
        }
        if (Change.Type.ADDED.equals(change.getType())) {
            return ChangeType.ADDED;
        }
        if (Change.Type.REMOVED.equals(change.getType())) {
            return ChangeType.REMOVED;
        }
        String apiDiffChange = change.getDescription().split(":")[0];
        switch (apiDiffChange) {
            case "Move Method":
            case "Pull Up Method":
            case "Push Down Method":
                return ChangeType.MOVED;

            case "Rename Method":
                return ChangeType.RENAMED;
            case "Remove Method":
                return ChangeType.REMOVED;
            case "Change in Parameter List":
                return ChangeType.CHANGED_PARAMETER;
            case "Change in Exception List":
                return ChangeType.CHANGED_THROWN_EXCEPTION;
            case "Change in Return Type Method":
                return ChangeType.CHANGED_RETURN_TYPE;
            case "Gain Visibility in Method":
            case "Lost Visibility in Method":
            case "Remove Final Modifier in Method":
            case "Add Final Modifier in Method":
            case "Remove Static Modifier in Method":
            case "Add Static Modifier in Method":
                return ChangeType.CHANGED_MODIFIER;
            case "Deprecated Method":
                return ChangeType.CHANGED_ANNOTATION;
            case "Add Method":
                return ChangeType.ADDED;
        }
        throw new RuntimeException(change.getType().getTitle() + " UNKNOWN CHANGE TYPE!!!!!!");
    }

    private static ChangeType getChangeTypeRefactoringMiner(Change change) {
        switch (change.getType()) {
            case MODIFIED:
                return ChangeType.CHANGED_BODY;
            case CONTAINER_CHANGE:
                return ChangeType.CHANGED_CONTAINER;
            case REFACTORED:
                return getChangeType(((Refactored) change).getRefactoring());
            case ADDED:
            case EXTRACTED:
                return ChangeType.ADDED;
            case INLINED:
            case REMOVED:
                return ChangeType.REMOVED;
        }

        throw new RuntimeException(change.getType().getTitle() + " UNKNOWN CHANGE TYPE!!!!!!");
    }

    private static ChangeType getChangeType(Refactoring refactoring) {
        switch (refactoring.getRefactoringType()) {
            case ADD_PARAMETER:
            case RENAME_PARAMETER:
            case CHANGE_PARAMETER_TYPE:
            case MERGE_PARAMETER:
            case REMOVE_PARAMETER:
            case REORDER_PARAMETER:
            case SPLIT_PARAMETER:
            case ADD_PARAMETER_ANNOTATION:
            case REMOVE_PARAMETER_ANNOTATION:
            case MODIFY_PARAMETER_ANNOTATION:
            case PARAMETERIZE_VARIABLE:
            case REMOVE_PARAMETER_MODIFIER:
            case ADD_PARAMETER_MODIFIER:
                return ChangeType.CHANGED_PARAMETER;

            case ADD_THROWN_EXCEPTION_TYPE:
            case CHANGE_THROWN_EXCEPTION_TYPE:
            case REMOVE_THROWN_EXCEPTION_TYPE:
                return ChangeType.CHANGED_THROWN_EXCEPTION;

            case CHANGE_OPERATION_ACCESS_MODIFIER:
            case REMOVE_METHOD_MODIFIER:
            case ADD_METHOD_MODIFIER:
            case ADD_ATTRIBUTE_MODIFIER:
            case ADD_CLASS_MODIFIER:
            case ADD_VARIABLE_MODIFIER:
            case REMOVE_VARIABLE_MODIFIER:
            case CHANGE_ATTRIBUTE_ACCESS_MODIFIER:
            case CHANGE_CLASS_ACCESS_MODIFIER:
            case REMOVE_ATTRIBUTE_MODIFIER:
            case REMOVE_CLASS_MODIFIER:
                return ChangeType.CHANGED_MODIFIER;

            case PULL_UP_OPERATION:
            case PUSH_DOWN_OPERATION:
            case MOVE_OPERATION:
                return ChangeType.MOVED;
            case CHANGE_RETURN_TYPE:
                return ChangeType.CHANGED_RETURN_TYPE;
            case RENAME_METHOD:
            case RENAME_VARIABLE:
                return ChangeType.RENAMED;

            case ADD_METHOD_ANNOTATION:
            case MODIFY_METHOD_ANNOTATION:
            case REMOVE_METHOD_ANNOTATION:
            case ADD_ATTRIBUTE_ANNOTATION:
            case ADD_CLASS_ANNOTATION:
            case ADD_VARIABLE_ANNOTATION:
            case MODIFY_ATTRIBUTE_ANNOTATION:
            case MODIFY_CLASS_ANNOTATION:
            case MODIFY_VARIABLE_ANNOTATION:
            case REMOVE_ATTRIBUTE_ANNOTATION:
            case REMOVE_CLASS_ANNOTATION:
            case REMOVE_VARIABLE_ANNOTATION:
                return ChangeType.CHANGED_ANNOTATION;

            case MOVE_AND_RENAME_OPERATION:
                return ChangeType.MOVE_AND_RENAME;

            case CHANGE_VARIABLE_TYPE:
            case CHANGE_ATTRIBUTE_TYPE:
                return ChangeType.CHANGE_TYPE;
            case MERGE_VARIABLE:
                return ChangeType.MERGE_VARIABLE;

        }

        throw new RuntimeException(refactoring.getRefactoringType().getDisplayName() + " UNKNOWN CHANGE TYPE!!!!!!");
    }

    private static ChangeType getChangeType(String ychange) {
        switch (ychange) {
            case "Ybodychange":
                return ChangeType.CHANGED_BODY;
            case "Yparametermetachange":
            case "Yparameterchange":
                return ChangeType.CHANGED_PARAMETER;
            case "Yfilerename":
                return ChangeType.CHANGED_CONTAINER;
            case "Ymodifierchange":
                return ChangeType.CHANGED_MODIFIER;
            case "Yintroduced":
                return ChangeType.ADDED;
            case "Yexceptionschange":
                return ChangeType.CHANGED_THROWN_EXCEPTION;
            case "Ymovefromfile":
                return ChangeType.MOVED;
            case "Yrename":
                return ChangeType.RENAMED;
            case "Yreturntypechange":
                return ChangeType.CHANGED_RETURN_TYPE;
        }
        throw new RuntimeException(ychange + " UNKNOWN CHANGE TYPE!!!!!!");
    }

    public void correctDBType() throws IOException {
        ClassLoader classLoader = Starter.class.getClassLoader();
        File historyFolder = new File(classLoader.getResource("history/method/test").getFile());
        HashMap<String, String> db = new HashMap<>();

        for (File file : historyFolder.listFiles()) {
            HistoryInfo historyInfo = mapper.readValue(file, HistoryInfo.class);
            db.put(historyInfo.getFunctionKey(), "test");
        }

        historyFolder = new File(classLoader.getResource("history/method/train").getFile());
        for (File file : historyFolder.listFiles()) {
            HistoryInfo historyInfo = mapper.readValue(file, HistoryInfo.class);
            db.put(historyInfo.getFunctionKey(), "training");
        }

        Session sessionObj = null;
        try {
            sessionObj = buildSessionFactory().openSession();
            List<HistoryResult> allHistory = getAllMethodHistory(sessionObj);
            for (HistoryResult historyResult : allHistory) {
                historyResult.setOracle(db.get(historyResult.getElementKey()));
            }
            saveAllResultsToDatabase(allHistory, sessionObj);
        } catch (Exception sqlException) {
            if (null != sessionObj.getTransaction()) {
                System.out.println("\n.......Transaction Is Being Rolled Back.......");
                sessionObj.getTransaction().rollback();
            }
            sqlException.printStackTrace();
        } finally {
            if (sessionObj != null) {
                sessionObj.close();
            }
        }
    }

    private void historyTest(String processedFilePath) throws Exception {
        Session sessionObj = null;
        try {
            sessionObj = buildSessionFactory().openSession();
            Set<String> processedFiles = new HashSet<>(Files.readAllLines(Paths.get(processedFilePath)));
            ClassLoader classLoader = Starter.class.getClassLoader();
            File historyFolder = new File(classLoader.getResource("history/method/test").getFile());
            List<HistoryResult> allMethodHistory = getAllMethodHistory(sessionObj);
            HashMap<String, HashMap<String, HashMap<String, HistoryResult>>> allResults = getHistoryResultMap(allMethodHistory);

            for (File file : historyFolder.listFiles()) {
                if (processedFiles.contains(file.getName()))
                    continue;

                HistoryInfo historyInfo = mapper.readValue(file, HistoryInfo.class);

                String repositoryWebURL = historyInfo.getRepositoryWebURL();
                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
                String projectDirectory = FOLDER_TO_CLONE + repositoryName;

                HashMap<String, HistoryResult> historyResultHashMap = allResults.getOrDefault(repositoryWebURL, new HashMap<>()).getOrDefault(historyInfo.getFunctionKey(), new HashMap<>());

//                codeShovelOracle(methodHistoryInfo, repositoryWebURL, historyResultHashMap);
                long startTime = System.nanoTime();
                try {
                    refactoringRefiner(historyResultHashMap, historyInfo, projectDirectory, false);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                long refactoringMinerProcessingTime = (System.nanoTime() - startTime) / 1000000;
                startTime = System.nanoTime();
                try {
//                refactoringRefiner(historyResultHashMap, methodHistoryInfo, projectDirectory, true);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                long apidiffProcessingTime = (System.nanoTime() - startTime) / 1000000;
                startTime = System.nanoTime();
                try {
//                codeShovel(historyResultHashMap, methodHistoryInfo, projectDirectory, repositoryWebURL);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                long shovelProcessingTime = (System.nanoTime() - startTime) / 1000000;

                saveAllResultsToDatabase(historyResultHashMap.values(), sessionObj);
                writeToFile("E:\\Data\\History\\result.csv", String.format("%s;; %s;; %d;; %d;; %d" + System.lineSeparator(), repositoryWebURL, historyInfo.getFunctionKey(), refactoringMinerProcessingTime, apidiffProcessingTime, shovelProcessingTime), StandardOpenOption.APPEND);
                writeToFile(processedFilePath, file.getName() + System.lineSeparator(), StandardOpenOption.APPEND);
            }
        } catch (Exception sqlException) {
            if (null != sessionObj.getTransaction()) {
                System.out.println("\n.......Transaction Is Being Rolled Back.......");
                sessionObj.getTransaction().rollback();
            }
            sqlException.printStackTrace();
        } finally {
            if (sessionObj != null) {
                sessionObj.close();
            }
        }
    }

    private HashMap<String, HashMap<String, HashMap<String, HistoryResult>>> getHistoryResultMap(List<HistoryResult> allHistory) {
        HashMap<String, HashMap<String, HashMap<String, HistoryResult>>> allResults = new HashMap<>();
        for (HistoryResult historyResult : allHistory) {
            allResults.putIfAbsent(historyResult.getRepository(), new HashMap<>());
            HashMap<String, HashMap<String, HistoryResult>> repositoryMap = allResults.get(historyResult.getRepository());
            repositoryMap.putIfAbsent(historyResult.getElementKey(), new HashMap<>());
            HashMap<String, HistoryResult> historyResultHashMap = repositoryMap.get(historyResult.getElementKey());
            historyResultHashMap.putIfAbsent(String.format("%s-%s", historyResult.getElementVersionIdAfter(), historyResult.getChangeType()), historyResult);
        }
        return allResults;
    }

//    private void codeShovel(HashMap<String, HistoryResult> result, MethodHistoryInfo methodHistoryInfo, String projectDirectory, String repositoryWebURL) throws Exception {
//        Yresult yresult = runShovelExecution(projectDirectory,
//                methodHistoryInfo.getRepositoryName(),
//                methodHistoryInfo.getStartCommitName(),
//                methodHistoryInfo.getFilePath(),
//                methodHistoryInfo.getFunctionName(),
//                String.format("E:\\Data\\History\\shovel\\%s.csv", methodHistoryInfo.getFunctionKey()),
//                methodHistoryInfo.getFunctionStartLine()
//        );
//        for (Map.Entry<String, Ychange> entry : yresult.entrySet()) {
//            if (entry.getValue() instanceof Ynochange)
//                continue;
//            List<Ychange> changes = new ArrayList<>();
//            if (entry.getValue() instanceof Ymultichange) {
//                Ymultichange ymultichange = (Ymultichange) entry.getValue();
//                changes.addAll(ymultichange.getChanges());
//            } else {
//                changes.add(entry.getValue());
//            }
//            for (Ychange ychange : changes) {
//                ChangeType changeType = getChangeType(ychange);
//                String commitId = entry.getKey();
//                String elementFileBefore = null;
//                String elementFileAfter = null;
//
//                String elementNameBefore = null;
//                String elementNameAfter = null;
//
//                String elementVersionIdBefore = null;
//                String elementVersionIdAfter = null;
//
//                long elementVersionTimeBefore = 0;
//                long elementVersionTimeAfter = 0;
//
//                if (ychange instanceof Ycomparefunctionchange) {
//                    Ycomparefunctionchange ycomparefunctionchange = (Ycomparefunctionchange) ychange;
//                    elementFileBefore = ycomparefunctionchange.getOldFunction().getSourceFilePath();
//                    elementFileAfter = ycomparefunctionchange.getNewFunction().getSourceFilePath();
//
//                    elementNameBefore = ycomparefunctionchange.getOldFunction().getId();
//                    elementNameAfter = ycomparefunctionchange.getNewFunction().getId();
//
//                    elementVersionIdBefore = ycomparefunctionchange.getOldFunction().getCommitName();
//                    elementVersionIdAfter = ycomparefunctionchange.getNewFunction().getCommitName();
//
//                    elementVersionTimeBefore = ycomparefunctionchange.getOldFunction().getCommit().getCommitTime();
//                    elementVersionTimeAfter = ycomparefunctionchange.getNewFunction().getCommit().getCommitTime();
//                } else if (ychange instanceof Yintroduced) {
//                    Yintroduced yintroduced = (Yintroduced) ychange;
//
//                    elementFileBefore = yintroduced.getNewFunction().getSourceFilePath();
//                    elementFileAfter = yintroduced.getNewFunction().getSourceFilePath();
//
//                    elementNameBefore = yintroduced.getNewFunction().getId();
//                    elementNameAfter = yintroduced.getNewFunction().getId();
//
//                    elementVersionIdBefore = yintroduced.getNewFunction().getCommitName();
//                    elementVersionIdAfter = entry.getKey();
//
//                    elementVersionTimeBefore = yintroduced.getNewFunction().getCommit().getCommitTime();
//                    elementVersionTimeAfter = yintroduced.getNewFunction().getCommit().getCommitTime();
//                }
//
//                addHistoryResult(result,
//                        changeType,
//                        commitId,
//                        methodHistoryInfo.getFunctionKey(),
//                        Detector.SHOVEL,
//                        ychange.toString(),
//                        repositoryWebURL,
//                        elementFileBefore,
//                        elementFileAfter,
//                        elementNameBefore,
//                        elementNameAfter,
//                        elementVersionIdBefore,
//                        elementVersionIdAfter,
//                        elementVersionTimeBefore,
//                        elementVersionTimeAfter);
//            }
//        }
//    }

    private void variableHistoryTest(String processedFilePath) throws Exception {
        Session sessionObj = null;
        try {
            sessionObj = buildSessionFactory().openSession();
            Set<String> processedFiles = new HashSet<>(Files.readAllLines(Paths.get(processedFilePath)));
            File historyFolder = new File(Starter.class.getClassLoader().getResource("history/variable/training").getFile());

            List<HistoryResult> allVariableHistory = getAllVariableHistory(sessionObj);
            HashMap<String, HashMap<String, HashMap<String, HistoryResult>>> allResults = getHistoryResultMap(allVariableHistory);

            for (File file : historyFolder.listFiles()) {
                if (processedFiles.contains(file.getName()))
                    continue;

                HistoryInfo historyInfo = mapper.readValue(file, HistoryInfo.class);

                String repositoryWebURL = historyInfo.getRepositoryWebURL();
                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
                String projectDirectory = FOLDER_TO_CLONE + repositoryName;

                try {
                    RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
                    HistoryImpl<CodeElement, Edge> variableHistory = (HistoryImpl<CodeElement, Edge>) refactoringRefinerImpl.findVariableHistory(projectDirectory, repositoryWebURL, historyInfo.getStartCommitName(), historyInfo.getFilePath(), historyInfo.getFunctionKey(), historyInfo.getVariableKey());
                    HashMap<String, HistoryResult> historyResultHashMap = allResults.getOrDefault(repositoryWebURL, new HashMap<>()).getOrDefault(historyInfo.getFunctionKey(), new HashMap<>());
                    for(Map.Entry<String, String> entry: historyInfo.getExpectedResult().entrySet()){
                        String key = String.format("%s-%s", entry.getKey(), entry.getValue());
                        if(!historyResultHashMap.containsKey(key)){
                            historyResultHashMap.put(key, new HistoryResult());
                        }
                        HistoryResult historyResult = historyResultHashMap.get(key);
                        historyResult.setOracle("refactoring-miner");
                        historyResult.setCodeShovelOracleVote(1);
                    }
                    processHistory(variableHistory, historyResultHashMap, repositoryWebURL, String.format("%s$%s", historyInfo.getFunctionKey(), historyInfo.getVariableKey()), Detector.MINER, "variable", Starter::getChangeTypeRefactoringMiner);
                    saveAllResultsToDatabase(historyResultHashMap.values(), sessionObj);
                    writeToFile(processedFilePath, file.getName() + System.lineSeparator(), StandardOpenOption.APPEND);

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        } catch (Exception sqlException) {
            if (null != sessionObj.getTransaction()) {
                System.out.println("\n.......Transaction Is Being Rolled Back.......");
                sessionObj.getTransaction().rollback();
            }
            sqlException.printStackTrace();
        } finally {
            if (sessionObj != null) {
                sessionObj.close();
            }
        }
    }

    private void createVariableDataset() throws Exception {
        Session sessionObj = null;
        try {
            sessionObj = buildSessionFactory().openSession();

            String processedFilePath = "E:\\Data\\History\\Variable\\dataset\\processed.csv";
            String finishedFilePath = "E:\\Data\\History\\Variable\\dataset\\report.csv";
            Set<String> processedFiles = new HashSet<>(Files.readAllLines(Paths.get(processedFilePath)));
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            ClassLoader classLoader = Starter.class.getClassLoader();
            File historyFolder = new File(classLoader.getResource("history/method/test").getFile());
//        File resultFolder = new File(classLoader.getResource("history/variable/training").getFile());
            File resultFolder = new File("E:\\Data\\History\\Variable\\dataset\\test");
            for (File file : historyFolder.listFiles()) {
                if (processedFiles.contains(file.getName()))
                    continue;

                HistoryInfo historyInfo = mapper.readValue(file, HistoryInfo.class);
                String repositoryWebURL = historyInfo.getRepositoryWebURL();
                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
                String projectDirectory = FOLDER_TO_CLONE + repositoryName;

                GitService gitService = new GitServiceImpl();
                try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                    try (Git git = new Git(repository)) {
                        RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
                        UMLModel startModel = refactoringMiner.getUMLModel(historyInfo.getStartCommitName(), Collections.singletonList(historyInfo.getFilePath()));
                        Method startMethod = RefactoringMiner.getMethodByName(startModel, refactoringMiner.getVersion(historyInfo.getStartCommitName()), historyInfo.getFunctionKey());
                        if (startMethod == null)
                            continue;
                        for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
                            if (variableDeclaration.isParameter())
                                continue;
                            HistoryInfo variableHistoryInfo = new HistoryInfo();
                            variableHistoryInfo.setRepositoryName(repositoryName);
                            variableHistoryInfo.setRepositoryWebURL(repositoryWebURL);
                            variableHistoryInfo.setFilePath(startMethod.getFilePath());
                            variableHistoryInfo.setFunctionName(startMethod.getUmlOperation().getName());
                            variableHistoryInfo.setFunctionKey(startMethod.getName());
                            variableHistoryInfo.setFunctionStartLine(historyInfo.getFunctionStartLine());

                            variableHistoryInfo.setVariableName(variableDeclaration.getVariableName());
                            variableHistoryInfo.setVariableKey(variableDeclaration.toString());
                            variableHistoryInfo.setVariableStartLine(variableDeclaration.getLocationInfo().getStartLine());

                            variableHistoryInfo.setStartCommitName(historyInfo.getStartCommitName());

                            HistoryResult methodIntroducedCommit = getMethodIntroducedCommit(sessionObj, repositoryWebURL, historyInfo.getFunctionKey());

                            boolean addedFound = false;
                            if (methodIntroducedCommit != null) {
                                UMLModel addedModel = refactoringMiner.getUMLModel(methodIntroducedCommit.getElementVersionIdAfter(), Collections.singletonList(methodIntroducedCommit.getElementFileAfter()));
                                Method addedMethod = RefactoringMiner.getMethodByName(addedModel, refactoringMiner.getVersion(methodIntroducedCommit.getElementVersionIdAfter()), methodIntroducedCommit.getElementNameAfter());
                                if (addedMethod != null) {
                                    for (VariableDeclaration addedVariableDeclaration : addedMethod.getUmlOperation().getAllVariableDeclarations()) {
                                        if (addedVariableDeclaration.isParameter())
                                            continue;

                                        if (addedVariableDeclaration.getVariableName().equals(variableDeclaration.getVariableName())) {
                                            variableHistoryInfo.getExpectedResult().put(methodIntroducedCommit.getElementVersionIdAfter(), "added");
                                            addedFound = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!addedFound) {
                                List<HistoryResult> methodChangedBodyCommits = getMethodChangedBodyCommits(sessionObj, repositoryWebURL, historyInfo.getFunctionKey());
                                for (HistoryResult historyResult : methodChangedBodyCommits) {
                                    if (addedFound)
                                        break;
                                    UMLModel changedBodyModelRight = refactoringMiner.getUMLModel(historyResult.getElementVersionIdAfter(), Collections.singletonList(historyResult.getElementFileAfter()));
                                    Method changedBodyMethodRight = RefactoringMiner.getMethodByName(changedBodyModelRight, refactoringMiner.getVersion(historyResult.getElementVersionIdAfter()), historyResult.getElementNameAfter());

                                    UMLModel changedBodyModelLeft = refactoringMiner.getUMLModel(historyResult.getElementVersionIdBefore(), Collections.singletonList(historyResult.getElementFileBefore()));
                                    Method changedBodyMethodLeft = RefactoringMiner.getMethodByName(changedBodyModelLeft, refactoringMiner.getVersion(historyResult.getElementVersionIdBefore()), historyResult.getElementNameBefore());
                                    if (changedBodyMethodLeft == null || changedBodyMethodRight == null)
                                        continue;
                                    UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(changedBodyMethodLeft.getUmlOperation(), changedBodyMethodRight.getUmlOperation(), null);
                                    umlOperationBodyMapper.getRefactorings();
                                    for (VariableDeclaration addedVariableDeclaration : umlOperationBodyMapper.getAddedVariables().stream().map(Pair::getLeft).collect(Collectors.toList())) {
                                        if (addedVariableDeclaration.isParameter())
                                            continue;

                                        if (addedVariableDeclaration.getVariableName().equals(variableDeclaration.getVariableName())) {
                                            variableHistoryInfo.getExpectedResult().put(historyResult.getElementVersionIdAfter(), "added");
                                            addedFound = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            File newFile = new File(resultFolder.getPath() + "\\" + file.getName().replace(".json", "") + "-" + variableDeclaration.getVariableName() + ".json");
                            int i = 1;
                            while (newFile.exists()) {
                                newFile = new File(resultFolder.getPath() + "\\" + file.getName() + "-" + variableDeclaration.getVariableName() + i + ".json");
                                i++;
                            }
                            writer.writeValue(newFile, variableHistoryInfo);
                            writeToFile(finishedFilePath, newFile.getName() + ", " + (addedFound ? "found" : "not found") + System.lineSeparator(), StandardOpenOption.APPEND);
                        }


//                    HistoryResult methodIntroducedCommit = getMethodIntroducedCommit(repositoryWebURL, historyInfo.getFunctionKey());
//                    if(methodIntroducedCommit!=null) {
//                        UMLModel addedModel = refactoringMiner.getUMLModel(methodIntroducedCommit.getElementVersionIdAfter(), Collections.singletonList(methodIntroducedCommit.getElementFileAfter()));
//                        Method addedMethod = RefactoringMiner.getMethodByName(addedModel, refactoringMiner.getVersion(methodIntroducedCommit.getElementVersionIdAfter()), methodIntroducedCommit.getElementNameAfter());
//                        UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(addedMethod.getUmlOperation(), startMethod.getUmlOperation(), null);
//                        System.out.println("");
//                    }else {
//                        System.out.println(file.getName());
//                    }
                    }
                }
                writeToFile(processedFilePath, file.getName() + System.lineSeparator(), StandardOpenOption.APPEND);
            }
        } catch (Exception sqlException) {
            if (null != sessionObj.getTransaction()) {
                System.out.println("\n.......Transaction Is Being Rolled Back.......");
                sessionObj.getTransaction().rollback();
            }
            sqlException.printStackTrace();
        } finally {
            if (sessionObj != null) {
                sessionObj.close();
            }
        }
    }

    private void codeShovelOracle(HistoryInfo historyInfo, String repositoryWebURL, HashMap<String, HistoryResult> historyResultHashMap) {
        for (Map.Entry<String, String> entry : historyInfo.getExpectedResult().entrySet()) {
            if (entry.getValue().equals("Ynochange"))
                continue;
            List<String> changes = new ArrayList<>();
            if (entry.getValue().contains("Ymultichange")) {
                for (String change : entry.getValue().replace("Ymultichange(", "").replace(")", "").split(",")) {
                    changes.add(change);
                }
            } else {
                changes.add(entry.getValue());
            }
            for (String ychange : changes) {
                ChangeType changeType = getChangeType(ychange);
                String commitId = entry.getKey();

                addHistoryResult(historyResultHashMap,
                        changeType,
                        commitId,
                        historyInfo.getFunctionKey(),
                        "method",
                        Detector.SHOVEL_ORACLE,
                        ychange,
                        repositoryWebURL,
                        null,
                        null,
                        null,
                        null,
                        null,
                        commitId,
                        -1,
                        -1);
            }
        }
    }

    private void refactoringRefiner(HashMap<String, HistoryResult> result, HistoryInfo historyInfo, String projectDirectory, boolean useRefDiffAsChangeDetector) {
        for (Map.Entry<String, HistoryResult> entry : result.entrySet()) {
            if (useRefDiffAsChangeDetector) {
                entry.getValue().setRefdiffVote(-1);
                entry.getValue().setRefDiffDesc(null);
            } else {
                entry.getValue().setRefactoringMinerVote(-1);
                entry.getValue().setRefactoringMinerDesc(null);
            }
        }
        RefactoringRefinerImpl refactoringRefinerImpl = (RefactoringRefinerImpl) RefactoringRefinerImpl.factory();
//        GitService gitService = new GitServiceImpl();
        String repositoryWebURL = historyInfo.getRepositoryWebURL();
        System.out.printf("Start processing [%s,%s,%s,%s] By %s%n", repositoryWebURL, historyInfo.getFilePath(), historyInfo.getFunctionKey(), historyInfo.getStartCommitName(), useRefDiffAsChangeDetector ? "RefDiff" : "RefactoringMiner");
        HistoryImpl<CodeElement, Edge> historyImpl;
        if (useRefDiffAsChangeDetector) {
            historyImpl = (HistoryImpl<CodeElement, Edge>) refactoringRefinerImpl.findHistory(projectDirectory, repositoryWebURL, historyInfo.getStartCommitName(), historyInfo.getFilePath(), historyInfo.getFunctionKey(), RefactoringRefiner.CodeElementType.METHOD, useRefDiffAsChangeDetector);
        } else {
            historyImpl = (HistoryImpl<CodeElement, Edge>) refactoringRefinerImpl.findMethodHistory(projectDirectory, repositoryWebURL, historyInfo.getStartCommitName(), historyInfo.getFilePath(), historyInfo.getFunctionKey());
        }
        processHistory(historyImpl, result, repositoryWebURL, historyInfo.getFunctionKey(), useRefDiffAsChangeDetector ? Detector.APIDIFF : Detector.MINER, "method", useRefDiffAsChangeDetector ? Starter::getChangeTypeRefDiff : Starter::getChangeTypeRefactoringMiner);
    }

    private void processHistory(HistoryImpl<CodeElement, Edge> historyImpl, HashMap<String, HistoryResult> result, String repositoryWebURL, String elementKey, Detector detector, String elementType, Function<Change, ChangeType> changeMapper) {
        if (historyImpl.getGraph() == null)
            return;
        Set<EndpointPair<CodeElement>> edges = historyImpl.getGraph().getEdges();

        for (EndpointPair<CodeElement> edge : edges) {
            EdgeImpl edgeValue = (EdgeImpl) historyImpl.getGraph().getEdgeValue(edge).get();
            for (Change change : edgeValue.getChangeList()) {
                if (Change.Type.NO_CHANGE.equals(change.getType()))
                    continue;
                ChangeType changeType = changeMapper.apply(change);

                String commitId = edge.target().getVersion().getId();

                String elementFileBefore = edge.source().getFilePath();
                String elementFileAfter = edge.target().getFilePath();

                String elementNameBefore = edge.source().getName();
                String elementNameAfter = edge.target().getName();

                String elementVersionIdBefore = edge.source().getVersion().getId();
                String elementVersionIdAfter = commitId;

                long elementVersionTimeBefore = edge.source().getVersion().getTime();
                long elementVersionTimeAfter = edge.target().getVersion().getTime();

                if (ChangeType.MOVE_AND_RENAME.equals(changeType)) {
                    addHistoryResult(result,
                            ChangeType.MOVED,
                            commitId,
                            elementKey,
                            elementType,
                            detector,
                            change.getDescription(),
                            repositoryWebURL,
                            elementFileBefore,
                            elementFileAfter,
                            elementNameBefore,
                            elementNameAfter,
                            elementVersionIdBefore,
                            elementVersionIdAfter,
                            elementVersionTimeBefore,
                            elementVersionTimeAfter);

                    addHistoryResult(result,
                            ChangeType.RENAMED,
                            commitId,
                            elementKey,
                            elementType,
                            detector,
                            change.getDescription(),
                            repositoryWebURL,
                            elementFileBefore,
                            elementFileAfter,
                            elementNameBefore,
                            elementNameAfter,
                            elementVersionIdBefore,
                            elementVersionIdAfter,
                            elementVersionTimeBefore,
                            elementVersionTimeAfter);
                } else {
                    addHistoryResult(result,
                            changeType,
                            commitId,
                            elementKey,
                            elementType,
                            detector,
                            change.getDescription(),
                            repositoryWebURL,
                            elementFileBefore,
                            elementFileAfter,
                            elementNameBefore,
                            elementNameAfter,
                            elementVersionIdBefore,
                            elementVersionIdAfter,
                            elementVersionTimeBefore,
                            elementVersionTimeAfter);
                }

            }
        }
    }

    private String getChangeKey(ChangeType changeType, String commitId) {
        return String.format("%s-%s", commitId, changeType.name.toLowerCase());
    }

//    private ChangeType getChangeType(Ychange ychange) {
//        return getChangeType(ychange.getTypeAsString());
//    }

    private void addHistoryResult(HashMap<String, HistoryResult> result,
                                  ChangeType changeType,
                                  String commitId,
                                  String elementKey,
                                  String elementType,
                                  Detector detector,
                                  String desc,
                                  String repositoryWebURL,
                                  String elementFileBefore,
                                  String elementFileAfter,
                                  String elementNameBefore,
                                  String elementNameAfter,
                                  String elementVersionIdBefore,
                                  String elementVersionIdAfter,
                                  long elementVersionTimeBefore,
                                  long elementVersionTimeAfter
    ) {
        String changeKey = getChangeKey(changeType, commitId);
        HistoryResult historyResult;
        if (result.containsKey(changeKey)) {
            historyResult = result.get(changeKey);
        } else {
            historyResult = new HistoryResult();
            result.put(changeKey, historyResult);
        }
        historyResult.setElementType(elementType);
        historyResult.setRepository(repositoryWebURL);
        historyResult.setChangeType(changeType.name);
        historyResult.setElementKey(elementKey);
        if (detector != null) {
            switch (detector) {
                case MINER:
                    if (historyResult.getRefactoringMinerVote() != 1) {
                        historyResult.setRefactoringMinerVote(1);
                        if (historyResult.getRefactoringMinerDesc() != null)
                            historyResult.setRefactoringMinerDesc(historyResult.getRefactoringMinerDesc() + ";" + desc);
                        else
                            historyResult.setRefactoringMinerDesc(desc);
                        historyResult.setElementFileBefore(elementFileBefore);
                        historyResult.setElementFileAfter(elementFileAfter);
                        historyResult.setElementNameBefore(elementNameBefore);
                        historyResult.setElementNameAfter(elementNameAfter);
                        historyResult.setElementVersionIdBefore(elementVersionIdBefore);
                        historyResult.setElementVersionIdAfter(elementVersionIdAfter);
                        historyResult.setElementVersionTimeBefore(elementVersionTimeBefore);
                        historyResult.setElementVersionTimeAfter(elementVersionTimeAfter);
                    }
                    break;
                case SHOVEL:
                    if (historyResult.getCodeShovelVote() != 1) {
                        historyResult.setCodeShovelVote(1);
                        if (historyResult.getCodeShovelDesc() != null)
                            historyResult.setCodeShovelDesc(historyResult.getCodeShovelDesc() + ";" + desc);
                        else
                            historyResult.setCodeShovelDesc(desc);
                    }
                    break;
                case SHOVEL_ORACLE:
                    historyResult.setCodeShovelOracleVote(1);
                    break;
                case REFDIFF:
                    if (historyResult.getRefdiffVote() != 1) {
                        historyResult.setRefdiffVote(1);
                        if (historyResult.getRefDiffDesc() != null)
                            historyResult.setRefDiffDesc(historyResult.getRefDiffDesc() + ";" + desc);
                        else
                            historyResult.setRefDiffDesc(desc);
                    }
                    break;
            }
        }

        if (historyResult.getElementFileBefore() == null)
            historyResult.setElementFileBefore(elementFileBefore);
        if (historyResult.getElementFileAfter() == null)
            historyResult.setElementFileAfter(elementFileAfter);

        if (historyResult.getElementNameBefore() == null)
            historyResult.setElementNameBefore(elementNameBefore);
        if (historyResult.getElementNameAfter() == null)
            historyResult.setElementNameAfter(elementNameAfter);

        if (historyResult.getElementVersionIdBefore() == null)
            historyResult.setElementVersionIdBefore(elementVersionIdBefore);
        if (historyResult.getElementVersionIdAfter() == null)
            historyResult.setElementVersionIdAfter(elementVersionIdAfter);

        if (historyResult.getElementVersionTimeBefore() <= 0)
            historyResult.setElementVersionTimeBefore(elementVersionTimeBefore);
        if (historyResult.getElementVersionTimeAfter() <= 0)
            historyResult.setElementVersionTimeAfter(elementVersionTimeAfter);
    }

    public enum Detector {
        MINER,
        SHOVEL,
        SHOVEL_ORACLE,
        APIDIFF,
        REFDIFF
    }

    private enum ChangeType {
        CHANGED_BODY("changed body"),
        CHANGED_PARAMETER("changed parameter"),
        CHANGED_CONTAINER("changed container"),
        CHANGED_MODIFIER("changed modifier"),
        CHANGED_THROWN_EXCEPTION("change thrown exception"),
        MOVED("moved"),
        RENAMED("renamed"),
        CHANGED_RETURN_TYPE("changed return type"),
        ADDED("added"),
        REMOVED("removed"),
        CHANGED_ANNOTATION("changed annotation"),
        MOVE_AND_RENAME("move and rename"),
        CHANGE_TYPE("change type"),
        MERGE_VARIABLE("Merge Variable");

        private final String name;

        ChangeType(String name) {
            this.name = name;
        }
    }
}

