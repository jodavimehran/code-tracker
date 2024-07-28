package org.codetracker.api;

import org.codetracker.AnnotationTrackerImpl;
import org.codetracker.element.Annotation;
import org.eclipse.jgit.lib.Repository;

import gr.uom.java.xmi.LocationInfo.CodeElementType;

public interface AnnotationTracker extends CodeTracker {
    default History.HistoryInfo<Annotation> blame() throws Exception{
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
        private int annotationStartLineNumber;
        private int annotationEndLineNumber;

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

        public Builder annotationStartLineNumber(int annotationStartLineNumber) {
            this.annotationStartLineNumber = annotationStartLineNumber;
            return this;
        }

        public Builder annotationEndLineNumber(int annotationEndLineNumber) {
            this.annotationEndLineNumber = annotationEndLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public AnnotationTracker build() {
            checkInput();
            return new AnnotationTrackerImpl(repository, startCommitId, filePath, methodName, methodDeclarationLineNumber,
                    codeElementType, annotationStartLineNumber, annotationEndLineNumber);
        }
    }
}
