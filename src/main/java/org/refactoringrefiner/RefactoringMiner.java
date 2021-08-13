package org.refactoringrefiner;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.api.*;
import org.refactoringrefiner.edge.ChangeFactory;
import org.refactoringrefiner.element.Class;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.element.Variable;
import org.refactoringrefiner.util.GitRepository;
import org.refactoringrefiner.util.IRepository;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RefactoringMiner implements ChangeDetector {
    private final GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner;
    private final GitServiceImpl gitService;
    private final RefactoringHandlerImpl refactoringHandler;
    private final Repository repository;
    private final HashSet<String> analysedCommits = new HashSet<>();
    private final String repositoryWebURL;

    public RefactoringMiner(Repository repository, String repositoryWebURL) {
        this.repository = repository;
        this.gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
        this.gitService = new GitServiceImpl();
        this.refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
        this.repositoryWebURL = repositoryWebURL;
    }

    public static Class getClass(UMLModel umlModel, String key) {
        return getClass(umlModel, key, null);
    }

    public static Class getClass(UMLModel umlModel, String key, Version version) {
        for (UMLClass umlClass : umlModel.getClassList()) {
            Class clazz = Class.of(umlClass, version);
            if (key.equals(clazz.getName())) {
                return clazz;
            }
        }
        return null;
    }

//    public static Attribute getAttribute(UMLModel umlModel, String key) {
//        return getAttribute(umlModel, key, null);
//
//    }

//    public static Attribute getAttribute(UMLModel umlModel, String key, Version version) {
//        for (UMLClass umlClass : umlModel.getClassList()) {
//            for (UMLAttribute umlAttribute : umlClass.getAttributes()) {
//                Attribute attribute = Attribute.of(umlAttribute, version);
//                if (key.equals(attribute.getName())) {
//                    return attribute;
//                }
//            }
//        }
//        return null;
//    }

    public static Method getMethod(UMLModel umlModel, Version version, Predicate<Method> predicate) {
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

    public static Method getMethodByName(UMLModel umlModel, Version version, String key) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Method method = getMethod(version, key, umlClass.getOperations());
                if (method != null) return method;
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    method = getMethod(version, key, anonymousClass.getOperations());
                    if (method != null) return method;
                }
            }
        return null;
    }

    private static Method getMethod(Version version, String key, List<UMLOperation> operations) {
        for (UMLOperation umlOperation : operations) {
            Method method = Method.of(umlOperation, version);
            if (key.equals(method.getName()))
                return method;
        }
        return null;
    }

    @Override
    public void detectAtCommit(String commitId) {
        if (analysedCommits.contains(commitId))
            return;
        gitHistoryRefactoringMiner.detectAtCommit(repository, commitId, refactoringHandler, 36000);
        analysedCommits.add(commitId);
    }

//    public boolean isChanged(Pair<UMLModel, UMLModel> umlModel, String elementKey, RefactoringRefiner.CodeElementType codeElementType) {
//        if (umlModel == null)
//            return false;
//
//        UMLModel leftSideUMLModel = umlModel.getLeft();
//        UMLModel rightSideUMLModel = umlModel.getRight();
//        if (leftSideUMLModel == null || rightSideUMLModel == null)
//            return true;
//
//        //UMLModelDiff diff = leftSideUMLModel.diff(rightSideUMLModel);
//        //TODO: check local refactorings for more improvement
//        switch (codeElementType) {
//            case METHOD:
//                return isMethodChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//            case CLASS:
//                return isClassChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//            case ATTRIBUTE:
//                return isAttributeChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//            case VARIABLE:
//                return isVariableChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//        }
//
//        return true;
//    }

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
                walk.parseCommit(currentCommit.getParent(1));
                parentCommit2 = currentCommit.getParent(1);

            }
            return getCommitModel(parentCommit1, parentCommit2, currentCommit);
        }
    }

    public List<ModelDiff> getUMLModelDiff(String commitId, List<String> rightSideFileNames) throws Exception {
        List<ModelDiff> result = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
            if (currentCommit.getParentCount() == 1 || currentCommit.getParentCount() == 2) {
                walk.parseCommit(currentCommit.getParent(0));
                RevCommit parentCommit = currentCommit.getParent(0);
                result.add(getUMLModelDiff(rightSideFileNames, currentCommit, parentCommit));
            }

            if (currentCommit.getParentCount() == 2) {
                walk.parseCommit(currentCommit.getParent(1));
                RevCommit parentCommit = currentCommit.getParent(1);
                result.add(getUMLModelDiff(rightSideFileNames, currentCommit, parentCommit));
            }
            return result;
        }
    }

    private CommitModel getCommitModel(RevCommit parentCommit1, RevCommit parentCommit2, RevCommit currentCommit) throws Exception {
        Map<String, String> renamedFilesHint = new HashMap<>();
        List<String> filePathsBefore1 = new ArrayList<>();
        List<String> filePathsCurrent1 = new ArrayList<>();
        if (parentCommit1 != null) {
            gitService.fileTreeDiff(repository, parentCommit1, currentCommit, filePathsBefore1, filePathsCurrent1, renamedFilesHint);
        }

        List<String> filePathsBefore2 = new ArrayList<>();
        List<String> filePathsCurrent2 = new ArrayList<>();
        if (parentCommit2 != null) {
            gitService.fileTreeDiff(repository, parentCommit2, currentCommit, filePathsBefore2, filePathsCurrent2, renamedFilesHint);
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
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, currentCommit, new ArrayList<>(filePathsCurrent), fileContentsCurrent, repositoryDirectoriesCurrent);

        Map<String, String> fileContentsBeforeTrimmed = new HashMap<>(fileContentsBefore);
        Map<String, String> fileContentsCurrentTrimmed = new HashMap<>(fileContentsCurrent);
        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBeforeTrimmed, fileContentsCurrentTrimmed, renamedFilesHint);

        return new CommitModel(repositoryDirectoriesBefore, fileContentsBefore, fileContentsBeforeTrimmed, repositoryDirectoriesCurrent, fileContentsCurrent, fileContentsCurrentTrimmed, renamedFilesHint, moveSourceFolderRefactorings);
    }

    private ModelDiff getUMLModelDiff(List<String> rightSideFileNames, RevCommit currentCommit, RevCommit parentCommit) throws Exception {
        List<String> filePathsBefore = new ArrayList<>();
        List<String> filePathsCurrent = new ArrayList<>();
        Map<String, String> renamedFilesHint = new HashMap<>();
        gitService.fileTreeDiff(repository, parentCommit, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();

        GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit, filePathsBefore, fileContentsBefore, repositoryDirectoriesBefore);
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);

        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint);

        UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
        //UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
        UMLModel rightSideUMLModel;
        if (rightSideFileNames != null)
            rightSideUMLModel = GitHistoryRefactoringMinerImpl.getUmlModel(repository, currentCommit, rightSideFileNames);
        else
            rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);

        ModelDiff modelDiff = new ModelDiff(leftSideUMLModel.diff(rightSideUMLModel, renamedFilesHint), filePathsBefore, filePathsCurrent, renamedFilesHint, moveSourceFolderRefactorings);
        return modelDiff;
    }

    public Pair<UMLModel, UMLModel> getUMLModelPair(final RefactoringMiner.CommitModel commitModel, final String rightSideFileName, final Set<String> rightSideFileNames, final boolean filterLeftSide) throws Exception {
        if (rightSideFileName == null)
            throw new IllegalArgumentException("File name could not be null.");

        if(filterLeftSide) {
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
            UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeOriginal.entrySet().stream().filter(map -> map.getKey().equals(leftSideFileNameFinal)).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue())), commitModel.repositoryDirectoriesBefore);
            UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> map.getKey().equals(rightSideFileName)).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue())), commitModel.repositoryDirectoriesCurrent);
            return Pair.of(leftSideUMLModel, rightSideUMLModel);
        }else {
            UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeTrimmed, commitModel.repositoryDirectoriesBefore);
            UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> rightSideFileNames.contains(map.getKey())).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue())), commitModel.repositoryDirectoriesCurrent);
            return Pair.of(leftSideUMLModel, rightSideUMLModel);
        }

    }

    public UMLModel getUMLModel(String commitId, List<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty())
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            return GitHistoryRefactoringMinerImpl.getUmlModel(repository, revCommit, fileNames);
        }
    }

    public List<CodeElement> findMostLeftElement(RefactoringRefiner.CodeElementType codeElementType, String codeElementKey) {
        switch (codeElementType) {
            case CLASS:
                return refactoringHandler.getClassChangeHistoryGraph().findMostLeftSide(codeElementKey);
            case ATTRIBUTE:
                return refactoringHandler.getAttributeChangeHistory().findMostLeftSide(codeElementKey);
            case METHOD:
                return refactoringHandler.getMethodChangeHistoryGraph().findMostLeftSide(codeElementKey);
            case VARIABLE:
                return refactoringHandler.getVariableChangeHistoryGraph().findMostLeftSide(codeElementKey);
        }
        return Collections.emptyList();
    }

    public Graph<CodeElement, Edge> findSubGraph(RefactoringRefiner.CodeElementType codeElementType, CodeElement start) {
        switch (codeElementType) {
            case CLASS:
                return refactoringHandler.getClassChangeHistoryGraph().findSubGraph(start);
            case ATTRIBUTE:
                return refactoringHandler.getAttributeChangeHistory().findSubGraph(start);
            case METHOD:
                return refactoringHandler.getMethodChangeHistoryGraph().findSubGraph(start);
            case VARIABLE:
                return refactoringHandler.getVariableChangeHistoryGraph().findSubGraph(start);
        }
        return null;
    }

//    private <U, E extends CodeElement> boolean isChanged(UMLModel leftSide, UMLModel rightSide, String key, CodeElementFinder<U> codeElementFinder) {
//        CodeElement leftSideCodeElement = codeElementFinder.getCodeElement(leftSide, key);
//        CodeElement rightSideCodeElement = codeElementFinder.getCodeElement(rightSide, key);
//        if (leftSideCodeElement == null || rightSideCodeElement == null)
//            return true;
//
//        return !leftSideCodeElement.getIdentifierIgnoringVersion().equals(rightSideCodeElement.getIdentifierIgnoringVersion());
//    }
//
//    private boolean isClassChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        return isChanged(leftSideUMLModel, rightSideUMLModel, key, RefactoringMiner::getClass);
//    }
//
//    private boolean isMethodChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        return false;
//        //        return isChanged(leftSideUMLModel, rightSideUMLModel, key, RefactoringMiner::getMethod);
//    }
//
//    private boolean isVariableChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        throw new UnsupportedOperationException();
//    }
//
//    private boolean isAttributeChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        return isChanged(leftSideUMLModel, rightSideUMLModel, key, RefactoringMiner::getAttribute);
//    }

    public IRepository getRepository() {
        return refactoringHandler.getRepository();
    }

    @Override
    public void addNode(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement) {
        switch (codeElementType) {
            case CLASS: {
                refactoringHandler.getClassChangeHistoryGraph().addNode(codeElement);
                break;
            }
            case ATTRIBUTE: {
                refactoringHandler.getAttributeChangeHistory().addNode(codeElement);
                break;
            }
            case METHOD: {
                refactoringHandler.getMethodChangeHistoryGraph().addNode(codeElement);
                break;
            }
            case VARIABLE: {
                refactoringHandler.getVariableChangeHistoryGraph().addNode(codeElement);
                break;
            }
        }
    }

    public void addEdge(RefactoringRefiner.CodeElementType codeElementType, CodeElement leftSide, CodeElement rightSide, ChangeFactory changeFactory) {
        switch (codeElementType) {
            case CLASS: {
                refactoringHandler.getClassChangeHistoryGraph().addChange(leftSide, rightSide, changeFactory);
                break;
            }
            case ATTRIBUTE: {
                refactoringHandler.getAttributeChangeHistory().addChange(leftSide, rightSide, changeFactory);
                break;
            }
            case METHOD: {
                refactoringHandler.getMethodChangeHistoryGraph().addChange(leftSide, rightSide, changeFactory);
                break;
            }
            case VARIABLE: {
                refactoringHandler.getVariableChangeHistoryGraph().addChange(leftSide, rightSide, changeFactory);
                break;
            }
        }
    }

    public Set<CodeElement> predecessors(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement) {
        switch (codeElementType) {
            case CLASS:
                return refactoringHandler.getClassChangeHistoryGraph().predecessors(codeElement);
            case ATTRIBUTE:
                return refactoringHandler.getAttributeChangeHistory().predecessors(codeElement);
            case METHOD:
                return refactoringHandler.getMethodChangeHistoryGraph().predecessors(codeElement);
            case VARIABLE:
                return refactoringHandler.getVariableChangeHistoryGraph().predecessors(codeElement);
        }
        return Collections.emptySet();
    }

    public Version getVersion(String commitId) {
        return refactoringHandler.getRepository().getVersion(commitId);
    }

    public RefactoringHandlerImpl getRefactoringHandler() {
        return refactoringHandler;
    }

    public void connectRelatedNodes(RefactoringRefiner.CodeElementType codeElementType) {
        switch (codeElementType) {
            case CLASS: {
                refactoringHandler.getClassChangeHistoryGraph().connectRelatedNodes();
                break;
            }
            case ATTRIBUTE: {
                refactoringHandler.getAttributeChangeHistory().connectRelatedNodes();
                break;
            }
            case METHOD: {
                refactoringHandler.getMethodChangeHistoryGraph().connectRelatedNodes();
                break;
            }
            case VARIABLE: {
                refactoringHandler.getVariableChangeHistoryGraph().connectRelatedNodes();
                break;
            }
        }
    }

    public Set<Variable> analyseVariableRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        Set<Variable> leftVariableSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            List<Variable> variableBeforeList = new ArrayList<>();
            List<Variable> variableAfterList = new ArrayList<>();

            switch (refactoring.getRefactoringType()) {
                case RENAME_VARIABLE:
                case RENAME_PARAMETER:
                case PARAMETERIZE_VARIABLE: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    variableAfterList.add(Variable.of(renameVariableRefactoring.getRenamedVariable(), renameVariableRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(renameVariableRefactoring.getOriginalVariable(), renameVariableRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case CHANGE_VARIABLE_TYPE:
                case CHANGE_PARAMETER_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    variableAfterList.add(Variable.of(changeVariableTypeRefactoring.getChangedTypeVariable(), changeVariableTypeRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(changeVariableTypeRefactoring.getOriginalVariable(), changeVariableTypeRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case ADD_VARIABLE_MODIFIER:
                case ADD_PARAMETER_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    variableAfterList.add(Variable.of(addVariableModifierRefactoring.getVariableAfter(), addVariableModifierRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(addVariableModifierRefactoring.getVariableBefore(), addVariableModifierRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case REMOVE_VARIABLE_MODIFIER:
                case REMOVE_PARAMETER_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    variableAfterList.add(Variable.of(removeVariableModifierRefactoring.getVariableAfter(), removeVariableModifierRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(removeVariableModifierRefactoring.getVariableBefore(), removeVariableModifierRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case ADD_VARIABLE_ANNOTATION:
                case ADD_PARAMETER_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    variableAfterList.add(Variable.of(addVariableAnnotationRefactoring.getVariableAfter(), addVariableAnnotationRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(addVariableAnnotationRefactoring.getVariableBefore(), addVariableAnnotationRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case MODIFY_VARIABLE_ANNOTATION:
                case MODIFY_PARAMETER_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    variableAfterList.add(Variable.of(modifyVariableAnnotationRefactoring.getVariableAfter(), modifyVariableAnnotationRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(modifyVariableAnnotationRefactoring.getVariableBefore(), modifyVariableAnnotationRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case REMOVE_VARIABLE_ANNOTATION:
                case REMOVE_PARAMETER_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    variableAfterList.add(Variable.of(removeVariableAnnotationRefactoring.getVariableAfter(), removeVariableAnnotationRefactoring.getOperationAfter(), currentVersion));
                    variableBeforeList.add(Variable.of(removeVariableAnnotationRefactoring.getVariableBefore(), removeVariableAnnotationRefactoring.getOperationBefore(), parentVersion));
                    break;
                }

                case SPLIT_PARAMETER:
                case SPLIT_VARIABLE: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) refactoring;
                    for (VariableDeclaration splitVariable : splitVariableRefactoring.getSplitVariables()) {
                        variableAfterList.add(Variable.of(splitVariable, splitVariableRefactoring.getOperationAfter(), currentVersion));
                    }
                    variableBeforeList.add(Variable.of(splitVariableRefactoring.getOldVariable(), splitVariableRefactoring.getOperationBefore(), parentVersion));
                    break;
                }
                case MERGE_PARAMETER:
                case MERGE_VARIABLE: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                    for (VariableDeclaration mergeVariable : mergeVariableRefactoring.getMergedVariables()) {
                        variableBeforeList.add(Variable.of(mergeVariable, mergeVariableRefactoring.getOperationBefore(), currentVersion));
                    }
                    variableAfterList.add(Variable.of(mergeVariableRefactoring.getNewVariable(), mergeVariableRefactoring.getOperationAfter(), parentVersion));
                    break;
                }
            }
            for (Variable variableAfter : variableAfterList) {
                if (equalOperator.test(variableAfter)) {
                    for (Variable variableBefore : variableBeforeList) {
                        //TODO: add change type for every case
                        //refactoringHandler.getVariableChangeHistoryGraph().addRefactored(variableBefore, variableAfter, refactoring);
                        leftVariableSet.add(variableBefore);
                    }
                }
            }
        }
        return leftVariableSet;
    }

    public Set<Method> analyseMethodRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {
        Set<Method> leftMethodSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            UMLOperation operationBefore = null;
            UMLOperation operationAfter = null;
            Change.Type changeType = null;

            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.METHOD_MOVE;
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.METHOD_MOVE;
                    break;
                }
                case MOVE_AND_RENAME_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.METHOD_MOVE;
                    addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, operationBefore, operationAfter, Change.Type.RENAME);
                    break;
                }
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.METHOD_MOVE;
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case ADD_METHOD_ANNOTATION: {
                    AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) refactoring;
                    operationBefore = addMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = addMethodAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_METHOD_ANNOTATION: {
                    ModifyMethodAnnotationRefactoring modifyMethodAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
                    operationBefore = modifyMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = modifyMethodAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_METHOD_ANNOTATION: {
                    RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) refactoring;
                    operationBefore = removeMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = removeMethodAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case CHANGE_RETURN_TYPE: {
                    ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) refactoring;
                    operationBefore = changeReturnTypeRefactoring.getOperationBefore();
                    operationAfter = changeReturnTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.RETURN_TYPE_CHANGE;
                    break;
                }
                case SPLIT_PARAMETER: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) refactoring;
                    operationBefore = splitVariableRefactoring.getOperationBefore();
                    operationAfter = splitVariableRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case MERGE_PARAMETER: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                    operationBefore = mergeVariableRefactoring.getOperationBefore();
                    operationAfter = mergeVariableRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case RENAME_PARAMETER:
                case PARAMETERIZE_VARIABLE: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    if (!renameVariableRefactoring.isExtraction()) {
                        operationBefore = renameVariableRefactoring.getOperationBefore();
                        operationAfter = renameVariableRefactoring.getOperationAfter();
                        changeType = Change.Type.PARAMETER_CHANGE;
                    }
                    break;
                }
                case CHANGE_PARAMETER_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    operationBefore = changeVariableTypeRefactoring.getOperationBefore();
                    operationAfter = changeVariableTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_PARAMETER: {
                    AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) refactoring;
                    operationBefore = addParameterRefactoring.getOperationBefore();
                    operationAfter = addParameterRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REMOVE_PARAMETER: {
                    RemoveParameterRefactoring removeParameterRefactoring = (RemoveParameterRefactoring) refactoring;
                    operationBefore = removeParameterRefactoring.getOperationBefore();
                    operationAfter = removeParameterRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REORDER_PARAMETER: {
                    ReorderParameterRefactoring reorderParameterRefactoring = (ReorderParameterRefactoring) refactoring;
                    operationBefore = reorderParameterRefactoring.getOperationBefore();
                    operationAfter = reorderParameterRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_PARAMETER_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    operationBefore = addVariableModifierRefactoring.getOperationBefore();
                    operationAfter = addVariableModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REMOVE_PARAMETER_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    operationBefore = removeVariableModifierRefactoring.getOperationBefore();
                    operationAfter = removeVariableModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_PARAMETER_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    operationBefore = addVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = addVariableAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REMOVE_PARAMETER_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    operationBefore = removeVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = removeVariableAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case MODIFY_PARAMETER_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    operationBefore = modifyVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = modifyVariableAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_THROWN_EXCEPTION_TYPE: {
                    AddThrownExceptionTypeRefactoring addThrownExceptionTypeRefactoring = (AddThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = addThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = addThrownExceptionTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.EXCEPTION_CHANGE;
                    break;
                }
                case CHANGE_THROWN_EXCEPTION_TYPE: {
                    ChangeThrownExceptionTypeRefactoring changeThrownExceptionTypeRefactoring = (ChangeThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = changeThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = changeThrownExceptionTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.EXCEPTION_CHANGE;
                    break;
                }
                case REMOVE_THROWN_EXCEPTION_TYPE: {
                    RemoveThrownExceptionTypeRefactoring removeThrownExceptionTypeRefactoring = (RemoveThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = removeThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = removeThrownExceptionTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.EXCEPTION_CHANGE;
                    break;
                }
                case CHANGE_OPERATION_ACCESS_MODIFIER: {
                    ChangeOperationAccessModifierRefactoring changeOperationAccessModifierRefactoring = (ChangeOperationAccessModifierRefactoring) refactoring;
                    operationBefore = changeOperationAccessModifierRefactoring.getOperationBefore();
                    operationAfter = changeOperationAccessModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case ADD_METHOD_MODIFIER: {
                    AddMethodModifierRefactoring addMethodModifierRefactoring = (AddMethodModifierRefactoring) refactoring;
                    operationBefore = addMethodModifierRefactoring.getOperationBefore();
                    operationAfter = addMethodModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_METHOD_MODIFIER: {
                    RemoveMethodModifierRefactoring removeMethodModifierRefactoring = (RemoveMethodModifierRefactoring) refactoring;
                    operationBefore = removeMethodModifierRefactoring.getOperationBefore();
                    operationAfter = removeMethodModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case MOVE_AND_INLINE_OPERATION:
                case INLINE_OPERATION: {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    UMLOperation inlinedOperation = inlineOperationRefactoring.getInlinedOperation();
                    operationBefore = inlineOperationRefactoring.getTargetOperationBeforeInline();
                    operationAfter = inlineOperationRefactoring.getTargetOperationAfterInline();
                    changeType = Change.Type.BODY_CHANGE;
                    break;
                }
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    operationBefore = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                    operationAfter = extractOperationRefactoring.getSourceOperationAfterExtraction();
                    changeType = Change.Type.BODY_CHANGE;

                    UMLOperation extractedOperation = extractOperationRefactoring.getExtractedOperation();
                    Method extractedOperationAfter = Method.of(extractedOperation, currentVersion);
                    if (equalOperator.test(extractedOperationAfter)) {
                        Method extractedOperationBefore = Method.of(extractedOperation, parentVersion);
                        extractedOperationBefore.setAdded(true);
                        refactoringHandler.getMethodChangeHistoryGraph().addChange(extractedOperationBefore, extractedOperationAfter, ChangeFactory.forMethod(Change.Type.INTRODUCED).refactoring(extractOperationRefactoring).codeElement(extractedOperationAfter));
                        refactoringHandler.getMethodChangeHistoryGraph().connectRelatedNodes();
                        leftMethodSet.add(extractedOperationBefore);
                        return leftMethodSet;
                    }
                    break;
                }
            }

            addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, operationBefore, operationAfter, changeType);
        }
        refactoringHandler.getMethodChangeHistoryGraph().connectRelatedNodes();
        return leftMethodSet;
    }

    public boolean addMethodChange(Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, Set<Method> leftMethodSet, Refactoring refactoring, UMLOperation operationBefore, UMLOperation operationAfter, Change.Type changeType) {
        if (operationAfter != null) {
            Method methodAfter = Method.of(operationAfter, currentVersion);
            if (equalOperator.test(methodAfter)) {
                Method methodBefore = Method.of(operationBefore, parentVersion);
                refactoringHandler.getMethodChangeHistoryGraph().addChange(methodBefore, methodAfter, ChangeFactory.forMethod(changeType).refactoring(refactoring));
                if (RefactoringHandlerImpl.checkOperationBodyChanged(methodBefore.getUmlOperation().getBody(), methodAfter.getUmlOperation().getBody())) {
                    refactoringHandler.getMethodChangeHistoryGraph().addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                }
                if (RefactoringHandlerImpl.checkOperationDocumentationChanged(methodBefore.getUmlOperation(), methodAfter.getUmlOperation())) {
                    refactoringHandler.getMethodChangeHistoryGraph().addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                }
//                if(!Change.Type.METHOD_MOVE.equals(changeType) && !operationBefore.getLocationInfo().getFilePath().equals(operationAfter.getLocationInfo().getFilePath()) || !operationBefore.getClassName().equals(operationAfter.getClassName())){
//                    refactoringHandler.getMethodChangeHistoryGraph().addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.CONTAINER_CHANGE));
//                }
                leftMethodSet.add(methodBefore);
                return true;
            }
        }
        return false;
    }

    //    public Pair<UMLModel, UMLModel> getUMLModel(String commitId) {
//        return getUMLModel(commitId, (s -> true));
//    }
//
//    public Pair<UMLModel, UMLModel> getUMLModel(String commitId, Predicate<String> filterFile) {
//        try (RevWalk walk = new RevWalk(repository)) {
//            RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
//            if (currentCommit.getParentCount() != 1)
//                return null;
//            walk.parseCommit(currentCommit.getParent(0));
//            RevCommit parentCommit = currentCommit.getParent(0);
//            List<String> filePathsBefore = new ArrayList<>();
//            List<String> filePathsCurrent = new ArrayList<>();
//            Map<String, String> renamedFilesHint = new HashMap<>();
//            gitService.fileTreeDiff(repository, parentCommit, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
//
//            List<String> filePathBeforeFilter = filePathsBefore.stream().filter(filterFile).collect(Collectors.toList());
//            List<String> filePathCurrentFilter = filePathsCurrent.stream().filter(filterFile).collect(Collectors.toList());
//            UMLModel leftSideUMLModel = null;
//            UMLModel rightSideUMLModel = null;
//            if (!filePathBeforeFilter.isEmpty())
//                leftSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, parentCommit, filePathBeforeFilter);
//            if (!filePathCurrentFilter.isEmpty())
//                rightSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, currentCommit, filePathCurrentFilter);
//            if (leftSideUMLModel == null && rightSideUMLModel == null)
//                return null;
//
//            return Pair.of(leftSideUMLModel, rightSideUMLModel);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
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

//    private interface CodeElementFinder<U> {
//        CodeElement getCodeElement(UMLModel umlModel, String key);
//    }
}
