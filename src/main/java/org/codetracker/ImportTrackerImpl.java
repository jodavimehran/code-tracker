package org.codetracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.ImportTracker;
import org.codetracker.api.Version;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.change.Change;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.BodyChange;
import org.codetracker.element.Class;
import org.codetracker.element.Import;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class ImportTrackerImpl extends BaseTracker implements ImportTracker {
	private final ImportTrackerChangeHistory changeHistory;

	public ImportTrackerImpl(Repository repository, String startCommitId, String filePath, String className, int classDeclarationLineNumber,
			CodeElementType codeElementType, int importStartLineNumber, int importEndLineNumber) {
		super(repository, startCommitId, filePath);
		this.changeHistory = new ImportTrackerChangeHistory(className, classDeclarationLineNumber, codeElementType, importStartLineNumber, importEndLineNumber);
	}

    public History.HistoryInfo<Import> blame() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Class startClass = getClass(umlModel, startVersion, changeHistory::isStartClass);
            if (startClass == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getClassName(), changeHistory.getClassDeclarationLineNumber());
            }
            Import startImport = startClass.findImport(changeHistory::isStartImport);
            if (startImport == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getImportType().name(), changeHistory.getImportStartLineNumber());
            }
            changeHistory.get().addNode(startImport);
            
            changeHistory.addFirst(startImport);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!changeHistory.isEmpty()) {
            	History.HistoryInfo<Import> blame = blameReturn();
            	if (blame != null) return blame;
                Import currentImport = changeHistory.poll();
                if (currentImport.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentImport.getFilePath().equals(lastFileName)) {
                    lastFileName = currentImport.getFilePath();
                    commits = getCommits(repository, currentImport.getVersion().getId(), currentImport.getFilePath(), git);
                    historyReport.gitLogCommandCallsPlusPlus();
                    analysedCommits.clear();
                }
                if (analysedCommits.containsAll(commits))
                    continue;
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    //System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);
                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);
                    Class currentClass = Class.of(currentImport.getClazz(), currentVersion);
                	UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentClass.getFilePath()));
                	Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
                    if (rightClass == null) {
                        continue;
                    }
                    Import rightImport = rightClass.findImport(currentImport::equalIdentifierIgnoringVersion);
                    if (rightImport == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
                        Import leftImport = Import.of(rightImport.getUmlImport(), leftClass);
                        changeHistory.get().handleAdd(leftImport, rightImport, "Initial commit!");
                        changeHistory.get().connectRelatedNodes();
                        changeHistory.add(leftImport);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentClass.getFilePath()));
                    //NO CHANGE
                    Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                    if (leftClass != null) {
                        historyReport.step2PlusPlus();
                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftClass.getUmlClass(), rightClass.getUmlClass());
                        changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, lightweightClassDiff);
                        continue;
                    }
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        //Local Refactoring
                    	UMLClassBaseDiff classDiff = getUMLClassDiff(umlModelDiffLocal, rightClass.getUmlClass().getName());
                        boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, classDiff);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }
                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            String leftFilePath = null;
                            for (MoveSourceFolderRefactoring ref : commitModel.moveSourceFolderRefactorings) {
                                if (ref.getIdenticalFilePaths().containsValue(currentImport.getFilePath())) {
                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
                                        if (entry.getValue().equals(currentImport.getFilePath())) {
                                            leftFilePath = entry.getKey();
                                            break;
                                        }
                                    }
                                    if (leftFilePath != null) {
                                        break;
                                    }
                                }
                            }
                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, currentClass.getFilePath(), s -> true, true);
                            if (leftFilePath != null) {
                                boolean found = false;
                                for (UMLClass umlClass : umlModelPairPartial.getLeft().getClassList()) {
                                    if (umlClass.getSourceFile().equals(leftFilePath)) {
                                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(umlClass, rightClass.getUmlClass());
                                        found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, lightweightClassDiff);
                                        if (found) {
                                            break;
                                        }
                                    }
                                }
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }
                            else {
                                UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
                                //List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
                                UMLClassBaseDiff classDiff = getUMLClassDiff(umlModelDiffPartial, rightClass.getUmlClass().getName());
    	                        boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, classDiff);
    	                        if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }
                        }
                        {
                            //Set<String> fileNames = getRightSideFileNames(currentClass.getFilePath(), currentClass.getUmlClass().getName(), Collections.emptySet(), commitModel, umlModelDiffLocal);
                        	Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
                            UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightClass.getUmlClass().getName());
                            if (classDiff != null) {
                                boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, classDiff);
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }

                            UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightClass.getUmlClass().getName());
                            if (umlClassDiff != null) {
                                boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, umlClassDiff);
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }

                            if (isClassAdded(umlModelDiffAll, rightClass.getUmlClass().getName())) {
                                Import importBefore = Import.of(rightImport.getUmlImport(), rightImport.getClazz(), parentVersion);
                                changeHistory.get().handleAdd(importBefore, rightImport, "added with class");
                                changeHistory.add(importBefore);
                                changeHistory.get().connectRelatedNodes();
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

    private History.HistoryInfo<Import> blameReturn() {
    	List<HistoryInfo<Import>> history = changeHistory.getHistory();
		for (History.HistoryInfo<Import> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof BodyChange || change instanceof Introduced) {
					return historyInfo;
				}
			}
		}
		return null;
    }
}
