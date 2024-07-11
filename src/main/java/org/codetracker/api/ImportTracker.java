package org.codetracker.api;

import org.codetracker.ImportTrackerImpl;
import org.codetracker.element.Import;
import org.eclipse.jgit.lib.Repository;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

public interface ImportTracker {
	default History.HistoryInfo<Import> blame() throws Exception{
        throw new UnsupportedOperationException();
    }

    class Builder {
        private Repository repository;
        private String gitURL;
        private String startCommitId;
        private String filePath;
        private String className;
        private int classDeclarationLineNumber;
        private CodeElementType codeElementType;
        private int importStartLineNumber;
        private int importEndLineNumber;

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

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder classDeclarationLineNumber(int classDeclarationLineNumber) {
            this.classDeclarationLineNumber = classDeclarationLineNumber;
            return this;
        }

        public Builder codeElementType(CodeElementType codeElementType) {
            this.codeElementType = codeElementType;
            return this;
        }

        public Builder importStartLineNumber(int importStartLineNumber) {
            this.importStartLineNumber = importStartLineNumber;
            return this;
        }

        public Builder importEndLineNumber(int importEndLineNumber) {
            this.importEndLineNumber = importEndLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public ImportTracker build() {
            checkInput();
            return new ImportTrackerImpl(repository, startCommitId, filePath, className, classDeclarationLineNumber,
                    codeElementType, importStartLineNumber, importEndLineNumber);
        }
    }
}
