package ca.dal.treefactor.github;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GithubUtil {

	public GithubUtil() {}

	/**
	 * Downloads the specified repository into a Temp directory.
	 * @param repoLink Link to the repository
	 * @return The Git object pointing to the repo.
	 */
	public static Git getRepositoryPat(String repoLink)
			throws IOException, GitAPIException {

		// Setting up folder paths
		File dir = Files.createTempDirectory("Treefactor").toFile();
		String token=getTokenFromProperties();
		// Try to make the created directory delete when the program is shut down
		dir.deleteOnExit();

		// Cloning repo into temp dir
		return Git.cloneRepository()
				.setURI(repoLink)
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
				.setDirectory(dir)
				.call();
	}
	public static String getTokenFromProperties() {
		Properties properties = new Properties();

		// Assuming github.properties is in the root folder of the project
		Path propertiesPath = Paths.get("github.properties");  // Relative path to the file

		try (FileInputStream input = new FileInputStream(propertiesPath.toFile())) {
			properties.load(input);
			return properties.getProperty("github.token");
		} catch (IOException e) {
			System.err.println("Failed to load properties file: " + e.getMessage());
			return null; // or throw an exception
		}
	}

}
