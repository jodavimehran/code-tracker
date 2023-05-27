package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
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
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

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
                                                found = isMatched(bodyMapper, blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion);
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
                                found = checkBodyOfMatchedOperations(blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper);
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

                            boolean found = checkForExtractionOrInline(blocks, currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = isBlockRefactored(refactorings, blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = checkRefactoredMethod(blocks, currentVersion, parentVersion, equalMethod, rightBlock, refactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }


                            UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodClassName);
                            if (umlClassDiff != null) {
                                found = checkClassDiffForBlockChange(blocks, currentVersion, parentVersion, equalMethod, equalBlock, umlClassDiff);

                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }

                            if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
                            }, currentVersion)) {
                                Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                                blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
                                blocks.add(blockBefore);
                                blockChangeHistory.connectRelatedNodes();
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
            return new HistoryImpl<>(blockChangeHistory.findSubGraph(startBlock), historyReport);
        }
    }

    private boolean checkClassDiffForBlockChange(ArrayDeque<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Block> equalBlock, UMLClassBaseDiff umlClassDiff) throws RefactoringMinerTimedOutException {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                if (isBlockRefactored(operationBodyMapper.getRefactoringsAfterPostProcessing(), blocks, currentVersion, parentVersion, equalBlock))
                    return true;
                // check if it is in the matched
                if (isMatched(operationBodyMapper, blocks, currentVersion, parentVersion, equalBlock))
                    return true;
                //Check if is added
                if (isAdded(operationBodyMapper, blocks, currentVersion, parentVersion, equalBlock))
                    return true;
            }
        }
        return false;
    }

    private boolean checkForExtractionOrInline(ArrayDeque<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
        for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        CompositeStatementObject matchedBlockFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                            if (mapping instanceof CompositeStatementObjectMapping) {
                                Block matchedBlockInsideExtractedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    matchedBlockFromSourceMethod = (CompositeStatementObject) mapping.getFragment1();
                                    break;
                                }
                            }
                        }
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        if (matchedBlockFromSourceMethod == null) {
                            blockChangeHistory.handleAdd(blockBefore, rightBlock, extractOperationRefactoring.toString());
                        }
                        else {
                            VariableDeclarationContainer sourceOperation = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            blockChangeHistory.addChange(blockBefore, rightBlock, ChangeFactory.forBlock(Change.Type.INTRODUCED)
                                    .refactoring(extractOperationRefactoring).codeElement(rightBlock).hookedElement(Block.of(matchedBlockFromSourceMethod, sourceMethod)));
                            blockBefore.setAdded(true);
                        }
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
                case MERGE_OPERATION: {
                    MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                    Method methodAfter = Method.of(mergeOperationRefactoring.getNewMethodAfterMerge(), currentVersion);
                    if (equalMethod.test(methodAfter)) {
                        for (UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                            for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                                if (mapping instanceof CompositeStatementObjectMapping) {
                                    Block matchedBlockInsideMergedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                    if (matchedBlockInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                        // implementation for introduced
                                        /*
                                        Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                        blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                        blocks.add(blockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return true;
                                        */
                                        Set<Refactoring> mapperRefactorings = bodyMapper.getRefactoringsAfterPostProcessing();
                                        //Check if refactored
                                        if (isBlockRefactored(mapperRefactorings, blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        // check if it is in the matched
                                        if (isMatched(bodyMapper, blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case SPLIT_OPERATION: {
                    SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
                    for (VariableDeclarationContainer splitMethod : splitOperationRefactoring.getSplitMethods()) {
                        Method methodAfter = Method.of(splitMethod, currentVersion);
                        if (equalMethod.test(methodAfter)) {
                            for (UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
                                for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                                    if (mapping instanceof CompositeStatementObjectMapping) {
                                        Block matchedBlockInsideSplitMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                        if (matchedBlockInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                        // implementation for introduced
                                        /*
                                        Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                        blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                        blocks.add(blockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return true;
                                        */
                                        Set<Refactoring> mapperRefactorings = bodyMapper.getRefactoringsAfterPostProcessing();
                                        //Check if refactored
                                        if (isBlockRefactored(mapperRefactorings, blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        // check if it is in the matched
                                        if (isMatched(bodyMapper, blocks, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        }
                                    }
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

    private boolean checkBodyOfMatchedOperations(Queue<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper) throws RefactoringMinerTimedOutException {
        if (umlOperationBodyMapper == null)
            return false;
        Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactoringsAfterPostProcessing();
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
                    for (AbstractCodeFragment fragment : loopWithPipelineRefactoring.getCodeFragmentsAfter()) {
                        if (fragment instanceof StatementObject) {
                            StatementObject statement = (StatementObject) fragment;
                            Block addedBlockAfter = Block.of(statement, loopWithPipelineRefactoring.getOperationAfter(), currentVersion);
                            if (equalOperator.test(addedBlockAfter)) {
                                Set<AbstractCodeFragment> fragmentsBefore = loopWithPipelineRefactoring.getCodeFragmentsBefore();
                                for (AbstractCodeFragment fragmentBefore : fragmentsBefore) {
                                    if (fragmentBefore instanceof CompositeStatementObject) {
                                        if (fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
                                                fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
                                                fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
                                                fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
                                            blockBefore = Block.of((CompositeStatementObject) fragmentBefore, loopWithPipelineRefactoring.getOperationBefore(), parentVersion);
                                            blockAfter = addedBlockAfter;
                                            changeType = Change.Type.REPLACE_LOOP_WITH_PIPELINE;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case REPLACE_PIPELINE_WITH_LOOP: {
                    ReplacePipelineWithLoopRefactoring pipelineWithLoopRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
                    for (AbstractCodeFragment fragment : pipelineWithLoopRefactoring.getCodeFragmentsAfter()) {
                        if (fragment instanceof CompositeStatementObject) {
                            CompositeStatementObject composite = (CompositeStatementObject) fragment;
                            Block addedBlockAfter = Block.of(composite, pipelineWithLoopRefactoring.getOperationAfter(), currentVersion);
                            if (equalOperator.test(addedBlockAfter)) {
                                // implementation for introduced
                                /*
                                Block addedBlockBefore = Block.of(composite, pipelineWithLoopRefactoring.getOperationAfter(), parentVersion);
                                addedBlockBefore.setAdded(true);
                                ChangeFactory changeFactory = ChangeFactory.forBlock(Change.Type.INTRODUCED)
                                        .comment(pipelineWithLoopRefactoring.toString()).refactoring(pipelineWithLoopRefactoring).codeElement(addedBlockAfter);
                                blockChangeHistory.addChange(addedBlockBefore, addedBlockAfter, changeFactory);
                                leftBlockSet.add(addedBlockBefore);
                                blockChangeHistory.connectRelatedNodes();
                                return leftBlockSet;
                                 */
                                Set<AbstractCodeFragment> fragmentsBefore = pipelineWithLoopRefactoring.getCodeFragmentsBefore();
                                if (fragmentsBefore.size() == 1 && fragmentsBefore.iterator().next() instanceof StatementObject) {
                                    StatementObject streamStatement = (StatementObject) fragmentsBefore.iterator().next();
                                    blockBefore = Block.of(streamStatement, pipelineWithLoopRefactoring.getOperationBefore(), parentVersion);
                                    blockAfter = addedBlockAfter;
                                    changeType = Change.Type.REPLACE_PIPELINE_WITH_LOOP;
                                }
                            }
                            else {
                                //check if a nested composite statement matches
                                List<CompositeStatementObject> innerNodes = composite.getInnerNodes();
                                for (CompositeStatementObject innerNode : innerNodes) {
                                    addedBlockAfter = Block.of(innerNode, pipelineWithLoopRefactoring.getOperationAfter(), currentVersion);
                                    if (equalOperator.test(addedBlockAfter)) {
                                        Block addedBlockBefore = Block.of(innerNode, pipelineWithLoopRefactoring.getOperationAfter(), parentVersion);
                                        addedBlockBefore.setAdded(true);
                                        ChangeFactory changeFactory = ChangeFactory.forBlock(Change.Type.INTRODUCED)
                                                .comment(pipelineWithLoopRefactoring.toString()).refactoring(pipelineWithLoopRefactoring).codeElement(addedBlockAfter);
                                        blockChangeHistory.addChange(addedBlockBefore, addedBlockAfter, changeFactory);
                                        leftBlockSet.add(addedBlockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return leftBlockSet;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case SPLIT_CONDITIONAL: {
                    SplitConditionalRefactoring splitConditionalRefactoring = (SplitConditionalRefactoring) refactoring;
                    for (AbstractCodeFragment splitConditional : splitConditionalRefactoring.getSplitConditionals()) {
                        if (splitConditional instanceof CompositeStatementObject) {
                            Block addedBlockAfter = Block.of((CompositeStatementObject) splitConditional, splitConditionalRefactoring.getOperationAfter(), currentVersion);
                            if (equalOperator.test(addedBlockAfter)) {
                                // implementation with evolution hook
                                /*
                                Block addedBlockBefore = Block.of((CompositeStatementObject) splitConditional, splitConditionalRefactoring.getOperationAfter(), parentVersion);
                                addedBlockBefore.setAdded(true);
                                ChangeFactory changeFactory = ChangeFactory.forBlock(Change.Type.BLOCK_SPLIT)
                                        .comment(splitConditionalRefactoring.toString()).refactoring(splitConditionalRefactoring).codeElement(addedBlockAfter);
                                if (splitConditionalRefactoring.getOriginalConditional() instanceof CompositeStatementObject) {
                                    blockBefore = Block.of((CompositeStatementObject) splitConditionalRefactoring.getOriginalConditional(), splitConditionalRefactoring.getOperationBefore(), parentVersion);
                                    changeFactory.hookedElement(blockBefore);
                                }
                                blockChangeHistory.addChange(addedBlockBefore, addedBlockAfter, changeFactory);
                                leftBlockSet.add(addedBlockBefore);
                                blockChangeHistory.connectRelatedNodes();
                                return leftBlockSet;
                                 */
                                // implementation without evolution hook
                                if (splitConditionalRefactoring.getOriginalConditional() instanceof CompositeStatementObject) {
                                    blockBefore = Block.of((CompositeStatementObject) splitConditionalRefactoring.getOriginalConditional(), splitConditionalRefactoring.getOperationBefore(), parentVersion);
                                }
                                blockAfter = addedBlockAfter;
                                changeType = Change.Type.BLOCK_SPLIT;
                            }
                        }
                    }
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
                    boolean bodyChange = false;
                    boolean catchOrFinallyChange = false;
                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), umlOperationBodyMapper.getContainer1(), parentVersion);
                    List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                    List<String> stringRepresentationAfter = blockAfter.getComposite().stringRepresentation();
                    if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                        if (!stringRepresentationBefore.get(0).equals(stringRepresentationAfter.get(0))) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                        }
                        List<String> stringRepresentationBodyBefore = stringRepresentationBefore.subList(1, stringRepresentationBefore.size());
                        List<String> stringRepresentationBodyAfter = stringRepresentationAfter.subList(1, stringRepresentationAfter.size());
                        if (!stringRepresentationBodyBefore.equals(stringRepresentationBodyAfter)) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                        }
                        bodyChange = true;
                    }
                    if (blockBefore.getComposite() instanceof TryStatementObject && blockAfter.getComposite() instanceof TryStatementObject) {
                        TryStatementObject tryBefore = (TryStatementObject) blockBefore.getComposite();
                        TryStatementObject tryAfter = (TryStatementObject) blockAfter.getComposite();
                        List<CompositeStatementObject> catchBlocksBefore = new ArrayList<>(tryBefore.getCatchClauses());
                        List<CompositeStatementObject> catchBlocksAfter = new ArrayList<>(tryAfter.getCatchClauses());
                        for (AbstractCodeMapping m : umlOperationBodyMapper.getMappings()) {
                            if (m instanceof CompositeStatementObjectMapping) {
                                CompositeStatementObject fragment1 = (CompositeStatementObject) m.getFragment1();
                                CompositeStatementObject fragment2 = (CompositeStatementObject) m.getFragment2();
                                if (m.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                                        m.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                                        tryBefore.getCatchClauses().contains(fragment1) &&
                                        tryAfter.getCatchClauses().contains(fragment2)) {
                                    List<String> catchStringRepresentationBefore = fragment1.stringRepresentation();
                                    List<String> catchStringRepresentationAfter = fragment2.stringRepresentation();
                                    catchBlocksBefore.remove(fragment1);
                                    catchBlocksAfter.remove(fragment2);
                                    if (!catchStringRepresentationBefore.equals(catchStringRepresentationAfter)) {
                                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                                        catchOrFinallyChange = true;
                                    }
                                }
                            }
                        }
                        Set<CompositeStatementObject> catchBlocksBeforeToRemove = new LinkedHashSet<>();
                        Set<CompositeStatementObject> catchBlocksAfterToRemove = new LinkedHashSet<>();
                        for (int i=0; i<Math.min(catchBlocksBefore.size(), catchBlocksAfter.size()); i++) {
                            List<UMLType> typesBefore = new ArrayList<>();
                            for (VariableDeclaration variableDeclaration : catchBlocksBefore.get(i).getVariableDeclarations()) {
                                typesBefore.add(variableDeclaration.getType());
                            }
                            List<UMLType> typesAfter = new ArrayList<>();
                            for (VariableDeclaration variableDeclaration : catchBlocksAfter.get(i).getVariableDeclarations()) {
                                typesAfter.add(variableDeclaration.getType());
                            }
                            if (typesBefore.equals(typesAfter)) {
                                List<String> catchStringRepresentationBefore = catchBlocksBefore.get(i).stringRepresentation();
                                List<String> catchStringRepresentationAfter = catchBlocksAfter.get(i).stringRepresentation();
                                catchBlocksBeforeToRemove.add(catchBlocksBefore.get(i));
                                catchBlocksAfterToRemove.add(catchBlocksAfter.get(i));
                                if (!catchStringRepresentationBefore.equals(catchStringRepresentationAfter)) {
                                    blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                                    catchOrFinallyChange = true;
                                }
                            }
                        }
                        catchBlocksBefore.removeAll(catchBlocksBeforeToRemove);
                        catchBlocksAfter.removeAll(catchBlocksAfterToRemove);
                        for (CompositeStatementObject catchBlockBefore : catchBlocksBefore) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_REMOVED));
                            catchOrFinallyChange = true;
                        }
                        for (CompositeStatementObject catchBlockAfter : catchBlocksAfter) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_ADDED));
                            catchOrFinallyChange = true;
                        }
                        if (tryBefore.getFinallyClause() != null && tryAfter.getFinallyClause() != null) {
                            List<String> finallyStringRepresentationBefore = tryBefore.getFinallyClause().stringRepresentation();
                            List<String> finallyStringRepresentationAfter = tryAfter.getFinallyClause().stringRepresentation();
                            if (!finallyStringRepresentationBefore.equals(finallyStringRepresentationAfter)) {
                                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_CHANGE));
                                catchOrFinallyChange = true;
                            }
                        }
                        else if (tryBefore.getFinallyClause() == null && tryAfter.getFinallyClause() != null) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_ADDED));
                            catchOrFinallyChange = true;
                        }
                        else if (tryBefore.getFinallyClause() != null && tryAfter.getFinallyClause() == null) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_REMOVED));
                            catchOrFinallyChange = true;
                        }
                    }
                    if (!bodyChange && !catchOrFinallyChange) {
                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    }
                    blocks.add(blockBefore);
                    blockChangeHistory.connectRelatedNodes();
                    return true;
                }
            }
            else if (mapping instanceof LeafMapping && mapping.getFragment2() instanceof StatementObject) {
                Block blockAfter = Block.of((StatementObject) mapping.getFragment2(), umlOperationBodyMapper.getContainer2(), currentVersion);
                if (blockAfter != null && equalOperator.test(blockAfter)) {
                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), umlOperationBodyMapper.getContainer1(), parentVersion);
                    if (!blockBefore.getComposite().toString().equals(blockAfter.getComposite().toString())) {
                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                    }
                    else {
                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    }
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

    private boolean checkRefactoredMethod(ArrayDeque<Block> blocks, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
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
