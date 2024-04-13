package org.codetracker;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.AttributeTracker;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.element.Attribute;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import static org.codetracker.AttributeTrackerChangeHistory.getAttribute;

import java.util.*;

public class AttributeTrackerImpl extends BaseTracker implements AttributeTracker {
    private final AttributeTrackerChangeHistory changeHistory;

    public AttributeTrackerImpl(Repository repository, String startCommitId, String filePath, String attributeName, int attributeDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.changeHistory = new AttributeTrackerChangeHistory(attributeName, attributeDeclarationLineNumber);
    }

    @Override
    public History<Attribute> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Attribute start = getAttribute(umlModel, startVersion, changeHistory::isStartAttribute);
            if (start == null) {
                return null;
            }
            start.setStart(true);
            changeHistory.get().addNode(start);

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
                    //System.out.println("processing " + commitId);
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
                        changeHistory.get().handleAdd(leftAttribute, rightAttribute, "Initial commit!");
                        changeHistory.get().connectRelatedNodes();
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

                    String extractedClassFilePath = null;
                    //Local Refactoring
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        Set<Attribute> attributeContainerChanged = changeHistory.isAttributeContainerChanged(umlModelDiffLocal, refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffLocal));
                        boolean containerChanged = !attributeContainerChanged.isEmpty();

                        String renamedAttributeClassType = null;
                        for (Refactoring r : refactorings) {
                            if (r.getRefactoringType().equals(RefactoringType.RENAME_ATTRIBUTE)) {
                                RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring)r;
                                if (renameAttributeRefactoring.getRenamedAttribute().getType() != null) {
                                    renamedAttributeClassType = renameAttributeRefactoring.getRenamedAttribute().getType().getClassType();
                                }
                                if (renamedAttributeClassType != null) {
                                    CommitModel commitModel = getCommitModel(currentVersion.getId());
                                    for (String filePath : commitModel.fileContentsCurrentOriginal.keySet()) {
                                        if (filePath.endsWith(renamedAttributeClassType + ".java") && !commitModel.fileContentsBeforeOriginal.keySet().contains(filePath)) {
                                            extractedClassFilePath = filePath;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        Set<Attribute> attributeRefactored = null;
                        if (extractedClassFilePath == null)
                            attributeRefactored = changeHistory.analyseAttributeRefactorings(refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                        else
                            attributeRefactored = Collections.emptySet();
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
                            Set<Attribute> attributeContainerChanged = changeHistory.isAttributeContainerChanged(umlModelDiffPartial, refactoringsPartial, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffPartial));
                            boolean containerChanged = !attributeContainerChanged.isEmpty();

                            Set<Attribute> attributeRefactored = changeHistory.analyseAttributeRefactorings(refactoringsPartial, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
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
                            if (extractedClassFilePath != null) {
                                fileNames.add(extractedClassFilePath);
                            }
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentAttribute.getFilePath(), fileNames::contains, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();

                            int moveAttributeRefactorings = 0;
                            for (Refactoring refactoring : refactorings) {
                                if (RefactoringType.MOVE_ATTRIBUTE.equals(refactoring.getRefactoringType())) {
                                    MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
                                    Attribute movedAttribute = Attribute.of(moveAttributeRefactoring.getMovedAttribute(), currentVersion);
                                    if (rightAttribute.equalIdentifierIgnoringVersion(movedAttribute)) {
                                        fileNames.add(moveAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath());
                                        moveAttributeRefactorings++;
                                    }
                                }
                            }
                            if (moveAttributeRefactorings == 1) {
                                umlModelPairAll = getUMLModelPair(commitModel, currentAttribute.getFilePath(), fileNames::contains, false);
                                umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                                refactorings = umlModelDiffAll.getRefactorings();
                            }

                            Set<Attribute> attributeContainerChanged = changeHistory.isAttributeContainerChanged(umlModelDiffAll, refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiffAll));
                            boolean containerChanged = !attributeContainerChanged.isEmpty();

                            Set<Attribute> attributeRefactored;
                            if (moveAttributeRefactorings <= 1) {
                                attributeRefactored = changeHistory.analyseAttributeRefactorings(refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
                            }
                            else {
                                attributeRefactored = Collections.emptySet();
                            }
                            boolean refactored = !attributeRefactored.isEmpty();

                            if (containerChanged || refactored) {
                                Set<Attribute> leftAttributes = new HashSet<>();
                                leftAttributes.addAll(attributeContainerChanged);
                                leftAttributes.addAll(attributeRefactored);
                                leftAttributes.forEach(attributes::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (changeHistory.isAttributeAdded(umlModelDiffAll, attributes, rightAttribute.getUmlAttribute().getClassName(), currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
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
}
