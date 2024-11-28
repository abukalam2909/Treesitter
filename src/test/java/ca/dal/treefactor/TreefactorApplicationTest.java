package ca.dal.treefactor;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TreefactorApplicationTest {

    private static final String TEST_REPO_PATH = "TreefactorTest-Py";
    private static final String INVALID_REPO_PATH = "NonExistentRepo";
    private static final String COMMIT_ID = "1105dbaf0706ffe22faea5f932dd73da2ef95118";
    private static final String INVALID_COMMIT_ID = "invalidCommitHash";
    private static final String GITHUB_REPO_URL = "https://github.com/hxu47/TreefactorTest-Py";
    private static final String INVALID_GITHUB_REPO_URL = "https://invalid.github.com/repo";
    private static Git git;

//    @BeforeAll
//    static void setUpBeforeAll() throws Exception {
//        File repoDir = new File(TEST_REPO_PATH);
//        if (!repoDir.exists()) {
//            // Clone the repository automatically if it doesn't exist
//            cloneRepository(GITHUB_REPO_URL, TEST_REPO_PATH);
//        }
//    }

    @BeforeAll
    static void setUpBeforeAll() throws Exception {
        // Delete any existing repository first
        File repoDir = new File(TEST_REPO_PATH);
        if (repoDir.exists()) {
            deleteDirectory(repoDir);
        }

        // Clone fresh repository
        git = Git.cloneRepository()
                .setURI(GITHUB_REPO_URL)
                .setDirectory(new File(TEST_REPO_PATH))
                .call();

        // Fetch all commits to ensure they're available
        git.fetch().setRemote("origin").call();
    }

    @Test
    void testHandleAllCommits_ValidRepo() throws Exception {
        String[] args = { "-a", TEST_REPO_PATH, "main" };
        assertDoesNotThrow(() -> TreefactorApplication.handleAllCommits(args));
    }

    @Test
    void testHandleAllCommits_InvalidRepo() {
        String[] args = { "-a", INVALID_REPO_PATH, "main" };
        assertThrows(Exception.class, () -> TreefactorApplication.handleAllCommits(args));
    }

    @Test
    void testHandleCommit_ValidCommit() throws Exception {
        String[] args = { "-c", TEST_REPO_PATH, COMMIT_ID };
        assertDoesNotThrow(() -> TreefactorApplication.handleCommit(args));
    }

    @Test
    void testHandleCommit_InvalidCommit() {
        String[] args = { "-c", TEST_REPO_PATH, INVALID_COMMIT_ID };
        assertThrows(Exception.class, () -> TreefactorApplication.handleCommit(args));
    }

    @Test
    void testHandleCommit_InvalidRepo() {
        String[] args = { "-c", INVALID_REPO_PATH, COMMIT_ID };
        assertThrows(Exception.class, () -> TreefactorApplication.handleCommit(args));
    }

    @Test
    void testHandleGitHubCommit_ValidRepo() throws Exception {
        String[] args = { "-gc", GITHUB_REPO_URL, COMMIT_ID };
        assertDoesNotThrow(() -> TreefactorApplication.handleGitHubCommit(args));
    }

    @Test
    void testHandleGitHubCommit_InvalidCommit() {
        String[] args = { "-gc", GITHUB_REPO_URL, INVALID_COMMIT_ID };
        assertThrows(Exception.class, () -> TreefactorApplication.handleGitHubCommit(args));
    }

    @Test
    void testDeleteDirectory() {
        File tempDir = new File("temp_test_dir");
        tempDir.mkdir();
        File tempFile = new File(tempDir, "test_file.txt");
        try {
            tempFile.createNewFile();
        } catch (Exception e) {
            fail("Failed to create temporary file.");
        }

        TreefactorApplication.deleteDirectory(tempDir);
        assertFalse(tempDir.exists(), "Temporary directory should be deleted.");
    }

    // Helper method to clone a repository using JGit
    private static void cloneRepository(String repoUrl, String repoDirectory) throws Exception {
        Git.cloneRepository()
           .setURI(repoUrl)
           .setDirectory(new File(repoDirectory))
           .call();
        System.out.println("Repository cloned to: " + repoDirectory);
    }

//    @AfterAll
//    static void cleanupAfterAll() {
//        File repoDir = new File(TEST_REPO_PATH);
//        if (repoDir.exists()) {
//            deleteDirectory(repoDir);
//            System.out.println("Repository deleted: " + TEST_REPO_PATH);
//        }
//    }

    @AfterAll
    static void cleanupAfterAll() {
        if (git != null) {
            git.close();
        }
        File repoDir = new File(TEST_REPO_PATH);
        if (repoDir.exists()) {
            deleteDirectory(repoDir);
            System.out.println("Repository deleted: " + TEST_REPO_PATH);
        }
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

    private Repository getRepository(String repoPath) throws Exception {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File(repoPath + "/.git"))
                      .readEnvironment()
                      .findGitDir()
                      .build();
    }
}