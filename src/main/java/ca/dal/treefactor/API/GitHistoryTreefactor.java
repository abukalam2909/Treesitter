package ca.dal.treefactor.API;

import org.eclipse.jgit.lib.Repository;

public interface GitHistoryTreefactor {
    void detectAll(Repository repository, String branch) throws Exception;

    void detectAtCommit(Repository repository, String commitId);

}
