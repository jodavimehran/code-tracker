package org.codetracker.blame;

import org.codetracker.blame.impl.CliGitBlame;
import org.codetracker.blame.impl.JGitBlame;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.codetracker.blame.util.Utils.*;

/* Created by pourya on 2024-08-22*/
public class JGitBlameVerificationTest {
    private static final GitService gitService = new GitServiceImpl();
    private final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";
    @Test
    public void testGitBlame() throws Exception {
        String url = "https://github.com/javaparser/javaparser/commit/97555053af3025556efe1a168fd7943dac28a2a6";
        String path = "javaparser-symbol-solver-core/src/main/java/com/github/javaparser/symbolsolver/javaparsermodel/contexts/MethodCallExprContext.java";
        String jgit = getBlameOutput(url, path, new JGitBlame(), REPOS_PATH, gitService);
        String cgit = getBlameOutput(url, path, new CliGitBlame(), REPOS_PATH, gitService);
        Files.write(Path.of("jgit.txt"), jgit.getBytes());
//        Files.write(Path.of("cgit.txt"), cgit.getBytes());

    }


}
