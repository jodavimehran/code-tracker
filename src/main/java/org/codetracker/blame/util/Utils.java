package org.codetracker.blame.util;

import org.codetracker.blame.model.IBlame;
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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/* Created by pourya on 2024-06-26*/
public class Utils {
    public static List<String> getFileContentByCommit(Repository repository, String commitId, String filePath) throws Exception     {
        return contentToLines(getContentAsString(repository, commitId, filePath));
    }

    public static List<String> contentToLines(String content) {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line;
        while (true) {
            try {
                line = reader.readLine();
                if ((line) == null) break;
            } catch (IOException e) {
//                throw new RuntimeException(e);
                e.printStackTrace();
                break;
            }
            lines.add(line);
        }
        return lines;
    }

    public static String getContentAsString(Repository repository, String commitId, String filePath) throws IOException, FileNotFoundInThePrevCommitException {
        ObjectId commitObjectId = repository.resolve(commitId);
        String content;
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
                    throw new FileNotFoundInThePrevCommitException("Did not find expected file: " + filePath);
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);

                // Read the file content
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                loader.copyTo(output);
                content = output.toString();
                // Convert the content to a list of lines
            }
        }
        return content;
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

    public static Repository getRepository(String url, GitService _gitService, String reposPath) throws Exception {
        String owner = getOwner(url);
        String project = getProject(url);
        String ownerSlashProject = owner + "/" + project;
        return _gitService.cloneIfNotExists(reposPath + "/" + ownerSlashProject, URLHelper.getRepo(url));
    }

    // Find the parent commit ID of the given commit
    public static String findParentCommitId(Repository repository, String commitId) {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
            if (commit.getParentCount() > 0) {
                return commit.getParent(0).getName();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // No parent found or error
    }

    // Find the commit date of the given commit
    public static long findCommitDate(Repository repository, String commitId) {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
            return commit.getCommitTime(); // TODO: Shall we convert to milliseconds or not? * 1000 ?
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1; // Error case
    }

    // Find the committer of the given commit
    public static String findCommitter(Repository repository, String commitId) {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
            return commit.getCommitterIdent().getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Error case
    }
    public static class FileNotFoundInThePrevCommitException extends Exception{
        public FileNotFoundInThePrevCommitException() {
        }

        public FileNotFoundInThePrevCommitException(String message) {
            super(message);
        }
    }
}
