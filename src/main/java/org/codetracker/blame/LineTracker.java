package org.codetracker.blame;

import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.eclipse.jgit.lib.Repository;

/* Created by pourya on 2024-06-26*/
public interface LineTracker {
    History<? extends CodeElement> track(
            Repository repository,
            String filePath,
            String commitId,
            String name,
            Integer lineNumber,
            CodeElement codeElement);
}
