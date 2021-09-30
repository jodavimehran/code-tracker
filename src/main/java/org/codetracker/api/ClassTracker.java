package org.codetracker.api;

import org.codetracker.ClassTrackerImpl;
import org.codetracker.element.Class;
import org.eclipse.jgit.lib.Repository;

public interface ClassTracker extends CodeTracker {

    History<Class> track() throws Exception;

    class Builder {
        private Repository repository;
        private String startCommitId;
        private String filePath;
        private String className;
        private int classDeclarationLineNumber;

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

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder classDeclarationLineNumber(int classDeclarationLineNumber) {
            this.classDeclarationLineNumber = classDeclarationLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public ClassTracker build() {
            checkInput();
            return new ClassTrackerImpl(repository, startCommitId, filePath, className, classDeclarationLineNumber);
        }

    }
}
