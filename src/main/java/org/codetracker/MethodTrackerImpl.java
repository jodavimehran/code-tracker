package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.api.Version;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.MethodSignatureChange;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.*;
import java.util.stream.Collectors;

public class MethodTrackerImpl extends BaseTracker implements MethodTracker {
	private final MethodTrackerChangeHistory changeHistory;

    public MethodTrackerImpl(Repository repository, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.changeHistory = new MethodTrackerChangeHistory(methodName, methodDeclarationLineNumber);
    }

    @Override
    public History<Method> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method start = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
            if (start == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
            }
            changeHistory.get().addNode(start);

            ArrayDeque<Method> methods = new ArrayDeque<>();
            methods.addFirst(start);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!methods.isEmpty()) {
                Method currentMethod = methods.poll();
                if (currentMethod.isAdded() || currentMethod.getVersion().getId().equals("0")) {
                    commits = null;
                    continue;
                }
                final String currentMethodFilePath = currentMethod.getFilePath();
                if (commits == null || !currentMethodFilePath.equals(lastFileName)) {
                    lastFileName = currentMethodFilePath;
                    commits = getCommits(repository, currentMethod.getVersion().getId(), lastFileName, git);
                    historyReport.gitLogCommandCallsPlusPlus();
                    analysedCommits.clear();
                }
                if (analysedCommits.containsAll(commits))
                    break;
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    //System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);


                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethodFilePath));
                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                    if (rightMethod == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                        changeHistory.get().handleAdd(leftMethod, rightMethod, "Initial commit!");
                        changeHistory.get().connectRelatedNodes();
                        methods.add(leftMethod);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethodFilePath));

                    //NO CHANGE
                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                    if (leftMethod != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

                    //CHANGE BODY OR DOCUMENT
                    leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);

                    if (leftMethod != null) {
                        if (!leftMethod.equalBody(rightMethod))
                        	changeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                        if (!leftMethod.equalDocuments(rightMethod))
                        	changeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                        changeHistory.get().connectRelatedNodes();
                        currentMethod = leftMethod;
                        historyReport.step3PlusPlus();
                        continue;
                    }

                    //Local Refactoring
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        Set<Method> leftSideMethods = changeHistory.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                        boolean refactored = !leftSideMethods.isEmpty();
                        if (refactored) {
                            leftSideMethods.forEach(methods::addFirst);
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }
                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Set<Method> methodContainerChanged = null;
                            boolean containerChanged = false;
                            boolean found = false;
                            for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : commitModel.moveSourceFolderRefactorings) {
                                if (found)
                                    break;
                                for (Map.Entry<String, String> identicalPath : moveSourceFolderRefactoring.getIdenticalFilePaths().entrySet()) {
                                    if (identicalPath.getValue().equals(currentMethodFilePath)) {
                                        String leftSideFileName = identicalPath.getKey();

                                        UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeOriginal.entrySet().stream().filter(map -> map.getKey().equals(leftSideFileName)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesBefore);
                                        UMLClass originalClass = null;
                                        for(UMLClass leftSideClass : leftSideUMLModel.getClassList()){
                                            if(leftSideClass.getName().equals(currentMethod.getUmlOperation().getClassName())){
                                                originalClass = leftSideClass;
                                                break;
                                            }
                                        }
                                        UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> map.getKey().equals(currentMethodFilePath)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesCurrent);
                                        UMLClass movedClass = null;
                                        for(UMLClass rightSideClass : rightSideUMLModel.getClassList()){
                                            if(rightSideClass.getName().equals(currentMethod.getUmlOperation().getClassName())){
                                                movedClass = rightSideClass;
                                                break;
                                            }
                                        }
                                        moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder().add(new MovedClassToAnotherSourceFolder(originalClass, movedClass, identicalPath.getKey(), identicalPath.getValue()));
                                        methodContainerChanged = changeHistory.isMethodContainerChanged(null, Collections.singletonList(moveSourceFolderRefactoring), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, Collections.emptyList());
                                        containerChanged = !methodContainerChanged.isEmpty();
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (containerChanged) {
                                methodContainerChanged.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
                            Set<String> fileNames = getRightSideFileNames(currentMethod, commitModel, umlModelDiffLocal);
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentMethodFilePath, fileNames::contains, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
                            Set<Method> methodContainerChanged = changeHistory.isMethodContainerChanged(umlModelDiffAll, moveRenameClassRefactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffAll));
                            if (!methodContainerChanged.isEmpty()) {
                                UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightMethod.getUmlOperation().getClassName());
                                if (classDiff != null) {
                                    List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
                                    changeHistory.analyseMethodRefactorings(classLevelRefactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                                }
                                Set<Method> leftMethods = new HashSet<>();
                                leftMethods.addAll(methodContainerChanged);
                                leftMethods.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();
                            boolean flag = false;
                            for (Refactoring refactoring : refactorings) {
                                if (RefactoringType.MOVE_AND_RENAME_OPERATION.equals(refactoring.getRefactoringType()) || RefactoringType.MOVE_OPERATION.equals(refactoring.getRefactoringType())) {
                                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                                    Method movedOperation = Method.of(moveOperationRefactoring.getMovedOperation(), currentVersion);
                                    if (rightMethod.equalIdentifierIgnoringVersion(movedOperation)) {
                                        fileNames.add(moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath());
                                        flag = true;
                                    }
                                }
                            }
                            if (flag) {
                                umlModelPairAll = getUMLModelPair(commitModel, currentMethodFilePath, fileNames::contains, false);
                                umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                                refactorings = umlModelDiffAll.getRefactorings();
                            }

                            methodContainerChanged = changeHistory.isMethodContainerChanged(umlModelDiffAll, refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffAll));
                            boolean containerChanged = !methodContainerChanged.isEmpty();

                            Set<Method> methodRefactored = changeHistory.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                            boolean refactored = !methodRefactored.isEmpty();

                            if (containerChanged || refactored) {
                                Set<Method> leftMethods = new HashSet<>();
                                leftMethods.addAll(methodContainerChanged);
                                leftMethods.addAll(methodRefactored);
                                leftMethods.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (changeHistory.isMethodAdded(umlModelDiffAll, methods, rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
            return new HistoryImpl<>(changeHistory.get().getCompleteGraph(), historyReport);
        }
    }

    public History.HistoryInfo<Method> blame() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method start = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
            if (start == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
            }
            start.checkClosingBracket(changeHistory.getMethodDeclarationLineNumber());
            changeHistory.get().addNode(start);

            ArrayDeque<Method> methods = new ArrayDeque<>();
            methods.addFirst(start);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!methods.isEmpty()) {
            	History.HistoryInfo<Method> blame = blameReturn(start);
            	if (blame != null) return blame;
                Method currentMethod = methods.poll();
                if (currentMethod.isAdded() || currentMethod.getVersion().getId().equals("0")) {
                    commits = null;
                    continue;
                }
                final String currentMethodFilePath = currentMethod.getFilePath();
                if (commits == null || !currentMethodFilePath.equals(lastFileName)) {
                    lastFileName = currentMethodFilePath;
                    commits = getCommits(repository, currentMethod.getVersion().getId(), lastFileName, git);
                    historyReport.gitLogCommandCallsPlusPlus();
                    analysedCommits.clear();
                }
                if (analysedCommits.containsAll(commits))
                    break;
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    //System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);


                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethodFilePath));
                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                    if (rightMethod == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                        changeHistory.get().handleAdd(leftMethod, rightMethod, "Initial commit!");
                        changeHistory.get().connectRelatedNodes();
                        methods.add(leftMethod);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethodFilePath));

                    //NO CHANGE
                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                    if (leftMethod != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

                    //CHANGE BODY OR DOCUMENT
                    leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);

                    if (leftMethod != null) {
                        if (!leftMethod.equalBody(rightMethod))
                        	changeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                        if (!leftMethod.equalDocuments(rightMethod))
                        	changeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                        changeHistory.get().connectRelatedNodes();
                        currentMethod = leftMethod;
                        historyReport.step3PlusPlus();
                        continue;
                    }

                    //Local Refactoring
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        Set<Method> leftSideMethods = changeHistory.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                        boolean refactored = !leftSideMethods.isEmpty();
                        if (refactored) {
                            leftSideMethods.forEach(methods::addFirst);
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }
                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Set<Method> methodContainerChanged = null;
                            boolean containerChanged = false;
                            boolean found = false;
                            for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : commitModel.moveSourceFolderRefactorings) {
                                if (found)
                                    break;
                                for (Map.Entry<String, String> identicalPath : moveSourceFolderRefactoring.getIdenticalFilePaths().entrySet()) {
                                    if (identicalPath.getValue().equals(currentMethodFilePath)) {
                                        String leftSideFileName = identicalPath.getKey();

                                        UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeOriginal.entrySet().stream().filter(map -> map.getKey().equals(leftSideFileName)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesBefore);
                                        UMLClass originalClass = null;
                                        for(UMLClass leftSideClass : leftSideUMLModel.getClassList()){
                                            if(leftSideClass.getName().equals(currentMethod.getUmlOperation().getClassName())){
                                                originalClass = leftSideClass;
                                                break;
                                            }
                                        }
                                        UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> map.getKey().equals(currentMethodFilePath)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesCurrent);
                                        UMLClass movedClass = null;
                                        for(UMLClass rightSideClass : rightSideUMLModel.getClassList()){
                                            if(rightSideClass.getName().equals(currentMethod.getUmlOperation().getClassName())){
                                                movedClass = rightSideClass;
                                                break;
                                            }
                                        }
                                        moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder().add(new MovedClassToAnotherSourceFolder(originalClass, movedClass, identicalPath.getKey(), identicalPath.getValue()));
                                        methodContainerChanged = changeHistory.isMethodContainerChanged(null, Collections.singletonList(moveSourceFolderRefactoring), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, Collections.emptyList());
                                        containerChanged = !methodContainerChanged.isEmpty();
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (containerChanged) {
                                methodContainerChanged.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
                            Set<String> fileNames = getRightSideFileNames(currentMethod, commitModel, umlModelDiffLocal);
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentMethodFilePath, fileNames::contains, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
                            Set<Method> methodContainerChanged = changeHistory.isMethodContainerChanged(umlModelDiffAll, moveRenameClassRefactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffAll));
                            if (!methodContainerChanged.isEmpty()) {
                                UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightMethod.getUmlOperation().getClassName());
                                if (classDiff != null) {
                                    List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
                                    changeHistory.analyseMethodRefactorings(classLevelRefactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                                }
                                Set<Method> leftMethods = new HashSet<>();
                                leftMethods.addAll(methodContainerChanged);
                                leftMethods.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();
                            boolean flag = false;
                            for (Refactoring refactoring : refactorings) {
                                if (RefactoringType.MOVE_AND_RENAME_OPERATION.equals(refactoring.getRefactoringType()) || RefactoringType.MOVE_OPERATION.equals(refactoring.getRefactoringType())) {
                                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                                    Method movedOperation = Method.of(moveOperationRefactoring.getMovedOperation(), currentVersion);
                                    if (rightMethod.equalIdentifierIgnoringVersion(movedOperation)) {
                                        fileNames.add(moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath());
                                        flag = true;
                                    }
                                }
                            }
                            if (flag) {
                                umlModelPairAll = getUMLModelPair(commitModel, currentMethodFilePath, fileNames::contains, false);
                                umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                                refactorings = umlModelDiffAll.getRefactorings();
                            }

                            methodContainerChanged = changeHistory.isMethodContainerChanged(umlModelDiffAll, refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffAll));
                            boolean containerChanged = !methodContainerChanged.isEmpty();

                            Set<Method> methodRefactored = changeHistory.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                            boolean refactored = !methodRefactored.isEmpty();

                            if (containerChanged || refactored) {
                                Set<Method> leftMethods = new HashSet<>();
                                leftMethods.addAll(methodContainerChanged);
                                leftMethods.addAll(methodRefactored);
                                leftMethods.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (changeHistory.isMethodAdded(umlModelDiffAll, methods, rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private History.HistoryInfo<Method> blameReturn(Method startMethod) {
    	List<HistoryInfo<Method>> history = HistoryImpl.processHistory(changeHistory.get().getCompleteGraph());
        Collections.reverse(history); 
		for (History.HistoryInfo<Method> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (startMethod.isClosingCurlyBracket()) {
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
				else {
					if (change instanceof MethodSignatureChange || change instanceof Introduced) {
						return historyInfo;
					}
				}
			}
		}
		return null;
    }
}
