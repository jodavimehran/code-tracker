package org.codetracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.AnnotationTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.element.Annotation;
import org.codetracker.element.Attribute;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.MoveAndRenameAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class AnnotationTrackerImpl extends BaseTracker implements AnnotationTracker {
	private final AnnotationTrackerChangeHistory changeHistory;
	
	public AnnotationTrackerImpl(Repository repository, String startCommitId, String filePath,
            String methodName, int methodDeclarationLineNumber,
            CodeElementType annotationType, int annotationStartLineNumber, int annotationEndLineNumber) {
		super(repository, startCommitId, filePath);
		this.changeHistory = new AnnotationTrackerChangeHistory(methodName, methodDeclarationLineNumber, annotationType, annotationStartLineNumber, annotationEndLineNumber);
	}

    public History.HistoryInfo<Annotation> blame() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method startMethod = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
            Attribute startAttribute = getAttribute(umlModel, startVersion, changeHistory::isStartAttribute);
            Class startClass = getClass(umlModel, startVersion, changeHistory::isStartClass);
            if (startMethod == null && startClass == null && startAttribute == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
            }
            Annotation startAnnotation = null;
            if (startMethod != null)
            	startAnnotation = startMethod.findAnnotation(changeHistory::isStartAnnotation);
            if (startClass != null)
            	startAnnotation = startClass.findAnnotation(changeHistory::isStartAnnotation);
            if (startAttribute != null)
            	startAnnotation = startAttribute.findAnnotation(changeHistory::isStartAnnotation);
            if (startAnnotation == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getAnnotationType().name(), changeHistory.getAnnotationStartLineNumber());
            }
            changeHistory.get().addNode(startAnnotation);

            changeHistory.addFirst(startAnnotation);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!changeHistory.isEmpty()) {
            	History.HistoryInfo<Annotation> blame = changeHistory.blameReturn();
            	if (blame != null) return blame;
            	Annotation currentAnnotation = changeHistory.poll();
                if (currentAnnotation.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentAnnotation.getFilePath().equals(lastFileName)) {
                    lastFileName = currentAnnotation.getFilePath();
                    commits = getCommits(repository, currentAnnotation.getVersion().getId(), currentAnnotation.getFilePath(), git);
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
                    if (currentAnnotation.getOperation().isPresent()) {
                    	if (currentAnnotation.getOperation().get() instanceof UMLOperation || currentAnnotation.getOperation().get() instanceof UMLInitializer) {
		                    Method currentMethod = Method.of(currentAnnotation.getOperation().get(), currentVersion);
		                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
		                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
		                    if (rightMethod == null) {
		                        continue;
		                    }
		                    String rightMethodClassName = rightMethod.getUmlOperation().getClassName();
		                    String rightMethodSourceFolder = rightMethod.getUmlOperation().getLocationInfo().getSourceFolder();
		                    Annotation rightAnnotation = rightMethod.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
		                    if (rightAnnotation == null) {
		                        continue;
		                    }
		                    Predicate<Method> equalMethod = rightMethod::equalIdentifierIgnoringVersion;
		                    Predicate<Annotation> equalAnnotation = rightAnnotation::equalIdentifierIgnoringVersion;
		                    historyReport.analysedCommitsPlusPlus();
		                    if ("0".equals(parentCommitId)) {
		                        Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
		                        Annotation leftAnnotation = Annotation.of(rightAnnotation.getAnnotation(), leftMethod);
		                        changeHistory.get().handleAdd(leftAnnotation, rightAnnotation, "Initial commit!");
		                        changeHistory.get().connectRelatedNodes();
		                        changeHistory.add(leftAnnotation);
		                        break;
		                    }
		                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethod.getFilePath()));
		                    //NO CHANGE
		                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
		                    if (leftMethod != null) {
		                    	Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(leftMethod.getUmlOperation(), rightMethod.getUmlOperation());
								changeHistory.checkBodyOfMatched(currentVersion, parentVersion, equalAnnotation, pair);
		                        historyReport.step2PlusPlus();
		                        continue;
		                    }
		                    //CHANGE BODY OR DOCUMENT
		                    leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
		                    //check if there is another method in leftModel with identical bodyHashCode to the rightMethod
		                    boolean otherExactMatchFound = false;
		                    if (leftMethod != null) {
		                        for (UMLClass leftClass : leftModel.getClassList()) {
		                            for (UMLOperation leftOperation : leftClass.getOperations()) {
		                                if (leftOperation.getBodyHashCode() == rightMethod.getUmlOperation().getBodyHashCode() && !leftOperation.equals(leftMethod.getUmlOperation())) {
		                                    otherExactMatchFound = true;
		                                    break;
		                                }
		                            }
		                            if(otherExactMatchFound) {
		                                break;
		                            }
		                        }
		                    }
		                    if (leftMethod != null && !otherExactMatchFound) {
		                        VariableDeclarationContainer leftOperation = leftMethod.getUmlOperation();
		                        VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
		                        UMLOperationBodyMapper bodyMapper = null;
		                        if (leftOperation instanceof UMLOperation && rightOperation instanceof UMLOperation) {
		                            UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
		                            bodyMapper = new UMLOperationBodyMapper((UMLOperation) leftOperation, (UMLOperation) rightOperation, lightweightClassDiff);
		                        }
		                        else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
		                            UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
		                            bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
		                        }
		                        if (changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, bodyMapper)) {
		                            historyReport.step3PlusPlus();
		                            break;
		                        }
		                    }
		                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
		                    {
		                        //Local Refactoring
		                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
		                        boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightAnnotation, refactorings);
		                        if (found) {
		                            historyReport.step4PlusPlus();
		                            break;
		                        }
		                        found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightAnnotation, refactorings);
		                        if (found) {
		                            historyReport.step4PlusPlus();
		                            break;
		                        }
		                        found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
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
		                                if (ref.getIdenticalFilePaths().containsValue(currentAnnotation.getFilePath())) {
		                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
		                                        if (entry.getValue().equals(currentAnnotation.getFilePath())) {
		                                            leftFilePath = entry.getKey();
		                                            break;
		                                        }
		                                    }
		                                    if (leftFilePath != null) {
		                                        break;
		                                    }
		                                }
		                            }
		                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, currentMethod.getFilePath(), s -> true, true);
		                            if (leftFilePath != null) {
		                                boolean found = false;
		                                for (UMLClass umlClass : umlModelPairPartial.getLeft().getClassList()) {
		                                    if (umlClass.getSourceFile().equals(leftFilePath)) {
		                                        for (UMLOperation operation : umlClass.getOperations()) {
		                                            if (operation.equals(rightMethod.getUmlOperation())) {
		                                                VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
		                                                UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(umlModelPairPartial.getLeft(), umlModelPairPartial.getRight(), operation, rightOperation);
		                                                UMLOperationBodyMapper bodyMapper = new UMLOperationBodyMapper(operation, (UMLOperation) rightOperation, lightweightClassDiff);
		                                                found = changeHistory.isMatched(bodyMapper, currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion);
		                                                if (found) {
		                                                    break;
		                                                }
		                                            }
		                                        }
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
		
		                                boolean found;
		                                UMLOperationBodyMapper bodyMapper = findBodyMapper(umlModelDiffPartial, rightMethod, currentVersion, parentVersion);
		                                found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, bodyMapper);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		                            }
		                        }
		                        {
		                            Set<String> fileNames = getRightSideFileNames(currentMethod, commitModel, umlModelDiffLocal);
		                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
		                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
		
		                            Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
		                            UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightMethodClassName);
		                            if (classDiff != null) {
		                                List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
		                                boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightAnnotation, classLevelRefactorings);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		
		                                found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightAnnotation, classLevelRefactorings);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		
		                                found = changeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, equalMethod, equalAnnotation, classDiff);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
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
		                                umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
		                                umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
		                                refactorings = umlModelDiffAll.getRefactorings();
		                            }
		
		                            boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightAnnotation, refactorings);
		                            if (found) {
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		
		                            found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightAnnotation, refactorings);
		                            if (found) {
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		
		
		                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodSourceFolder, rightMethodClassName);
		                            if (umlClassDiff != null) {
		                                found = changeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, equalMethod, equalAnnotation, umlClassDiff);
		
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		                            }
		
		                            if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
		                            }, currentVersion)) {
		                            	Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getOperation().get(), parentVersion);
		                                changeHistory.get().handleAdd(annotationBefore, rightAnnotation, "added with method");
		                                changeHistory.add(annotationBefore);
		                                changeHistory.get().connectRelatedNodes();
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		                        }
		                    }
                    	}
                    	else if (currentAnnotation.getOperation().get() instanceof UMLAttribute) {
                    		// container is an Attribute
                    		Attribute currentAttribute = Attribute.of((UMLAttribute) currentAnnotation.getOperation().get(), currentVersion);
		                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentAttribute.getFilePath()));
		                    Attribute rightAttribute = getAttribute(rightModel, currentVersion, currentAttribute::equalIdentifierIgnoringVersion);
		                    if (rightAttribute == null) {
		                        continue;
		                    }
		                    String rightAttributeClassName = rightAttribute.getUmlAttribute().getClassName();
		                    String rightAttributeSourceFolder = rightAttribute.getUmlAttribute().getLocationInfo().getSourceFolder();
		                    Annotation rightAnnotation = rightAttribute.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
		                    if (rightAnnotation == null) {
		                        continue;
		                    }
		                    Predicate<Attribute> equalAttribute = rightAttribute::equalIdentifierIgnoringVersion;
		                    Predicate<Annotation> equalAnnotation = rightAnnotation::equalIdentifierIgnoringVersion;
		                    historyReport.analysedCommitsPlusPlus();
		                    if ("0".equals(parentCommitId)) {
		                        Attribute leftAttribute = Attribute.of(rightAttribute.getUmlAttribute(), parentVersion);
		                        Annotation leftAnnotation = Annotation.of(rightAnnotation.getAnnotation(), leftAttribute);
		                        changeHistory.get().handleAdd(leftAnnotation, rightAnnotation, "Initial commit!");
		                        changeHistory.get().connectRelatedNodes();
		                        changeHistory.add(leftAnnotation);
		                        break;
		                    }
		                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentAttribute.getFilePath()));
		                    //NO CHANGE
		                    Attribute leftAttribute = getAttribute(leftModel, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
		                    if (leftAttribute != null) {
		                    	Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(leftAttribute.getUmlAttribute(), rightAttribute.getUmlAttribute());
								changeHistory.checkBodyOfMatched(currentVersion, parentVersion, equalAnnotation, pair);
		                        historyReport.step2PlusPlus();
		                        continue;
		                    }
		                    
		                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
		                    {
		                        //Local Refactoring
		                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
		                        boolean found = changeHistory.checkRefactoredAttribute(currentVersion, parentVersion, equalAttribute, rightAnnotation, refactorings);
		                        if (found) {
		                            historyReport.step4PlusPlus();
		                            break;
		                        }
		                        UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffLocal, rightAttributeSourceFolder, rightAttributeClassName);
		                        found = changeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, rightAttribute, equalAnnotation, umlClassDiff);
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
		                                if (ref.getIdenticalFilePaths().containsValue(currentAnnotation.getFilePath())) {
		                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
		                                        if (entry.getValue().equals(currentAnnotation.getFilePath())) {
		                                            leftFilePath = entry.getKey();
		                                            break;
		                                        }
		                                    }
		                                    if (leftFilePath != null) {
		                                        break;
		                                    }
		                                }
		                            }
		                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, currentAttribute.getFilePath(), s -> true, true);
		                            if (leftFilePath != null) {
		                                boolean found = false;
		                                for (UMLClass umlClass : umlModelPairPartial.getLeft().getClassList()) {
		                                    if (umlClass.getSourceFile().equals(leftFilePath)) {
		                                        for (UMLAttribute attribute : umlClass.getAttributes()) {
		                                            if (attribute.equals(rightAttribute.getUmlAttribute())) {
		                                                VariableDeclarationContainer rightField = rightAttribute.getUmlAttribute();
		                                                Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(attribute, rightField);
		                                                found = changeHistory.isMatched(pair, currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion);
		                                                if (found) {
		                                                    break;
		                                                }
		                                            }
		                                        }
		                                        for (UMLEnumConstant attribute : umlClass.getEnumConstants()) {
		                                            if (attribute.equals(rightAttribute.getUmlAttribute())) {
		                                                VariableDeclarationContainer rightField = rightAttribute.getUmlAttribute();
		                                                Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(attribute, rightField);
		                                                found = changeHistory.isMatched(pair, currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion);
		                                                if (found) {
		                                                    break;
		                                                }
		                                            }
		                                        }
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
		
		                                UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffPartial, rightAttributeSourceFolder, rightAttributeClassName);
		                                boolean found = changeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, rightAttribute, equalAnnotation, umlClassDiff);
				                        if (found) {
				                            historyReport.step5PlusPlus();
				                            break;
				                        }
		                            }
		                        }
		                        {
		                        	Set<String> fileNames = getRightSideFileNames(currentAttribute.getFilePath(), currentAttribute.getUmlAttribute().getClassName(), Collections.emptySet(), commitModel, umlModelDiffLocal);
		                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentAttribute.getFilePath(), fileNames::contains, false);
		                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
		
		                            Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
		                            UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightAttributeClassName);
		                            if (classDiff != null) {
		                                List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
		
		                                boolean found = changeHistory.checkRefactoredAttribute(currentVersion, parentVersion, equalAttribute, rightAnnotation, classLevelRefactorings);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		
		                                found = changeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, rightAttribute, equalAnnotation, classDiff);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		                            }
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
		                                else if (RefactoringType.MOVE_RENAME_ATTRIBUTE.equals(refactoring.getRefactoringType())) {
		                                	MoveAndRenameAttributeRefactoring moveAttributeRefactoring = (MoveAndRenameAttributeRefactoring) refactoring;
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
		
		                            boolean found = changeHistory.checkRefactoredAttribute(currentVersion, parentVersion, equalAttribute, rightAnnotation, refactorings);
		                            if (found) {
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		
		
		                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightAttributeSourceFolder, rightAttributeClassName);
		                            if (umlClassDiff != null) {
		                                found = changeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, rightAttribute, equalAnnotation, umlClassDiff);
		
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		                            }
		
		                            if (isAttributeAdded(umlModelDiffAll, rightAttribute.getUmlAttribute().getClassName(), rightAttribute::equalIdentifierIgnoringVersion, currentVersion)) {
		                            	Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getOperation().get(), parentVersion);
		                                changeHistory.get().handleAdd(annotationBefore, rightAnnotation, "added with attribute");
		                                changeHistory.add(annotationBefore);
		                                changeHistory.get().connectRelatedNodes();
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		                        }
		                    }
                    	}
	                }
                    else if (currentAnnotation.getClazz().isPresent()) {
                    	Class currentClass = Class.of(currentAnnotation.getClazz().get(), currentVersion);
                    	UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentClass.getFilePath()));
                    	Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
	                    if (rightClass == null) {
	                        continue;
	                    }
	                    String rightClassName = rightClass.getUmlClass().getName();
	                    String rightClassSourceFolder = rightClass.getUmlClass().getLocationInfo().getSourceFolder();
	                    Annotation rightAnnotation = rightClass.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
	                    if (rightAnnotation == null) {
	                        continue;
	                    }
	                    historyReport.analysedCommitsPlusPlus();
	                    if ("0".equals(parentCommitId)) {
	                        Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
	                        Annotation leftAnnotation = Annotation.of(rightAnnotation.getAnnotation(), leftClass);
	                        changeHistory.get().handleAdd(leftAnnotation, rightAnnotation, "Initial commit!");
	                        changeHistory.get().connectRelatedNodes();
	                        changeHistory.add(leftAnnotation);
	                        break;
	                    }
	                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentClass.getFilePath()));
	                    //NO CHANGE
	                    Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
	                    if (leftClass != null) {
	                        historyReport.step2PlusPlus();
	                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftClass.getUmlClass(), rightClass.getUmlClass());
	                        changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, lightweightClassDiff);
	                        continue;
	                    }
	                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
	                    {
	                        //Local Refactoring
	                    	UMLAbstractClassDiff classDiff = getUMLClassDiff(umlModelDiffLocal, rightClassSourceFolder, rightClassName);
	                        boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, classDiff);
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
	                                if (ref.getIdenticalFilePaths().containsValue(currentAnnotation.getFilePath())) {
	                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
	                                        if (entry.getValue().equals(currentAnnotation.getFilePath())) {
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
                                            found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, lightweightClassDiff);
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
	                                UMLAbstractClassDiff classDiff = getUMLClassDiff(umlModelDiffPartial, rightClassSourceFolder, rightClassName);
	    	                        boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, classDiff);
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
	                                boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, classDiff);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightClassSourceFolder, rightClassName);
	                            if (umlClassDiff != null) {
	                                boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, umlClassDiff);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            if (isClassAdded(umlModelDiffAll, rightClass.getUmlClass().getName())) {
	                            	Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getClazz().get(), parentVersion);
	                                changeHistory.get().handleAdd(annotationBefore, rightAnnotation, "added with class");
	                                changeHistory.add(annotationBefore);
	                                changeHistory.get().connectRelatedNodes();
	                                historyReport.step5PlusPlus();
	                                break;
	                            }
	                        }
	                    }
                    }
                }
            }
        }
        return null;
    }
}
