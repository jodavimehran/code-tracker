package org.refactoringrefiner;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.api.*;
import org.refactoringrefiner.change.AbstractChange;
import org.refactoringrefiner.edge.ChangeFactory;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.element.Variable;
import org.refactoringrefiner.test.RefactoringResult;
import org.refactoringrefiner.util.GitHubRepository;
import org.refactoringrefiner.util.GitRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
public class RefactoringRefinerImpl implements RefactoringRefiner {

    private final static int TIMEOUT = 12000;
//    private final HashMap<String, RefactoringMiner> refactoringMinerCache = new HashMap<>();
//    private final HashMap<String, RefDiffChangeDetector> refDiffCache = new HashMap<>();

    public static RefactoringRefiner factory() {
        return new RefactoringRefinerImpl();
    }

    public static Boolean variablesEqualIdentifierIgnoringVersion(Variable variable1, Variable variable2) {
        return variable1.equalIdentifierIgnoringVersion(variable2);
    }

    @Override
    public Result analyseAllCommits(Repository repository, String branch) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            miner.detectAll(repository, branch, refactoringHandler);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseBetweenTags(Repository repository, String startTag, String endTag) throws Exception {
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));

        long startTime = System.nanoTime();
        miner.detectBetweenTags(repository, startTag, endTag, refactoringHandler);
        long refactoringMinerEndTime = System.nanoTime();

        ResultImpl result = new ResultImpl(refactoringHandler);
        long refactoringRefinerEndTime = System.nanoTime();
        result.setRefactoringMinerProcessTime((refactoringMinerEndTime - startTime) / 1000000);
        result.setRefactoringRefinerProcessTime((refactoringRefinerEndTime - startTime) / 1000000);
        return result;
    }

    @Override
    public Result analyseBetweenCommits(Repository repository, String startCommitId, String endCommitId) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            miner.detectBetweenCommits(repository, startCommitId, endCommitId, refactoringHandler);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseCommit(Repository repository, String commitId) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            miner.detectAtCommit(repository, commitId, refactoringHandler, TIMEOUT);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseCommits(Repository repository, List<String> commitList) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            for (String commitId : commitList)
                miner.detectAtCommit(repository, commitId, refactoringHandler, TIMEOUT);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseFileCommits(Repository repository, String filePath) {
        try (Git git = new Git(repository)) {
            git.pull().call();
            List<String> commitList = getCommits(filePath, git);
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            for (String commitId : commitList)
                miner.detectAtCommit(repository, commitId, refactoringHandler, TIMEOUT);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    public Result analyseCommits(String gitURL, List<String> commitList) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitHubRepository(miner.getGitHubRepository(gitURL)));
        for (String commitId : commitList)
            miner.detectAtCommit(gitURL, commitId, refactoringHandler, TIMEOUT);
        return new ResultImpl(refactoringHandler);
    }

    public List<Refactoring> detectAtCommits(String gitURL, List<String> commitIdList) throws Exception {
        Result result = analyseCommits(gitURL, commitIdList);
        return result.getAggregatedRefactorings();
    }

    public List<Refactoring> detectAtCommit(Repository repository, String startCommitId, String endCommitId) {
        Result result = analyseBetweenCommits(repository, startCommitId, endCommitId);
        return result.getAggregatedRefactorings();
    }

    public History<CodeElement, Edge> findHistory(String projectDirectory, String repositoryWebURL, String startCommitId, String filePath, String elementKey, CodeElementType codeElementType, boolean useRefDiffAsChangeDetector) {
//        GitService gitService = new GitServiceImpl();
//        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
//            try (Git git = new Git(repository)) {
//                git.fetch().setRemote("origin").call();
//                refactoringMinerCache.putIfAbsent(repositoryWebURL, new RefactoringMiner(repository, repositoryWebURL));
//                RefactoringMiner refactoringMiner = refactoringMinerCache.get(repositoryWebURL);
//                ChangeDetector changeDetector;
//                if (useRefDiffAsChangeDetector) {
//                    refDiffCache.putIfAbsent(repositoryWebURL, new RefDiffChangeDetector(refactoringMiner, new File(projectDirectory + "/.git")));
//                    changeDetector = refDiffCache.get(repositoryWebURL);
//                } else
//                    changeDetector = refactoringMiner;
//                boolean flag = true;
//                CodeElement start = null;
//                HashSet<String> analysedCommits = new HashSet<>();
//                LinkedList<CodeElement> codeElementLinkedList = new LinkedList<>();
//                String elementIdentifier = null;
//                while (flag) {
//                    List<String> commits = getCommits(repository, startCommitId, filePath, git);
//                    if (analysedCommits.containsAll(commits))
//                        break;
//                    for (String commitId : commits) {
//                        if (analysedCommits.contains(commitId))
//                            continue;
//                        System.out.println("processing " + commitId);
//                        Pair<UMLModel, UMLModel> umlModel = refactoringMiner.getUMLModel(commitId, filePath::equals);
//                        if (start == null) {
//                            Method method = RefactoringMiner.getMethod(umlModel.getRight(), elementKey, refactoringMiner.getVersion(commitId), false, true);
//                            if (method != null) {
//                                changeDetector.addNode(codeElementType, method);
//                                start = method;
//                                elementIdentifier = method.getIdentifierExcludeVersion();
//                            }
//                        }
//                        if (elementIdentifier != null && refactoringMiner.isChanged(umlModel, elementIdentifier, codeElementType)) {
//                            changeDetector.detectAtCommit(commitId);
//                            List<CodeElement> mostLeftSide = changeDetector.findMostLeftElement(codeElementType, elementKey);
//                            codeElementLinkedList.addAll(mostLeftSide);
//                            if (!codeElementLinkedList.isEmpty()) {
//                                CodeElement mostLeftSideElement = codeElementLinkedList.pollFirst();
//                                if (mostLeftSideElement.isAdded() && codeElementLinkedList.isEmpty()) {
//                                    flag = false;
//                                    break;
//                                }
//                                if (mostLeftSideElement.isAdded() && !codeElementLinkedList.isEmpty()) {
//                                    mostLeftSideElement = codeElementLinkedList.pollFirst();
//                                }
////                                elementKey = mostLeftSideElement.getName();
//                                elementIdentifier = mostLeftSideElement.getIdentifierExcludeVersion();
//                                if (!filePath.equals(mostLeftSideElement.getFilePath())) {
//                                    filePath = mostLeftSideElement.getFilePath();
//                                    startCommitId = mostLeftSideElement.getVersion().getId();
//                                    break;
//                                }
//                            }
//                        }
//                        analysedCommits.add(commitId);
//                    }
//                }
//                return new HistoryImpl<>(changeDetector.findSubGraph(codeElementType, start));
//            }
//        } catch (Exception exception) {
//            exception.printStackTrace();
        return null;
//        }
    }

    public History<CodeElement, Edge> findMethodHistory(String projectDirectory, String repositoryWebURL, String startCommitId, String filePath, String methodKey) throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        GitService gitService = new GitServiceImpl();
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
            try (Git git = new Git(repository)) {
                RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
                refactoringMiner.getRefactoringHandler().setTrackAttributes(false);
                refactoringMiner.getRefactoringHandler().setTrackClasses(false);
                refactoringMiner.getRefactoringHandler().setTrackVariables(false);

                UMLModel umlModel = refactoringMiner.getUMLModel(startCommitId, Collections.singletonList(filePath));
                Method start = RefactoringMiner.getMethodByName(umlModel, refactoringMiner.getVersion(startCommitId), methodKey);
                if (start == null) {
                    return null;
                }
                CodeElementType codeElementType = CodeElementType.METHOD;
                refactoringMiner.addNode(codeElementType, start);

                Queue<Method> methods = new ArrayDeque<>();
                methods.add(start);
                HashSet<String> analysedCommits = new HashSet<>();
                List<String> commits = null;
                while (!methods.isEmpty()) {
                    Method currentMethod = methods.poll();
                    if (currentMethod.isAdded()) {
                        continue;
                    }
                    if (!filePath.equals(currentMethod.getFilePath()) || commits == null) {
                        commits = getCommits(repository, currentMethod.getVersion().getId(), currentMethod.getFilePath(), git);
                        historyReport.gitLogCommandCallsPlusPlus();
                        analysedCommits.clear();
                    }
                    if (analysedCommits.containsAll(commits))
                        break;
                    for (String commitId : commits) {
                        if (analysedCommits.contains(commitId))
                            continue;
                        System.out.println("processing " + commitId);
                        analysedCommits.add(commitId);

                        Version currentVersion = refactoringMiner.getVersion(commitId);
                        String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
                        Version parentVersion = refactoringMiner.getVersion(parentCommitId);

                        UMLModel rightModel = refactoringMiner.getUMLModel(commitId, Collections.singletonList(currentMethod.getFilePath()));
                        UMLModel leftModel = refactoringMiner.getUMLModel(parentCommitId, Collections.singletonList(currentMethod.getFilePath()));

                        Method rightMethod = RefactoringMiner.getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                        if (rightMethod == null) {
                            continue;
                        }

                        ChangeHistory methodChangeHistoryGraph = refactoringMiner.getRefactoringHandler().getMethodChangeHistoryGraph();
                        Method leftMethod;
//                        //handle first commit of repository which doesn't have any parent
//                        if (umlModelPair.getLeft() == null) {
//                            leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
//                            methodChangeHistoryGraph.handleAdd(leftMethod, rightMethod);
//                            refactoringMiner.connectRelatedNodes(codeElementType);
//                            methods.add(leftMethod);
//                            break;
//                        }
                        historyReport.analysedCommitsPlusPlus();
                        //NO CHANGE
                        leftMethod = RefactoringMiner.getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                        if (leftMethod != null) {
                            historyReport.step2PlusPlus();
                            continue;
                        }

                        //CHANGE BODY OR DOCUMENT
                        leftMethod = RefactoringMiner.getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);

                        if (leftMethod != null) {
                            if (!leftMethod.equalBody(rightMethod))
                                methodChangeHistoryGraph.addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                            if (!leftMethod.equalDocuments(rightMethod))
                                methodChangeHistoryGraph.addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                            methodChangeHistoryGraph.connectRelatedNodes();
                            currentMethod = leftMethod;
                            historyReport.step3PlusPlus();
                            continue;
                        }

                        //Local Refactoring
                        {
                            UMLModelDiff umlModelDiff = leftModel.diff(rightModel, new HashMap<>());
                            List<Refactoring> refactorings = umlModelDiff.getRefactorings();
                            boolean refactored = isMethodRefactored(refactorings, refactoringMiner, methods, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                            if (refactored) {
                                historyReport.step4PlusPlus();
                                break;
                            }
                        }
                        //All refactorings
                        {

                            RefactoringMiner.CommitModel commitModel = refactoringMiner.getCommitModel(commitId);
                            {
                                Pair<UMLModel, UMLModel> umlModelPairPartial = refactoringMiner.getUMLModelPair(commitModel, currentMethod.getFilePath(), true);
                                UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight(), commitModel.renamedFilesHint);
                                List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
                                boolean containerChanged = isMethodContainerChanged(commitModel, refactoringsPartial, refactoringMiner, methods, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);

                                boolean refactored = isMethodRefactored(refactoringsPartial, refactoringMiner, methods, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);

                                if (containerChanged || refactored) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }
                            {

                                Pair<UMLModel, UMLModel> umlModelPairAll = refactoringMiner.getUMLModelPair(commitModel, currentMethod.getFilePath(), false);
                                UMLModelDiff umlModelDiff = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight(), commitModel.renamedFilesHint);

                                List<Refactoring> refactorings = umlModelDiff.getRefactorings();

                                boolean containerChanged = isMethodContainerChanged(commitModel, refactorings, refactoringMiner, methods, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);

                                boolean refactored = isMethodRefactored(refactorings, refactoringMiner, methods, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);

                                if (containerChanged || refactored) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }

                                if (isMethodAdded(umlModelDiff, refactoringMiner, methods, rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion)) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }
                        }
                    }
                }
                return new HistoryImpl<>(refactoringMiner.findSubGraph(codeElementType, start), historyReport);
            }
        }
    }

    public History<CodeElement, Edge> findVariableHistory(String projectDirectory, String repositoryWebURL, String startCommitId, String filePath, String methodKey, String variableName, int variableDeclarationLineNumber) {
        GitService gitService = new GitServiceImpl();
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
            try (Git git = new Git(repository)) {
                RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
                refactoringMiner.getRefactoringHandler().setTrackAttributes(false);
                refactoringMiner.getRefactoringHandler().setTrackClasses(false);
                refactoringMiner.getRefactoringHandler().setTrackMethods(false);
                UMLModel umlModel = refactoringMiner.getUMLModel(startCommitId, Collections.singletonList(filePath));
                Variable start = null;
                Version startVersion = refactoringMiner.getVersion(startCommitId);
                Method methodByName = RefactoringMiner.getMethodByName(umlModel, startVersion, methodKey);
                start = findVariable(variableName, variableDeclarationLineNumber, methodByName);
                if (start == null) {
                    return null;
                }
                CodeElementType codeElementType = CodeElementType.VARIABLE;
                refactoringMiner.addNode(codeElementType, start);

                Queue<Variable> variables = new ArrayDeque<>();
                variables.add(start);
                HashSet<String> analysedCommits = new HashSet<>();
                List<String> commits = null;
                while (!variables.isEmpty()) {
                    Variable currentVariable = variables.poll();
                    if (currentVariable.isAdded()) {
                        continue;
                    }
                    if (!filePath.equals(currentVariable.getFilePath()) || commits == null) {
                        commits = getCommits(repository, currentVariable.getVersion().getId(), currentVariable.getFilePath(), git);
                        analysedCommits.clear();
                    }
                    if (analysedCommits.containsAll(commits))
                        break;
                    for (String commitId : commits) {
                        if (analysedCommits.contains(commitId))
                            continue;
                        System.out.println("processing " + commitId);
                        analysedCommits.add(commitId);

                        Version currentVersion = refactoringMiner.getVersion(commitId);
                        String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
                        Version parentVersion = refactoringMiner.getVersion(parentCommitId);
                        Method currentMethod = Method.of(currentVariable.getOperation(), currentVersion);

//                        Pair<Pair<UMLModel, UMLModel>, UMLModel> umlModelPair = refactoringMiner.getUMLModelPair(commitId, Collections.singletonList(currentMethod.getFilePath()));
                        Pair<Pair<UMLModel, UMLModel>, UMLModel> umlModelPair = null;
                        if (umlModelPair == null) {
                            continue;
                        }
                        Method rightMethod = RefactoringMiner.getMethod(umlModelPair.getRight(), currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                        if (rightMethod == null) {
                            continue;
                        }
                        Variable rightVariable = findVariable(currentVariable, rightMethod);
                        if (rightVariable == null) {
                            continue;
                        }

                        UMLOperation umlOperationAfter = rightMethod.getUmlOperation();
                        if (umlModelPair.getLeft() == null) {
                            Method leftMethod = Method.of(umlOperationAfter, parentVersion);
                            Variable leftVariable = Variable.of(rightVariable.getVariableDeclaration(), leftMethod);
                            leftVariable.setAdded(true);
                            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleAdd(leftVariable, rightVariable);
                            refactoringMiner.connectRelatedNodes(codeElementType);
                            variables.add(leftVariable);
                            break;
                        }

                        //NO CHANGE
                        Method leftMethod = getLeftMethod(umlModelPair, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                        if (leftMethod != null) {
                            System.out.println(commitId + " : No Change");
                            continue;
                        }

                        //Change Body
                        leftMethod = getLeftMethod(umlModelPair, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
                        if (leftMethod != null) {
                            {
                                UMLOperation umlOperationBefore = leftMethod.getUmlOperation();
                                if (checkBodyOfMatchedOperations(refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable, umlOperationAfter, umlOperationBefore))
                                    break;
                            }
                        }

                        //Local Refactoring
                        {
                            UMLModelDiff umlModelDiff = umlModelPair.getLeft().getLeft().diff(umlModelPair.getRight(), new ConcurrentHashMap<>());
                            List<Refactoring> refactorings = umlModelDiff.getRefactorings();

                            boolean found;

                            found = isVariableContainerChanged(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable, rightMethod);
                            if (found)
                                break;

                            found = isMovedFromExtractionOrInline(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;

                            //Check if refactored
                            found = isVariableRefactored(refactorings, refactoringMiner, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                            if (found)
                                break;

                            // check if it is in the matched
                            found = isMatched(umlModelDiff.getMatchedVariables(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;

                            found = isAdded(umlModelDiff.getAddedVariables(), refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;
                        }

                        //All refactorings
                        {
                            List<RefactoringMiner.ModelDiff> umlModelDiffList = refactoringMiner.getUMLModelDiff(commitId, Collections.singletonList(rightMethod.getFilePath()));
                            boolean found = false;
                            for (RefactoringMiner.ModelDiff modelDiff : umlModelDiffList) {
                                if (found) {
                                    break;
                                }
                                List<Refactoring> refactorings = modelDiff.umlModelDiff.getRefactorings();

                                found = isVariableContainerChanged(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable, rightMethod);
                                if (found)
                                    break;

                                found = isMovedFromExtractionOrInline(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;

                                //Check if refactored
                                found = isVariableRefactored(refactorings, refactoringMiner, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                                if (found)
                                    break;

                                found = isMatched(modelDiff.umlModelDiff.getMatchedVariables(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;

                                found = isAdded(modelDiff.umlModelDiff.getAddedVariables(), refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;

                                UMLClassBaseDiff umlClassDiff = modelDiff.umlModelDiff.getUMLClassDiff(umlOperationAfter.getClassName());
                                if (umlClassDiff != null) {
                                    List<UMLOperation> addedOperations = umlClassDiff.getAddedOperations();
                                    for (UMLOperation operation : addedOperations) {
                                        Method method = Method.of(operation, parentVersion);
                                        if (method.equalIdentifierIgnoringVersion(rightMethod)) {
                                            List<Pair<VariableDeclaration, UMLOperation>> addedVariables = method.getUmlOperation().getBody().getAllVariableDeclarations().stream().map(variableDeclaration -> Pair.of(variableDeclaration, method.getUmlOperation())).collect(Collectors.toList());
                                            found = isAdded(addedVariables, refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                            if (found)
                                                break;
                                        }
                                    }
                                }
                                if (found) {
                                    break;
                                }

                                UMLClass addedClass = modelDiff.umlModelDiff.getAddedClass(umlOperationAfter.getClassName());
                                if (addedClass != null) {
                                    List<UMLOperation> addedOperations = addedClass.getOperations();
                                    for (UMLOperation operation : addedOperations) {
                                        Method method = Method.of(operation, currentVersion);
                                        if (method.equalIdentifierIgnoringVersion(rightMethod)) {
                                            List<Pair<VariableDeclaration, UMLOperation>> addedVariables = method.getUmlOperation().getBody().getAllVariableDeclarations().stream().map(variableDeclaration -> Pair.of(variableDeclaration, method.getUmlOperation())).collect(Collectors.toList());
                                            found = isAdded(addedVariables, refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                            if (found)
                                                break;
                                        }
                                    }
                                }
                            }
                        }


                        //==============================================================================================

//                        Pair<Method, Method> methodPair = getMethodPair(currentMethod, commitId, refactoringMiner);
//                        if (methodPair == null)
//                            continue;
//                        //No Change
//                        if (methodPair.getLeft().equalIdentifierIgnoringVersion(methodPair.getRight()))
//                            continue;
//                        Variable rightVariable = findVariable(currentVariable, methodPair.getRight());
//                        Variable leftVariable = findVariable(currentVariable, methodPair.getLeft());
//                        //Again No Change
//                        if (rightVariable != null && leftVariable != null && rightVariable.equalIdentifierIgnoringVersion(leftVariable)) {
//                            variables.add(leftVariable);
//                            break;
//                        }
//
//                        if (rightVariable != null && leftVariable != null && rightVariable.equalIdentifierIgnoringVersionAndContainer(leftVariable)) {
//                            refactoringMiner.addEdge(codeElementType, leftVariable, rightVariable, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
//                            refactoringMiner.connectRelatedNodes(codeElementType);
//                            variables.add(leftVariable);
//                            break;
//                        }

                    }
                }
                return new HistoryImpl<>(refactoringMiner.findSubGraph(codeElementType, start), null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private boolean checkBodyOfMatchedOperations(RefactoringMiner refactoringMiner, CodeElementType codeElementType, Queue<Variable> variables, Version currentVersion, Version parentVersion, Variable rightVariable, UMLOperation umlOperationAfter, UMLOperation umlOperationBefore) throws RefactoringMinerTimedOutException {
        UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(umlOperationBefore, umlOperationAfter, null);
        Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();

        //Check if refactored

        if (isVariableRefactored(refactorings, refactoringMiner, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
            return true;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper.getMatchedVariablesPair(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper.getAddedVariables(), refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
    }

    private boolean isAdded(Collection<Pair<VariableDeclaration, UMLOperation>> addedVariables, RefactoringMiner refactoringMiner, CodeElementType codeElementType, Queue<Variable> variables, Version currentVersion, Version parentVersion, Variable rightVariable) {
        for (Pair<VariableDeclaration, UMLOperation> addedVariable : addedVariables) {
            Variable variableAfter = Variable.of(addedVariable.getLeft(), addedVariable.getRight(), currentVersion);
            if (variableAfter.equalIdentifierIgnoringVersion(rightVariable)) {
                Variable variableBefore = Variable.of(addedVariable.getLeft(), addedVariable.getRight(), parentVersion);
                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleAdd(variableBefore, variableAfter);
                variables.add(variableBefore);
                refactoringMiner.connectRelatedNodes(codeElementType);
                return true;
            }
        }
        return false;
    }

    private boolean isMatched(Set<Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>>> matchedVariables, RefactoringMiner refactoringMiner, Queue<Variable> variables, CodeElementType codeElementType, Version currentVersion, Version parentVersion, Variable rightVariable) {
        for (Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair : matchedVariables) {
            Variable variableAfter = Variable.of(matchedVariablePair.getRight().getLeft(), matchedVariablePair.getRight().getRight(), currentVersion);
            if (variableAfter.equalIdentifierIgnoringVersion(rightVariable)) {
                Variable variableBefore = Variable.of(matchedVariablePair.getLeft().getLeft(), matchedVariablePair.getLeft().getRight(), parentVersion);
                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().addChange(variableBefore, variableAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                variables.add(variableBefore);
                refactoringMiner.connectRelatedNodes(codeElementType);
                return true;
            }
        }
        return false;
    }

    private boolean isMethodRefactored(Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Method> methods, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {
        Set<Method> leftMethodSet = refactoringMiner.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, equalOperator);
        methods.addAll(leftMethodSet);
        return !leftMethodSet.isEmpty();
    }

    private boolean isMethodContainerChanged(RefactoringMiner.CommitModel commitModel, Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Method> methods, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {
        Set<Method> leftMethodSet = new HashSet<>();
        boolean found = false;
        Change.Type changeType = Change.Type.CONTAINER_CHANGE;
        for (Refactoring refactoring : refactorings) {
            if (found)
                break;
            switch (refactoring.getRefactoringType()) {
                case RENAME_CLASS: {
                    RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                    UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                    UMLClass renamedClass = renameClassRefactoring.getRenamedClass();

                    for (String filePathsCurrent : commitModel.fileContentsCurrentOriginal.keySet()) {
                        if (originalClass.getLocationInfo().getFilePath().equals(filePathsCurrent)) {
                            changeType = Change.Type.METHOD_MOVE;
                        }
                    }
                    found = isMethodMatched(originalClass.getOperations(), renamedClass.getOperations(), refactoringMiner, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);

                    break;
                }
                case MOVE_CLASS: {
                    MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                    UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                    UMLClass movedClass = moveClassRefactoring.getMovedClass();
                    for (String filePathsCurrent : commitModel.fileContentsCurrentOriginal.keySet()) {
                        if (originalClass.getLocationInfo().getFilePath().equals(filePathsCurrent)) {
                            changeType = Change.Type.METHOD_MOVE;
                        }
                    }
                    found = isMethodMatched(originalClass.getOperations(), movedClass.getOperations(), refactoringMiner, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_RENAME_CLASS: {
                    MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                    UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                    UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                    for (String filePathsCurrent : commitModel.fileContentsCurrentOriginal.keySet()) {
                        if (originalClass.getLocationInfo().getFilePath().equals(filePathsCurrent)) {
                            changeType = Change.Type.METHOD_MOVE;
                        }
                    }
                    found = isMethodMatched(originalClass.getOperations(), renamedClass.getOperations(), refactoringMiner, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_SOURCE_FOLDER: {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                    for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                        UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                        UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                        found = isMethodMatched(originalClass.getOperations(), movedClass.getOperations(), refactoringMiner, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                        if (found)
                            break;
                    }
                    break;
                }
            }
        }
        if (found) {
            methods.addAll(leftMethodSet);
            refactoringMiner.getRefactoringHandler().getMethodChangeHistoryGraph().connectRelatedNodes();
            return true;
        }
        return false;
    }

    private boolean isMethodMatched(List<UMLOperation> leftSide, List<UMLOperation> rightSide, RefactoringMiner refactoringMiner, Set<Method> leftMethodSet, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, Refactoring refactoring, Change.Type changeType) {
        Set<UMLOperation> leftMatched = new HashSet<>();
        Set<UMLOperation> rightMatched = new HashSet<>();
        for (UMLOperation leftOperation : leftSide) {
            if (leftMatched.contains(leftOperation))
                continue;
            for (UMLOperation rightOperation : rightSide) {
                if (rightMatched.contains(rightOperation))
                    continue;
                if (leftOperation.equalSignature(rightOperation)) {
                    if (refactoringMiner.addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, leftOperation, rightOperation, changeType))
                        return true;
                    leftMatched.add(leftOperation);
                    rightMatched.add(rightOperation);
                    break;
                }
            }
        }
        return false;
    }

    private boolean isMethodAdded(UMLModelDiff modelDiff, RefactoringMiner refactoringMiner, Queue<Method> methods, String className, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {

        for (UMLOperation operation : modelDiff.getAddedOperations()) {
            if (handleAddOperation(refactoringMiner, methods, currentVersion, parentVersion, equalOperator, operation))
                return true;
        }

        UMLClass addedClass = modelDiff.getAddedClass(className);
        if (addedClass != null) {
            for (UMLOperation operation : addedClass.getOperations()) {
                if (handleAddOperation(refactoringMiner, methods, currentVersion, parentVersion, equalOperator, operation))
                    return true;
            }
        }
        return false;
    }

    private boolean handleAddOperation(RefactoringMiner refactoringMiner, Queue<Method> methods, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, UMLOperation operation) {
        Method rightMethod = Method.of(operation, currentVersion);
        if (equalOperator.test(rightMethod)) {
            Method leftMethod = Method.of(operation, parentVersion);
            refactoringMiner.getRefactoringHandler().getMethodChangeHistoryGraph().handleAdd(leftMethod, rightMethod);
            refactoringMiner.getRefactoringHandler().getMethodChangeHistoryGraph().connectRelatedNodes();
            methods.add(leftMethod);
            return true;
        }
        return false;
    }

    private boolean isVariableRefactored(Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        Set<Variable> leftVariableSet = refactoringMiner.analyseVariableRefactorings(refactorings, currentVersion, parentVersion, equalOperator);
        for (Variable leftVariable : leftVariableSet) {
            variables.add(leftVariable);
            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().connectRelatedNodes();
            return true;
        }
        return false;
    }

    private boolean isMovedFromExtractionOrInline(Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Variable> variables, CodeElementType codeElementType, Version currentVersion, Version parentVersion, Variable rightVariable) {
        for (Refactoring refactoring : refactorings) {
            if (RefactoringType.EXTRACT_OPERATION.equals(refactoring.getRefactoringType())) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                if (isMatched(extractOperationRefactoring.getBodyMapper().getMatchedVariablesPair(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable))
                    return true;
            }

            if (RefactoringType.INLINE_OPERATION.equals(refactoring.getRefactoringType())) {
                InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                if (isMatched(inlineOperationRefactoring.getBodyMapper().getMatchedVariablesPair(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable))
                    return true;
            }
        }
        return false;
    }

    private boolean isVariableContainerChanged(Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Variable> variables, CodeElementType codeElementType, Version currentVersion, Version parentVersion, Variable rightVariable, Method rightMethod) throws RefactoringMinerTimedOutException {
        for (Refactoring refactoring : refactorings) {
            UMLOperation operationBefore = null;
            UMLOperation operationAfter = null;
            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    break;
                }
                case MOVE_AND_RENAME_OPERATION:
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    break;
                }
                case ADD_METHOD_ANNOTATION: {
                    AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) refactoring;
                    operationBefore = addMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = addMethodAnnotationRefactoring.getOperationAfter();
                    break;
                }
                case MODIFY_METHOD_ANNOTATION: {
                    ModifyMethodAnnotationRefactoring modifyMethodAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
                    operationBefore = modifyMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = modifyMethodAnnotationRefactoring.getOperationAfter();
                    break;
                }
                case REMOVE_METHOD_ANNOTATION: {
                    RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) refactoring;
                    operationBefore = removeMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = removeMethodAnnotationRefactoring.getOperationAfter();
                    break;
                }
                case CHANGE_RETURN_TYPE: {
                    ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) refactoring;
                    operationBefore = changeReturnTypeRefactoring.getOperationBefore();
                    operationAfter = changeReturnTypeRefactoring.getOperationAfter();
                    break;
                }
                case SPLIT_PARAMETER: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) refactoring;
                    operationBefore = splitVariableRefactoring.getOperationBefore();
                    operationAfter = splitVariableRefactoring.getOperationAfter();
                    break;
                }
                case MERGE_PARAMETER: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                    operationBefore = mergeVariableRefactoring.getOperationBefore();
                    operationAfter = mergeVariableRefactoring.getOperationAfter();
                    break;
                }
                case RENAME_PARAMETER:
                case PARAMETERIZE_VARIABLE: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    if (!renameVariableRefactoring.isExtraction()) {
                        operationBefore = renameVariableRefactoring.getOperationBefore();
                        operationAfter = renameVariableRefactoring.getOperationAfter();
                    }
                    break;
                }
                case CHANGE_PARAMETER_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    operationBefore = changeVariableTypeRefactoring.getOperationBefore();
                    operationAfter = changeVariableTypeRefactoring.getOperationAfter();
                    break;
                }
                case ADD_PARAMETER: {
                    AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) refactoring;
                    operationBefore = addParameterRefactoring.getOperationBefore();
                    operationAfter = addParameterRefactoring.getOperationAfter();
                    break;
                }
                case REMOVE_PARAMETER: {
                    RemoveParameterRefactoring removeParameterRefactoring = (RemoveParameterRefactoring) refactoring;
                    operationBefore = removeParameterRefactoring.getOperationBefore();
                    operationAfter = removeParameterRefactoring.getOperationAfter();
                    break;
                }
                case REORDER_PARAMETER: {
                    ReorderParameterRefactoring reorderParameterRefactoring = (ReorderParameterRefactoring) refactoring;
                    operationBefore = reorderParameterRefactoring.getOperationBefore();
                    operationAfter = reorderParameterRefactoring.getOperationAfter();
                    break;
                }
                case ADD_PARAMETER_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    operationBefore = addVariableModifierRefactoring.getOperationBefore();
                    operationAfter = addVariableModifierRefactoring.getOperationAfter();
                    break;
                }
                case REMOVE_PARAMETER_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    operationBefore = removeVariableModifierRefactoring.getOperationBefore();
                    operationAfter = removeVariableModifierRefactoring.getOperationAfter();
                    break;
                }
                case ADD_PARAMETER_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    operationBefore = addVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = addVariableAnnotationRefactoring.getOperationAfter();
                    break;
                }
                case REMOVE_PARAMETER_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    operationBefore = removeVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = removeVariableAnnotationRefactoring.getOperationAfter();
                    break;
                }
                case MODIFY_PARAMETER_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    operationBefore = modifyVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = modifyVariableAnnotationRefactoring.getOperationAfter();
                    break;
                }
                case ADD_THROWN_EXCEPTION_TYPE: {
                    AddThrownExceptionTypeRefactoring addThrownExceptionTypeRefactoring = (AddThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = addThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = addThrownExceptionTypeRefactoring.getOperationAfter();
                    break;
                }
                case CHANGE_THROWN_EXCEPTION_TYPE: {
                    ChangeThrownExceptionTypeRefactoring changeThrownExceptionTypeRefactoring = (ChangeThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = changeThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = changeThrownExceptionTypeRefactoring.getOperationAfter();
                    break;
                }
                case REMOVE_THROWN_EXCEPTION_TYPE: {
                    RemoveThrownExceptionTypeRefactoring removeThrownExceptionTypeRefactoring = (RemoveThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = removeThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = removeThrownExceptionTypeRefactoring.getOperationAfter();
                    break;
                }
                case CHANGE_OPERATION_ACCESS_MODIFIER: {
                    ChangeOperationAccessModifierRefactoring changeOperationAccessModifierRefactoring = (ChangeOperationAccessModifierRefactoring) refactoring;
                    operationBefore = changeOperationAccessModifierRefactoring.getOperationBefore();
                    operationAfter = changeOperationAccessModifierRefactoring.getOperationAfter();
                    break;
                }
                case ADD_METHOD_MODIFIER: {
                    AddMethodModifierRefactoring addMethodModifierRefactoring = (AddMethodModifierRefactoring) refactoring;
                    operationBefore = addMethodModifierRefactoring.getOperationBefore();
                    operationAfter = addMethodModifierRefactoring.getOperationAfter();
                    break;
                }
                case REMOVE_METHOD_MODIFIER: {
                    RemoveMethodModifierRefactoring removeMethodModifierRefactoring = (RemoveMethodModifierRefactoring) refactoring;
                    operationBefore = removeMethodModifierRefactoring.getOperationBefore();
                    operationAfter = removeMethodModifierRefactoring.getOperationAfter();
                    break;
                }
            }

            if (operationAfter != null) {
                Method methodAfter = Method.of(operationAfter, currentVersion);
                if (rightMethod.equalIdentifierIgnoringVersion(methodAfter)) {
                    if (checkBodyOfMatchedOperations(refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable, operationAfter, operationBefore))
                        return true;
                }
            }
        }
        return false;
    }

    private Variable findVariable(String variableName, int variableDeclarationLineNumber, Method methodByName) {
        for (VariableDeclaration variable : methodByName.getUmlOperation().getAllVariableDeclarations()) {
            if (variable.getVariableName().equals(variableName) &&
                    variable.getLocationInfo().getStartLine() <= variableDeclarationLineNumber &&
                    variable.getLocationInfo().getEndLine() >= variableDeclarationLineNumber) {
                return Variable.of(variable, methodByName);

            }
        }
        return null;
    }

//    private void handleAddedVariables(RefactoringMiner refactoringMiner, String commitId, String parentCommitId, Collection<Pair<VariableDeclaration, UMLOperation>> addedVariables) {
//        for (Pair<VariableDeclaration, UMLOperation> addedVariable : addedVariables) {
//            refactoringMiner.getRefactoringHandler().handleAddedVariable(commitId, parentCommitId, addedVariable.getRight(), addedVariable.getLeft());
//        }
//    }
//
//    private void handleMatchedVariables(RefactoringMiner refactoringMiner, String commitId, String parentCommitId, Collection<Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>>> matchedVariables) {
//        for (Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair : matchedVariables) {
//            refactoringMiner.getRefactoringHandler().handleMatchedVariable(commitId, parentCommitId, matchedVariablePair);
//        }
//    }


    private Variable findVariable(Variable currentVariable, Method method) {
        for (VariableDeclaration variableDeclaration : method.getUmlOperation().getAllVariableDeclarations()) {
            Variable variable = Variable.of(variableDeclaration, method);
            if (currentVariable.equalIdentifierIgnoringVersion(variable)) {
                return variable;
            }
        }
        return null;
    }

//    private Pair<Method, Method> getMethodPair(Method currentMethod, String commitId, RefactoringMiner refactoringMiner) throws RefactoringMinerTimedOutException {
//        CodeElementType codeElementType = CodeElementType.METHOD;
//        Pair<Pair<UMLModel, UMLModel>, UMLModel> umlModelPair = refactoringMiner.getUMLModelPair(commitId, Collections.singletonList(currentMethod.getFilePath()));
//        if (umlModelPair == null) {
//            return null;
//        }
//        Version currentVersion = refactoringMiner.getVersion(commitId);
//        String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
//        Version leftVersion = refactoringMiner.getVersion(parentCommitId);
//        Method rightMethod = RefactoringMiner.getMethod(umlModelPair.getRight(), currentVersion, currentMethod::equalIdentifierIgnoringVersion);
//        if (rightMethod == null) {
//            return null;
//        }
//        refactoringMiner.addNode(codeElementType, rightMethod);
//        if (umlModelPair.getLeft() == null) {
//            Method left = Method.of(rightMethod.getUmlOperation(), leftVersion);
//            left.setAdded(true);
//            refactoringMiner.getRefactoringHandler().getMethodChangeHistoryGraph().handleAdd(left, rightMethod);
//            refactoringMiner.connectRelatedNodes(codeElementType);
//            return Pair.of(left, rightMethod);
//        }
//
//        //NO CHANGE
//        Method leftMethod = getLeftMethod(umlModelPair, leftVersion, currentMethod::equalIdentifierIgnoringVersion);
//        if (leftMethod != null)
//            return Pair.of(leftMethod, rightMethod);
//
//        //Change Documentation
//        leftMethod = getLeftMethod(umlModelPair, leftVersion, currentMethod::equalIdentifierIgnoringVersionAndDocument);
//        if (leftMethod != null) {
//            refactoringMiner.addEdge(codeElementType, leftMethod, rightMethod, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The documentation of the method element is changed."));
//            refactoringMiner.connectRelatedNodes(codeElementType);
//            return Pair.of(leftMethod, rightMethod);
//        }
//
//        //Change Body
//        leftMethod = getLeftMethod(umlModelPair, leftVersion, currentMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
//        if (leftMethod != null) {
//            refactoringMiner.addEdge(codeElementType, leftMethod, rightMethod, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
//            refactoringMiner.connectRelatedNodes(codeElementType);
//            return Pair.of(leftMethod, rightMethod);
//        }
//        //Local Refactoring
//        {
//            UMLModelDiff umlModelDiff = umlModelPair.getLeft().getLeft().diff(umlModelPair.getRight(), new ConcurrentHashMap<>());
//
//            List<Refactoring> refactorings = umlModelDiff.getRefactorings();
//            handleMatchedVariables(refactoringMiner, commitId, parentCommitId, rightMethod, umlModelDiff);
//            for (Pair<VariableDeclaration, UMLOperation> removedVariable : umlModelDiff.getRemovedVariables()) {
//                refactoringMiner.getRefactoringHandler().handleRemovedVariable(commitId, parentCommitId, removedVariable.getRight(), removedVariable.getLeft());
//            }
//            for (Pair<VariableDeclaration, UMLOperation> addedVariable : umlModelDiff.getAddedVariables()) {
//                refactoringMiner.getRefactoringHandler().handleAddedVariable(commitId, parentCommitId, addedVariable.getRight(), addedVariable.getLeft());
//            }
//
//            refactoringMiner.getRefactoringHandler().handle(commitId, refactorings);
//            refactoringMiner.connectRelatedNodes(codeElementType);
//
//            Set<CodeElement> predecessors = refactoringMiner.predecessors(codeElementType, rightMethod);
//            if (!predecessors.isEmpty()) {
//                for (CodeElement leftElement : predecessors) {
//                    leftMethod = (Method) leftElement;
//                    return Pair.of(leftMethod, rightMethod);
//                }
//            }
//        }
//        //All refactorings
//        {
//            List<UMLModelDiff> umlModelDiffList = refactoringMiner.getUMLModelDiff(commitId, Collections.singletonList(currentMethod.getFilePath()));
//            for (UMLModelDiff modelDiff : umlModelDiffList) {
//                List<Refactoring> refactorings = modelDiff.getRefactorings();
//                refactoringMiner.getRefactoringHandler().handle(commitId, refactorings);
//                refactoringMiner.connectRelatedNodes(codeElementType);
//                UMLClassBaseDiff umlClassDiff = modelDiff.getUMLClassDiff(currentMethod.getUmlOperation().getClassName());
//                if (umlClassDiff != null) {
//                    for (UMLOperation operation : umlClassDiff.getAddedOperations()) {
//                        Pair<Method, Method> left = getMethodPair(currentMethod, refactoringMiner, codeElementType, currentVersion, leftVersion, rightMethod, operation);
//                        if (left != null) return left;
//                    }
//                }
//                UMLClass addedClass = modelDiff.getAddedClass(currentMethod.getUmlOperation().getClassName());
//                if (addedClass != null) {
//                    for (UMLOperation operation : addedClass.getOperations()) {
//                        Pair<Method, Method> left = getMethodPair(currentMethod, refactoringMiner, codeElementType, currentVersion, leftVersion, rightMethod, operation);
//                        if (left != null) return left;
//                    }
//                }
//            }
//            Set<CodeElement> predecessors = refactoringMiner.predecessors(codeElementType, rightMethod);
//            if (!predecessors.isEmpty()) {
//                for (CodeElement leftElement : predecessors) {
//                    leftMethod = (Method) leftElement;
//                    return Pair.of(leftMethod, rightMethod);
//                }
//            }
//        }
//
//        return null;
//    }

//    private void handleBodyChange(RefactoringMiner refactoringMiner, CodeElementType codeElementType, Method rightMethod, Method leftMethod) {
//        if (RefactoringHandlerImpl.checkOperationBodyChanged(leftMethod.getUmlOperation().getBody(), rightMethod.getUmlOperation().getBody())) {
//            refactoringMiner.addEdge(codeElementType, leftMethod, rightMethod, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
//        }
//        refactoringMiner.addEdge(codeElementType, leftMethod, rightMethod, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
//        refactoringMiner.connectRelatedNodes(codeElementType);
//    }

    private Pair<Method, Method> getMethodPair(Method currentMethod, RefactoringMiner refactoringMiner, CodeElementType codeElementType, Version currentVersion, Version leftVersion, Method rightMethod, UMLOperation operation) {
        Method method = Method.of(operation, currentVersion);
        if (method.equalIdentifierIgnoringVersion(currentMethod)) {
            Method left = Method.of(rightMethod.getUmlOperation(), leftVersion);
            left.setAdded(true);
            refactoringMiner.getRefactoringHandler().getMethodChangeHistoryGraph().handleAdd(left, rightMethod);
            refactoringMiner.connectRelatedNodes(codeElementType);
            return Pair.of(left, rightMethod);
        }
        return null;
    }

    private Method getLeftMethod(Pair<Pair<UMLModel, UMLModel>, UMLModel> umlModelPair, Version leftVersion, Predicate<Method> predicate) {
        Method left1 = RefactoringMiner.getMethod(umlModelPair.getLeft().getLeft(), leftVersion, predicate);
        Method left2 = RefactoringMiner.getMethod(umlModelPair.getLeft().getRight(), leftVersion, predicate);

        if (left1 != null || left2 != null) {
            if (left1 != null)
                return left1;
            return left2;
        }
        return null;
    }

    private List<String> getCommits(String filePath, Git git) throws GitAPIException {
        LogCommand logCommandFile = git.log().addPath(filePath).setRevFilter(RevFilter.NO_MERGES);
        Iterable<RevCommit> fileRevisions = logCommandFile.call();
        return StreamSupport.stream(fileRevisions.spliterator(), false).map(revCommit -> revCommit.getId().getName()).collect(Collectors.toList());
    }

    private List<String> getCommits(Repository repository, String startCommitId, String filePath, Git git) throws IOException, GitAPIException {
        LogCommand logCommandFile = git.log().add(repository.resolve(startCommitId)).addPath(filePath).setRevFilter(RevFilter.ALL);
        Iterable<RevCommit> fileRevisions = logCommandFile.call();
        return StreamSupport.stream(fileRevisions.spliterator(), false).map(revCommit -> revCommit.getId().getName()).collect(Collectors.toList());
    }

    public RefactoringResult detect(Repository repository, String startTag, String endTag) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        final RefactoringResult result = new RefactoringResult();

        ResultImpl refinerResult = (ResultImpl) analyseBetweenTags(repository, startTag, endTag);
        result.setRefactoringMinerAllCommitProcessTime(refinerResult.getRefactoringMinerProcessTime());
        result.setRefactoringRefinerProcessTime(refinerResult.getRefactoringRefinerProcessTime());
        result.getRefactoringRefiner().addAll(refinerResult.getAggregatedRefactorings());
        result.getRefactoringMinerAllCommits().addAll(refinerResult.getRefactorings());

        long startTime = System.nanoTime();
        miner.detectAtCommit(repository, startTag, endTag, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                result.getRefactoringMinerFirstAndLast().addAll(refactorings);
            }
        });
        result.setRefactoringMinerFirstLastCommitProcessTime((System.nanoTime() - startTime) / 1000000);
        result.calculateOtherAttributes();
        result.setNumberOfCommits(refinerResult.getCommitCount());
        result.setSameCodeElementChangeRate(refinerResult.getSameCodeElementChangeRate());
        return result;
    }

}
