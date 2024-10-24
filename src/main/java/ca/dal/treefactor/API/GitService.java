package ca.dal.treefactor.API;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;

public interface  GitService {

    public Repository openRepository(String Folder) throws Exception;

    RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception;

}
