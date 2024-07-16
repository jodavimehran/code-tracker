package org.codetracker;

import java.util.ArrayDeque;
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
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.change.Change;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.BodyChange;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
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
            Class startClass = getClass(umlModel, startVersion, changeHistory::isStartClass);
            if (startMethod == null && startClass == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
            }
            Comment startComment = null;
            if (startMethod != null)
            	startComment = startMethod.findComment(changeHistory::isStartComment);
            if (startClass != null)
            	startComment = startClass.findComment(changeHistory::isStartComment);
            if (startComment == null) {
                throw new CodeElementNotFoundException(filePath, changeHistory.getCommentType().name(), changeHistory.getCommentStartLineNumber());
            }
            changeHistory.get().addNode(startComment);

            ArrayDeque<Comment> comments = new ArrayDeque<>();
            comments.addFirst(startComment);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!comments.isEmpty()) {
            	History.HistoryInfo<Comment> blame = blameReturn();
            	if (blame != null) return blame;
                Comment currentComment = comments.poll();
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
	                    Method currentMethod = Method.of(currentComment.getOperation().get(), currentVersion);
	                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
	                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
	                    if (rightMethod == null) {
	                        continue;
	                    }
	                    String rightMethodClassName = rightMethod.getUmlOperation().getClassName();
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
	                        comments.add(leftComment);
	                        break;
	                    }
	                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethod.getFilePath()));
	                    //NO CHANGE
	                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
	                    if (leftMethod != null) {
	                    	if (leftMethod.getUmlOperation().getJavadoc() == null && rightMethod.getUmlOperation().getJavadoc() != null &&
	                    			rightComment.getComment().getLocationInfo().getCodeElementType().equals(CodeElementType.JAVADOC)) {
	                    		Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
                                changeHistory.get().handleAdd(commentBefore, rightComment, "new javadoc");
                                comments.add(commentBefore);
                                changeHistory.get().connectRelatedNodes();
                                break;
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
	                            if (containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
	                                bodyMapper = null;
	                            }
	                        }
	                        else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
	                            UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
	                            bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
	                            if (containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
	                                bodyMapper = null;
	                            }
	                        }
	                        if (changeHistory.checkBodyOfMatchedOperations(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper)) {
	                            historyReport.step3PlusPlus();
	                            break;
	                        }
	                    }
	                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
	                    {
	                        //Local Refactoring
	                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
	                        boolean found = changeHistory.checkForExtractionOrInline(comments, currentVersion, parentVersion, equalMethod, rightComment, refactorings);
	                        if (found) {
	                            historyReport.step4PlusPlus();
	                            break;
	                        }
	                        found = changeHistory.checkRefactoredMethod(comments, currentVersion, parentVersion, equalMethod, rightComment, refactorings);
	                        if (found) {
	                            historyReport.step4PlusPlus();
	                            break;
	                        }
	                        found = changeHistory.checkBodyOfMatchedOperations(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
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
	                                                found = changeHistory.isMatched(bodyMapper, comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion);
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
	                                found = changeHistory.checkBodyOfMatchedOperations(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper);
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
	                                boolean found = changeHistory.checkForExtractionOrInline(comments, currentVersion, parentVersion, equalMethod, rightComment, classLevelRefactorings);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	
	                                found = changeHistory.checkRefactoredMethod(comments, currentVersion, parentVersion, equalMethod, rightComment, classLevelRefactorings);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	
	                                found = changeHistory.checkClassDiffForCommentChange(comments, currentVersion, parentVersion, equalMethod, equalComment, classDiff);
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
	
	                            boolean found = changeHistory.checkForExtractionOrInline(comments, currentVersion, parentVersion, equalMethod, rightComment, refactorings);
	                            if (found) {
	                                historyReport.step5PlusPlus();
	                                break;
	                            }
	
	                            found = changeHistory.checkRefactoredMethod(comments, currentVersion, parentVersion, equalMethod, rightComment, refactorings);
	                            if (found) {
	                                historyReport.step5PlusPlus();
	                                break;
	                            }
	
	
	                            UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodClassName);
	                            if (umlClassDiff != null) {
	                                found = changeHistory.checkClassDiffForCommentChange(comments, currentVersion, parentVersion, equalMethod, equalComment, umlClassDiff);
	
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
	                            }, currentVersion)) {
	                                Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
	                                changeHistory.get().handleAdd(commentBefore, rightComment, "added with method");
	                                comments.add(commentBefore);
	                                changeHistory.get().connectRelatedNodes();
	                                historyReport.step5PlusPlus();
	                                break;
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
	                        comments.add(leftComment);
	                        break;
	                    }
	                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentClass.getFilePath()));
	                    //NO CHANGE
	                    Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
	                    if (leftClass != null) {
	                        historyReport.step2PlusPlus();
	                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftClass.getUmlClass(), rightClass.getUmlClass());
	                        changeHistory.checkBodyOfMatchedClasses(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, lightweightClassDiff);
	                        continue;
	                    }
	                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
	                    {
	                        //Local Refactoring
	                    	UMLClassBaseDiff classDiff = getUMLClassDiff(umlModelDiffLocal, rightClass.getUmlClass().getName());
	                        boolean found = changeHistory.checkBodyOfMatchedClasses(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
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
                                            found = changeHistory.checkBodyOfMatchedClasses(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, lightweightClassDiff);
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
	    	                        boolean found = changeHistory.checkBodyOfMatchedClasses(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
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
	                                boolean found = changeHistory.checkBodyOfMatchedClasses(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightClass.getUmlClass().getName());
	                            if (umlClassDiff != null) {
	                                boolean found = changeHistory.checkBodyOfMatchedClasses(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, umlClassDiff);
	                                if (found) {
	                                    historyReport.step5PlusPlus();
	                                    break;
	                                }
	                            }
	
	                            if (isClassAdded(umlModelDiffAll, rightClass.getUmlClass().getName())) {
	                                Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getClazz().get(), parentVersion);
	                                changeHistory.get().handleAdd(commentBefore, rightComment, "added with class");
	                                comments.add(commentBefore);
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

    private History.HistoryInfo<Comment> blameReturn() {
    	List<HistoryInfo<Comment>> history = HistoryImpl.processHistory(changeHistory.get().getCompleteGraph());
        Collections.reverse(history); 
		for (History.HistoryInfo<Comment> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof BodyChange || change instanceof Introduced) {
					return historyInfo;
				}
			}
		}
		return null;
    }
}
