package ca.dal.treefactor.API;

import org.eclipse.jgit.lib.Repository;

public interface  GitService {

    public Repository openRepository(String Folder) throws Exception;

}
