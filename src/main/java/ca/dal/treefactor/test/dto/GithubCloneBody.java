package ca.dal.treefactor.test.dto;

public class GithubCloneBody {
	String owner;
	String repoName;
	String oldCommitId;
	String newCommitId;
	String token;
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getRepoName() {
		return repoName;
	}
	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}
	public String getOldCommitId() {
		return oldCommitId;
	}
	public void setOldCommitId(String oldCommitId) {
		this.oldCommitId = oldCommitId;
	}
	public String getNewCommitId() {
		return newCommitId;
	}
	public void setNewCommitId(String newCommitId) {
		this.newCommitId = newCommitId;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
}
