package org.refactoringrefiner;

import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;
import org.refactoringrefiner.api.RefactoringRefiner;

import java.util.HashSet;
import java.util.List;

public interface ChangeDetector {

    void detectAtCommit(String commitId);

    void addNode(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement);

    List<CodeElement> findMostLeftElement(RefactoringRefiner.CodeElementType codeElementType, String elementKey);

    Graph<CodeElement, Edge> findSubGraph(RefactoringRefiner.CodeElementType codeElementType, CodeElement start);

}
