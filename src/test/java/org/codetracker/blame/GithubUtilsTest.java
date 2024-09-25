package org.codetracker.blame;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.codetracker.blame.util.GithubUtils.areSameConsideringMerge;
import static org.junit.Assert.assertTrue;

/* Created by pourya on 2024-09-24*/
public class GithubUtilsTest {
    String repo = "javaparser";
    String project = "javaparser";
    @Test
    public void testMerge() {
        boolean result = areSameConsideringMerge(repo, project,
                "76f1b28ce",
                "860a4c0c0"
        );
        Assertions.assertTrue(result);
    }
    @Test
    public void testDifferentMerge() {
        boolean result = areSameConsideringMerge(repo, project,
                "76f1b28ce",
                "624b42041a451db6d9ab20b99e37b8af957b5ea0"
        );
        Assertions.assertFalse(result);
    }

    @Test
    public void testIrrelevantCommits() {
        boolean result = areSameConsideringMerge(repo, project,
                "76f1b28ce",
                "21f247faedfd151e4c2214ddca0b19c740f6255a"
        );
        Assertions.assertFalse(result);
    }

}
