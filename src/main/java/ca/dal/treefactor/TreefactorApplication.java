package ca.dal.treefactor;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ca.dal.treefactor.API.GitHistoryTreefactor;
import ca.dal.treefactor.API.GitService;
import ca.dal.treefactor.github.GithubUtil;
import ca.dal.treefactor.util.GitHistoryTreefactorImpl;
import ca.dal.treefactor.util.GitServiceImpl;

@SpringBootApplication
public class TreefactorApplication {

	public static void main(String[] args) throws Exception{
		SpringApplication.run(TreefactorApplication.class, args);
		if (args.length < 1) {
			help();
			System.exit(0);
		}
		final String option = args[0];
		if (option.equals("-h") || option.equals("--h") || option.equals("-help") || option.equals("--help")) {
			help();
			System.exit(0);
		}
		switch (option) {
			case "-a": {
				handleAllCommits(args);
				break;
			}
			case "-c": {
				handleCommit(args);
				break;
			}
			case "-gc": {
				handleGitHubCommit(args);
				break;
			}
			default:
				throw new ArgumentException("Invalid option: "+ option);
		}

		String mainFolderPath = "commit_contents";
		File mainFolder = new File(mainFolderPath);
		deleteDirectory(mainFolder);
		System.exit(0);
	}
	static GithubUtil gutil = new GithubUtil();

	private static void handleAllCommits(String[] args) throws Exception{
		if (args.length < 2) {
			throw new ArgumentException("Insufficient arguments for -a option.");
		}
		String repoFolder = args[1];
		String branch = args.length >= 3 ? args[2] : null;  // Analyze all branches if branch is not provided
		
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(repoFolder)) {
			GitHistoryTreefactor detector = new GitHistoryTreefactorImpl();
			detector.detectAll(repo, branch);
		}
	}

	private static void handleCommit(String[] args) throws Exception {
		if (args.length < 3) {
			throw new ArgumentException("Insufficient arguments for -c option.");
		}
		String folder = args[1];
		String commitId = args[2];
		
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			GitHistoryTreefactor detector = new GitHistoryTreefactorImpl();
			detector.detectAtCommit(repo, commitId);
		}
	}

	private static void handleGitHubCommit(String[] args) throws Exception {
		Git gitHubRepo = null;
		if (args.length < 3) {
			throw new ArgumentException("Insufficient arguments for -gc option.");
		}
		String gitURL = args[1];
		String commitId = args[2];
		
		try {
			 gitHubRepo = gutil.getRepositoryPat(gitURL);
		} catch (GitAPIException ex) {
			System.err.println("Failed to clone repository. Please ensure that the PAT and commit ID are correct.");
			ex.printStackTrace();
			return;
		}
		
		try (Repository repo = gitHubRepo.getRepository()){
			GitHistoryTreefactor detector = new GitHistoryTreefactorImpl();
			detector.detectAtCommit(repo, commitId);
		}
	}

	private static void help() {
		System.out.println("-h\t\t\t\t\t\t\t\t\t\t\tShow options");
		System.out.println(
				"\n-a <git-repo-folder> <branch>\t\t\t\tDetect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
		System.out.println(
				"\n-c <git-repo-folder> <commit-sha1>\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"\n-gc <git-URL> <commit-sha1>\t\tDetect refactorings at specified commit <commit-sha1> for project <git-URL>");
	}

	// Delete a directory and its contents
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            // Delete all files and subdirectories within the directory
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);  // Recursively delete subdirectories
                    } else {
                        file.delete();  // Delete files
                    }
                }
            }
            // Delete the empty directory
            directory.delete();
        }
    }

	private static class ArgumentException extends Exception {
		public ArgumentException(String message) {
			super(message);
			System.err.println(message);
			System.exit(1);
		}
	}
}