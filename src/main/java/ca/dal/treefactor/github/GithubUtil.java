package ca.dal.treefactor.github;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GithubUtil {

	public GithubUtil() {}

	/**
	 * Downloads the specified repository into a Temp directory.
	 * @param repoLink Link to the repository
	 * @param token Access token needed for repository
	 * @return The Git object pointing to the repo.
	 */
	public static Git getRepositoryPat(String repoLink, String token)
			throws IOException, GitAPIException {

		// Setting up folder paths
		File dir = Files.createTempDirectory("Treefactor").toFile();

		// Try to make the created directory delete when the program is shut down
		dir.deleteOnExit();

		// Cloning repo into temp dir
		return Git.cloneRepository()
				.setURI(repoLink)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
				.setDirectory(dir)
				.call();
	}
}
