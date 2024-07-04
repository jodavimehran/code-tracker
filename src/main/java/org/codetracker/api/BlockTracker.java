package org.codetracker.api;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import org.codetracker.element.Block;
import org.codetracker.BlockTrackerImpl;
import org.codetracker.BlockTrackerWithLocalFiles;
import org.eclipse.jgit.lib.Repository;

public interface BlockTracker extends CodeTracker {

    History<Block> track() throws Exception;
    default History.HistoryInfo<Block> blame() throws Exception{
        throw new UnsupportedOperationException();
    }

    class Builder {
        private Repository repository;
        private String gitURL;
        private String startCommitId;
        private String filePath;
        private String methodName;
        private int methodDeclarationLineNumber;
        private CodeElementType codeElementType;
        private int blockStartLineNumber;
        private int blockEndLineNumber;
        private int blameLineNumber;

        public BlockTracker.Builder repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Builder gitURL(String gitURL) {
            this.gitURL = gitURL;
            return this;
        }

        public BlockTracker.Builder startCommitId(String startCommitId) {
            this.startCommitId = startCommitId;
            return this;
        }

        public BlockTracker.Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public BlockTracker.Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public BlockTracker.Builder methodDeclarationLineNumber(int methodDeclarationLineNumber) {
            this.methodDeclarationLineNumber = methodDeclarationLineNumber;
            return this;
        }

        public Builder codeElementType(CodeElementType codeElementType) {
            this.codeElementType = codeElementType;
            return this;
        }

        public BlockTracker.Builder blockStartLineNumber(int blockStartLineNumber) {
            this.blockStartLineNumber = blockStartLineNumber;
            return this;
        }

        public BlockTracker.Builder blockEndLineNumber(int blockEndLineNumber) {
            this.blockEndLineNumber = blockEndLineNumber;
            return this;
        }

        public BlockTracker.Builder blameLineNumber(int blameLineNumber) {
            this.blameLineNumber = blameLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public BlockTracker build() {
            checkInput();
            return new BlockTrackerImpl(repository, startCommitId, filePath, methodName, methodDeclarationLineNumber,
                    codeElementType, blockStartLineNumber, blockEndLineNumber, blameLineNumber);
        }

        public BlockTracker buildWithLocalFiles() {
            checkInput();
            return new BlockTrackerWithLocalFiles(gitURL, startCommitId, filePath, methodName, methodDeclarationLineNumber,
                    codeElementType, blockStartLineNumber, blockEndLineNumber);
        }
    }
}
