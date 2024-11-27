package org.codetracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.CommentTracker;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Attribute;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.MoveAndRenameAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class CommentTrackerImpl extends BaseTracker implements CommentTracker {
	private final CommentTrackerChangeHistory changeHistory;
	
	public CommentTrackerImpl(Repository repository, String startCommitId, String filePath,
            String methodName, int methodDeclarationLineNumber,
            CodeElementType commentType, int commentStartLineNumber, int commentEndLineNumber) {
		super(repository, startCommitId, filePath);
		this.changeHistory = new CommentTrackerChangeHistory(methodName, methodDeclarationLineNumber, commentType, commentStartLineNumber, commentEndLineNumber);
	}

    public History.HistoryInfo<Comment> blame() throws Exception {
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
            Comment startComment = null;
            if (startMethod != null)
            	startComment = startMethod.findComment(changeHistory::isStartComment);
            if (startClass != null)
            	startComment = startClass.findComment(changeHistory::isStartComment);
            if (startAttribute != null)
            	startComment = startAttribute.findComment(changeHistory::isStartComment);
            if (startComment == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getCommentType().name(), changeHistory.getCommentStartLineNumber());
            }
            changeHistory.get().addNode(startComment);

            changeHistory.addFirst(startComment);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!changeHistory.isEmpty()) {
            	History.HistoryInfo<Comment> blame = changeHistory.blameReturn();
            	if (blame != null) return blame;
                Comment currentComment = changeHistory.poll();
                if (currentComment.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentComment.getFilePath().equals(lastFileName)) {
                    lastFileName = currentComment.getFilePath();
                    commits = getCommits(repository, currentComment.getVersion().getId(), currentComment.getFilePath(), git);
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
                    if (currentComment.getOperation().isPresent()) {
                    	if (currentComment.getOperation().get() instanceof UMLOperation || currentComment.getOperation().get() instanceof UMLInitializer) {
		                    Method currentMethod = Method.of(currentComment.getOperation().get(), currentVersion);
		                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
		                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
		                    if (rightMethod == null) {
		                        continue;
		                    }
		                    String rightMethodClassName = rightMethod.getUmlOperation().getClassName();
		                    String rightMethodSourceFolder = rightMethod.getUmlOperation().getLocationInfo().getSourceFolder();
		                    Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
		                    if (rightComment == null) {
		                        continue;
		                    }
		                    Predicate<Method> equalMethod = rightMethod::equalIdentifierIgnoringVersion;
		                    Predicate<Comment> equalComment = rightComment::equalIdentifierIgnoringVersion;
		                    historyReport.analysedCommitsPlusPlus();
		                    if ("0".equals(parentCommitId)) {
		                        Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
		                        Comment leftComment = Comment.of(rightComment.getComment(), leftMethod);
		                        changeHistory.get().handleAdd(leftComment, rightComment, "Initial commit!");
		                        changeHistory.get().connectRelatedNodes();
		                        changeHistory.add(leftComment);
		                        break;
		                    }
		                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethod.getFilePath()));
		                    //NO CHANGE
		                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
		                    if (leftMethod != null) {
		                    	UMLJavadoc leftJavadoc = leftMethod.getUmlOperation().getJavadoc();
								UMLJavadoc rightJavadoc = rightMethod.getUmlOperation().getJavadoc();
								if (leftJavadoc == null && rightJavadoc != null &&
		                    			rightComment.getComment().getLocationInfo().getCodeElementType().equals(CodeElementType.JAVADOC)) {
		                    		Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
	                                changeHistory.get().handleAdd(commentBefore, rightComment, "new javadoc");
	                                changeHistory.add(commentBefore);
	                                changeHistory.get().connectRelatedNodes();
	                                break;
		                    	}
								else if (leftJavadoc != null && rightJavadoc != null && !leftJavadoc.getFullText().equals(rightJavadoc.getFullText()) &&
		                    			rightComment.getComment().getLocationInfo().getCodeElementType().equals(CodeElementType.JAVADOC)) {
									Comment commentBefore = Comment.of(leftJavadoc, leftMethod.getUmlOperation(), parentVersion);
									Comment commentAfter = Comment.of(rightJavadoc, rightMethod.getUmlOperation(), currentVersion);
									changeHistory.processChange(commentBefore, commentAfter);
									changeHistory.addFirst(commentBefore);
									changeHistory.get().connectRelatedNodes();
								}
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
		                        if (changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper)) {
		                            historyReport.step3PlusPlus();
		                            break;
		                        }
		                    }
		                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
		                    {
		                        //Local Refactoring
		                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
		                        boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightComment, refactorings);
		                        if (found) {
		                            historyReport.step4PlusPlus();
		                            break;
		                        }
		                        found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightComment, refactorings);
		                        if (found) {
		                            historyReport.step4PlusPlus();
		                            break;
		                        }
		                        found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
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
		                                if (ref.getIdenticalFilePaths().containsValue(currentComment.getFilePath())) {
		                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
		                                        if (entry.getValue().equals(currentComment.getFilePath())) {
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
		                                                found = changeHistory.isMatched(bodyMapper, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion);
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
		                                found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper);
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
		                                boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightComment, classLevelRefactorings);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		
		                                found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightComment, classLevelRefactorings);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		
		                                found = changeHistory.checkClassDiffForCommentChange(currentVersion, parentVersion, equalMethod, equalComment, classDiff);
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
		
		                            boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightComment, refactorings);
		                            if (found) {
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		
		                            found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightComment, refactorings);
		                            if (found) {
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		
		
		                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodSourceFolder, rightMethodClassName);
		                            if (umlClassDiff != null) {
		                                found = changeHistory.checkClassDiffForCommentChange(currentVersion, parentVersion, equalMethod, equalComment, umlClassDiff);
		
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		                            }
		
		                            if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
		                            }, currentVersion)) {
		                                Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
		                                changeHistory.get().handleAdd(commentBefore, rightComment, "added with method");
		                                changeHistory.add(commentBefore);
		                                changeHistory.get().connectRelatedNodes();
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		                        }
		                    }
                    	}
                    	else if (currentComment.getOperation().get() instanceof UMLAttribute) {
                    		// container is an Attribute
                    		Attribute currentAttribute = Attribute.of((UMLAttribute) currentComment.getOperation().get(), currentVersion);
		                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentAttribute.getFilePath()));
		                    Attribute rightAttribute = getAttribute(rightModel, currentVersion, currentAttribute::equalIdentifierIgnoringVersion);
		                    if (rightAttribute == null) {
		                        continue;
		                    }
		                    String rightAttributeClassName = rightAttribute.getUmlAttribute().getClassName();
		                    String rightAttributeSourceFolder = rightAttribute.getUmlAttribute().getLocationInfo().getSourceFolder();
		                    Comment rightComment = rightAttribute.findComment(currentComment::equalIdentifierIgnoringVersion);
		                    if (rightComment == null) {
		                        continue;
		                    }
		                    Predicate<Attribute> equalAttribute = rightAttribute::equalIdentifierIgnoringVersion;
		                    Predicate<Comment> equalComment = rightComment::equalIdentifierIgnoringVersion;
		                    historyReport.analysedCommitsPlusPlus();
		                    if ("0".equals(parentCommitId)) {
		                        Attribute leftAttribute = Attribute.of(rightAttribute.getUmlAttribute(), parentVersion);
		                        Comment leftComment = Comment.of(rightComment.getComment(), leftAttribute);
		                        changeHistory.get().handleAdd(leftComment, rightComment, "Initial commit!");
		                        changeHistory.get().connectRelatedNodes();
		                        changeHistory.add(leftComment);
		                        break;
		                    }
		                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentAttribute.getFilePath()));
		                    //NO CHANGE
		                    Attribute leftAttribute = getAttribute(leftModel, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
		                    if (leftAttribute != null) {
		                    	Pair<UMLAttribute, UMLAttribute> pair = Pair.of(leftAttribute.getUmlAttribute(), rightAttribute.getUmlAttribute());
								changeHistory.checkBodyOfMatchedAttributes(currentVersion, parentVersion, equalComment, pair);
		                        historyReport.step2PlusPlus();
		                        continue;
		                    }
		                    
		                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
		                    {
		                        //Local Refactoring
		                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
		                        boolean found = changeHistory.checkRefactoredAttribute(currentVersion, parentVersion, equalAttribute, rightComment, refactorings);
		                        if (found) {
		                            historyReport.step4PlusPlus();
		                            break;
		                        }
		                        UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffLocal, rightAttributeSourceFolder, rightAttributeClassName);
		                        found = changeHistory.checkClassDiffForCommentChange(currentVersion, parentVersion, rightAttribute, equalComment, umlClassDiff);
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
		                                if (ref.getIdenticalFilePaths().containsValue(currentComment.getFilePath())) {
		                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
		                                        if (entry.getValue().equals(currentComment.getFilePath())) {
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
		                                                UMLAttribute rightField = rightAttribute.getUmlAttribute();
		                                                Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, rightField);
		                                                found = changeHistory.isMatched(pair, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion);
		                                                if (found) {
		                                                    break;
		                                                }
		                                            }
		                                        }
		                                        for (UMLEnumConstant attribute : umlClass.getEnumConstants()) {
		                                            if (attribute.equals(rightAttribute.getUmlAttribute())) {
		                                            	UMLAttribute rightField = rightAttribute.getUmlAttribute();
		                                                Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, rightField);
		                                                found = changeHistory.isMatched(pair, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion);
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
		                                boolean found = changeHistory.checkClassDiffForCommentChange(currentVersion, parentVersion, rightAttribute, equalComment, umlClassDiff);
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
		
		                                boolean found = changeHistory.checkRefactoredAttribute(currentVersion, parentVersion, equalAttribute, rightComment, classLevelRefactorings);
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		
		                                found = changeHistory.checkClassDiffForCommentChange(currentVersion, parentVersion, rightAttribute, equalComment, classDiff);
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
		
		                            boolean found = changeHistory.checkRefactoredAttribute(currentVersion, parentVersion, equalAttribute, rightComment, refactorings);
		                            if (found) {
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		
		
		                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightAttributeSourceFolder, rightAttributeClassName);
		                            if (umlClassDiff != null) {
		                                found = changeHistory.checkClassDiffForCommentChange(currentVersion, parentVersion, rightAttribute, equalComment, umlClassDiff);
		
		                                if (found) {
		                                    historyReport.step5PlusPlus();
		                                    break;
		                                }
		                            }
		
		                            if (isAttributeAdded(umlModelDiffAll, rightAttribute.getUmlAttribute().getClassName(), rightAttribute::equalIdentifierIgnoringVersion, currentVersion)) {
		                            	Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
		                                changeHistory.get().handleAdd(commentBefore, rightComment, "added with attribute");
		                                changeHistory.add(commentBefore);
		                                changeHistory.get().connectRelatedNodes();
		                                historyReport.step5PlusPlus();
		                                break;
		                            }
		                        }
		                    }
                    	}
	                }
                    else if (currentComment.getClazz().isPresent()) {
                    	Class currentClass = Class.of(currentComment.getClazz().get(), currentVersion);
                    	UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentClass.getFilePath()));
                    	Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
	                    if (rightClass == null) {
	                        continue;
	                    }
	                    String rightClassName = rightClass.getUmlClass().getName();
	                    String rightClassSourceFolder = rightClass.getUmlClass().getLocationInfo().getSourceFolder();
	                    Comment rightComment = rightClass.findComment(currentComment::equalIdentifierIgnoringVersion);
	                    if (rightComment == null) {
	                        continue;
	                    }
	                    historyReport.analysedCommitsPlusPlus();
	                    if ("0".equals(parentCommitId)) {
	                        Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
	                        Comment leftComment = Comment.of(rightComment.getComment(), leftClass);
	                        changeHistory.get().handleAdd(leftComment, rightComment, "Initial commit!");
	                        changeHistory.get().connectRelatedNodes();
	                        changeHistory.add(leftComment);
	                        break;
	                    }
	                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentClass.getFilePath()));
	                    //NO CHANGE
	                    Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
	                    if (leftClass != null) {
	                        historyReport.step2PlusPlus();
	                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftClass.getUmlClass(), rightClass.getUmlClass());
	                        changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, lightweightClassDiff);
	                        continue;
	                    }
	                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
	                    {
	                        //Local Refactoring
	                    	UMLAbstractClassDiff classDiff = getUMLClassDiff(umlModelDiffLocal, rightClassSourceFolder, rightClassName);
	                        boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
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
	                                if (ref.getIdenticalFilePaths().containsValue(currentComment.getFilePath())) {
	                                    for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
	                                        if (entry.getValue().equals(currentComment.getFilePath())) {
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
                                            found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, lightweightClassDiff);
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
	    	                        boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
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
	                                boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightClassSourceFolder, rightClassName);
	                            if (umlClassDiff != null) {
	                                boolean found = changeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, umlClassDiff);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            if (isClassAdded(umlModelDiffAll, rightClass.getUmlClass().getName())) {
	                                Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getClazz().get(), parentVersion);
	                                changeHistory.get().handleAdd(commentBefore, rightComment, "added with class");
	                                changeHistory.add(commentBefore);
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
