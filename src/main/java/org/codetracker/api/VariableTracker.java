package org.codetracker.api;

import org.eclipse.jgit.lib.Repository;
import org.codetracker.VariableTrackerImpl;
import org.codetracker.element.Variable;

public interface VariableTracker extends CodeTracker {

    History<Variable> track() throws Exception;

    class Builder {
        private Repository repository;
        private String startCommitId;
        private String filePath;
        private String methodName;
        private int methodDeclarationLineNumber;
        private String variableName;
        private int variableDeclarationLineNumber;

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

        public Builder variableDeclarationLineNumber(int variableDeclarationLineNumber) {
            this.variableDeclarationLineNumber = variableDeclarationLineNumber;
            return this;
        }

        public Builder variableName(String variableName) {
            this.variableName = variableName;
            return this;
        }

        private void checkInput() {

        }

        public VariableTracker build() {
            checkInput();
            return new VariableTrackerImpl(repository, startCommitId, filePath, methodName, methodDeclarationLineNumber, variableName, variableDeclarationLineNumber);
        }

    }

}
