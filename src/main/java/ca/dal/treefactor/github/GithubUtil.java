package ca.dal.treefactor.github;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ca.dal.treefactor.util.IOUtil;

public class GithubUtil {

	private GithubUtil() {}

	/**
	 * Downloads the specified repository into a Temp directory.
	 * @param owner The owner of the repo
	 * @param repoName Name of the repo to be checked
	 * @param token The auth token for accessing the repo
	 * @return The File object pointing to the directory with the folders containing the new and old commits.
	 */
	public static File getRepositoryPat(String owner, String repoName, String token)
			throws IOException, InterruptedException {

		// Setting up folder paths
		File dir = Files.createTempDirectory("Treefactor").toFile();

		// Try to make the created directory delete when the program is shut down
		dir.deleteOnExit();

		// Cloning repo into temp dir
		String[] cloneArgs = new String[] {
				"git", "clone", "https://oauth:" + token + "@github.com/" + owner + "/" + repoName + ".git"
		};
		new ProcessBuilder(cloneArgs).directory(dir).start().waitFor();

		File repoDir = new File(dir.getAbsolutePath(), repoName);
		if(!repoDir.exists()) {
			throw new IllegalArgumentException("There was a problem accessing the repo.");
		}

		return dir;

	}

	/*
	 * TODO If we don't need two folders,
	 *  remove the functions "createDirectoriesFromRepo and "downloadAndCreateRepoDirectoriesPat"
	 */

	/**
	 * Takes the repo folder and converts it into two folders, both checking different commits.
	 * @param homeDir The directory the repo is located in (Usually in the Temp folder)
	 * @param repoName Name of the repo to be checked
	 * @param oldCommitId The original commit
	 * @param newCommitId The latest commit
	 */
	public static void createDirectoriesFromRepo(File homeDir, String repoName, String oldCommitId, String newCommitId)
			throws IOException, InterruptedException {

		File repoDir = new File(homeDir.getAbsolutePath(), repoName);
		File oldRepoDir = new File(homeDir.getAbsolutePath(), "old");
		File newRepoDir = new File(homeDir.getAbsolutePath(), "new");

		// Renaming repo's folder name to "old"
		if(!repoDir.renameTo(oldRepoDir)) {
			throw new IOException("There was a problem accessing the file system");
		}

		// Copying the repo to a new folder called "new"
		IOUtil.copyDirectory(oldRepoDir.getAbsolutePath(), newRepoDir.getAbsolutePath());

		// Checking out the commits associated with the folder names
		checkoutCommit(oldRepoDir, oldCommitId);
		checkoutCommit(newRepoDir, newCommitId);
	}

	/**
	 * Downloads the specified repository into a Temp directory, and creates two folders.
	 * Both folders will have code from the specified commits.
	 * The code from the original commit will be in "old", and the code from the latest commit will be in "new".
	 * @param owner The owner of the repo
	 * @param repoName Name of the repo to be checked
	 * @param token The auth token for accessing the repo
	 * @param oldCommitId The original commit
	 * @param newCommitId The latest commit
	 * @return The File object pointing to the directory with the folders containing the new and old commits.
	 */
	public static File downloadAndCreateRepoDirectoriesPat(String owner, String repoName, String token,
														   String oldCommitId, String newCommitId)
			throws IOException, InterruptedException {
		File homeDir = getRepositoryPat(owner, repoName, token);
		createDirectoriesFromRepo(homeDir, repoName, oldCommitId, newCommitId);

		return homeDir;
	}

	/**
	 * Will check out the specified commit, for a given directory.
	 * @param repoDir The directory to the repository
	 * @param commitId The ID of the commit that will be checked out
	 */
	public static void checkoutCommit(File repoDir, String commitId)
			throws InterruptedException, IOException {
		String[] newCommitArgs = new String[] {"git", "checkout", commitId};
		new ProcessBuilder(newCommitArgs).directory(repoDir).start().waitFor();
	}
}
