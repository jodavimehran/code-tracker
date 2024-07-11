package org.codetracker.blame.adaptor;

import org.codetracker.api.*;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Import;
import org.codetracker.element.Method;
import org.eclipse.jgit.lib.Repository;

/* Created by pourya on 2024-06-26*/
public class LineTrackerFromCodeTracker {
    public History.HistoryInfo<? extends CodeElement> blame(
            Repository repository,
            String filePath,
            String commitId,
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
                            .blameLineNumber(lineNumber)
                            .build();
                    blame = blockTracker.blame();
                    break;
                case "Comment":
                    Comment comment = (Comment) codeElement;
                    CommentTracker.Builder builder = CodeTracker
                            .commentTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .codeElementType(codeElement.getLocation().getCodeElementType())
                            .commentStartLineNumber(codeElement.getLocation().getStartLine())
                            .commentEndLineNumber(codeElement.getLocation().getEndLine());
                    if (comment.getOperation().isPresent()) {
                    	builder
                    	.methodName(comment.getOperation().get().getName())
                        .methodDeclarationLineNumber(
                                comment.getOperation().get().getLocationInfo().getStartLine()
                        );
                    }
                    else if (comment.getClazz().isPresent()) {
                    	builder
                    	.methodName(comment.getClazz().get().getName())
                        .methodDeclarationLineNumber(
                                comment.getClazz().get().getLocationInfo().getStartLine()
                        );
                    }
                    CommentTracker commentTracker = builder.build();
                    blame = commentTracker.blame();
                    break;
                case "Import":
                    Import imp = (Import) codeElement;
                    ImportTracker importTracker = CodeTracker
                            .importTracker()
                            .repository(repository)
                            .filePath(filePath)
                            .startCommitId(commitId)
                            .className(imp.getClazz().getName())
                            .classDeclarationLineNumber(
                                    imp.getClazz().getLocationInfo().getStartLine()
                            )
                            .codeElementType(codeElement.getLocation().getCodeElementType())
                            .importStartLineNumber(codeElement.getLocation().getStartLine())
                            .importEndLineNumber(codeElement.getLocation().getEndLine())
                            .build();
                    blame = importTracker.blame();
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


