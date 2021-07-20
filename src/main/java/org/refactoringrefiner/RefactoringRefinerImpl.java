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
import org.refactoringrefiner.edge.AbstractChange;
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

    public History<CodeElement, Edge> findMethodHistory(String projectDirectory, String repositoryWebURL, String startCommitId, String filePath, String elementKey) {
        GitService gitService = new GitServiceImpl();
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
            try (Git git = new Git(repository)) {
                git.fetch().setRemote("origin").call();
                RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);

                UMLModel umlModel = refactoringMiner.getUMLModel(startCommitId, Collections.singletonList(filePath));
                Method start = RefactoringMiner.getMethodByName(umlModel, refactoringMiner.getVersion(startCommitId), elementKey);
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
                    Method currentElement = methods.poll();
                    if (currentElement.isAdded()) {
                        continue;
                    }
                    if (!filePath.equals(currentElement.getFilePath()) || commits == null) {
                        commits = getCommits(repository, currentElement.getVersion().getId(), currentElement.getFilePath(), git);
                        analysedCommits.clear();
                    }
                    if (analysedCommits.containsAll(commits))
                        break;
                    for (String commitId : commits) {
                        if (analysedCommits.contains(commitId))
                            continue;
                        System.out.println("processing " + commitId);
                        analysedCommits.add(commitId);
                        Pair<Pair<UMLModel, UMLModel>, UMLModel> umlModelPair = refactoringMiner.getUMLModelPair(commitId, Collections.singletonList(currentElement.getFilePath()));
                        if (umlModelPair == null) {
                            break;
                        }
                        Version currentVersion = refactoringMiner.getVersion(commitId);
                        Method right = RefactoringMiner.getMethod(umlModelPair.getRight(), currentVersion, currentElement::equalIdentifierIgnoringVersion);
                        if (right == null) {
                            //System.out.println("Something is wrong!");
                            continue;
                        }

                        //handle first commit of repository which doesn't have any parent
                        if (umlModelPair.getLeft() == null) {
                            refactoringMiner.getRefactoringHandler().handleAddedMethod(commitId, "0", right.getUmlOperation());
                            refactoringMiner.connectRelatedNodes(codeElementType);
                            break;
                        }

                        String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
                        Version leftVersion = refactoringMiner.getVersion(parentCommitId);
                        Method left1 = RefactoringMiner.getMethod(umlModelPair.getLeft().getLeft(), leftVersion, currentElement::equalIdentifierIgnoringVersion);
                        Method left2 = RefactoringMiner.getMethod(umlModelPair.getLeft().getRight(), leftVersion, currentElement::equalIdentifierIgnoringVersion);

                        //NO CHANGE
                        if (left1 != null || left2 != null) {
                            System.out.println("No Change!");
                            continue;
                        }

                        //Change Documentation
                        left1 = RefactoringMiner.getMethod(umlModelPair.getLeft().getLeft(), leftVersion, currentElement::equalIdentifierIgnoringVersionAndDocument);
                        left2 = RefactoringMiner.getMethod(umlModelPair.getLeft().getRight(), leftVersion, currentElement::equalIdentifierIgnoringVersionAndDocument);


                        if (left1 != null || left2 != null) {
                            if (left1 != null) {
                                refactoringMiner.addEdge(codeElementType, left1, right, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The documentation of the method element is changed."));
                                methods.add(left1);
                            } else {
                                refactoringMiner.addEdge(codeElementType, left2, right, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The documentation of the method element is changed."));
                                methods.add(left2);
                            }
                            refactoringMiner.connectRelatedNodes(codeElementType);
                            System.out.println("Documentation Change!");
                            break;
                        }

                        //CHANGE BODY
                        left1 = RefactoringMiner.getMethod(umlModelPair.getLeft().getLeft(), leftVersion, currentElement::equalIdentifierIgnoringVersionAndDocumentAndBody);
                        left2 = RefactoringMiner.getMethod(umlModelPair.getLeft().getRight(), leftVersion, currentElement::equalIdentifierIgnoringVersionAndDocumentAndBody);


                        if (left1 != null || left2 != null) {
                            if (left1 != null) {
                                refactoringMiner.addEdge(codeElementType, left1, right, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
                                methods.add(left1);
                            } else {
                                refactoringMiner.addEdge(codeElementType, left2, right, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
                                methods.add(left2);
                            }
                            refactoringMiner.connectRelatedNodes(codeElementType);
                            System.out.println("BODY Change!");
                            break;
                        }

                        //Local Refactoring
                        {
                            refactoringMiner.addNode(codeElementType, right);
                            UMLModelDiff umlModelDiff = umlModelPair.getLeft().getLeft().diff(umlModelPair.getRight(), new ConcurrentHashMap<>());

                            UMLClassBaseDiff umlClassDiff = umlModelDiff.getUMLClassDiff(currentElement.getUmlOperation().getClassName());
                            if (umlClassDiff != null) {
                                for (UMLOperationDiff operationDiff : umlClassDiff.getOperationDiffList()) {
                                    Method method = Method.of(operationDiff.getAddedOperation(), currentVersion);
                                    if (method.equalIdentifierIgnoringVersion(currentElement)) {
                                        refactoringMiner.getRefactoringHandler().analyze(commitId, operationDiff.getRefactorings());
                                        Method left = Method.of(operationDiff.getRemovedOperation(), leftVersion);
                                        handleBodyChange(refactoringMiner, codeElementType, right, left);
                                        break;
                                    }
                                }
                            }
//                            List<Refactoring> refactorings = umlModelDiff.getRefactorings();
//                            refactoringMiner.getRefactoringHandler().handle(commitId, refactorings);

                            Set<CodeElement> predecessors = refactoringMiner.predecessors(codeElementType, right);
                            if (!predecessors.isEmpty()) {
                                for (CodeElement leftElement : predecessors) {
                                    Method left = (Method) leftElement;

                                    methods.add(left);
                                }
                                System.out.println("Local Refactoring!");
                                break;
                            }
                        }
                        //All refactorings
                        {
                            List<UMLModelDiff> umlModelDiffList = refactoringMiner.getUMLModelDiff(commitId, Collections.singletonList(currentElement.getFilePath()));
                            for (UMLModelDiff modelDiff : umlModelDiffList) {
                                List<Refactoring> refactorings = modelDiff.getRefactorings();
                                refactoringMiner.getRefactoringHandler().handle(commitId, refactorings);
                                UMLClassBaseDiff umlClassDiff = modelDiff.getUMLClassDiff(currentElement.getUmlOperation().getClassName());
                                boolean flag = true;
                                if (umlClassDiff != null) {
                                    for (UMLOperation operation : umlClassDiff.getAddedOperations()) {
                                        Method method = Method.of(operation, currentVersion);
                                        if (method.equalIdentifierIgnoringVersion(currentElement)) {
                                            refactoringMiner.getRefactoringHandler().handleAddedMethod(commitId, parentCommitId, operation);
                                            flag = false;
                                            break;
                                        }
                                    }
                                }
                                if (flag) {
                                    UMLClass addedClass = modelDiff.getAddedClass(currentElement.getUmlOperation().getClassName());
                                    if (addedClass != null) {
                                        for (UMLOperation operation : addedClass.getOperations()) {
                                            Method method = Method.of(operation, currentVersion);
                                            if (method.equalIdentifierIgnoringVersion(currentElement)) {
                                                refactoringMiner.getRefactoringHandler().handleAddedMethod(commitId, parentCommitId, operation);
                                                break;
                                            }
                                        }
                                    }
                                }


                                refactoringMiner.connectRelatedNodes(codeElementType);
                            }
//                            refactoringMiner.detectAtCommit(commitId);
                            Set<CodeElement> predecessors = refactoringMiner.predecessors(codeElementType, right);
                            if (!predecessors.isEmpty()) {
                                for (CodeElement leftElement : predecessors) {
                                    methods.add((Method) leftElement);
                                }
                                System.out.println("Global Refactoring!");
                                break;
                            }
                        }
                        //Match with signature
                        left1 = RefactoringMiner.getMethod(umlModelPair.getLeft().getLeft(), leftVersion, currentElement::equalSignature);
                        left2 = RefactoringMiner.getMethod(umlModelPair.getLeft().getRight(), leftVersion, currentElement::equalSignature);


                        if (left1 != null || left2 != null) {
                            if (left1 != null) {
                                refactoringMiner.addEdge(codeElementType, left1, right, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                                methods.add(left1);
                            } else {
                                refactoringMiner.addEdge(codeElementType, left2, right, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                                methods.add(left2);
                            }
                            refactoringMiner.connectRelatedNodes(codeElementType);
                            System.out.println("Match Signature!");
                            break;
                        }
                    }
                }
                return new HistoryImpl<>(refactoringMiner.findSubGraph(codeElementType, start));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public History<CodeElement, Edge> findVariableHistory(String projectDirectory, String repositoryWebURL, String startCommitId, String filePath, String methodKey, String variableKey) {
        GitService gitService = new GitServiceImpl();
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
            try (Git git = new Git(repository)) {
                git.fetch().setRemote("origin").call();
                RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
                refactoringMiner.getRefactoringHandler().setTrackAttributes(false);
                refactoringMiner.getRefactoringHandler().setTrackClasses(false);
                refactoringMiner.getRefactoringHandler().setTrackMethods(false);
                UMLModel umlModel = refactoringMiner.getUMLModel(startCommitId, Collections.singletonList(filePath));
                Variable start = null;
                Version startVersion = refactoringMiner.getVersion(startCommitId);
                Method methodByName = RefactoringMiner.getMethodByName(umlModel, startVersion, methodKey);
                start = findVariable(variableKey, methodByName);
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

                        Pair<Pair<UMLModel, UMLModel>, UMLModel> umlModelPair = refactoringMiner.getUMLModelPair(commitId, Collections.singletonList(currentMethod.getFilePath()));
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


                        if (umlModelPair.getLeft() == null) {
                            Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
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
                                UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(leftMethod.getUmlOperation(), rightMethod.getUmlOperation(), null);
                                Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();

                                //Check if refactored
                                boolean found = isRefactored(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;
                                // check if it is in the matched
                                found = isMatched(umlOperationBodyMapper.getMatchedVariablesPair(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;
                                //Check if is added
                                found = isAdded(umlOperationBodyMapper.getAddedVariables(), refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;
                            }
                        }

                        //Local Refactoring
                        {
                            UMLModelDiff umlModelDiff = umlModelPair.getLeft().getLeft().diff(umlModelPair.getRight(), new ConcurrentHashMap<>());
                            List<Refactoring> refactorings = umlModelDiff.getRefactorings();

                            //Check if refactored
                            boolean found = isRefactored(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;

                            // check if it is in the matched
                            found = isMatched(umlModelDiff.getMatchedVariables(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;

                            found = isAdded(umlModelDiff.getAddedVariables(), refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;

                            found = isMovedFromExtraction(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                            if (found)
                                break;
                        }

                        //All refactorings
                        {
                            List<UMLModelDiff> umlModelDiffList = refactoringMiner.getUMLModelDiff(commitId, Collections.singletonList(rightMethod.getFilePath()));
                            boolean found = false;
                            for (UMLModelDiff modelDiff : umlModelDiffList) {
                                if (found) {
                                    break;
                                }
                                List<Refactoring> refactorings = modelDiff.getRefactorings();

                                UMLClassBaseDiff umlClassDiff = modelDiff.getUMLClassDiff(rightMethod.getUmlOperation().getClassName());
                                if (umlClassDiff != null) {
                                    for (UMLOperation operation : umlClassDiff.getAddedOperations()) {
                                        Method method = Method.of(operation, parentVersion);
                                        if (method.equalIdentifierIgnoringVersion(rightMethod)) {
                                            List<Pair<VariableDeclaration, UMLOperation>> addedVariables = method.getUmlOperation().getBody().getAllVariableDeclarations().stream().filter(variableDeclaration -> !variableDeclaration.isParameter()).map(variableDeclaration -> Pair.of(variableDeclaration, method.getUmlOperation())).collect(Collectors.toList());
                                            found = isAdded(addedVariables, refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                            if (found)
                                                break;
                                        }
                                    }
                                }
                                if (found) {
                                    break;
                                }

                                UMLClass addedClass = modelDiff.getAddedClass(rightMethod.getUmlOperation().getClassName());
                                if (addedClass != null) {
                                    for (UMLOperation operation : addedClass.getOperations()) {
                                        Method method = Method.of(operation, currentVersion);
                                        if (method.equalIdentifierIgnoringVersion(rightMethod)) {
                                            List<Pair<VariableDeclaration, UMLOperation>> addedVariables = method.getUmlOperation().getBody().getAllVariableDeclarations().stream().filter(variableDeclaration -> !variableDeclaration.isParameter()).map(variableDeclaration -> Pair.of(variableDeclaration, method.getUmlOperation())).collect(Collectors.toList());
                                            found = isAdded(addedVariables, refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                            if (found)
                                                break;
                                        }
                                    }
                                }
                                if (found)
                                    break;

                                //Check if refactored
                                found = isRefactored(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;

                                found = isMatched(modelDiff.getMatchedVariables(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;

                                found = isAdded(modelDiff.getAddedVariables(), refactoringMiner, codeElementType, variables, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;

                                found = isMovedFromExtraction(refactorings, refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable);
                                if (found)
                                    break;
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
                return new HistoryImpl<>(refactoringMiner.findSubGraph(codeElementType, start));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
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

    private boolean isRefactored(Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Variable> variables, CodeElementType codeElementType, Version currentVersion, Version parentVersion, Variable rightVariable) {
        Variable leftVariable = null;
        for (Refactoring refactoring : refactorings) {
            Variable variableAfter = null;
            Variable variableBefore = null;
            switch (refactoring.getRefactoringType()) {
                case RENAME_VARIABLE: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    variableAfter = Variable.of(renameVariableRefactoring.getRenamedVariable(), renameVariableRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(renameVariableRefactoring.getOriginalVariable(), renameVariableRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
                case CHANGE_VARIABLE_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    variableAfter = Variable.of(changeVariableTypeRefactoring.getChangedTypeVariable(), changeVariableTypeRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(changeVariableTypeRefactoring.getOriginalVariable(), changeVariableTypeRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
                case ADD_VARIABLE_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    variableAfter = Variable.of(addVariableModifierRefactoring.getVariableAfter(), addVariableModifierRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(addVariableModifierRefactoring.getVariableBefore(), addVariableModifierRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
                case REMOVE_VARIABLE_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    variableAfter = Variable.of(removeVariableModifierRefactoring.getVariableAfter(), removeVariableModifierRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(removeVariableModifierRefactoring.getVariableBefore(), removeVariableModifierRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
                case ADD_VARIABLE_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    variableAfter = Variable.of(addVariableAnnotationRefactoring.getVariableAfter(), addVariableAnnotationRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(addVariableAnnotationRefactoring.getVariableBefore(), addVariableAnnotationRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
                case MODIFY_VARIABLE_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    variableAfter = Variable.of(modifyVariableAnnotationRefactoring.getVariableAfter(), modifyVariableAnnotationRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(modifyVariableAnnotationRefactoring.getVariableBefore(), modifyVariableAnnotationRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
                case REMOVE_VARIABLE_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    variableAfter = Variable.of(removeVariableAnnotationRefactoring.getVariableAfter(), removeVariableAnnotationRefactoring.getOperationAfter(), currentVersion);
                    variableBefore = Variable.of(removeVariableAnnotationRefactoring.getVariableBefore(), removeVariableAnnotationRefactoring.getOperationBefore(), parentVersion);
                    break;
                }
            }
            if (variableAfter != null && variableAfter.equalIdentifierIgnoringVersion(rightVariable)) {
                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().addRefactored(variableBefore, variableAfter, refactoring);
                leftVariable = variableBefore;
            }
        }
        if (leftVariable != null) {
            variables.add(leftVariable);
            refactoringMiner.connectRelatedNodes(codeElementType);
            return true;
        }
        return false;
    }

    private boolean isMovedFromExtraction(Collection<Refactoring> refactorings, RefactoringMiner refactoringMiner, Queue<Variable> variables, CodeElementType codeElementType, Version currentVersion, Version parentVersion, Variable rightVariable) {
        for (Refactoring refactoring : refactorings) {
            if (RefactoringType.EXTRACT_OPERATION.equals(refactoring.getRefactoringType())) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                if (isMatched(extractOperationRefactoring.getBodyMapper().getMatchedVariablesPair(), refactoringMiner, variables, codeElementType, currentVersion, parentVersion, rightVariable))
                    return true;
            }
        }
        return false;
    }

    private Variable findVariable(String variableKey, Method methodByName) {
        for (VariableDeclaration variable : methodByName.getUmlOperation().getAllVariableDeclarations()) {
            if (variable.isParameter())
                continue;
            if (variable.toString().equals(variableKey)) {
                return Variable.of(variable, methodByName);

            }
        }
        return null;
    }

    private void handleAddedVariables(RefactoringMiner refactoringMiner, String commitId, String parentCommitId, Collection<Pair<VariableDeclaration, UMLOperation>> addedVariables) {
        for (Pair<VariableDeclaration, UMLOperation> addedVariable : addedVariables) {
            refactoringMiner.getRefactoringHandler().handleAddedVariable(commitId, parentCommitId, addedVariable.getRight(), addedVariable.getLeft());
        }
    }

    private void handleMatchedVariables(RefactoringMiner refactoringMiner, String commitId, String parentCommitId, Collection<Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>>> matchedVariables) {
        for (Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair : matchedVariables) {
            refactoringMiner.getRefactoringHandler().handleMatchedVariable(commitId, parentCommitId, matchedVariablePair);
        }
    }


    private Variable findVariable(Variable currentVariable, Method method) {
        for (VariableDeclaration variableDeclaration : method.getUmlOperation().getAllVariableDeclarations()) {
            if (variableDeclaration.isParameter())
                continue;
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

    private void handleBodyChange(RefactoringMiner refactoringMiner, CodeElementType codeElementType, Method rightMethod, Method leftMethod) {
        if (RefactoringHandlerImpl.checkOperationBodyChanged(leftMethod.getUmlOperation().getBody(), rightMethod.getUmlOperation().getBody())) {
            refactoringMiner.addEdge(codeElementType, leftMethod, rightMethod, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
        }
        refactoringMiner.addEdge(codeElementType, leftMethod, rightMethod, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
        refactoringMiner.connectRelatedNodes(codeElementType);
    }

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
