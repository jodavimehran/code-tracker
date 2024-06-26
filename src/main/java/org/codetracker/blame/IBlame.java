package org.codetracker.blame;

import org.eclipse.jgit.lib.Repository;

import java.util.ArrayList;
import java.util.List;

/* Created by pourya on 2024-06-26*/
public interface IBlame {
    List<String[]> blameFile(Repository repository, String commitId, String filePath) throws Exception;

}
