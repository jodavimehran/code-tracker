package org.codetracker.blame;

import org.codetracker.blame.impl.CliGitBlame;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import static org.codetracker.blame.CodeTrackerBlameTest.assertEqualWithFile;
import static org.codetracker.blame.util.Utils.*;

/* Created by pourya on 2024-08-22*/
public class CliBlameTest {
    private static final GitService gitService = new GitServiceImpl();
    private final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";

    @Test
    public void testCliGitBlame() throws Exception {
        String url = "https://github.com/hibernate/hibernate-orm/commit/9e063ffa2";
        String path = "hibernate-core/src/main/java/org/hibernate/cfg/AnnotationBinder.java";
        String iw = getBlameOutput(url, path, new CliGitBlame(true), REPOS_PATH, gitService);
        String def = getBlameOutput(url, path, new CliGitBlame(false), REPOS_PATH, gitService);
        String expected_iw = System.getProperty("user.dir") + "/src/test/resources/blame/gitcli/9e063ffa2_cgit_iw.txt";
        String expected_def = System.getProperty("user.dir") + "/src/test/resources/blame/gitcli/9e063ffa2_cgit_def.txt";
        assertEqualWithFile(expected_iw,iw);
        assertEqualWithFile(expected_def,def);
    }
}
