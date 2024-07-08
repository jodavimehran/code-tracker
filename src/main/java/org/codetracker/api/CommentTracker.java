package org.codetracker.api;

import org.codetracker.CommentTrackerImpl;
import org.codetracker.element.Comment;
import org.eclipse.jgit.lib.Repository;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

public interface CommentTracker extends CodeTracker {
    default History.HistoryInfo<Comment> blame() throws Exception{
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
        private int commentStartLineNumber;
        private int commentEndLineNumber;

        public Builder repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Builder gitURL(String gitURL) {
            this.gitURL = gitURL;
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

        public Builder commentStartLineNumber(int commentStartLineNumber) {
            this.commentStartLineNumber = commentStartLineNumber;
            return this;
        }

        public Builder commentEndLineNumber(int commentEndLineNumber) {
            this.commentEndLineNumber = commentEndLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public CommentTracker build() {
            checkInput();
            return new CommentTrackerImpl(repository, startCommitId, filePath, methodName, methodDeclarationLineNumber,
                    codeElementType, commentStartLineNumber, commentEndLineNumber);
        }
    }
}
