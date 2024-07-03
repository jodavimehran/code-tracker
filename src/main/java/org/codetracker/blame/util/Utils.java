package org.codetracker.blame.util;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/* Created by pourya on 2024-06-26*/
public class Utils {
    public static List<String> getFileContentByCommit(Repository repository, String commitId, String filePath) throws Exception     {
        List<String> lines = new ArrayList<>();

        // Resolve the commit ID to a full ObjectId
        ObjectId commitObjectId = repository.resolve(commitId);

        // Get the commit object
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitObjectId);

            // Get the tree from the commit
            ObjectId treeId = commit.getTree().getId();

            // Set up the TreeWalk to access the file
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(treeId);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));

                if (!treeWalk.next()) {
                    throw new IllegalStateException("Did not find expected file: " + filePath);
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);

                // Read the file content
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                loader.copyTo(output);
                String content = output.toString();

                // Convert the content to a list of lines
                try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
            }
        }
        return lines;
    }
}
