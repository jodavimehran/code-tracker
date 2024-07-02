package org.codetracker.blame;

import org.codetracker.api.*;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.eclipse.jgit.lib.Repository;

/* Created by pourya on 2024-06-26*/
public class LineTrackerImpl implements LineTracker {
    public History.HistoryInfo<? extends CodeElement> blame(
            Repository repository,
            String filePath,
            String commitId,
            String name,
            Integer lineNumber,
            CodeElement codeElement
    ) {
        try {
            History.HistoryInfo<? extends CodeElement> blame = null;
            switch (codeElement.getClass().getSimpleName()) {
                case "Class" :
                    String className = ((Class) codeElement).getUmlClass().getNonQualifiedName();
                    ClassTracker classTracker = CodeTracker
                            .classTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .className(className)
                            .classDeclarationLineNumber(lineNumber)
                            .build();
                    blame = classTracker.blame();
                    break;
                case "Method":
                    String methodName = ((Method) codeElement).getUmlOperation().getName();
                    MethodTracker methodTracker = CodeTracker
                            .methodTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .methodName(methodName)
                            .methodDeclarationLineNumber(lineNumber)
                            .build();
                    blame = methodTracker.blame();
                    break;
                case "Attribute":
                    String attrName = ((Attribute) codeElement).getUmlAttribute().getName();
                    AttributeTracker attributeTracker = CodeTracker
                            .attributeTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .attributeName(attrName)
                            .attributeDeclarationLineNumber(lineNumber)
                            .build();
                    blame = attributeTracker.blame();
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
                    blame = blockTracker.blame();
                    break;
                default:
                    break;
            }
            return blame;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


