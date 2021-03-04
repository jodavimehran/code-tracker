package org.refactoringrefiner.api;

public interface Change {

    Type getType();

    String toSummary();

    enum Type {
        NO_CHANGE("Not Changed"),
        ADDED("Added"),
        REMOVED("Removed"),
        REFACTORED("Refactored"),
        MODIFIED("Modified"),
        INLINED("Inlined"),
        EXTRACTED("Extracted"),
        CONTAINER_CHANGE("Changed in Container"),
        MULTI_CHANGE("Changed Multiple Times");

        private final String title;

        Type(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public boolean isNoChange() {
            return NO_CHANGE.equals(this);
        }

        public boolean isRefactoring() {
            return REFACTORED.equals(this);
        }
    }
}
