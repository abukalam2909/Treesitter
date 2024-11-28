package ca.dal.treefactor.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GitHistoryTreefactorImplTest {

    private static Repository repository;
    private static GitHistoryTreefactorImpl gitHistoryTreefactor;
    private static String repoDirectory = "TreefactorTest-Py"; // Directory where the repo will be cloned

    // Before all tests, clone the repository and initialize objects
    @BeforeAll
    static void setup() throws Exception {
        String repoUrl = "https://github.com/hxu47/TreefactorTest-Py"; // Repository URL

        // Check if the repo exists, otherwise clone it
        if (!Files.exists(Paths.get(repoDirectory))) {
            cloneRepository(repoUrl, repoDirectory);  // Automatically clone the repo
        }

        // Initialize repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(repoDirectory + "/.git"))
                            .readEnvironment()
                            .findGitDir()
                            .build();
        
        gitHistoryTreefactor = new GitHistoryTreefactorImpl();
    }

    // Test to detect commit at a specific commitId
    @Test
    void testDetectAtCommit() throws Exception {
        String commitId = "1105dbaf0706ffe22faea5f932dd73da2ef95118";

        // Create a temporary folder for commit contents
        String mainFolderPath = "commit_contents";
        File mainFolder = new File(mainFolderPath);
        if (mainFolder.exists()) {
            deleteDirectory(mainFolder); // Cleanup before test
        }

        // Run the detectAtCommit method
        gitHistoryTreefactor.detectAtCommit(repository, commitId);

        // Verify that the commit folder exists
        File commitFolder = new File(mainFolderPath + File.separator + commitId);
        assertTrue(commitFolder.exists(), "Commit folder should be created.");

        // Verify that the UML model files exist in the commit folder (or verify specific file contents)
        assertTrue(commitFolder.listFiles().length > 0, "Commit folder should contain files.");
    }

    // Test to detect all commits in the given branch
    @Test
    void testDetectAll() throws Exception {
        // Create a temporary folder for commit contents
        String mainFolderPath = "commit_contents";
        File mainFolder = new File(mainFolderPath);
        if (mainFolder.exists()) {
            deleteDirectory(mainFolder); // Cleanup before test
        }

        // Run the detectAll method
        gitHistoryTreefactor.detectAll(repository, "main"); // Change "main" to the branch you want to test

        // Verify that the main folder contains subfolders for commits
        assertTrue(mainFolder.listFiles().length > 0, "Main folder should contain commit folders.");
    }

    // Helper method to clone a repository using JGit
    private static void cloneRepository(String repoUrl, String repoDirectory) throws Exception {
        Git.cloneRepository()
           .setURI(repoUrl)
           .setDirectory(new File(repoDirectory))
           .call();
        System.out.println("Repository cloned to: " + repoDirectory);
    }

    // Helper method to delete a directory recursively
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    // After all tests, delete the cloned repository
    @AfterAll
    static void cleanup() {
        File repoFolder = new File(repoDirectory);
        if (repoFolder.exists()) {
            deleteDirectory(repoFolder); // Cleanup the cloned repository
            System.out.println("Repository deleted: " + repoDirectory);
        }
    }
}
