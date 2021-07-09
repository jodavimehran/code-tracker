package org.refactoringrefiner.test;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.RefactoringMiner;
import org.refactoringrefiner.element.Method;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AddMethodSignatureToDataset {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final static String FOLDER_TO_CLONE = "H:\\Projects\\";

    public static void main(String[] args) throws Exception {
        Map<String, String> repo = new HashMap<>();
        repo.put("commons-io", "https://github.com/apache/commons-io.git");
        repo.put("elasticsearch","https://github.com/elastic/elasticsearch.git");
        repo.put("hadoop","https://github.com/apache/hadoop.git");
        repo.put("hibernate-search","https://github.com/hibernate/hibernate-search.git");
        repo.put("intellij-community","https://github.com/JetBrains/intellij-community.git");
        repo.put("jetty.project","https://github.com/eclipse/jetty.project.git");
        repo.put("lucene-solr","https://github.com/apache/lucene-solr.git");
        repo.put("mockito","https://github.com/mockito/mockito.git");
        repo.put("pmd","https://github.com/pmd/pmd.git");
        repo.put("spring-boot","https://github.com/spring-projects/spring-boot.git");


        File historyFolder = new File(AddMethodSignatureToDataset.class.getClassLoader().getResource("history/shovelTraining").getFile());
        File resultFolder = new File(AddMethodSignatureToDataset.class.getClassLoader().getResource("history/method/test").getFile());
        for (File file : historyFolder.listFiles()) {
            HistoryInfo historyInfo = mapper.readValue(file, HistoryInfo.class);

            GitService gitService = new GitServiceImpl();
            historyInfo.setRepositoryWebURL(repo.get(historyInfo.getRepositoryName()));
            String repositoryWebURL = historyInfo.getRepositoryWebURL();
            String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
            String projectDirectory = FOLDER_TO_CLONE + repositoryName;
            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                try (Git git = new Git(repository)) {
                    git.fetch().setRemote("origin").call();
                    RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);

                    UMLModel umlModel = refactoringMiner.getUMLModel(historyInfo.getStartCommitName(), Collections.singletonList(historyInfo.getFilePath()));
                    for (UMLClass umlClass : umlModel.getClassList()) {
                        for (UMLOperation umlOperation : umlClass.getOperations()) {
                            if (umlOperation.getName().equals(historyInfo.getFunctionName()) &&
                                    umlOperation.getLocationInfo().getStartLine() <= historyInfo.getFunctionStartLine() &&
                                    umlOperation.getLocationInfo().getEndLine() >= historyInfo.getFunctionStartLine()) {
                                historyInfo.setFunctionKey(Method.of(umlOperation, null).getName());
                                historyInfo.setBranchName(repository.getBranch());
                                ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
                                writer.writeValue(new File(resultFolder.getPath() + "\\" +file.getName().replace("Z_", "")), historyInfo);
                                break;
                            }
                        }
                    }
                }

            }
        }
    }
}
