package org.codetracker.util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.decomposition.*;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.element.Block;

import java.util.*;

public class CommentTransitionAnalyzer {

    public static Set<Change.Type> detectCommentTransitionsInsideMatchedBlock(UMLOperationBodyMapper umlOperationBodyMapper, Block blockBefore, Block blockAfter, Version currentVersion, Version parentVersion) {
        Set<Change.Type> changeTypes = new HashSet<>();
        List<UMLComment> deletedComments = umlOperationBodyMapper.getCommentListDiff().getDeletedComments();
        List<UMLComment> addedComments = umlOperationBodyMapper.getCommentListDiff().getAddedComments();
        Map<String, UMLComment> deletedCommentsHash;
        Map<String, UMLComment> addedCommentsHash;

        if (!deletedComments.isEmpty()) {
            List<UMLComment> inScopeDeletedComments;
            inScopeDeletedComments = removeOutOfScopeComments(deletedComments, blockBefore);
            deletedCommentsHash = generateCommentTextHashMap(inScopeDeletedComments);
            if (isUncommentedCodeDetected(deletedCommentsHash, umlOperationBodyMapper, blockAfter)) {
                changeTypes.add(Change.Type.UNCOMMENTED_STATEMENT);
            }
        }
        if (!addedComments.isEmpty()) {
            List<UMLComment> inScopeAddedComments;
            inScopeAddedComments = removeOutOfScopeComments(addedComments, blockAfter);
            addedCommentsHash = generateCommentTextHashMap(inScopeAddedComments);
            if (isCommentedOutCodeDetected(addedCommentsHash, umlOperationBodyMapper, blockBefore)) {
                changeTypes.add(Change.Type.COMMENTED_OUT_STATEMENT);
            }
        }
        return changeTypes;
    }
    private static List<UMLComment> removeOutOfScopeComments(List<UMLComment> commentList, Block block){
        List<UMLComment> inScopeComments = new ArrayList<>();
        for (int i = 0; i < commentList.size(); i++) {
            if ((commentList.get(i).getLocationInfo().getStartLine() > block.getLocation().getStartLine() &&
                    commentList.get(i).getLocationInfo().getEndLine() < block.getLocation().getEndLine())) {
                inScopeComments.add(commentList.get(i));
            }
        }
        return inScopeComments;
    }
    private static boolean isUncommentedCodeDetected(Map<String, UMLComment> deletedCommentsHash, UMLOperationBodyMapper umlOperationBodyMapper, Block blockAfter) {
        if (deletedCommentsHash.isEmpty())
            return false;
        // find unmatched leaf statements afterBlock in deleted comments
        if (!umlOperationBodyMapper.getNonMappedLeavesT2().isEmpty()) {
            for (AbstractCodeFragment abstractCodeFragment : umlOperationBodyMapper.getNonMappedLeavesT2()) {
                if (blockAfter.getComposite().getLeaves().contains(abstractCodeFragment)) { //if nonmappedLeave actually belongs to the target Block
                    String codeFragmentHash = Util.getSHA512(abstractCodeFragment.getString().trim());
                    if (deletedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        //find unmatched composite statements afterBlock in deleted comments
        if (!umlOperationBodyMapper.getNonMappedInnerNodesT2().isEmpty()) {
            for (CompositeStatementObject compositeStatementObject : umlOperationBodyMapper.getNonMappedInnerNodesT2()) {
                if (((CompositeStatementObject) blockAfter.getComposite()).contains(compositeStatementObject)) { //if nonMappedInnerNode actually belongs to the current block
                    String codeFragmentHash = Util.getSHA512(compositeStatementObject.getActualSignature());
                    if (deletedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        // RefactoringMiner may create leaf mappings between semantically different statements when textual similarity is high enough,
        // even when one side is actually a commented/uncommented transition.
        if (!umlOperationBodyMapper.getMappings().isEmpty()) {
            for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
                if (!(mapping instanceof LeafMapping)) // does not happen in composite
                    continue;
                if(mapping.getReplacements().isEmpty()) //real mapping
                    continue;
                if (!isMappingBelongToTrackedBlock(mapping, blockAfter, true))
                    continue;
                String codeFragmentHashCurrent = Util.getSHA512(mapping.getFragment2().getString().trim());
                if (deletedCommentsHash.containsKey(codeFragmentHashCurrent)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCommentedOutCodeDetected(Map<String, UMLComment> addedCommentsHash, UMLOperationBodyMapper umlOperationBodyMapper, Block blockBefore) {
        if (addedCommentsHash.isEmpty())
            return false;
        if (!umlOperationBodyMapper.getNonMappedLeavesT1().isEmpty()) {
            //find unmatched leaf statements parent1 in added comments
            for (AbstractCodeFragment abstractCodeFragment : umlOperationBodyMapper.getNonMappedLeavesT1()) {
                if (blockBefore.getComposite().getLeaves().contains(abstractCodeFragment)) { // if nonMappedleaf actually belongs to parent block
                    String codeFragmentHash = Util.getSHA512(abstractCodeFragment.getString().trim());
                    if (addedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        if (!umlOperationBodyMapper.getNonMappedInnerNodesT1().isEmpty()) {
            //find unmatched composite statements parent in added comments
            for (CompositeStatementObject compositeStatementObject : umlOperationBodyMapper.getNonMappedInnerNodesT1()) {
                if (((CompositeStatementObject) blockBefore.getComposite()).contains(compositeStatementObject)) { // if nonMappedInnerNode actually belongs to parent block
                    if (!isBracketAndBelongsToStandAloneBlock(compositeStatementObject))
                        continue;
                    String codeFragmentHash = Util.getSHA512(compositeStatementObject.getActualSignature());
                    if (addedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        // RefactoringMiner may create leaf mappings between semantically different statements when textual similarity is high enough,
        // even when one side is actually a commented/uncommented transition.
        if (!umlOperationBodyMapper.getMappings().isEmpty()) {
            for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
                if (!(mapping instanceof LeafMapping)) // does not happen in composite
                    continue;
                if(mapping.getReplacements().isEmpty()) //real mapping
                    continue;
                if (!isMappingBelongToTrackedBlock(mapping, blockBefore, false))
                    continue;
                String codeFragmentHashParent = Util.getSHA512(mapping.getFragment1().getString().trim());
                if (!addedCommentsHash.isEmpty() && addedCommentsHash.containsKey(codeFragmentHashParent)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean isBlockBodyUnchangedDuringUncomment (UMLOperationBodyMapper umlOperationBodyMapper, CompositeStatementObject uncommentedBlock, Map<String, UMLComment> deletedComments, Block blockAfter) {
        Set<String> mappedFragments = new HashSet<>();
        for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
            if(!isMappingBelongToTrackedBlock(mapping, blockAfter, true))
                continue;
            if(mapping instanceof CompositeStatementObjectMapping) {
                CompositeStatementObject composite = (CompositeStatementObject) mapping.getFragment2();
                if (!isBracketAndBelongsToStandAloneBlock(composite))
                    continue;
                mappedFragments.add(Util.getSHA512(composite.getActualSignature()));
            }
            else if(mapping instanceof LeafMapping && mapping.getFragment2() instanceof StatementObject){ // mappings belong to the currentblock
                mappedFragments.add(Util.getSHA512(mapping.getFragment2().getString().trim()));
            }
        }
        for (CompositeStatementObject inner : uncommentedBlock.getInnerNodes()) {
            if(inner.getActualSignature().equals(uncommentedBlock.getActualSignature())){
                continue;
            }
            if (!isBracketAndBelongsToStandAloneBlock(inner)) {
                continue;
            }
            String hash = Util.getSHA512(inner.getActualSignature().replaceAll("\\s+", " ").trim());
            if(!existsAsMappedOrComment(hash, mappedFragments, deletedComments)){
                return false;
            }
        }
        for (AbstractCodeFragment leaf : uncommentedBlock.getLeaves()) {
            String hash = Util.getSHA512(((StatementObject) leaf).getActualSignature().replaceAll("\\s+", " ").trim());
            if(!existsAsMappedOrComment(hash, mappedFragments, deletedComments))
                return false;
        }
        return true;
    }

    private static Change.Type partialBlockUncommented(CompositeStatementObject compositeStatementObject, Block blockBefore, Block blockAfter) {
        //TODO: else/catch/finally uncommented support should be added
        if (blockBefore.getComposite().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT) &&
                blockAfter.getComposite().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
            CompositeStatementObject ifBefore = (CompositeStatementObject) blockBefore.getComposite();
            CompositeStatementObject ifAfter = (CompositeStatementObject) blockAfter.getComposite();
            if (ifBefore.getStatements().size() == 1 && ifAfter.getStatements().size() == 2) {
                return Change.Type.UNCOMMENTED_STATEMENT;
            }
        }
        return null;
    }

    public static boolean isBracketAndBelongsToStandAloneBlock(CompositeStatementObject compositeStatementObject){ //heuristic
        if ((compositeStatementObject.getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.BLOCK) && (!compositeStatementObject.getParent().getActualSignature().equals("{")))
            return false;
        return true;
    }
    private static boolean isMappingBelongToTrackedBlock(AbstractCodeMapping mapping, Block block, boolean isAfter){
        if(mapping instanceof CompositeStatementObjectMapping){
            CompositeStatementObject composite = (CompositeStatementObject) mapping.getFragment2();
            if(!((CompositeStatementObject) block.getComposite()).contains(composite))
                return false;
        }
        else if(mapping instanceof LeafMapping && (isAfter ? mapping.getFragment2() : mapping.getFragment1()) instanceof StatementObject){
            if(!block.getComposite().getLeaves().contains(isAfter ? mapping.getFragment2() : mapping.getFragment1()))
                return false;
        }
        return true;
    }
    private static boolean existsAsMappedOrComment(String hash, Set<String> mappedFragments, Map<String, UMLComment> commentsHash){
        return mappedFragments.contains(hash) || commentsHash.containsKey(hash);
    }
    public static String normalizeCommentText(String commentText) {
        commentText = commentText.trim();
        if (commentText.startsWith("//")) {
            commentText = commentText.substring(2);
        }
        if (commentText.startsWith("/*")) {
            commentText = commentText.substring(2);
        }
        if (commentText.endsWith("*/")) {
            commentText = commentText.substring(0, commentText.length() - 2);
        }
        commentText = commentText.trim();
        if (commentText.startsWith("*")) {
            commentText = commentText.substring(1);
        }
        return commentText.trim();
    }

    public static Map<String, UMLComment> generateCommentTextHashMap(List<UMLComment> commentList) {
        Map<String, UMLComment> commentTextHashMap = new HashMap<>();
        for (UMLComment comment : commentList) {
            String text = comment.getText();
            text = normalizeCommentText(text);
            if(comment.getFullText().startsWith("/*")){
                commentTextHashMap.put(Util.getSHA512(text.replaceAll("\\s+", " ").trim()), comment);
            }
            String[] lines = text.split("\\R");
            for (String line : lines) {
                line = normalizeCommentText(line);
                if (line.isBlank()) {
                    continue;
                }
                commentTextHashMap.put(Util.getSHA512(line), comment);
            }
        }
        return commentTextHashMap;
    }
    public static Map<String, String> generateUnCommentedBlockSignaturetHashMap(List<UMLComment> commentList) {
        Map<String, String> blockSignatureUnCommentedHashMap = new HashMap<>();
        List<String> blockSignatureUnCommented = extractBlockSignaturesFromComments(commentList);
        for(String signature : blockSignatureUnCommented){
            String normalized = signature.replaceAll("\\s+", " ").trim();
            blockSignatureUnCommentedHashMap.put(Util.getSHA512(normalized), normalized);
        }
        return blockSignatureUnCommentedHashMap;
    }
public static List<String> extractBlockSignaturesFromComments(List<UMLComment> comments) {
    List<String> signatures = new ArrayList<>();

    for (int i = 0; i < comments.size(); i++) {
        UMLComment comment = comments.get(i);
        String text = normalizeCommentText(comment.getText());
        if (text.isBlank())
            continue;
        if (isLeafStatement(text))
            continue;
        // Complete one-line signature
        if (isCompleteBlockSignature(text)) {
            signatures.add(text);
            continue;
        }
        if (comment.getFullText().startsWith("/*")) {
            signatures.addAll(extractFromBlockComment(text));
        }
        else if (comment.getFullText().startsWith("//")) {
            if (!hasBlockKeyword(text))
                continue;
            SingleLineExtractionResult singleLineExtractionResult = extractFromSingleLineComments(comments, i, text);
            signatures.addAll(singleLineExtractionResult.getSignatures());
            i = singleLineExtractionResult.getLastConsumedIndex();
        }
    }
    return signatures;
}
private static boolean isCompleteBlockSignature(String text) {
        return hasBlockKeyword(text) && text.trim().endsWith("{");
    }
    private static boolean signatureEnds(String text) {
        return text.trim().endsWith("{");
    }
    private static boolean hasBlockKeyword(String text) {
        String trimmed = text.trim();
        return trimmed.startsWith("if")
                || trimmed.startsWith("else")
                || trimmed.startsWith("for")
                || trimmed.startsWith("while")
                || trimmed.startsWith("do")
                || trimmed.startsWith("try")
                || trimmed.startsWith("catch")
                || trimmed.startsWith("finally")
                || trimmed.startsWith("switch")
                || trimmed.startsWith("synchronized")
                || trimmed.startsWith("static");
        //TODO:Add support for  standalone block {}
    }
    private static boolean areConsecutiveComments(
            UMLComment current,
            UMLComment next) {

        return next.getLocationInfo().getStartLine()
                == current.getLocationInfo().getEndLine() + 1;
    }
    private static boolean isLeafStatement(String text) {
        return (!hasBlockKeyword(text) && text.trim().endsWith(";"));
    }

    private static List<String> extractFromBlockComment(String commentText) {
        List<String> signatures = new ArrayList<>();
        String[] lines = commentText.split("\\R");
        for (int j = 0; j < lines.length; j++) {
            if (isLeafStatement(lines[j]))
                continue;
            if (!hasBlockKeyword(lines[j]))
                continue;
            StringBuilder signatureBuilder = new StringBuilder();
            signatureBuilder.append(lines[j]);
            int currentIndex = j + 1;
            while (!signatureEnds(signatureBuilder.toString())
                    && currentIndex < lines.length) {
                String nextLine = lines[currentIndex];
                if (isLeafStatement(nextLine)) {
                    if (signatureBuilder.toString().trim().endsWith(")")) {
                        signatures.add(signatureBuilder.toString());
                    }
                    break;
                }
                // another block signature starts
                if (hasBlockKeyword(nextLine))
                    break;
                signatureBuilder.append("\n").append(nextLine);
                currentIndex++;
            }
            if (signatureEnds(signatureBuilder.toString())) {
                signatures.add(signatureBuilder.toString());
            }
            j = currentIndex - 1;
        }
    return signatures;
    }

    private static SingleLineExtractionResult extractFromSingleLineComments(List<UMLComment> comments, int index, String commentText){
        int lastConsumedIndex = index;
        List<String> signatures = new ArrayList<>();
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append(commentText);
        int currentIndex = index;
        while (!signatureEnds(signatureBuilder.toString())
                && currentIndex + 1 < comments.size()) {
            UMLComment current = comments.get(currentIndex);
            UMLComment next = comments.get(currentIndex + 1);
            if (!areConsecutiveComments(current, next))
                break;
            String nextText = normalizeCommentText(next.getText());
            if (isLeafStatement(nextText)) {
                if (signatureBuilder.toString().trim().endsWith(")")) {
                    signatures.add(signatureBuilder.toString());
                }
                break;
            }
            // another block signature starts inside the same comment
            if (hasBlockKeyword(nextText))
                break;
            signatureBuilder.append("\n").append(nextText);
            currentIndex++;
        }
        if (signatureEnds(signatureBuilder.toString())) {
            signatures.add(signatureBuilder.toString());
            lastConsumedIndex = currentIndex;
        }
        return new SingleLineExtractionResult(signatures, lastConsumedIndex);
    }
    private static class SingleLineExtractionResult {
        private  List<String> signatures;
        private  int lastConsumedIndex;

        SingleLineExtractionResult(List<String> signatures,
                                   int lastConsumedIndex) {
            this.signatures = signatures;
            this.lastConsumedIndex = lastConsumedIndex;
        }
        public List<String> getSignatures() {
            return signatures;
        }

        public int getLastConsumedIndex() {
            return lastConsumedIndex;
        }
    }
}

