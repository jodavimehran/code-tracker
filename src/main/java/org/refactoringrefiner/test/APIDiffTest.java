package org.refactoringrefiner.test;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.RefDiffChangeDetector;
import org.refactoringrefiner.RefactoringMiner;

import java.io.File;

public class APIDiffTest {
    private final static String FOLDER_TO_CLONE = "H:\\Projects\\";
    private static final String[] commitIds = new String[]{
            "b1b49751d38af0bf2476aea1f4595283615ab7de",
            "dab682c2f0e2853858a6d24e1fe2c2088315a0cc",
            "de022d2434e58dd633fd50a7f9bb50565a8767b5",
            "6ee753af51a58c1c6ecc5e6d5946a32ee937eaa6",
            "a6ab6053e6b3d421d19764418cba3858c683e6e8",
            "1549ea4822139938296a58f59c38ae14f633c5aa",
            "68b49fd2843f23f55dc711a89213d59f2acf3a0a",
            "25621a3c3391ddf4bc0bb56535d23e73cd293657",
            "1b72b4905bf3520aa47697cc7d1bcdad8b7ad1e6",
            "c225479c7d3a7b82fec7d26131ac27496d336853",
            "f65b17cbc30795247fef3077cce16a8bb53e9ffc",
            "f020066f8bdfb378df36904af3df8b5bc48858fd",
            "5391df43637f6522979d46c5c4e35f772f08f4ac",
            "b6d9344d1c152f225a22ce9ce09341b3cc9488af",
            "0fc8b62a35beddc89cb3412388c27e88cd8cab4a",
            "8c3e4a50749044ae3177fc4c84db9c4fd93abca2",
            "da6a8d083cb82a94707146de559911578f39affe",
            "86826e1fa3fe3d793ee4723bb84ee0fc4ca38df1",
            "2880edd6554400955fb950bf8127311c436d2a7a",
            "aaf39002ac7fa00b95f4719beca058f6f7445574",
            "4299a4b408304cd0bcad6c25b4a322dbd94169a3",
            "fe6db3ab8a864e11eebfb0496f6a41852bdee019",
            "1a2c318e22a0b2b22ccc76019217c0892fe2d59b",
            "c0446a825514ea0279b8c22f633f2c4e3c73dc1f",
            "ab2f93f9bf61816d84154e636d32c81c05854e24",
            "702a1a957607881e72bb94b3104c2603ef10891f",
            "b9a1bec65f9eec7b96add7336dfcca4bf284e479",
            "b8ca6a585b824e91b3b8c72dd5cc53c0eb0ab0f1",
            "f85edb712767e01dafb8bf4a4a07d0d0ed3e9a38",
            "f1efb27670a93690577f1bae17fc9dcbd88a795d",
            "fe9057366308642868c25d427c3ee94411b37170",
            "f38d8abd42c3e824635e542e6031b3da1997c02e",
            "f1afaf83f39b83cc3bd07a2388448b262652a76b"
    };

    public static void main(String[] args) throws Exception {
//        String commitId = "4baf0a4de8d03022df48d696d210cc8b3117d38a";
//        String repositoryWebURL = "https://github.com/aws/aws-sdk-java.git";
        String repositoryWebURL = "https://github.com/checkstyle/checkstyle.git";
        String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
        String projectDirectory = FOLDER_TO_CLONE + repositoryName;

        GitService gitService = new GitServiceImpl();
        try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
            RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
            File repositoryFile = new File(projectDirectory + "/.git");
            RefDiffChangeDetector refDiffChangeDetector = new RefDiffChangeDetector(refactoringMiner, repositoryFile);
            for (String commitId : commitIds) {
                refDiffChangeDetector.detectAtCommit(commitId);
            }
        }
    }

//        [[1,2,2,1],[3,1,2],[1,3,2],[2,4],[3,1,2],[1,3,1,1]]

//        APIDiff diff = new APIDiff("checkstyle\\checkstyle", "https://github.com/checkstyle/checkstyle.git");
////        diff.setPath("H://Projects");
//        diff.setPath("H:\\Projects");
//
//        String[] commitIds = new String[]{
//                "b1b49751d38af0bf2476aea1f4595283615ab7de",
//                "dab682c2f0e2853858a6d24e1fe2c2088315a0cc",
//                "de022d2434e58dd633fd50a7f9bb50565a8767b5",
//                "6ee753af51a58c1c6ecc5e6d5946a32ee937eaa6",
//                "a6ab6053e6b3d421d19764418cba3858c683e6e8",
//                "1549ea4822139938296a58f59c38ae14f633c5aa",
//                "68b49fd2843f23f55dc711a89213d59f2acf3a0a",
//                "25621a3c3391ddf4bc0bb56535d23e73cd293657",
//                "1b72b4905bf3520aa47697cc7d1bcdad8b7ad1e6",
//                "c225479c7d3a7b82fec7d26131ac27496d336853",
//                "f65b17cbc30795247fef3077cce16a8bb53e9ffc",
//                "f020066f8bdfb378df36904af3df8b5bc48858fd",
//                "5391df43637f6522979d46c5c4e35f772f08f4ac",
//                "b6d9344d1c152f225a22ce9ce09341b3cc9488af",
//                "0fc8b62a35beddc89cb3412388c27e88cd8cab4a",
//                "8c3e4a50749044ae3177fc4c84db9c4fd93abca2",
//                "da6a8d083cb82a94707146de559911578f39affe",
//                "86826e1fa3fe3d793ee4723bb84ee0fc4ca38df1",
//                "2880edd6554400955fb950bf8127311c436d2a7a",
//                "aaf39002ac7fa00b95f4719beca058f6f7445574",
//                "4299a4b408304cd0bcad6c25b4a322dbd94169a3",
//                "fe6db3ab8a864e11eebfb0496f6a41852bdee019",
//                "1a2c318e22a0b2b22ccc76019217c0892fe2d59b",
//                "c0446a825514ea0279b8c22f633f2c4e3c73dc1f",
//                "ab2f93f9bf61816d84154e636d32c81c05854e24",
//                "702a1a957607881e72bb94b3104c2603ef10891f",
//                "b9a1bec65f9eec7b96add7336dfcca4bf284e479",
//                "b8ca6a585b824e91b3b8c72dd5cc53c0eb0ab0f1",
//                "f85edb712767e01dafb8bf4a4a07d0d0ed3e9a38",
//                "f1efb27670a93690577f1bae17fc9dcbd88a795d",
//                "fe9057366308642868c25d427c3ee94411b37170",
//                "f38d8abd42c3e824635e542e6031b3da1997c02e",
//                "f1afaf83f39b83cc3bd07a2388448b262652a76b"
//        };
//        for (String commit : commitIds) {
//            apidiff.Result result = diff.detectChangeAtCommit(commit, Classifier.API);
//            List<Change> changeMethod = result.getChangeMethod();
//        }


}


