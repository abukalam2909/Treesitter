package ca.dal.treefactor.util;

import ca.dal.treefactor.API.GitService;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import java.io.File;
import java.io.FileNotFoundException;

public class GitServiceImpl implements GitService {

    @Override
    public Repository openRepository(String repositoryPath ) throws  Exception{

        File folder = new File(repositoryPath);
        Repository repository;
        if (folder.exists()) {
            String[] contents = folder.list();
            boolean dotGitFound = false;
            for(String content : contents) {
                if(content.equals(".git")) {
                    dotGitFound = true;
                    break;
                }
            }
            RepositoryBuilder builder = new RepositoryBuilder();
            repository = builder
                    .setGitDir(dotGitFound ? new File(folder, ".git") : folder)
                    .readEnvironment()
                    .findGitDir().build();

        } else {
            throw new FileNotFoundException(repositoryPath);
        }
        return repository;
    }
}
