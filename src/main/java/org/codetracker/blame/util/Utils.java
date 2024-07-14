package org.codetracker.blame.util;

import org.codetracker.blame.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.utils.URLHelper;

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
    public static String getBlameOutput(String url, String filePath, IBlame blamer, String reposPath, GitService gitService) throws Exception {
        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(reposPath + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        BlameFormatter formatter = new BlameFormatter(lines);
        List<LineBlameResult> blameResult = apply(commitId, filePath, blamer, repository);
        return TabularPrint.make(formatter.make(blameResult));
    }

    public static String getBlameOutput(String url, String filePath, IBlame blamer, String reposPath, GitService gitService, int fromLine, int toLine) throws Exception {
        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(reposPath + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        List<String> lineRange = lines.subList(fromLine-1, toLine);
        BlameFormatter formatter = new BlameFormatter(lineRange, fromLine);
        List<LineBlameResult> blameResult = apply(commitId, filePath, blamer, repository, fromLine, toLine);
        return TabularPrint.make(formatter.make(blameResult));
    }

    private static List<LineBlameResult> apply(String commitId, String filePath, IBlame blamer, Repository repository) throws Exception {
        return blamer.blameFile(repository, commitId, filePath);
    }
    private static List<LineBlameResult> apply(String commitId, String filePath, IBlame blamer, Repository repository, int fromLine, int toLine) throws Exception {
        return blamer.blameFile(repository, commitId, filePath, fromLine, toLine);
    }
    public static String getOwner(String gitURL){
        return gitURL.split("/")[3];
    }
    public static String getProject(String gitURL){
        return gitURL.split("/")[4];
    }
}
