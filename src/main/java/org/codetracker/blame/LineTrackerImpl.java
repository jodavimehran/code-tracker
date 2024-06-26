package org.codetracker.blame;

import org.codetracker.api.*;
import org.codetracker.element.Block;
import org.codetracker.element.Variable;
import org.eclipse.jgit.lib.Repository;

/* Created by pourya on 2024-06-26*/
public class LineTrackerImpl implements LineTracker {
    public History<? extends CodeElement> track(
            Repository repository,
            String filePath,
            String commitId,
            String name,
            Integer lineNumber,
            CodeElement codeElement
    ) {
        try {
            History<? extends CodeElement> history = null;
            switch (codeElement.getClass().getSimpleName()) {
                case "Method":
                    MethodTracker methodTracker = CodeTracker
                            .methodTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .methodName(name)
                            .methodDeclarationLineNumber(lineNumber)
                            .build();
                    history = methodTracker.track();
                    break;
                case "Variable":
                    Variable variable = (Variable) codeElement;
                    VariableTracker variableTracker = CodeTracker
                            .variableTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .methodName(variable.getOperation().getName())
                            .methodDeclarationLineNumber(
                                    variable.getOperation().getLocationInfo().getStartLine()
                            )
                            .variableName(name)
                            .variableDeclarationLineNumber(lineNumber)
                            .build();
                    history = variableTracker.track();
                    break;
                case "Attribute":
                    AttributeTracker attributeTracker = CodeTracker
                            .attributeTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .attributeName(name)
                            .attributeDeclarationLineNumber(lineNumber)
                            .build();
                    history = attributeTracker.track();
                    break;
                case "Block":
                    Block block = (Block) codeElement;
                    BlockTracker blockTracker = CodeTracker
                            .blockTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .methodName(block.getOperation().getName())
                            .methodDeclarationLineNumber(
                                    block.getOperation().getLocationInfo().getStartLine()
                            )
                            .codeElementType(codeElement.getLocation().getCodeElementType())
                            .blockStartLineNumber(codeElement.getLocation().getStartLine())
                            .blockEndLineNumber(codeElement.getLocation().getEndLine())
                            .build();
                    history = blockTracker.track();
                    break;
                default:
                    break;
            }
            return history;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


