package ca.dal.treefactor.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import ca.dal.treefactor.API.GitHistoryTreefactor;
import ca.dal.treefactor.API.GitService;
import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;

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
            System.out.println("\n\nChild commit ID : " + currentCommit.getId().getName());
            System.out.println("Commit Message: " + currentCommit.getFullMessage());

            // Create a folder for this commit using its ID
            File commitFolder = new File(mainFolderPath + File.separator + commitId);
            if (!commitFolder.exists()) {
                commitFolder.mkdir(); // Create the commit folder
            }
            
            Map<String, String> fileContentsAfter = new HashMap<>();
            fileContentsAfter = processAllFilesInCommit(repository, currentCommit, commitFolder);

            if (currentCommit.getParentCount() == 0) {
                // Initial commit, process all files in the tree
                fileContentsAfter = processAllFilesInCommit(repository, currentCommit, commitFolder);
            }

            UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
            UMLModel currentUMLModel = currentUmlReader.getUmlModel();

            if (currentCommit.getParentCount() > 0){
                Map<String, String> fileContentsBefore = new HashMap<>();
                RevCommit parentCommit = walk.parseCommit(currentCommit.getParent(0).getId());
                String parentCommitId = parentCommit.getId().getName();

                System.out.println("Parent Commit Id : "+parentCommitId);
                System.out.println("Commit Message: " + parentCommit.getFullMessage());

                File parentCommitFolder = new File(mainFolderPath + File.separator + parentCommitId);
                if (!parentCommitFolder.exists()) {
                    parentCommitFolder.mkdir(); // Create the commit folder
                    fileContentsBefore = processAllFilesInCommit(repository, parentCommit, parentCommitFolder);
                }

                UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
                UMLModel parentUMLModel = parentUmlReader.getUmlModel();

                UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
                List<Refactoring> refactorings = modelDiff.detectRefactorings();
                System.out.println("Refactorings:");
                for (Refactoring refactoring : refactorings) {
                    System.out.println("\t"+refactoring);
                }
                System.out.println("\n\n");
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
        String mainFolderPath = "commit_contents";
        File mainFolder = new File(mainFolderPath);
        if (!mainFolder.exists()) {
            mainFolder.mkdir();
        }
        while (i.hasNext()) {
            RevCommit currentCommit = i.next();
            String commitId = currentCommit.getId().getName();
            System.out.println("\n\nCommit ID: " + commitId);
            System.out.println("Commit Message: " + currentCommit.getFullMessage().trim());

            // Create and process current commit
            File commitFolder = new File(mainFolderPath + File.separator + commitId);
            if (!commitFolder.exists()) {
                commitFolder.mkdir();
            }
            Map<String, String> fileContentsAfter = processAllFilesInCommit(repository, currentCommit, commitFolder);

            // Create UML model for current commit
            UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
            UMLModel currentUMLModel = currentUmlReader.getUmlModel();

            // Process parent commits and find differences
            if (currentCommit.getParentCount() > 0) {
                RevCommit parentCommit = currentCommit.getParent(0);
                String parentCommitId = parentCommit.getId().getName();

                System.out.println("Parent Commit ID: " + parentCommitId);

                // Create and process parent commit
                File parentCommitFolder = new File(mainFolderPath + File.separator + parentCommitId);
                Map<String, String> fileContentsBefore;

                if (!parentCommitFolder.exists()) {
                    parentCommitFolder.mkdir();
                    fileContentsBefore = processAllFilesInCommit(repository, parentCommit, parentCommitFolder);
                } else {
                    // If parent folder exists, reuse existing files
                    fileContentsBefore = readExistingFiles(parentCommitFolder);
                }

                // Create UML model for parent commit
                UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
                UMLModel parentUMLModel = parentUmlReader.getUmlModel();

                // Detect and print refactorings
                System.out.println("Refactorings:");
                UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
                List<Refactoring> refactorings = modelDiff.detectRefactorings();
                
                for (Refactoring refactoring : refactorings) {
                    System.out.println("\t"+refactoring);
                }
            }
            else {
                System.out.println("Initial Commit - No parent commit\n");
            }
        }
    }
    
    // Helper method to read existing files from a commit folder
    private Map<String, String> readExistingFiles(File commitFolder) throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        String basePath = commitFolder.getAbsolutePath();
        
        processDirectory(commitFolder, basePath, fileContents);
        return fileContents;
    }
    
    private void processDirectory(File directory, String basePath, Map<String, String> fileContents) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(file, basePath, fileContents);
                } else {
                    // Get relative path from the commit folder
                    String relativePath = file.getAbsolutePath().substring(basePath.length() + 1);
                    String content = new String(Files.readAllBytes(file.toPath()));
                    fileContents.put(relativePath, content);
                }
            }
        }
    }

    // Function to process all files in the commit tree (used for initial commit)
    private Map<String, String> processAllFilesInCommit(Repository repository, RevCommit currentCommit, File commitFolder) throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        
        RevTree tree = currentCommit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String filePath = treeWalk.getPathString();
                fileContents.put(filePath, saveFileContent(repository, filePath, commitFolder, currentCommit));
            }
        }
        return fileContents;
    }

    // Function to save the content of a file to the disk
    private String saveFileContent(Repository repository, String filePath, File commitFolder, RevCommit currentCommit) throws IOException {
        // Load the file content
        ObjectId objectId = currentCommit.getTree().getId();
        String fileContent = null;
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, objectId)) {
            if (treeWalk != null) {
                ObjectLoader loader = repository.open(treeWalk.getObjectId(0));

                // Read the content of the file as a string
                fileContent = new String(loader.getBytes(), StandardCharsets.UTF_8);

                // Create the file inside the commit folder
                File outputFile = new File(commitFolder + File.separator + filePath);

                // Make sure the directories exist before creating the file
                outputFile.getParentFile().mkdirs();

                // Write the file content
                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    outputStream.write(fileContent.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return fileContent;
    }

}
