package ca.dal.treefactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class TreefactorApplication {
	public static void main(String[] args) throws ArgumentException{
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
			case "-bc": {
				handleBetweenCommits(args);
				break;
			}
			case "-bt": {
				handleBetweenTags(args);
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
			case "-gp": {
				handleGitHubPullRequest(args);
				break;
			}
			default:
				throw new ArgumentException("Invalid option: "+ option);
		}
	}

	private static void handleAllCommits(String[] args) throws ArgumentException{
		int maxArgLength = setupJsonOutput(args, 3);
		if (args.length > maxArgLength) {
			throw new ArgumentException("Invalid number of arguments for -a option.");
		}
		String repoFolder = args[1];
		String branch = args.length >= 3 && !args[2].equals("-json") ? args[2] : null;  // Analyze all branches if branch is not provided
		Path jsonFilePath = JsonFilePath(maxArgLength, 3, args);
	}

	private static void handleBetweenCommits(String[] args) throws ArgumentException{
		int maxArgLength = setupJsonOutput(args, 4);
		if (!(args.length == maxArgLength-1 || args.length == maxArgLength)) {
			throw new ArgumentException("Invalid number of arguments for -bc option.");
		}
		String repoFolder = args[1];
		String startCommit = args[2];
		String endCommit = (args.length >= 4 && !args[3].equals("-json")) ? args[3] : null;
		Path jsonFilePath = JsonFilePath(maxArgLength, 4, args);
	}

	private static void handleBetweenTags(String[] args) throws ArgumentException{
		int maxArgLength = setupJsonOutput(args, 4);
		if (!(args.length == maxArgLength-1 || args.length == maxArgLength)) {
			throw new ArgumentException("Invalid number of arguments for -bt option.");
		}
		String repoFolder = args[1];
		String startTag = args[2];
		String endTag = (args.length >= 4 && !args[3].equals("-json")) ? args[3] : null;
		Path jsonFilePath = JsonFilePath(maxArgLength, 4, args);
	}

	private static void handleCommit(String[] args) throws ArgumentException {
		int maxArgLength = setupJsonOutput(args, 3);
		if (args.length != maxArgLength) {
			throw new ArgumentException("Invalid number of arguments for -c option.");
		}
		String repoFolder = args[1];
		String commitId = args[2];
		Path jsonFilePath = JsonFilePath(maxArgLength, 3, args);
	}

	private static void handleGitHubCommit(String[] args) throws ArgumentException {
		int maxArgLength = setupJsonOutput(args, 4);
		if (args.length != maxArgLength) {
			throw new ArgumentException("Invalid number of arguments for -gc option.");
		}
		String gitURL = args[1];
		String commitId = args[2];
		int timeout = Integer.parseInt(args[3]);
		Path jsonFilePath = JsonFilePath(maxArgLength, 4, args);
	}

	private static void handleGitHubPullRequest(String[] args) throws ArgumentException {
		int maxArgLength = setupJsonOutput(args, 4);
		if (args.length != maxArgLength) {
			throw new ArgumentException("Invalid number of arguments for -gp option.");
		}
		String gitURL = args[1];
		int pullId = Integer.parseInt(args[2]);
		int timeout = Integer.parseInt(args[3]);
		Path jsonFilePath = JsonFilePath(maxArgLength, 4, args);
	}

	private static int setupJsonOutput(String[] args, int maxArgLength) {
		if (args.length >= maxArgLength && args[args.length - 2].equals("-json")) {
			Path path = Paths.get(args[args.length - 1]);
			try {
				if (Files.exists(path) && Files.isRegularFile(path)) {
					Files.delete(path);
				}
				if (Files.notExists(path)) {
					Files.createFile(path);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			maxArgLength = maxArgLength + 2;  // Adjust maxArgLength for the -json option
		}
		return maxArgLength;
	}

	private static Path JsonFilePath(int maxArgLength, int initialMaxLength, String[] args){
		return (maxArgLength > initialMaxLength  && args[args.length - 2].equals("-json"))
				? Paths.get(args[args.length - 1])
				: null;
	}

	private static void help() {
		System.out.println("-h\t\t\t\t\t\t\t\t\t\t\tShow options");
		System.out.println(
				"\n-a <git-repo-folder> <branch> -json <path-to-json-file>\t\t\t\t\tDetect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
		System.out.println(
				"\n-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1> -json <path-to-json-file>\tDetect refactorings between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"\n-bt <git-repo-folder> <start-tag> <end-tag> -json <path-to-json-file>\t\t\tDetect refactorings between <start-tag> and <end-tag> for project <git-repo-folder>");
		System.out.println(
				"\n-c <git-repo-folder> <commit-sha1> -json <path-to-json-file>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"\n-gc <git-URL> <commit-sha1> <timeout> -json <path-to-json-file>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-URL> within the given <timeout> in seconds. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties");
		System.out.println(
				"\n-gp <git-URL> <pull-request> <timeout> -json <path-to-json-file>\t\t\t\tDetect refactorings at specified pull request <pull-request> for project <git-URL> within the given <timeout> in seconds for each commit in the pull request. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties");
	}

	private static class ArgumentException extends Exception {
		public ArgumentException(String message) {
			super(message);
			System.err.println(message);
			System.exit(1);
		}
	}
}
