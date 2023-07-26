package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.Version;
import org.codetracker.element.Method;
import org.codetracker.util.GitRepository;
import org.codetracker.util.IRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class BaseTracker {
    private static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    protected final GitServiceImpl gitService = new GitServiceImpl();
    protected final Repository repository;
    protected final IRepository gitRepository;
    protected final String startCommitId;
    protected final String filePath;

    public BaseTracker(Repository repository, String startCommitId, String filePath) {
        this.repository = repository;
        this.gitRepository = new GitRepository(repository);
        this.startCommitId = startCommitId;
        this.filePath = filePath;
    }

    protected static UMLClassBaseDiff lightweightClassDiff(UMLModel leftModel, UMLModel rightModel, VariableDeclarationContainer leftOperation, VariableDeclarationContainer rightOperation) {
        UMLClass leftClass = null;
        for (UMLClass clazz : leftModel.getClassList()) {
            if (clazz.getName().equals(leftOperation.getClassName())) {
                leftClass = clazz;
                break;
            }
        }
        UMLClass rightClass = null;
        for (UMLClass clazz : rightModel.getClassList()) {
            if (clazz.getName().equals(rightOperation.getClassName())) {
                rightClass = clazz;
                break;
            }
        }
        if (leftClass != null && rightClass != null) {
            UMLClassDiff classDiff = new UMLClassDiff(leftClass, rightClass, null);
            for (UMLOperation operation : leftClass.getOperations()) {
                int index = rightClass.getOperations().indexOf(operation);
                UMLOperation operation2 = null;
                if (index != -1) {
                    operation2 = rightClass.getOperations().get(index);
                }
                if (index == -1 || differentParameterNames(leftClass, rightClass, operation, operation2))
                    classDiff.getRemovedOperations().add(operation);
            }
            for (UMLOperation operation : rightClass.getOperations()) {
                int index = leftClass.getOperations().indexOf(operation);
                UMLOperation operation1 = null;
                if (index != -1) {
                    operation1 = leftClass.getOperations().get(index);
                }
                if (index == -1 || differentParameterNames(leftClass, rightClass, operation1, operation))
                    classDiff.getAddedOperations().add(operation);
            }
            return classDiff;
        }
        return null;
    }

    private static boolean differentParameterNames(UMLClass leftClass, UMLClass rightClass, UMLOperation operation1, UMLOperation operation2) {
        if (operation1 != null && operation2 != null && !operation1.getParameterNameList().equals(operation2.getParameterNameList())) {
            int methodsWithIdenticalName1 = 0;
            for (UMLOperation operation : leftClass.getOperations()) {
                if (operation != operation1 && operation.getName().equals(operation1.getName()) && !operation.hasVarargsParameter()) {
                    methodsWithIdenticalName1++;
                }
            }
            int methodsWithIdenticalName2 = 0;
            for (UMLOperation operation : rightClass.getOperations()) {
                if (operation != operation2 && operation.getName().equals(operation2.getName()) && !operation.hasVarargsParameter()) {
                    methodsWithIdenticalName2++;
                }
            }
            if (methodsWithIdenticalName1 > 0 && methodsWithIdenticalName2 > 0) {
                return true;
            }
        }
        return false;
    }

    protected static boolean containsCallToExtractedMethod(UMLOperationBodyMapper bodyMapper, UMLAbstractClassDiff classDiff) {
        if(classDiff != null) {
            List<UMLOperation> addedOperations = classDiff.getAddedOperations();
            for(AbstractCodeFragment leaf2 : bodyMapper.getNonMappedLeavesT2()) {
                AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
                if(invocation == null) {
                    invocation = leaf2.assignmentInvocationCoveringEntireStatement();
                }
                UMLOperation matchingOperation = null;
                if(invocation != null && (matchingOperation = matchesOperation(invocation, addedOperations, bodyMapper.getContainer2(), classDiff)) != null && matchingOperation.getBody() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static UMLOperation matchesOperation(AbstractCall invocation, List<UMLOperation> operations, VariableDeclarationContainer callerOperation, UMLAbstractClassDiff classDiff) {
        for(UMLOperation operation : operations) {
            if(invocation.matchesOperation(operation, callerOperation, classDiff,null))
                return operation;
        }
        return null;
    }

    protected static List<String> getCommits(Repository repository, String startCommitId, String filePath, Git git) throws IOException, GitAPIException {
        LogCommand logCommandFile = git.log().add(repository.resolve(startCommitId)).addPath(filePath).setRevFilter(RevFilter.ALL);
        Iterable<RevCommit> fileRevisions = logCommandFile.call();
        return StreamSupport.stream(fileRevisions.spliterator(), false).map(revCommit -> revCommit.getId().getName()).collect(Collectors.toList());
    }

    protected static UMLOperationBodyMapper findBodyMapper(UMLModelDiff umlModelDiff, Method method, Version currentVersion, Version parentVersion) {
        UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiff, method.getUmlOperation().getClassName());
        if (umlClassDiff != null) {
            for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
                Method methodLeft = Method.of(operationBodyMapper.getContainer1(), parentVersion);
                if (method.equalIdentifierIgnoringVersion(methodLeft)) {
                    return operationBodyMapper;
                }
                Method methodRight = Method.of(operationBodyMapper.getContainer2(), currentVersion);
                if (method.equalIdentifierIgnoringVersion(methodRight)) {
                    return operationBodyMapper;
                }
            }
        }
        return null;
    }

    protected static UMLClassBaseDiff getUMLClassDiff(UMLModelDiff umlModelDiff, String className) {
        int maxMatchedMembers = 0;
        UMLClassBaseDiff maxRenameDiff = null;
        UMLClassBaseDiff sameNameDiff = null;
        for (UMLClassBaseDiff classDiff : getAllClassesDiff(umlModelDiff)) {
            if (classDiff.getOriginalClass().getName().equals(className) || classDiff.getNextClass().getName().equals(className)) {
                if (classDiff instanceof UMLClassRenameDiff) {
                    UMLClassMatcher.MatchResult matchResult = ((UMLClassRenameDiff) classDiff).getMatchResult();
                    int matchedMembers = matchResult.getMatchedOperations() + matchResult.getMatchedAttributes();
                    if (matchedMembers > maxMatchedMembers) {
                        maxMatchedMembers = matchedMembers;
                        maxRenameDiff = classDiff;
                    }
                }
                else if (classDiff instanceof UMLClassMoveDiff) {
                    UMLClassMatcher.MatchResult matchResult = ((UMLClassMoveDiff) classDiff).getMatchResult();
                    int matchedMembers = matchResult.getMatchedOperations() + matchResult.getMatchedAttributes();
                    if (matchedMembers > maxMatchedMembers) {
                        maxMatchedMembers = matchedMembers;
                        maxRenameDiff = classDiff;
                    }
                }
                else {
                    sameNameDiff = classDiff;
                }
            }
        }
        return sameNameDiff != null ? sameNameDiff : maxRenameDiff;
    }

    protected static Pair<UMLModel, UMLModel> getUMLModelPair(final CommitModel commitModel, final String rightSideFileName, final Predicate<String> rightSideFileNamePredicate, final boolean filterLeftSide) throws Exception {
        if (rightSideFileName == null)
            throw new IllegalArgumentException("File name could not be null.");

        if (filterLeftSide) {
            String leftSideFileName = rightSideFileName;
            if (commitModel.moveSourceFolderRefactorings != null) {
                boolean found = false;
                for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : commitModel.moveSourceFolderRefactorings) {
                    if (found)
                        break;
                    for (Map.Entry<String, String> identicalPath : moveSourceFolderRefactoring.getIdenticalFilePaths().entrySet()) {
                        if (identicalPath.getValue().equals(rightSideFileName)) {
                            leftSideFileName = identicalPath.getKey();
                            found = true;
                            break;
                        }
                    }
                }
            }

            final String leftSideFileNameFinal = leftSideFileName;
            UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeOriginal.entrySet().stream().filter(map -> map.getKey().equals(leftSideFileNameFinal)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesBefore);
            UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> map.getKey().equals(rightSideFileName)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesCurrent);
            return optimizeUMLModelPair(leftSideUMLModel, rightSideUMLModel);
        } else {
            UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeTrimmed, commitModel.repositoryDirectoriesBefore);
            UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> rightSideFileNamePredicate.test(map.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesCurrent);
            return optimizeUMLModelPair(leftSideUMLModel, rightSideUMLModel);
        }

    }

    private static Pair<UMLModel, UMLModel> optimizeUMLModelPair(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel) {
        for (UMLClass leftClass : leftSideUMLModel.getClassList()) {
            UMLClass rightClass = rightSideUMLModel.getClass(leftClass);
            if (rightClass != null) {
                List<UMLOperation> leftOperationsToBeRemoved = new ArrayList<>();
                List<UMLOperation> rightOperationsToBeRemoved = new ArrayList<>();
                for (UMLOperation leftOperation : leftClass.getOperations()) {
                    int index = rightClass.getOperations().indexOf(leftOperation);
                    if (index != -1) {
                        UMLOperation rightOperation = rightClass.getOperations().get(index);
                        if (leftOperation.getBodyHashCode() == rightOperation.getBodyHashCode()) {
                            leftOperationsToBeRemoved.add(leftOperation);
                            rightOperationsToBeRemoved.add(rightOperation);
                        }
                    }
                }
                leftClass.getOperations().removeAll(leftOperationsToBeRemoved);
                rightClass.getOperations().removeAll(rightOperationsToBeRemoved);
            }
        }
        leftSideUMLModel.setPartial(true);
        rightSideUMLModel.setPartial(true);
        return Pair.of(leftSideUMLModel, rightSideUMLModel);
    }

    protected static boolean isNewlyAddedFile(CommitModel commitModel, String currentMethodFilePath) {
        return commitModel.fileContentsCurrentTrimmed.containsKey(currentMethodFilePath) && !commitModel.fileContentsBeforeTrimmed.containsKey(currentMethodFilePath) && commitModel.renamedFilesHint.values().stream().noneMatch(s -> s.equals(currentMethodFilePath));
    }

    protected static Set<String> getRightSideFileNames(Method currentMethod, CommitModel commitModel, UMLModelDiff umlModelDiff) {
        String currentFilePath = currentMethod.getFilePath();
        String currentClassName = currentMethod.getUmlOperation().getClassName();
        Set<String> toBeAddedFileNamesIfTheyAreNewFiles = new HashSet<>();
        if (currentMethod.getUmlOperation() instanceof UMLOperation) {
            UMLOperation operation = (UMLOperation) currentMethod.getUmlOperation();
            UMLParameter returnParameter = operation.getReturnParameter();
            if (returnParameter != null) {
                String parameterType = returnParameter.getType().getClassType();
                if (!"void".equals(parameterType)) {
                    toBeAddedFileNamesIfTheyAreNewFiles.add(parameterType + ".java");
                }
            }
        }
        for (UMLType parameter : currentMethod.getUmlOperation().getParameterTypeList()) {
            String parameterType = parameter.getClassType();
            if ("void".equals(parameterType))
                continue;
            toBeAddedFileNamesIfTheyAreNewFiles.add(parameterType + ".java");
        }
        return getRightSideFileNames(currentFilePath, currentClassName, toBeAddedFileNamesIfTheyAreNewFiles, commitModel, umlModelDiff);
    }

    protected static Set<String> getRightSideFileNames(String currentFilePath, String currentClassName, Set<String> toBeAddedFileNamesIfTheyAreNewFiles, CommitModel commitModel, UMLModelDiff umlModelDiff) {
        Set<String> fileNames = new HashSet<>();
        fileNames.add(currentFilePath);
        UMLAbstractClass classInChildModel = umlModelDiff.findClassInChildModel(currentClassName);
        boolean newlyAddedFile = isNewlyAddedFile(commitModel, currentFilePath);
        if (classInChildModel instanceof UMLClass) {
            UMLClass umlClass = (UMLClass) classInChildModel;

            StringBuilder regxSb = new StringBuilder();

            String orChar = "";
            if (umlClass.getSuperclass() != null) {
                regxSb.append(orChar).append("\\s*extends\\s*").append(umlClass.getSuperclass().getClassType());
                orChar = "|";
                if (newlyAddedFile) {
                    regxSb.append(orChar).append("\\s*class\\s*").append(umlClass.getSuperclass().getClassType()).append("\\s\\s*");
                }
            }

            for (UMLType implementedInterface : umlClass.getImplementedInterfaces()) {
                regxSb.append(orChar).append("\\s*implements\\s*.*").append(implementedInterface).append("\\s*");
                orChar = "|";
                if (newlyAddedFile) {
                    regxSb.append(orChar).append("\\s*interface\\s*").append(implementedInterface.getClassType()).append("\\s*\\{");
                }
            }

            //newly added file
            if (newlyAddedFile) {
                regxSb.append(orChar).append("@link\\s*").append(umlClass.getNonQualifiedName());
                orChar = "|";
                regxSb.append(orChar).append("new\\s*").append(umlClass.getNonQualifiedName()).append("\\(");
                regxSb.append(orChar).append("@deprecated\\s*.*").append(umlClass.getNonQualifiedName()).append("\\s*.*\n");
                regxSb.append(orChar).append("\\s*extends\\s*").append(umlClass.getNonQualifiedName()).append("\\s*\\{");
            }

            String regx = regxSb.toString();
            if (!regx.isEmpty()) {
                Pattern pattern = Pattern.compile(regx);
                for (Map.Entry<String, String> entry : commitModel.fileContentsCurrentTrimmed.entrySet()) {
                    Matcher matcher = pattern.matcher(entry.getValue());
                    if (matcher.find()) {
                        String matcherGroup = matcher.group().trim();
                        String filePath = entry.getKey();
                        boolean isAnExistingFile = commitModel.fileContentsBeforeTrimmed.containsKey(filePath) || commitModel.renamedFilesHint.values().stream().anyMatch(s -> s.equals(filePath));
                        if (matcherGroup.startsWith("extends") && matcherGroup.contains(umlClass.getNonQualifiedName())) {
                            if (isAnExistingFile) {
                                fileNames.add(filePath);
                            }
                        } else if (matcherGroup.startsWith("implements") || matcherGroup.startsWith("extends")) {
                            if (isAnExistingFile) {
                                String[] split = matcherGroup.split("\\s");
                                String className = split[split.length - 1];
                                if (className.contains(".")) {
                                    className = className.substring(0, className.indexOf("."));
                                }
                                String[] tokens = CAMEL_CASE_SPLIT_PATTERN.split(className);
                                final String fileName = className + ".java";
                                if (commitModel.fileContentsCurrentTrimmed.keySet().stream().anyMatch(s -> s.endsWith(fileName) || s.endsWith(tokens[tokens.length - 1] + ".java"))) {
                                    fileNames.add(filePath);
                                }
                            }
                        } else if (matcherGroup.startsWith("new")) {
                            if (isAnExistingFile) {
                                fileNames.add(filePath);
                            }
                        } else if (matcherGroup.startsWith("@link")) {
                            fileNames.add(filePath); //TODO: add existing file condition and test
                        } else if (matcherGroup.startsWith("class")) {
                            if (isAnExistingFile) {
                                fileNames.add(filePath);
                            }
                        } else if (matcherGroup.startsWith("@deprecated")) {
                            if (isAnExistingFile) {
                                fileNames.add(filePath);
                            }
                        } else if (matcherGroup.startsWith("interface")) {
                            if (isAnExistingFile) {
                                fileNames.add(filePath);
                            }
                        }

                    }
                }
            }
            if (!umlClass.isTopLevel()) {
                fileNames.addAll(getRightSideFileNames(currentFilePath, umlClass.getPackageName(), toBeAddedFileNamesIfTheyAreNewFiles, commitModel, umlModelDiff));
            }
        }


        fileNames.addAll(
                commitModel.fileContentsCurrentTrimmed.keySet().stream()
                        .filter(filePath -> toBeAddedFileNamesIfTheyAreNewFiles.stream().anyMatch(filePath::endsWith))
                        .filter(filePath -> isNewlyAddedFile(commitModel, filePath))
                        .collect(Collectors.toSet())
        );

        if (newlyAddedFile) {
            final String currentMethodFileName = currentFilePath.substring(currentFilePath.lastIndexOf("/"));
            fileNames.addAll(commitModel.fileContentsCurrentTrimmed.keySet().stream().filter(filePath -> filePath.endsWith(currentMethodFileName)).collect(Collectors.toSet()));
        }

        return fileNames;
    }

    protected static boolean isMethodAdded(UMLModelDiff modelDiff, String className, Predicate<Method> equalOperator, Consumer<Method> addedMethodHandler, Version currentVersion) {
        List<UMLOperation> addedOperations = getAllClassesDiff(modelDiff)
                .stream()
                .map(UMLClassBaseDiff::getAddedOperations)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLOperation operation : addedOperations) {
            if (isMethodAdded(operation, equalOperator, addedMethodHandler, currentVersion))
                return true;
        }

        UMLClass addedClass = modelDiff.getAddedClass(className);
        if (addedClass != null) {
            for (UMLOperation operation : addedClass.getOperations()) {
                if (isMethodAdded(operation, equalOperator, addedMethodHandler, currentVersion))
                    return true;
            }
        }

        for (UMLClassRenameDiff classRenameDiffList : modelDiff.getClassRenameDiffList()) {
            for (UMLAnonymousClass addedAnonymousClasses : classRenameDiffList.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClasses.getOperations()) {
                    if (isMethodAdded(operation, equalOperator, addedMethodHandler, currentVersion))
                        return true;
                }
            }
        }
        return false;
    }

    protected static boolean isMethodAdded(UMLOperation addedOperation, Predicate<Method> equalOperator, Consumer<Method> addedMethodHandler, Version currentVersion) {
        Method rightMethod = Method.of(addedOperation, currentVersion);
        if (equalOperator.test(rightMethod)) {
            addedMethodHandler.accept(rightMethod);
            return true;
        }
        return false;
    }

    protected static Method getMethod(UMLModel umlModel, Version version, Predicate<Method> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Method method = getMethod(version, predicate, umlClass.getOperations());
                if (method != null) return method;
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    method = getMethod(version, predicate, anonymousClass.getOperations());
                    if (method != null) return method;
                }
            }
        return null;
    }

    private static Method getMethod(Version version, Predicate<Method> predicate, List<UMLOperation> operations) {
        for (UMLOperation umlOperation : operations) {
            Method method = Method.of(umlOperation, version);
            if (predicate.test(method))
                return method;
        }
        return null;
    }

    public static UMLModel getUMLModel(Repository repository, String commitId, Set<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty())
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            UMLModel umlModel = getUmlModel(repository, revCommit, fileNames);
            umlModel.setPartial(true);
            return umlModel;
        }
    }

    public static UMLModel getUmlModel(Repository repository, RevCommit commit, Set<String> filePaths) throws Exception {
        Set<String> repositoryDirectories = new LinkedHashSet<>();
        Map<String, String> fileContents = new LinkedHashMap<>();
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePaths, fileContents, repositoryDirectories);
        return GitHistoryRefactoringMinerImpl.createModel(fileContents, repositoryDirectories);
    }

    public static List<UMLClassBaseDiff> getAllClassesDiff(UMLModelDiff modelDiff) {
        List<UMLClassBaseDiff> allClassesDiff = new ArrayList<>();
        allClassesDiff.addAll(modelDiff.getCommonClassDiffList());
        allClassesDiff.addAll(modelDiff.getClassMoveDiffList());
        allClassesDiff.addAll(modelDiff.getInnerClassMoveDiffList());
        allClassesDiff.addAll(modelDiff.getClassRenameDiffList());
        return allClassesDiff;
    }

    public List<UMLClassMoveDiff> getClassMoveDiffList(UMLModelDiff umlModelDiff) {
        List<UMLClassMoveDiff> allMoveClassesDiff = new ArrayList<>();
        allMoveClassesDiff.addAll(umlModelDiff.getClassMoveDiffList());
        allMoveClassesDiff.addAll(umlModelDiff.getInnerClassMoveDiffList());
        return allMoveClassesDiff;
    }

    protected UMLModel getUMLModel(String commitId, Set<String> fileNames) throws Exception {
        return getUMLModel(repository, commitId, fileNames);
    }

    public CommitModel getCommitModel(String commitId) throws Exception {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
            RevCommit parentCommit1 = null;
            if (currentCommit.getParentCount() == 1 || currentCommit.getParentCount() == 2) {
                walk.parseCommit(currentCommit.getParent(0));
                parentCommit1 = currentCommit.getParent(0);
            }
            RevCommit parentCommit2 = null;
            if (currentCommit.getParentCount() == 2) {
//                walk.parseCommit(currentCommit.getParent(1));
//                parentCommit2 = currentCommit.getParent(1);

            }
            CommitModel commitModel = getCommitModel(parentCommit1, parentCommit2, currentCommit);
            return commitModel;
        }
    }

    private CommitModel getCommitModel(RevCommit parentCommit1, RevCommit parentCommit2, RevCommit currentCommit) throws Exception {
        Map<String, String> renamedFilesHint = new HashMap<>();
        Set<String> filePathsBefore1 = new HashSet<>();
        Set<String> filePathsCurrent1 = new HashSet<>();
        if (parentCommit1 != null) {
            gitService.fileTreeDiff(repository, currentCommit, filePathsBefore1, filePathsCurrent1, renamedFilesHint);
        }

        Set<String> filePathsBefore2 = new HashSet<>();
        Set<String> filePathsCurrent2 = new HashSet<>();
        if (parentCommit2 != null) {
            gitService.fileTreeDiff(repository, currentCommit, filePathsBefore2, filePathsCurrent2, renamedFilesHint);
        }

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();

        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();

        if (parentCommit1 != null) {
            GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit1, filePathsBefore1, fileContentsBefore, repositoryDirectoriesBefore);
        }
        if (parentCommit2 != null) {
            GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit2, filePathsBefore2, fileContentsBefore, repositoryDirectoriesBefore);
        }
        Set<String> filePathsCurrent = new HashSet<>();
        filePathsCurrent.addAll(filePathsCurrent1);
        filePathsCurrent.addAll(filePathsCurrent2);
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);

        Map<String, String> fileContentsBeforeTrimmed = new HashMap<>(fileContentsBefore);
        Map<String, String> fileContentsCurrentTrimmed = new HashMap<>(fileContentsCurrent);
        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBeforeTrimmed, fileContentsCurrentTrimmed, renamedFilesHint, false);

        return new CommitModel(repositoryDirectoriesBefore, fileContentsBefore, fileContentsBeforeTrimmed, repositoryDirectoriesCurrent, fileContentsCurrent, fileContentsCurrentTrimmed, renamedFilesHint, moveSourceFolderRefactorings);
    }

    public static class CommitModel {
        public final Set<String> repositoryDirectoriesBefore;
        public final Map<String, String> fileContentsBeforeOriginal;
        public final Map<String, String> fileContentsBeforeTrimmed;

        public final Set<String> repositoryDirectoriesCurrent;
        public final Map<String, String> fileContentsCurrentOriginal;
        public final Map<String, String> fileContentsCurrentTrimmed;

        public final Map<String, String> renamedFilesHint;
        public final List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings;

        public CommitModel(Set<String> repositoryDirectoriesBefore, Map<String, String> fileContentsBeforeOriginal, Map<String, String> fileContentsBeforeTrimmed, Set<String> repositoryDirectoriesCurrent, Map<String, String> fileContentsCurrentOriginal, Map<String, String> fileContentsCurrentTrimmed, Map<String, String> renamedFilesHint, List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings) {
            this.repositoryDirectoriesBefore = repositoryDirectoriesBefore;
            this.fileContentsBeforeOriginal = fileContentsBeforeOriginal;
            this.fileContentsBeforeTrimmed = fileContentsBeforeTrimmed;
            this.repositoryDirectoriesCurrent = repositoryDirectoriesCurrent;
            this.fileContentsCurrentOriginal = fileContentsCurrentOriginal;
            this.fileContentsCurrentTrimmed = fileContentsCurrentTrimmed;
            this.renamedFilesHint = renamedFilesHint;
            this.moveSourceFolderRefactorings = moveSourceFolderRefactorings;
        }
    }

    public static class ModelDiff {
        public final UMLModelDiff umlModelDiff;
        public final List<String> filePathsBefore;
        public final List<String> filePathsCurrent;
        public final Map<String, String> renamedFilesHint;
        public final List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings;

        public ModelDiff(UMLModelDiff umlModelDiff, List<String> filePathsBefore, List<String> filePathsCurrent, Map<String, String> renamedFilesHint, List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings) {
            this.umlModelDiff = umlModelDiff;
            this.filePathsBefore = filePathsBefore;
            this.filePathsCurrent = filePathsCurrent;
            this.renamedFilesHint = renamedFilesHint;
            this.moveSourceFolderRefactorings = moveSourceFolderRefactorings;
        }
    }
}
