package org.codetracker.api;

import org.codetracker.AttributeTrackerImpl;
import org.codetracker.AttributeTrackerWithLocalFiles;
import org.codetracker.element.Attribute;
import org.eclipse.jgit.lib.Repository;

public interface AttributeTracker extends CodeTracker {

    History<Attribute> track() throws Exception;

    class Builder {
        private Repository repository;
        private String gitURL;
        private String startCommitId;
        private String filePath;
        private String attributeName;
        private int attributeDeclarationLineNumber;

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

        public Builder attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        public Builder attributeDeclarationLineNumber(int attributeDeclarationLineNumber) {
            this.attributeDeclarationLineNumber = attributeDeclarationLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public AttributeTracker build() {
            checkInput();
            return new AttributeTrackerImpl(repository, startCommitId, filePath, attributeName, attributeDeclarationLineNumber);
        }

        public AttributeTracker buildWithLocalFiles() {
            checkInput();
            return new AttributeTrackerWithLocalFiles(gitURL, startCommitId, filePath, attributeName, attributeDeclarationLineNumber);
        }
    }
}
