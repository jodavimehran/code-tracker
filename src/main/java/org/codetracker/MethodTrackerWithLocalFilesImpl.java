package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MethodTrackerWithLocalFilesImpl extends BaseTrackerWithLocalFiles implements MethodTracker {
	private final MethodTrackerChangeHistory changeHistory;

    public MethodTrackerWithLocalFilesImpl(String cloneURL, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber) {
        super(cloneURL, startCommitId, filePath);
        this.changeHistory = new MethodTrackerChangeHistory(methodName, methodDeclarationLineNumber);
    }

    @Override
    public History<Method> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        Version startVersion = new VersionImpl(startCommitId, 0, 0, "");
        CommitModel startModel = getCommitModel(startCommitId);
        Set<String> startFileNames = Collections.singleton(filePath);
    	Map<String, String> startFileContents = new LinkedHashMap<>();
    	for(String rightFileName : startFileNames) {
    		startFileContents.put(rightFileName, startModel.fileContentsCurrentOriginal.get(rightFileName));
    	}
    	UMLModel umlModel = GitHistoryRefactoringMinerImpl.createModel(startFileContents, startModel.repositoryDirectoriesCurrent);
    	umlModel.setPartial(true);
        Method start = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
        String startFilePath = start.getFilePath();
        if (start == null) {
            throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
        }
        changeHistory.get().addNode(start);

        changeHistory.addFirst(start);
        HashSet<String> analysedCommits = new HashSet<>();
        List<String> commits = null;
        String lastFileName = null;
        while (!changeHistory.isEmpty()) {
            Method currentMethod = changeHistory.poll();
            if (currentMethod.isAdded() || currentMethod.getVersion().getId().equals("0")) {
                commits = null;
                continue;
            }
            final String currentMethodFilePath = currentMethod.getFilePath();
            if (commits == null || !currentMethodFilePath.equals(lastFileName)) {
                lastFileName = currentMethodFilePath;
                String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
        		String className = startFilePath.substring(startFilePath.lastIndexOf("/") + 1);
        		className = className.endsWith(".java") ? className.substring(0, className.length()-5) : className;
                String jsonPath = System.getProperty("user.dir") + "/src/test/resources/method/" + repoName + "-" + className + "-" + changeHistory.getMethodName() + ".json";
                File jsonFile = new File(jsonPath);
                commits = getCommits(currentMethod.getVersion().getId(), jsonFile);
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
                
                CommitModel lightCommitModel = getLightCommitModel(commitId, currentMethodFilePath);
                String parentCommitId = lightCommitModel.parentCommitId;
                Version currentVersion = new VersionImpl(commitId, 0, 0, "");
                Version parentVersion = new VersionImpl(parentCommitId, 0, 0, "");
            	
            	UMLModel leftModel = GitHistoryRefactoringMinerImpl.createModel(lightCommitModel.fileContentsBeforeOriginal, lightCommitModel.repositoryDirectoriesBefore);
            	leftModel.setPartial(true);
            	UMLModel rightModel = GitHistoryRefactoringMinerImpl.createModel(lightCommitModel.fileContentsCurrentOriginal, lightCommitModel.repositoryDirectoriesCurrent);
            	rightModel.setPartial(true);
                Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                if (rightMethod == null) {
                    continue;
                }
                historyReport.analysedCommitsPlusPlus();
                if ("0".equals(parentCommitId)) {
                    Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                    changeHistory.get().handleAdd(leftMethod, rightMethod, "Initial commit!");
                    changeHistory.get().connectRelatedNodes();
                    changeHistory.add(leftMethod);
                    break;
                }

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
                        leftSideMethods.forEach(changeHistory::addFirst);
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
                            methodContainerChanged.forEach(changeHistory::addFirst);
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
                            leftMethods.forEach(changeHistory::addFirst);
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
                            leftMethods.forEach(changeHistory::addFirst);
                            historyReport.step5PlusPlus();
                            break;
                        }

                        if (changeHistory.isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
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
