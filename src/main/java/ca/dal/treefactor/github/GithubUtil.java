package ca.dal.treefactor.github;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ca.dal.treefactor.util.IOUtil;

public class GithubUtil {

	private GithubUtil() {}

	/**
	 * Downloads the specified repository into a Temp directory, and creates two folders. Both folders will have code from the specified commits. The code from the original commit will be in "old", and the code from the latest commit will be in "new".
	 * @param owner The owner of the repo
	 * @param repoName Name of the repo to be checked
	 * @param oldCommitId The original commit
	 * @param newCommitId The latest commit
	 * @param token The auth token for accessing the repo
	 * @return The File object pointing to the directory with the folders containing the new and old commits.
	 */
	public static File getRepositoryPat(String owner, String repoName, String oldCommitId, String newCommitId, String token) throws IOException, InterruptedException {

		// Setting up folder paths
		File dir = Files.createTempDirectory("Treefactor").toFile();
		File repoDir = new File(dir.getAbsolutePath(), repoName);
		File oldRepoDir = new File(dir.getAbsolutePath(), "old");
		File newRepoDir = new File(dir.getAbsolutePath(), "new");

		// Try to the created directory deletes when the program is shut down
		dir.deleteOnExit();

		// Cloning repo into temp dir
		String[] cloneArgs = new String[] {"git", "clone", "https://oauth2:" + token + "@github.com/" + owner + "/" + repoName + ".git"};
		new ProcessBuilder(cloneArgs).directory(dir).start().waitFor();

		// Renaming repo's folder name to "old"
		if(!repoDir.renameTo(oldRepoDir)) {
			throw new IOException("There was a problem accessing the file system");
		}

		// Copying the repo to a new folder called "new"
		IOUtil.copyDirectory(oldRepoDir.getAbsolutePath(), newRepoDir.getAbsolutePath());

		// Checking out the commits associated with the folder names
		checkoutCommit(oldRepoDir, oldCommitId);
		checkoutCommit(newRepoDir, newCommitId);

		return dir;

	}

	/**
	 * Will check out the specified commit, for a given directory.
	 * @param repoDir The directory to the repository
	 * @param commitId The ID of the commit that will be checked out
	 */
	public static void checkoutCommit(File repoDir, String commitId) throws InterruptedException, IOException {
		String[] newCommitArgs = new String[] {"git", "checkout", commitId};
		new ProcessBuilder(newCommitArgs).directory(repoDir).start().waitFor();
	}
}
