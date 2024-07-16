package org.codetracker;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.change.Change;
import org.codetracker.api.ClassTracker;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.change.Introduced;
import org.codetracker.change.clazz.ClassContainerChange;
import org.codetracker.change.clazz.ClassMove;
import org.codetracker.element.Class;
import org.codetracker.element.Package;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ClassTrackerImpl extends BaseTracker implements ClassTracker {
	private final ClassTrackerChangeHistory changeHistory;
    Logger logger = LoggerFactory.getLogger(ClassTrackerImpl.class);

    public ClassTrackerImpl(Repository repository, String startCommitId, String filePath, String className, int classDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.changeHistory = new ClassTrackerChangeHistory(className, classDeclarationLineNumber);
    }

    private boolean isStartClass(Class clazz) {
        return clazz.getUmlClass().getNonQualifiedName().equals(changeHistory.getClassName());
    }

    private boolean isStartComment(Package pack) {
    	return pack.getUmlClass().getName().endsWith(changeHistory.getClassName()) &&
    			pack.getUmlPackage().getLocationInfo().getStartLine() == changeHistory.getClassDeclarationLineNumber() &&
    			pack.getUmlPackage().getLocationInfo().getEndLine() == changeHistory.getClassDeclarationLineNumber();
    }

    @Override
    public History<Class> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Class start = getClass(umlModel, startVersion, this::isStartClass);
            if (start == null) {
                return null;
            }
            start.setStart(true);
            changeHistory.get().addNode(start);

            ArrayDeque<Class> classes = new ArrayDeque<>();
            classes.addFirst(start);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!classes.isEmpty()) {
                Class currentClass = classes.poll();
                if (currentClass.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentClass.getFilePath().equals(lastFileName)) {
                    lastFileName = currentClass.getFilePath();
                    commits = getCommits(repository, currentClass.getVersion().getId(), lastFileName, git);
                    historyReport.gitLogCommandCallsPlusPlus();
                    analysedCommits.clear();
                }
                if (analysedCommits.containsAll(commits))
                    break;
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    logger.debug("processing " + commitId);
                    analysedCommits.add(commitId);

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);


                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentClass.getFilePath()));
                    Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
                    if (rightClass == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
                        changeHistory.get().handleAdd(leftClass, rightClass, "Initial commit!");
                        changeHistory.get().connectRelatedNodes();
                        classes.add(leftClass);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(rightClass.getFilePath()));

                    //NO CHANGE
                    Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                    if (leftClass != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

//                    //Local Refactoring
//                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel, new HashMap<>());
//                    {
//                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
//
//                        Set<Class> classRefactored = analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
//                        boolean refactored = !classRefactored.isEmpty();
//                        if (refactored) {
//                            Set<Class> leftSideClasses = new HashSet<>(classRefactored);
//                            leftSideClasses.forEach(classes::addFirst);
//                            historyReport.step4PlusPlus();
//                            break;
//                        }
//                    }
                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, true);
                            UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
                            List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
                            Set<Class> classRefactored = changeHistory.analyseClassRefactorings(refactoringsPartial, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                            boolean refactored = !classRefactored.isEmpty();
                            if (refactored) {
                                Set<Class> leftSideClasses = new HashSet<>(classRefactored);
                                leftSideClasses.forEach(classes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
//                            Set<String> fileNames = getRightSideFileNames(rightClass.getFilePath(), rightClass.getUmlClass().getName(), Collections.emptySet(), commitModel, umlModelDiffLocal);
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();

                            Set<Class> classRefactored = changeHistory.analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                            boolean refactored = !classRefactored.isEmpty();
                            if (refactored) {
                                Set<Class> leftSideClasses = new HashSet<>(classRefactored);
                                leftSideClasses.forEach(classes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (changeHistory.isClassAdded(umlModelDiffAll, classes, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion)) {
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

    public HistoryInfo<Class> blame() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Class startClass = getClass(umlModel, startVersion, this::isStartClass);
            if (startClass == null) {
                return null;
            }
            startClass.checkClosingBracket(changeHistory.getClassDeclarationLineNumber());
            Package startPackage = startClass.findPackage(this::isStartComment);
            startClass.setStart(true);
            changeHistory.get().addNode(startClass);

            ArrayDeque<Class> classes = new ArrayDeque<>();
            classes.addFirst(startClass);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!classes.isEmpty()) {
            	History.HistoryInfo<Class> blame = startPackage != null ? blameReturn(startPackage) : blameReturn(startClass);
            	if (blame != null) return blame;
                Class currentClass = classes.poll();
                if (currentClass.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentClass.getFilePath().equals(lastFileName)) {
                    lastFileName = currentClass.getFilePath();
                    commits = getCommits(repository, currentClass.getVersion().getId(), lastFileName, git);
                    historyReport.gitLogCommandCallsPlusPlus();
                    analysedCommits.clear();
                }
                if (analysedCommits.containsAll(commits))
                    break;
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    logger.debug("processing " + commitId);
                    analysedCommits.add(commitId);

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);


                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentClass.getFilePath()));
                    Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
                    if (rightClass == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
                        changeHistory.get().handleAdd(leftClass, rightClass, "Initial commit!");
                        changeHistory.get().connectRelatedNodes();
                        classes.add(leftClass);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(rightClass.getFilePath()));

                    //NO CHANGE
                    Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                    if (leftClass != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, true);
                            UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
                            List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
                            Set<Class> classRefactored = changeHistory.analyseClassRefactorings(refactoringsPartial, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                            boolean refactored = !classRefactored.isEmpty();
                            if (refactored) {
                                Set<Class> leftSideClasses = new HashSet<>(classRefactored);
                                leftSideClasses.forEach(classes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();

                            Set<Class> classRefactored = changeHistory.analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                            boolean refactored = !classRefactored.isEmpty();
                            if (refactored) {
                                Set<Class> leftSideClasses = new HashSet<>(classRefactored);
                                leftSideClasses.forEach(classes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (changeHistory.isClassAdded(umlModelDiffAll, classes, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion)) {
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

    private History.HistoryInfo<Class> blameReturn(Class startClass) {
    	List<HistoryInfo<Class>> history = changeHistory.getHistory();
		for (History.HistoryInfo<Class> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (startClass.isClosingCurlyBracket()) {
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
				else {
					if (!(change instanceof ClassMove) && !(change instanceof ClassContainerChange)) {
						return historyInfo;
					}
				}
			}
		}
		return null;
    }

    private History.HistoryInfo<Class> blameReturn(Package startPackage) {
    	List<HistoryInfo<Class>> history = changeHistory.getHistory();
		for (History.HistoryInfo<Class> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof Introduced || change instanceof ClassMove) {
					return historyInfo;
				}
			}
		}
		return null;
    }
}
