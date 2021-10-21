package org.codetracker;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.change.Change;
import org.codetracker.api.ClassTracker;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Class;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;

import java.util.*;
import java.util.function.Predicate;

public class ClassTrackerImpl extends BaseTracker implements ClassTracker {
    private final ChangeHistory<Class> classChangeHistory = new ChangeHistory<>();
    private final String className;
    private final int classDeclarationLineNumber;

    public ClassTrackerImpl(Repository repository, String startCommitId, String filePath, String className, int classDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.className = className;
        this.classDeclarationLineNumber = classDeclarationLineNumber;
    }

    protected static Class getClass(UMLModel umlModel, Version version, Predicate<Class> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Class clazz = Class.of(umlClass, version);
                if (predicate.test(clazz))
                    return clazz;
            }
        return null;
    }

    private boolean isStartClass(Class clazz) {
        return clazz.getUmlClass().getNonQualifiedName().equals(className);
    }

    @Override
    public History<Class> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singletonList(filePath));
            Class start = getClass(umlModel, startVersion, this::isStartClass);
            if (start == null) {
                return null;
            }
            start.setStart(true);
            classChangeHistory.addNode(start);

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
                    System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);


                    UMLModel rightModel = getUMLModel(commitId, Collections.singletonList(currentClass.getFilePath()));
                    Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
                    if (rightClass == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
                        classChangeHistory.handleAdd(leftClass, rightClass, "Initial commit!");
                        classChangeHistory.connectRelatedNodes();
                        classes.add(leftClass);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singletonList(rightClass.getFilePath()));

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
                            UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight(), commitModel.renamedFilesHint);
                            List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
                            Set<Class> classRefactored = analyseClassRefactorings(refactoringsPartial, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
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
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight(), commitModel.renamedFilesHint);

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();

                            Set<Class> classRefactored = analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
                            boolean refactored = !classRefactored.isEmpty();
                            if (refactored) {
                                Set<Class> leftSideClasses = new HashSet<>(classRefactored);
                                leftSideClasses.forEach(classes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (isClassAdded(umlModelDiffAll, classes, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion)) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
            return new HistoryImpl<>(classChangeHistory.findSubGraph(start), historyReport);
        }
    }

    public Set<Class> analyseClassRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator) {
        Set<Class> leftClassSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            UMLClass leftUMLClass = null;
            UMLClass rightUMLClass = null;
            Change.Type changeType = null;
            Change.Type changeType2 = null;
            switch (refactoring.getRefactoringType()) {
                case MOVE_SOURCE_FOLDER: {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                    for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                        Class classAfter = Class.of(movedClassToAnotherSourceFolder.getMovedClass(), currentVersion);
                        if (equalOperator.test(classAfter)) {
                            leftUMLClass = movedClassToAnotherSourceFolder.getOriginalClass();
                            rightUMLClass = movedClassToAnotherSourceFolder.getMovedClass();
                            changeType = Change.Type.CONTAINER_CHANGE;
                            break;
                        }
                    }
                    break;
                }
                case MOVE_CLASS: {
                    MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                    leftUMLClass = moveClassRefactoring.getOriginalClass();
                    rightUMLClass = moveClassRefactoring.getMovedClass();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case RENAME_CLASS: {
                    RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                    leftUMLClass = renameClassRefactoring.getOriginalClass();
                    rightUMLClass = renameClassRefactoring.getRenamedClass();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case MOVE_RENAME_CLASS: {
                    MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                    leftUMLClass = moveAndRenameClassRefactoring.getOriginalClass();
                    rightUMLClass = moveAndRenameClassRefactoring.getRenamedClass();
                    changeType = Change.Type.RENAME;
                    changeType2 = Change.Type.MOVED;
                    break;
                }
                case ADD_CLASS_ANNOTATION: {
                    AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) refactoring;
                    leftUMLClass = addClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = addClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_CLASS_ANNOTATION: {
                    RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) refactoring;
                    leftUMLClass = removeClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = removeClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_CLASS_ANNOTATION: {
                    ModifyClassAnnotationRefactoring modifyClassAnnotationRefactoring = (ModifyClassAnnotationRefactoring) refactoring;
                    leftUMLClass = modifyClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = modifyClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case ADD_CLASS_MODIFIER: {
                    AddClassModifierRefactoring addClassModifierRefactoring = (AddClassModifierRefactoring) refactoring;
                    leftUMLClass = addClassModifierRefactoring.getClassBefore();
                    rightUMLClass = addClassModifierRefactoring.getClassAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_CLASS_MODIFIER: {
                    RemoveClassModifierRefactoring removeClassModifierRefactoring = (RemoveClassModifierRefactoring) refactoring;
                    leftUMLClass = removeClassModifierRefactoring.getClassBefore();
                    rightUMLClass = removeClassModifierRefactoring.getClassAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case CHANGE_CLASS_ACCESS_MODIFIER: {
                    ChangeClassAccessModifierRefactoring changeClassAccessModifierRefactoring = (ChangeClassAccessModifierRefactoring) refactoring;
                    leftUMLClass = changeClassAccessModifierRefactoring.getClassBefore();
                    rightUMLClass = changeClassAccessModifierRefactoring.getClassAfter();
                    changeType = Change.Type.ACCESS_MODIFIER_CHANGE;
                    break;
                }
                case EXTRACT_INTERFACE:
                case EXTRACT_SUPERCLASS: {
                    ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) refactoring;
                    leftUMLClass = extractSuperclassRefactoring.getExtractedClass();
                    rightUMLClass = extractSuperclassRefactoring.getExtractedClass();
                    changeType = Change.Type.INTRODUCED;
                    break;
                }
                case EXTRACT_SUBCLASS:
                case EXTRACT_CLASS: {
                    ExtractClassRefactoring extractClassRefactoring = (ExtractClassRefactoring) refactoring;
                    leftUMLClass = extractClassRefactoring.getExtractedClass();
                    rightUMLClass = extractClassRefactoring.getExtractedClass();
                    changeType = Change.Type.INTRODUCED;
                    break;
                }
                case CHANGE_TYPE_DECLARATION_KIND: {
                    ChangeTypeDeclarationKindRefactoring changeTypeDeclarationKindRefactoring = (ChangeTypeDeclarationKindRefactoring)refactoring;
                    leftUMLClass = changeTypeDeclarationKindRefactoring.getClassBefore();
                    rightUMLClass = changeTypeDeclarationKindRefactoring.getClassAfter();
                    changeType = Change.Type.TYPE_CHANGE;
                    break;
                }
            }

            if (rightUMLClass != null) {
                Class classAfter = Class.of(rightUMLClass, currentVersion);
                if (equalOperator.test(classAfter)) {
                    Class classBefore = Class.of(leftUMLClass, parentVersion);
                    if (Change.Type.INTRODUCED.equals(changeType)) {
                        classChangeHistory.handleAdd(classBefore, classAfter, refactoring.toString());
                    } else {
                        classChangeHistory.addChange(classBefore, classAfter, ChangeFactory.forClass(changeType).refactoring(refactoring));
                    }
                    if (changeType2 != null)
                        classChangeHistory.addChange(classBefore, classAfter, ChangeFactory.forClass(changeType2).refactoring(refactoring));
                    leftClassSet.add(classBefore);
                }
            }
        }

        if (!leftClassSet.isEmpty())
            classChangeHistory.connectRelatedNodes();
        return leftClassSet;
    }

    private boolean isClassAdded(UMLModelDiff modelDiff, ArrayDeque<Class> classes, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator) {
        List<UMLClass> addedClasses = modelDiff.getAddedClasses();
        for (UMLClass umlClass : addedClasses) {
            Class rightClass = Class.of(umlClass, currentVersion);
            if (equalOperator.test(rightClass)) {
                Class leftClass = Class.of(umlClass, parentVersion);
                classChangeHistory.handleAdd(leftClass, rightClass, "new class");
                classChangeHistory.connectRelatedNodes();
                classes.addFirst(leftClass);
                return true;
            }
        }
        return false;
    }

}
