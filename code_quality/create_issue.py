import os
import sys
import requests
from csv2md import table
import csv

commit = sys.argv[1]
pat = os.environ.get("PAT")
# Get repository details from GitHub Actions environment
repo_owner = os.environ.get("GITHUB_REPOSITORY_OWNER")
repo_name = os.environ.get("GITHUB_REPOSITORY").split("/")[1]

path_to_smells = "smells/"
smell_files = [f for f in os.listdir(path_to_smells) if os.path.isfile(
    os.path.join(path_to_smells, f)) and str(f).strip().endswith(".csv")]

headers = {
    "Authorization": f"Bearer {pat}",
    "Accept": "application/vnd.github+json",
}

for sf in smell_files:
    try:
        with open(os.path.join(path_to_smells, sf)) as csv_file:
            list_smells = list(csv.reader(csv_file))

        raw_md = table.Table(list_smells).markdown()

        title = str(sf).replace(".csv", "") + " for commit - " + str(commit)
        body = {
            "title": title,
            "body": raw_md
        }

        github_output = requests.post(
            f"https://api.github.com/repos/{repo_owner}/{repo_name}/issues",
            headers=headers,
            json=body
        )

        if not github_output.status_code == 201:
            print(f"Error creating issue: {github_output.status_code}")
            print(f"Response: {github_output.text}")
            raise Exception(f"Failed to create issue: {github_output.text}")

    except Exception as e:
        print(f"Error processing file {sf}: {str(e)}")
        raise