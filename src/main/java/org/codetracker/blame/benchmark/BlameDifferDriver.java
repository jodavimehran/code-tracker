package org.codetracker.blame.benchmark;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.util.GitServiceImpl;

import java.util.*;

import static org.codetracker.blame.util.Utils.getOwner;
import static org.codetracker.blame.util.Utils.getProject;

/* Created by pourya on 2024-07-14*/
public class BlameDifferDriver {
    private static final GitService gitService = new GitServiceImpl();
    private static final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";
    private static final EnumSet<BlamerFactory> blamerFactories =
            EnumSet.of(
                    BlamerFactory.CliGitBlameIgnoringWhiteSpace,
                    BlamerFactory.FileTrackerBlame
            );
    private static final String[][] dummies = {
            {"https://github.com/checkstyle/checkstyle/commit/119fd4fb33bef9f5c66fc950396669af842c21a3", "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java"},
            {"https://github.com/javaparser/javaparser/commit/97555053af3025556efe1a168fd7943dac28a2a6", "javaparser-core/src/main/java/com/github/javaparser/printer/lexicalpreservation/Difference.java"},
            {"https://github.com/javaparser/javaparser/commit/97555053af3025556efe1a168fd7943dac28a2a6", "javaparser-symbol-solver-core/src/main/java/com/github/javaparser/symbolsolver/javaparsermodel/contexts/MethodCallExprContext.java"},
            {"https://github.com/spring-projects/spring-framework/commit/b325c74216fd9564a36602158fa1269e2e832874", "spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodProcessor.java"},
            {"https://github.com/junit-team/junit5/commit/77cfe71e7f787c59626198e25350545f41e968bd", "junit-jupiter-engine/src/main/java/org/junit/jupiter/engine/descriptor/ClassTestDescriptor.java"},
            {"https://github.com/hibernate/hibernate-orm/commit/8bd79b29cfa7b2d539a746dc356d60b66e1e596b", "hibernate-core/src/main/java/org/hibernate/cfg/AnnotationBinder.java"},
            {"https://github.com/eclipse/jgit/commit/bd1a82502680b5de5bf86f6c4470185fd1602386", "org.eclipse.jgit/src/org/eclipse/jgit/internal/storage/pack/PackWriter.java"},
            {"https://github.com/JetBrains/intellij-community/commit/ecb1bb9d4d484ae63ee77f8ad45bdce154db9356", "java/compiler/impl/src/com/intellij/compiler/CompilerManagerImpl.java"},
            {"https://github.com/JetBrains/intellij-community/commit/ecb1bb9d4d484ae63ee77f8ad45bdce154db9356", "java/compiler/impl/src/com/intellij/compiler/actions/CompileDirtyAction.java"},
    };

    public static void main(String[] args) throws Exception {
        run(getDummyBlameCases());
    }


    private static Set<BlameCaseInfo> getDummyBlameCases() {
        Set<BlameCaseInfo> result = new LinkedHashSet<>();
        for (String[] dummy : dummies) {
            result.add(new BlameCaseInfo(dummy[0], dummy[1]));
        }
        return result;
    }

    static void run(Set<BlameCaseInfo> blameCases) throws Exception {
        StatsCollector statsCollector = new StatsCollector();
        for (BlameCaseInfo blameCase : blameCases) {
            System.out.println("Processing " + blameCase.url);
            process(blameCase.url, blameCase.filePath, statsCollector);
        }

    }
    public static void process(String url, String filePath, StatsCollector statsCollector) throws Exception {
        {
            String commitId = URLHelper.getCommit(url);
            String owner = getOwner(url);
            String project = getProject(url);
            String ownerSlashProject = owner + "/" + project;
            Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + ownerSlashProject, URLHelper.getRepo(url));
            BlameDiffer blameDiffer = new BlameDiffer(blamerFactories);
            Map<Integer, EnumMap<BlamerFactory, String>> result = blameDiffer.diff(repository, commitId, filePath);
            statsCollector.process(result, blameDiffer.getLegitSize(), blameDiffer.getCodeElementMap());
            new CsvWriter(owner, project, commitId, filePath, blameDiffer.getCodeElementMap()).writeToCSV(result);
            statsCollector.writeInfo();
        }
    }
}
