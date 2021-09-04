package org.codetracker.api;

public class CodeElementNotFoundException extends Exception {
    private final String filePath;
    private final String name;
    private final int declarationLine;

    public CodeElementNotFoundException(String filePath, String name, int declarationLine) {
        super(String.format("Element %s is not found in file '%s' at line %d!", filePath, name, declarationLine));
        this.filePath = filePath;
        this.name = name;
        this.declarationLine = declarationLine;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return name;
    }

    public int getDeclarationLine() {
        return declarationLine;
    }
}
