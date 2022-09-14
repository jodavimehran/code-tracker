package org.codetracker.api;

import org.eclipse.jgit.lib.Repository;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

import org.codetracker.BlockTrackerImpl;
import org.codetracker.element.Block;

public interface BlockTracker extends CodeTracker {

    History<Block> track() throws Exception;

    class Builder {        
        private Repository repository;
        private String startCommitId;
        private String filePath;
        private String methodName;
        private int methodDeclarationLineNumber;
        private CodeElementType codeElementType;
        private int blockDeclarationLineNumber;

        public Builder repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Builder startCommitId(String startCommitId) {
            this.startCommitId = startCommitId;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder methodDeclarationLineNumber(int methodDeclarationLineNumber) {
            this.methodDeclarationLineNumber = methodDeclarationLineNumber;
            return this;
        }


        public Builder codeElementType(CodeElementType codeElementType) {
            this.codeElementType = codeElementType;
            return this;
        }

        public Builder blockDeclarationLineNumber(int blockDeclarationLineNumber) {
            this.blockDeclarationLineNumber = blockDeclarationLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public BlockTracker build() {
            checkInput();
            return new BlockTrackerImpl(repository, startCommitId, filePath, methodName, methodDeclarationLineNumber, codeElementType, blockDeclarationLineNumber);
        }

    }
}
