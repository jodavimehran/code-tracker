package org.codetracker;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;

import java.util.*;
import java.util.function.Predicate;

public class BlockTrackerImpl extends BaseTracker implements BlockTracker {
    private final ChangeHistory<Block> blockChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType blockType;
    private final int blockStartLineNumber;
    private final int blockEndLineNumber;

    public BlockTrackerImpl(Repository repository, String startCommitId, String filePath,
                            String methodName, int methodDeclarationLineNumber,
                            CodeElementType blockType, int blockStartLineNumber, int blockEndLineNumber) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
        this.blockType = blockType;
        this.blockStartLineNumber = blockStartLineNumber;
        this.blockEndLineNumber = blockEndLineNumber;
    }

    private boolean isStartBlock(Block block) {
        return block.getComposite().getLocationInfo().getCodeElementType().equals(blockType) &&
                block.getComposite().getLocationInfo().getStartLine() == blockStartLineNumber &&
                block.getComposite().getLocationInfo().getEndLine() == blockEndLineNumber;
    }

    private boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    @Override
    public History<Block> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method startMethod = getMethod(umlModel, startVersion, this::isStartMethod);
            if (startMethod == null) {
                throw new CodeElementNotFoundException(filePath, methodName, methodDeclarationLineNumber);
            }
            Block startBlock = startMethod.findBlock(this::isStartBlock);
            if (startBlock == null) {
                throw new CodeElementNotFoundException(filePath, blockType.getName(), blockStartLineNumber);
            }
            blockChangeHistory.addNode(startBlock);

            ArrayDeque<Block> blocks = new ArrayDeque<>();
            blocks.addFirst(startBlock);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!blocks.isEmpty()) {
                Block currentBlock = blocks.poll();
                if (currentBlock.isAdded()) {
                    commits = null;
                    continue;
                }
                if (commits == null || !currentBlock.getFilePath().equals(lastFileName)) {
                    lastFileName = currentBlock.getFilePath();
                    commits = getCommits(repository, currentBlock.getVersion().getId(), currentBlock.getFilePath(), git);
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
                    Method currentMethod = Method.of(currentBlock.getOperation(), currentVersion);
                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
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
                        blockChangeHistory.handleAdd(leftBlock, rightBlock, "Initial commit!");
                        blockChangeHistory.connectRelatedNodes();
                        blocks.add(leftBlock);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethod.getFilePath()));
                    //NO CHANGE
                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                    if (leftMethod != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }
                    //CHANGE BODY OR DOCUMENT
                    leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
                    if (leftMethod != null) {
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
                        if (checkBodyOfMatchedOperations(blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper)) {
                            historyReport.step3PlusPlus();
                            break;
                        }
                    }
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        //Local Refactoring
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        boolean found = checkForExtractionOrInline(blocks, currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                        found = checkRefactoredMethod(blocks, currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                        found = checkBodyOfMatchedOperations(blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }
                }
            }
            return new HistoryImpl<>(blockChangeHistory.findSubGraph(startBlock), historyReport);
        }
    }

    private boolean checkForExtractionOrInline(ArrayDeque<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) {
        for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, extractOperationRefactoring.toString());
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        return true;
                    }
                    break;
                }
                case MOVE_AND_INLINE_OPERATION:
                case INLINE_OPERATION: {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    Method targetOperationAfterInline = Method.of(inlineOperationRefactoring.getTargetOperationAfterInline(), currentVersion);
                    if (equalMethod.test(targetOperationAfterInline)) {
                        UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                            if (mapping instanceof CompositeStatementObjectMapping) {
                                Block matchedBlockInsideInlinedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideInlinedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideInlinedMethodBody, inlineOperationRefactoring.toString());
                                    blocks.add(blockBefore);
                                    blockChangeHistory.connectRelatedNodes();
                                    return true;
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    private boolean checkBodyOfMatchedOperations(Queue<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper) {
        if (umlOperationBodyMapper == null)
            return false;
        Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();
        //Check if refactored
        if (isBlockRefactored(refactorings, blocks, currentVersion, parentVersion, equalOperator))
            return true;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, blocks, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, blocks, currentVersion, parentVersion, equalOperator);
    }

    private boolean isBlockRefactored(Collection<Refactoring> refactorings, Queue<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        Set<Block> leftBlockSet = analyseBlockRefactorings(refactorings, currentVersion, parentVersion, equalOperator);
        for (Block leftBlock : leftBlockSet) {
            blocks.add(leftBlock);
            blockChangeHistory.connectRelatedNodes();
            return true;
        }
        return false;
    }

    private Set<Block> analyseBlockRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        Set<Block> leftBlockSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            Block blockBefore = null;
            Block blockAfter = null;
            Change.Type changeType = null;
            switch (refactoring.getRefactoringType()) {
                case REPLACE_LOOP_WITH_PIPELINE: {
                    ReplaceLoopWithPipelineRefactoring loopWithPipelineRefactoring = (ReplaceLoopWithPipelineRefactoring) refactoring;
                    break;
                }
                case REPLACE_PIPELINE_WITH_LOOP: {
                    ReplacePipelineWithLoopRefactoring pipelineWithLoopRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
                    break;
                }
            }
            if (changeType != null) {
                if (equalOperator.test(blockAfter)) {
                    blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(changeType).refactoring(refactoring));
                    leftBlockSet.add(blockBefore);
                }
            }
        }
        blockChangeHistory.connectRelatedNodes();
        return leftBlockSet;
    }

    private boolean isMatched(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
            if (mapping instanceof CompositeStatementObjectMapping) {
                Block blockAfter = Block.of((CompositeStatementObject) mapping.getFragment2(), umlOperationBodyMapper.getContainer2(), currentVersion);
                if (equalOperator.test(blockAfter)) {
                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), umlOperationBodyMapper.getContainer1(), parentVersion);
                    blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    blocks.add(blockBefore);
                    blockChangeHistory.connectRelatedNodes();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        for (CompositeStatementObject composite : umlOperationBodyMapper.getNonMappedInnerNodesT2()) {
            Block blockAfter = Block.of(composite, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(blockAfter)) {
                Block blockBefore = Block.of(composite, umlOperationBodyMapper.getContainer2(), parentVersion);
                blockChangeHistory.handleAdd(blockBefore, blockAfter, "new block");
                blocks.add(blockBefore);
                blockChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    private boolean checkRefactoredMethod(ArrayDeque<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) {
        for (Refactoring refactoring : refactorings) {
            UMLOperation operationBefore = null;
            UMLOperation operationAfter = null;
            UMLOperationBodyMapper umlOperationBodyMapper = null;
            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pullUpOperationRefactoring.getBodyMapper();
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pushDownOperationRefactoring.getBodyMapper();
                    break;
                }
                case MOVE_AND_RENAME_OPERATION:
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = moveOperationRefactoring.getBodyMapper();
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    umlOperationBodyMapper = renameOperationRefactoring.getBodyMapper();
                    break;
                }
            }
            if (operationAfter != null) {
                Method methodAfter = Method.of(operationAfter, currentVersion);
                if (equalMethod.test(methodAfter)) {
                    boolean found = checkBodyOfMatchedOperations(blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, umlOperationBodyMapper);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

}
