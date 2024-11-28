package ca.dal.treefactor;

import java.io.File;
import java.util.Set;

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
	private static final Set<String> HELP_OPTIONS = Set.of("-h", "--h", "-help", "--help");
	private static final int MIN_ARGS_ALL_COMMITS = 2;
    private static final int MIN_ARGS_SINGLE_COMMIT = 3;
    private static final int REPO_FOLDER_INDEX = 1;
    private static final int BRANCH_INDEX = 2;
    private static final int COMMIT_ID_INDEX = 2;
	public static void main(String[] args) throws Exception{
		SpringApplication.run(TreefactorApplication.class, args);
		if (args.length < 1) {
			help();
			System.exit(0);
		}
		final String option = args[0];
		if (HELP_OPTIONS.contains(option)) {
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

	static void handleAllCommits(String[] args) throws Exception{
		if (args.length < MIN_ARGS_ALL_COMMITS) {
            throw new ArgumentException("Insufficient arguments for -a option.");
        }
        String repoFolder = args[REPO_FOLDER_INDEX];
        String branch = args.length >= MIN_ARGS_ALL_COMMITS + 1 ? args[BRANCH_INDEX] : null;  // Analyze all branches if branch is not provided
		
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(repoFolder)) {
			GitHistoryTreefactor detector = new GitHistoryTreefactorImpl();
			detector.detectAll(repo, branch);
		}
	}

	static void handleCommit(String[] args) throws Exception {
		if (args.length < MIN_ARGS_SINGLE_COMMIT) {
            throw new ArgumentException("Insufficient arguments for -c option.");
        }
        String folder = args[REPO_FOLDER_INDEX];
        String commitId = args[COMMIT_ID_INDEX];
		
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			GitHistoryTreefactor detector = new GitHistoryTreefactorImpl();
			detector.detectAtCommit(repo, commitId);
		}
	}

	static void handleGitHubCommit(String[] args) throws Exception {
		if (args.length < MIN_ARGS_SINGLE_COMMIT) {
            throw new ArgumentException("Insufficient arguments for -gc option.");
        }
		Git gitHubRepo;
        String gitURL = args[REPO_FOLDER_INDEX];
        String commitId = args[COMMIT_ID_INDEX];
		
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

	static void help() {
		System.out.println("-h\t\t\t\t\t\t\t\t\t\t\tShow options");
		
		String allCommitsHelp = "-a <git-repo-folder> <branch>\t\t\t\t" +
			"Detect all refactorings at <branch> for <git-repo-folder>. " +
			"If <branch> is not specified, commits from all branches are analyzed.";
		System.out.println(allCommitsHelp);
		
		String singleCommitHelp = "-c <git-repo-folder> <commit-sha1>\t\t\t" +
			"Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>";
		System.out.println(singleCommitHelp);
		
		String githubCommitHelp = "-gc <git-URL> <commit-sha1>\t\t" +
			"Detect refactorings at specified commit <commit-sha1> for project <git-URL>";
		System.out.println(githubCommitHelp);
	}

	// Delete a directory and its contents
    static void deleteDirectory(File directory) {
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

	static class ArgumentException extends Exception {
		public ArgumentException(String message) {
			super(message);
			System.err.println(message);
			System.exit(1);
		}
	}
}