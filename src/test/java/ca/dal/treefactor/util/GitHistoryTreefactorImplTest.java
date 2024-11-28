package ca.dal.treefactor.util;

import java.io.File;

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
    private static final String REPO_DIRECTORY = "TreefactorTest-Py";
    private static final String MAIN_FOLDER_PATH = "commit_contents";
    private static Git git;

    @BeforeAll
    static void setup() throws Exception {
        String repoUrl = "https://github.com/hxu47/TreefactorTest-Py";
        File repoFolder = new File(REPO_DIRECTORY);
        File mainFolder = new File(MAIN_FOLDER_PATH);

        // Cleanup any existing directories
        if (repoFolder.exists()) {
            deleteDirectory(repoFolder);
        }
        if (mainFolder.exists()) {
            deleteDirectory(mainFolder);
        }

        // Clone fresh repository
        git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoFolder)
                .call();

        // Ensure all commits are fetched
        git.fetch().setRemote("origin").call();

        // Initialize repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(REPO_DIRECTORY + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

        gitHistoryTreefactor = new GitHistoryTreefactorImpl();

        // Create main folder for commit contents
        mainFolder.mkdirs();
    }

    @Test
    void testDetectAtCommit() throws Exception {
        // Get a valid commit ID from the repository
        String commitId = git.log()
                .setMaxCount(1)
                .call()
                .iterator()
                .next()
                .getName();

        // Create commit folder
        File commitFolder = new File(MAIN_FOLDER_PATH, commitId);
        if (!commitFolder.exists()) {
            commitFolder.mkdirs();
        }

        // Run the detectAtCommit method
        gitHistoryTreefactor.detectAtCommit(repository, commitId);

        // Verify that the commit folder exists and contains files
        assertTrue(commitFolder.exists(), "Commit folder should be created.");
        assertTrue(commitFolder.listFiles().length > 0, "Commit folder should contain files.");
    }

    @Test
    void testDetectAll() throws Exception {
        // Create main folder if it doesn't exist
        File mainFolder = new File(MAIN_FOLDER_PATH);
        if (!mainFolder.exists()) {
            mainFolder.mkdirs();
        }

        // Run the detectAll method
        gitHistoryTreefactor.detectAll(repository, "main");

        // Verify that the main folder contains commit folders
        assertTrue(mainFolder.exists() && mainFolder.isDirectory(),
                "Main folder should exist and be a directory");
        assertTrue(mainFolder.list() != null && mainFolder.list().length > 0,
                "Main folder should contain commit folders.");
    }

    @AfterAll
    static void cleanup() {
        if (git != null) {
            git.close();
        }
        // Clean up both directories
        File repoFolder = new File(REPO_DIRECTORY);
        File mainFolder = new File(MAIN_FOLDER_PATH);

        if (repoFolder.exists()) {
            deleteDirectory(repoFolder);
            System.out.println("Repository deleted: " + REPO_DIRECTORY);
        }
        if (mainFolder.exists()) {
            deleteDirectory(mainFolder);
            System.out.println("Main folder deleted: " + MAIN_FOLDER_PATH);
        }
    }

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
}