package ca.dal.treefactor.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import io.github.treesitter.jtreesitter.Language;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import ca.dal.treefactor.API.GitHistoryTreefactor;
import ca.dal.treefactor.API.GitService;

public class GitHistoryTreefactorImpl implements GitHistoryTreefactor {

    @Override
	public void detectAll(Repository repository, String branch) throws Exception {
		GitService gitService = new GitServiceImpl();
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			detect(gitService, repository, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

    @Override
    public void detectAtCommit(Repository repository, String commitId) {
//        GitService gitService = new GitServiceImpl();
        RevWalk walk = new RevWalk(repository);

        // Create the main folder to store commit contents
        String mainFolderPath = "commit_contents"; // This will be the main folder name
        File mainFolder = new File(mainFolderPath);
        if (!mainFolder.exists()) {
            mainFolder.mkdir(); // Create the folder if it doesn't exist
        }

        try {
            RevCommit currentCommit =  walk.parseCommit(repository.resolve(commitId));
            System.out.println(currentCommit);

            // Create a folder for this commit using its ID
            File commitFolder = new File(mainFolderPath + File.separator + commitId);
            if (!commitFolder.exists()) {
                commitFolder.mkdir(); // Create the commit folder
            }
            processAllFilesInCommit(repository, currentCommit, commitFolder);
            if (currentCommit.getParentCount() == 0) {
                // Initial commit, process all files in the tree
                processAllFilesInCommit(repository, currentCommit, commitFolder);
            }
            if (currentCommit.getParentCount() > 0){
                RevCommit parentCommit = walk.parseCommit(currentCommit.getParent(0).getId());
                String parentCommitId = parentCommit.getId().getName();

                System.out.println("Parent commit Id : "+parentCommitId);
                File parentCommitFolder = new File(mainFolderPath + File.separator + parentCommitId);
                if (!parentCommitFolder.exists()) {
                    parentCommitFolder.mkdir(); // Create the commit folder
                    processAllFilesInCommit(repository, parentCommit, parentCommitFolder);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
			walk.dispose();
		}
    }

    private void detect(GitService gitService, Repository repository, Iterator<RevCommit> i) throws IOException {
    // Create the main folder to store commit contents
    String mainFolderPath = "commit_contents"; // This will be the main folder name
    File mainFolder = new File(mainFolderPath);
    if (!mainFolder.exists()) {
        mainFolder.mkdir(); // Create the folder if it doesn't exist
    }
    while (i.hasNext()) {
        RevCommit currentCommit = i.next();

        // Print commit details
        String commitId = currentCommit.getId().getName();
        System.out.println("Commit ID: " + commitId);
        System.out.println("Commit Message: " + currentCommit.getFullMessage());

        // Create a folder for this commit using its ID
        File commitFolder = new File(mainFolderPath + File.separator + commitId);
        if (!commitFolder.exists()) {
            commitFolder.mkdir(); // Create the commit folder
        }
        processAllFilesInCommit(repository, currentCommit, commitFolder);
    }
}

    // Function to process all files in the commit tree (used for initial commit)
    private void processAllFilesInCommit(Repository repository, RevCommit currentCommit, File commitFolder) throws IOException {
        RevTree tree = currentCommit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String filePath = treeWalk.getPathString();
                saveFileContent(repository, filePath, commitFolder, currentCommit);
            }
        }
    }

    // Function to save the content of a file to the disk
    private void saveFileContent(Repository repository, String filePath, File commitFolder, RevCommit currentCommit) throws IOException {
        // Load the file content
        ObjectId objectId = currentCommit.getTree().getId();
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, objectId)) {
            if (treeWalk != null) {
                ObjectLoader loader = repository.open(treeWalk.getObjectId(0));

                // Read the content of the file as a string
                String fileContent = new String(loader.getBytes(), StandardCharsets.UTF_8);

                // Create the file inside the commit folder
                File outputFile = new File(commitFolder + File.separator + filePath);

                // Make sure the directories exist before creating the file
                outputFile.getParentFile().mkdirs();

                // Write the file content
                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    outputStream.write(fileContent.getBytes(StandardCharsets.UTF_8));
                }

                // Check if the file type is supported by Tree-sitter
                if (isSupportedFile(filePath)) {
                    try {
                        Language language = TreeSitterUtil.loadLanguageForFileExtension(filePath);
                        String astString = TreeSitterUtil.generateAST(language, fileContent);
                        System.out.println(filePath);
                        // Save the AST to commitFolder
                        saveAST(commitFolder, filePath, astString);
                        //System.out.println("Abstract Syntax Tree:");
                        //System.out.println(astString);

                    } catch (Exception e) {
                        System.err.println("Error processing file: " + filePath);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Skipping unsupported file: " + filePath);
                }
            }
        }
    }

    // Helper method to check supported file extensions
    private boolean isSupportedFile(String filePath) {
        String[] supportedExtensions = { ".js", ".py", ".cpp" };  // Add extensions as needed
        for (String extension : supportedExtensions) {
            if (filePath.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to save AST as files
    private void saveAST(File commitFolder, String filePath, String astString) {
        String outputASTFileName = filePath.substring(0, filePath.lastIndexOf('.')) + "_[CST].txt";
        File outputASTFile = new File(commitFolder, outputASTFileName);
        try (FileOutputStream fos = new FileOutputStream(outputASTFile)) {
            fos.write(astString.getBytes(StandardCharsets.UTF_8));
            System.out.println("CST saved to " + outputASTFile);
        } catch (IOException e) {
            System.err.println("Error writing to file:");
            e.printStackTrace();
        }
    }

}
