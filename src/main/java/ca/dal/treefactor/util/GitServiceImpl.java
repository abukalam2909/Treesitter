package ca.dal.treefactor.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import ca.dal.treefactor.API.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GitServiceImpl implements GitService {

    private static final String REMOTE_REFS_PREFIX = "refs/remotes/origin/";
	Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

	DefaultCommitsFilter commitsFilter = new DefaultCommitsFilter();

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

    public boolean isCommitAnalyzed(String sha1) {
        return false;
    }

    private class DefaultCommitsFilter extends RevFilter {
		@Override
		public final boolean include(final RevWalk walker, final RevCommit c) {
			return c.getParentCount() == 1 && !isCommitAnalyzed(c.getName());
		}

		@Override
		public final RevFilter clone() {
			return this;
		}

		@Override
		public final boolean requiresCommitBody() {
			return false;
		}

		@Override
		public String toString() {
			return "RegularCommitsFilter";
		}
	}

    public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
		List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>(); 
		for (Ref ref : repository.getRefDatabase().getRefs()) {
			String refName = ref.getName();
			if (refName.startsWith(REMOTE_REFS_PREFIX)) {
				if (branch == null || refName.endsWith("/" + branch)) {
					currentRemoteRefs.add(ref.getObjectId());
				}
			}
		}
		
		RevWalk walk = new RevWalk(repository);
		for (ObjectId newRef : currentRemoteRefs) {
			walk.markStart(walk.parseCommit(newRef));
		}
		walk.setRevFilter(commitsFilter);
		return walk;
	}
}
