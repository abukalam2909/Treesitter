package ca.dal.treefactor.util;

import ca.dal.treefactor.API.GitHistoryRefactoringMiner;
import ca.dal.treefactor.API.GitService;
import ca.dal.treefactor.API.RefactoringHandler;
import com.sun.source.tree.Tree;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GitHistoryRefactoringMinerImpl implements GitHistoryRefactoringMiner {

    @Override
    public void detectAll(Repository repository, String branch) throws Exception {

    }

    @Override
    public void detectAtCommit(Repository repository, String commitId) {
//        GitService gitService = new GitServiceImpl();
        RevWalk walk = new RevWalk(repository);
        try {
            RevCommit currentCommit =  walk.parseCommit(repository.resolve(commitId));
            System.out.println(currentCommit);


            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                // Add a specific tree (e.g., the commit's tree)
                treeWalk.addTree(currentCommit.getTree());

                // Now you can start walking through the tree
                while (treeWalk.next()) {
                    ObjectId blobId = treeWalk.getObjectId(0);
                    String path = treeWalk.getPathString();
                    if (treeWalk.isSubtree()) {
                        System.out.println("Entering subtree: " + treeWalk.getPathString());
                        treeWalk.enterSubtree();  // Enter the directory and continue processing.
                    }
                    else{
                        ObjectLoader loader = repository.open(blobId);
//                        saveFile(blobId,path);
                        try (InputStream is = loader.openStream()) {
                            byte[] content = new byte[(int) loader.getSize()];
                            is.read(content);
                            // Print blob content
                            System.out.println("Path: " + path);
                            System.out.println("Content: " + new String(content));
                        }
                    }
                    // Process each entry
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //////////// use for generating file

//    private void saveFile(ObjectId blobId, String path) {
//
//        File outputFile = new File(outputDirectory, path);
//        outputFile.getParentFile().mkdirs(); // Create directories if needed
//
//        try (InputStream is = loader.openStream();
//             FileOutputStream fos = new FileOutputStream(outputFile)) {
//            byte[] buffer = new byte[1024]; // 1 KB buffer for reading
//            int bytesRead;
//
//            // Read from InputStream and write to the file
//            while ((bytesRead = is.read(buffer)) != -1) {
//                fos.write(buffer, 0, bytesRead);
//            }
//
//            System.out.println("File saved at: " + outputFile.getAbsolutePath());
//        } catch (IOException e) {
//            System.err.println("Error saving file: " + path);
//            e.printStackTrace();
//        }
//    }
    ///////////
}
