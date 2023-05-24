package org.codetracker;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.AttributeTracker;
import org.codetracker.change.Change;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Attribute;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AttributeTrackerImpl extends BaseTracker implements AttributeTracker {
    private final ChangeHistory<Attribute> attributeChangeHistory = new ChangeHistory<>();
    private final String attributeName;
    private final int attributeDeclarationLineNumber;

    public AttributeTrackerImpl(Repository repository, String startCommitId, String filePath, String attributeName, int attributeDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.attributeName = attributeName;
        this.attributeDeclarationLineNumber = attributeDeclarationLineNumber;
    }

    protected static Attribute getAttribute(UMLModel umlModel, Version version, Predicate<Attribute> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Attribute attribute = getAttribute(version, predicate, umlClass.getAttributes());
                if (attribute != null) return attribute;
                attribute = getAttribute(version, predicate, umlClass.getEnumConstants());
                if (attribute != null) return attribute;
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    attribute = getAttribute(version, predicate, anonymousClass.getAttributes());
                    if (attribute != null) return attribute;
                    attribute = getAttribute(version, predicate, anonymousClass.getEnumConstants());
                    if (attribute != null) return attribute;
                }
            }
        return null;
    }

    private static Attribute getAttribute(Version version, Predicate<Attribute> predicate, List<? extends UMLAttribute> attributes) {
        for (UMLAttribute umlAttribute : attributes) {
            Attribute attribute = Attribute.of(umlAttribute, version);
            if (predicate.test(attribute))
                return attribute;
        }
        return null;
    }

    private boolean isStartAttribute(Attribute attribute) {
        return attribute.getUmlAttribute().getName().equals(attributeName) &&
                attribute.getUmlAttribute().getLocationInfo().getStartLine() <= attributeDeclarationLineNumber &&
                attribute.getUmlAttribute().getLocationInfo().getEndLine() >= attributeDeclarationLineNumber;
    }

    @Override
    public History<Attribute> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Attribute start = getAttribute(umlModel, startVersion, this::isStartAttribute);
            if (start == null) {
                return null;
            }
            start.setStart(true);
            attributeChangeHistory.addNode(start);

            ArrayDeque<Attribute> attributes = new ArrayDeque<>();
            attributes.addFirst(start);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!attributes.isEmpty()) {
                Attribute currentAttribute = attributes.poll();
                if (currentAttribute.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentAttribute.getFilePath().equals(lastFileName)) {
                    lastFileName = currentAttribute.getFilePath();
                    commits = getCommits(repository, currentAttribute.getVersion().getId(), lastFileName, git);
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


                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentAttribute.getFilePath()));
                    Attribute rightAttribute = getAttribute(rightModel, currentVersion, currentAttribute::equalIdentifierIgnoringVersion);
                    if (rightAttribute == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Attribute leftAttribute = Attribute.of(rightAttribute.getUmlAttribute(), parentVersion);
                        attributeChangeHistory.handleAdd(leftAttribute, rightAttribute, "Initial commit!");
                        attributeChangeHistory.connectRelatedNodes();
                        attributes.add(leftAttribute);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(rightAttribute.getFilePath()));

                    //NO CHANGE
                    Attribute leftAttribute = getAttribute(leftModel, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                    if (leftAttribute != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

                    //Local Refactoring
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        Set<Attribute> attributeContainerChanged = isAttributeContainerChanged(umlModelDiffLocal, refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                        boolean containerChanged = !attributeContainerChanged.isEmpty();

                        Set<Attribute> attributeRefactored = analyseAttributeRefactorings(refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                        boolean refactored = !attributeRefactored.isEmpty();

                        if (containerChanged || refactored) {
                            Set<Attribute> leftSideAttributes = new HashSet<>();
                            leftSideAttributes.addAll(attributeContainerChanged);
                            leftSideAttributes.addAll(attributeRefactored);
                            leftSideAttributes.forEach(attributes::addFirst);
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }
                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, currentAttribute.getFilePath(), s -> true, true);
                            UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
                            List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
                            Set<Attribute> attributeContainerChanged = isAttributeContainerChanged(umlModelDiffPartial, refactoringsPartial, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                            boolean containerChanged = !attributeContainerChanged.isEmpty();

                            Set<Attribute> attributeRefactored = analyseAttributeRefactorings(refactoringsPartial, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                            boolean refactored = !attributeRefactored.isEmpty();

                            if (containerChanged || refactored) {
                                Set<Attribute> leftSideAttributes = new HashSet<>();
                                leftSideAttributes.addAll(attributeContainerChanged);
                                leftSideAttributes.addAll(attributeRefactored);
                                leftSideAttributes.forEach(attributes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
                            Set<String> fileNames = getRightSideFileNames(currentAttribute.getFilePath(), currentAttribute.getUmlAttribute().getClassName(), Collections.emptySet(), commitModel, umlModelDiffLocal);
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentAttribute.getFilePath(), fileNames::contains, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();

                            boolean flag = false;
                            for (Refactoring refactoring : refactorings) {
                                if (RefactoringType.MOVE_ATTRIBUTE.equals(refactoring.getRefactoringType())) {
                                    MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
                                    Attribute movedAttribute = Attribute.of(moveAttributeRefactoring.getMovedAttribute(), currentVersion);
                                    if (rightAttribute.equalIdentifierIgnoringVersion(movedAttribute)) {
                                        fileNames.add(moveAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath());
                                        flag = true;
                                    }
                                }
                            }
                            if (flag) {
                                umlModelPairAll = getUMLModelPair(commitModel, currentAttribute.getFilePath(), fileNames::contains, false);
                                umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                                refactorings = umlModelDiffAll.getRefactorings();
                            }

                            Set<Attribute> attributeContainerChanged = isAttributeContainerChanged(umlModelDiffAll, refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                            boolean containerChanged = !attributeContainerChanged.isEmpty();

                            Set<Attribute> attributeRefactored = analyseAttributeRefactorings(refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                            boolean refactored = !attributeRefactored.isEmpty();

                            if (containerChanged || refactored) {
                                Set<Attribute> leftAttributes = new HashSet<>();
                                leftAttributes.addAll(attributeContainerChanged);
                                leftAttributes.addAll(attributeRefactored);
                                leftAttributes.forEach(attributes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (isAttributeAdded(umlModelDiffAll, attributes, rightAttribute.getUmlAttribute().getClassName(), currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion)) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
            return new HistoryImpl<>(attributeChangeHistory.findSubGraph(start), historyReport);
        }
    }

    public Set<Attribute> analyseAttributeRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator) {
        Set<Attribute> leftAttributeSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            UMLAttribute attributeBefore = null;
            UMLAttribute attributeAfter = null;
            Change.Type changeType = null;

            switch (refactoring.getRefactoringType()) {
                case PULL_UP_ATTRIBUTE: {
                    PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
                    attributeBefore = pullUpAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = pullUpAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case PUSH_DOWN_ATTRIBUTE: {
                    PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) refactoring;
                    attributeBefore = pushDownAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = pushDownAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case MOVE_ATTRIBUTE: {
                    MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
                    attributeBefore = moveAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = moveAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case MOVE_RENAME_ATTRIBUTE: {
                    MoveAndRenameAttributeRefactoring moveAndRenameAttributeRefactoring = (MoveAndRenameAttributeRefactoring) refactoring;
                    attributeBefore = moveAndRenameAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = moveAndRenameAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, refactoring, attributeBefore, attributeAfter, Change.Type.RENAME);
                    break;
                }
                case RENAME_ATTRIBUTE: {
                    RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) refactoring;
                    attributeBefore = renameAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = renameAttributeRefactoring.getRenamedAttribute();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case ADD_ATTRIBUTE_ANNOTATION: {
                    AddAttributeAnnotationRefactoring addAttributeAnnotationRefactoring = (AddAttributeAnnotationRefactoring) refactoring;
                    attributeBefore = addAttributeAnnotationRefactoring.getAttributeBefore();
                    attributeAfter = addAttributeAnnotationRefactoring.getAttributeAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_ATTRIBUTE_ANNOTATION: {
                    ModifyAttributeAnnotationRefactoring modifyAttributeAnnotationRefactoring = (ModifyAttributeAnnotationRefactoring) refactoring;
                    attributeBefore = modifyAttributeAnnotationRefactoring.getAttributeBefore();
                    attributeAfter = modifyAttributeAnnotationRefactoring.getAttributeAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_ATTRIBUTE_ANNOTATION: {
                    RemoveAttributeAnnotationRefactoring removeAttributeAnnotationRefactoring = (RemoveAttributeAnnotationRefactoring) refactoring;
                    attributeBefore = removeAttributeAnnotationRefactoring.getAttributeBefore();
                    attributeAfter = removeAttributeAnnotationRefactoring.getAttributeAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case CHANGE_ATTRIBUTE_TYPE: {
                    ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) refactoring;
                    attributeBefore = changeAttributeTypeRefactoring.getOriginalAttribute();
                    attributeAfter = changeAttributeTypeRefactoring.getChangedTypeAttribute();
                    changeType = Change.Type.TYPE_CHANGE;
                    break;
                }
                case ADD_ATTRIBUTE_MODIFIER: {
                    AddAttributeModifierRefactoring addAttributeModifierRefactoring = (AddAttributeModifierRefactoring) refactoring;
                    attributeBefore = addAttributeModifierRefactoring.getAttributeBefore();
                    attributeAfter = addAttributeModifierRefactoring.getAttributeAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_ATTRIBUTE_MODIFIER: {
                    RemoveAttributeModifierRefactoring removeAttributeModifierRefactoring = (RemoveAttributeModifierRefactoring) refactoring;
                    attributeBefore = removeAttributeModifierRefactoring.getAttributeBefore();
                    attributeAfter = removeAttributeModifierRefactoring.getAttributeAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case CHANGE_ATTRIBUTE_ACCESS_MODIFIER: {
                    ChangeAttributeAccessModifierRefactoring changeAttributeAccessModifierRefactoring = (ChangeAttributeAccessModifierRefactoring) refactoring;
                    attributeBefore = changeAttributeAccessModifierRefactoring.getAttributeBefore();
                    attributeAfter = changeAttributeAccessModifierRefactoring.getAttributeAfter();
                    changeType = Change.Type.ACCESS_MODIFIER_CHANGE;
                    break;
                }
                case EXTRACT_ATTRIBUTE: {
                    ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) refactoring;
                    Attribute rightAttribute = Attribute.of(extractAttributeRefactoring.getVariableDeclaration(), currentVersion);
                    if (equalOperator.test(rightAttribute)) {
                        Attribute leftAttribute = Attribute.of(extractAttributeRefactoring.getVariableDeclaration(), parentVersion);
                        attributeChangeHistory.handleAdd(leftAttribute, rightAttribute , refactoring.toString());
                        attributeChangeHistory.connectRelatedNodes();
                        leftAttributeSet.add(leftAttribute);
                        return leftAttributeSet;
                    }
                    break;
                }

            }

            addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, refactoring, attributeBefore, attributeAfter, changeType);
        }
        attributeChangeHistory.connectRelatedNodes();
        return leftAttributeSet;
    }

    public boolean addAttributeChange(Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, Set<Attribute> leftAttributeSet, Refactoring refactoring, UMLAttribute umlAttributeBefore, UMLAttribute umlAttributeAfter, Change.Type changeType) {
        if (umlAttributeAfter != null) {
            Attribute attributeAfter = Attribute.of(umlAttributeAfter, currentVersion);
            if (equalOperator.test(attributeAfter)) {
                Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
                attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(changeType).refactoring(refactoring));
                leftAttributeSet.add(attributeBefore);
                return true;
            }
        }
        return false;
    }

    private boolean isAttributeAdded(UMLModelDiff modelDiff, ArrayDeque<Attribute> attributes, String className, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator) {
        List<UMLAttribute> addedAttributes = getAllClassesDiff(modelDiff)
                .stream()
                .map(UMLClassBaseDiff::getAddedAttributes)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLAttribute umlAttribute : addedAttributes) {
            if (handleAddAttribute(attributes, currentVersion, parentVersion, equalOperator, umlAttribute, "new attribute"))
                return true;
        }

        UMLClass addedClass = modelDiff.getAddedClass(className);
        if (addedClass != null) {
            for (UMLAttribute umlAttribute : addedClass.getAttributes()) {
                if (handleAddAttribute(attributes, currentVersion, parentVersion, equalOperator, umlAttribute, "added with new class"))
                    return true;
            }
        }

        for (UMLClassRenameDiff classRenameDiffList : modelDiff.getClassRenameDiffList()) {
            for (UMLAnonymousClass addedAnonymousClasses : classRenameDiffList.getAddedAnonymousClasses()) {
                for (UMLAttribute umlAttribute : addedAnonymousClasses.getAttributes()) {
                    if (handleAddAttribute(attributes, currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean handleAddAttribute(ArrayDeque<Attribute> attributes, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, UMLAttribute umlAttribute, String comment) {
        Attribute rightAttribute = Attribute.of(umlAttribute, currentVersion);
        if (equalOperator.test(rightAttribute)) {
            Attribute leftAttribute = Attribute.of(umlAttribute, parentVersion);
            attributeChangeHistory.handleAdd(leftAttribute, rightAttribute, comment);
            attributeChangeHistory.connectRelatedNodes();
            attributes.addFirst(leftAttribute);
            return true;
        }
        return false;
    }

    private Set<Attribute> isAttributeContainerChanged(UMLModelDiff umlModelDiffAll, Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator) {
        Set<Attribute> leftAttributeSet = new HashSet<>();
        boolean found = false;
        Change.Type changeType = Change.Type.CONTAINER_CHANGE;

        for (UMLClassMoveDiff umlClassMoveDiff : getClassMoveDiffList(umlModelDiffAll)) {
            for (UMLAttributeDiff attributeDiff : umlClassMoveDiff.getAttributeDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
        }

        for (UMLClassRenameDiff umlClassRenameDiff : umlModelDiffAll.getClassRenameDiffList()) {
            for (UMLAttributeDiff attributeDiff : umlClassRenameDiff.getAttributeDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new RenameClassRefactoring(umlClassRenameDiff.getOriginalClass(), umlClassRenameDiff.getRenamedClass()), attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
        }
        for (UMLClassMoveDiff umlClassMoveDiff : umlModelDiffAll.getInnerClassMoveDiffList()) {
            for (UMLAttributeDiff attributeDiff : umlClassMoveDiff.getAttributeDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
        }

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType() == RefactoringType.MOVE_SOURCE_FOLDER) {
                MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                    UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                    UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                    if (checkAttributeContainerChangeInMovedClasses(originalClass, movedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                        return leftAttributeSet;
                    }
                }
            } else if (refactoring.getRefactoringType() == RefactoringType.MOVE_CLASS) {
                MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                UMLClass movedClass = moveClassRefactoring.getMovedClass();
                if (checkAttributeContainerChangeInMovedClasses(originalClass, movedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                    return leftAttributeSet;
                }
            } else if (refactoring.getRefactoringType() == RefactoringType.RENAME_CLASS) {
                RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                UMLClass renamedClass = renameClassRefactoring.getRenamedClass();
                if (checkAttributeContainerChangeInMovedClasses(originalClass, renamedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                    return leftAttributeSet;
                }
            } else if (refactoring.getRefactoringType() == RefactoringType.MOVE_RENAME_CLASS) {
                MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                if (checkAttributeContainerChangeInMovedClasses(originalClass, renamedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                    return leftAttributeSet;
                }
            }
        }
        return Collections.emptySet();
    }

    public boolean checkAttributeContainerChangeInMovedClasses(UMLClass originalClass, UMLClass movedClass, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, Refactoring refactoring, Set<Attribute> leftAttributeSet) {
        for (UMLAttribute umlAttributeAfter : movedClass.getAttributes()) {
            Attribute attributeAfter = Attribute.of(umlAttributeAfter, currentVersion);
            if (equalOperator.test(attributeAfter)) {
                for (UMLAttribute umlAttributeBefore : originalClass.getAttributes()) {
                    if (umlAttributeAfter.equals(umlAttributeBefore)) {
                        Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
                        attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Change.Type.CONTAINER_CHANGE).refactoring(refactoring));
                        attributeChangeHistory.connectRelatedNodes();
                        leftAttributeSet.add(attributeBefore);

                        return true;
                    }
                }
            }

        }
        return false;
    }

}
