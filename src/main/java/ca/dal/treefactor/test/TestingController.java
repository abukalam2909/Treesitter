package ca.dal.treefactor.test;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ca.dal.treefactor.github.GithubUtil;
import ca.dal.treefactor.test.dto.GithubCloneBody;

// TODO Remove when CLI is built
@RestController
public class TestingController {

	@PostMapping("/github/clone")
	public void cloneRepos(@RequestBody GithubCloneBody body) throws IOException, InterruptedException {

		GithubUtil.downloadAndCreateRepoDirectoriesPat(body.getOwner(), body.getRepoName(), body.getToken(), body.getOldCommitId(), body.getNewCommitId());
	}

}
