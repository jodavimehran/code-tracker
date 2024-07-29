package org.codetracker;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class BlockTrackerWithLocalFiles extends BaseTrackerWithLocalFiles implements BlockTracker {
	private final BlockTrackerChangeHistory changeHistory;

    public BlockTrackerWithLocalFiles(String cloneURL, String startCommitId, String filePath,
                            String methodName, int methodDeclarationLineNumber,
                            CodeElementType blockType, int blockStartLineNumber, int blockEndLineNumber) {
        super(cloneURL, startCommitId, filePath);
        this.changeHistory = new BlockTrackerChangeHistory(methodName, methodDeclarationLineNumber, blockType, blockStartLineNumber, blockEndLineNumber);
    }

    @Override
    public History<Block> track() throws Exception {
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
        Method startMethod = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
        String startFilePath = startMethod.getFilePath();
        if (startMethod == null) {
            throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
        }
        Block startBlock = startMethod.findBlock(changeHistory::isStartBlock);
        if (startBlock == null) {
            throw new CodeElementNotFoundException(filePath, changeHistory.getBlockType().getName(), changeHistory.getBlockStartLineNumber());
        }
        changeHistory.get().addNode(startBlock);

        changeHistory.addFirst(startBlock);
        HashSet<String> analysedCommits = new HashSet<>();
        List<String> commits = null;
        String lastFileName = null;
        while (!changeHistory.isEmpty()) {
            Block currentBlock = changeHistory.poll();
            if (currentBlock.isAdded()) {
                commits = null;
                continue;
            }
            final String currentMethodFilePath = currentBlock.getFilePath();
            if (commits == null || !currentBlock.getFilePath().equals(lastFileName)) {
                lastFileName = currentBlock.getFilePath();
                String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
        		String className = startFilePath.substring(startFilePath.lastIndexOf("/") + 1);
        		className = className.endsWith(".java") ? className.substring(0, className.length()-5) : className;
                String jsonPath = System.getProperty("user.dir") + "/src/test/resources/block/" + repoName + "-" + className + "-" + changeHistory.getMethodName() + ".json";
                File jsonFile = new File(jsonPath);
                commits = getCommits(currentBlock.getVersion().getId(), jsonFile);
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
                Method currentMethod = Method.of(currentBlock.getOperation(), currentVersion);
                Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                if (rightMethod == null) {
                    continue;
                }
                String rightMethodClassName = rightMethod.getUmlOperation().getClassName();
                Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
                if (rightBlock == null) {
                    continue;
                }
                Predicate<Method> equalMethod = rightMethod::equalIdentifierIgnoringVersion;
                Predicate<Block> equalBlock = rightBlock::equalIdentifierIgnoringVersion;
                historyReport.analysedCommitsPlusPlus();
                if ("0".equals(parentCommitId)) {
                    Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                    Block leftBlock = Block.of(rightBlock.getComposite(), leftMethod);
                    changeHistory.get().handleAdd(leftBlock, rightBlock, "Initial commit!");
                    changeHistory.get().connectRelatedNodes();
                    changeHistory.add(leftBlock);
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
                    if (changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper)) {
                        historyReport.step3PlusPlus();
                        break;
                    }
                }
                UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                {
                    //Local Refactoring
                    List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                    boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                    if (found) {
                        historyReport.step4PlusPlus();
                        break;
                    }
                    found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                    if (found) {
                        historyReport.step4PlusPlus();
                        break;
                    }
                    found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
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
                            if (ref.getIdenticalFilePaths().containsValue(currentBlock.getFilePath())) {
                                for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
                                    if (entry.getValue().equals(currentBlock.getFilePath())) {
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
                                            found = changeHistory.isMatched(bodyMapper, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion);
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
                            found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper);
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
                            boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightBlock, classLevelRefactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = changeHistory.isBlockRefactored(classLevelRefactorings, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightBlock, classLevelRefactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = changeHistory.checkClassDiffForBlockChange(currentVersion, parentVersion, equalMethod, equalBlock, classDiff);
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

                        boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                        if (found) {
                            historyReport.step5PlusPlus();
                            break;
                        }

                        found = changeHistory.isBlockRefactored(refactorings, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion);
                        if (found) {
                            historyReport.step5PlusPlus();
                            break;
                        }

                        found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                        if (found) {
                            historyReport.step5PlusPlus();
                            break;
                        }


                        UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodClassName);
                        if (umlClassDiff != null) {
                            found = changeHistory.checkClassDiffForBlockChange(currentVersion, parentVersion, equalMethod, equalBlock, umlClassDiff);

                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }

                        if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
                        }, currentVersion)) {
                            Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                            changeHistory.get().handleAdd(blockBefore, rightBlock, "added with method");
                            changeHistory.add(blockBefore);
                            changeHistory.get().connectRelatedNodes();
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
