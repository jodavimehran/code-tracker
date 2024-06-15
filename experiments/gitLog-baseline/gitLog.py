import git
import csv
import json

from collections import defaultdict
import json
import os
from collections import defaultdict
import subprocess

oracle_commits = defaultdict(list)
oracle_stats = defaultdict(list)
empty_commits = defaultdict(list)

def is_git_diff_empty(commit, parentCommit, filepath, repo_path, fileName):
    if not parentCommit:
        return False
    try:
        # Run the git diff command with --ignore-cr-at-eol flag
        result_ig_whitespace = subprocess.run(
            ['git', '-C', repo_path, 'diff', '--stat', '--ignore-cr-at-eol', parentCommit, commit, '--', filepath],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )

        isEmpty = result_ig_whitespace.stdout.strip() == ''

        if not isEmpty:
            result = subprocess.run(
                ['git', '-C', repo_path, 'diff', '--stat', parentCommit, commit, '--', filepath],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                check=True
            )
            if (result.stdout.strip() == result_ig_whitespace.stdout.strip()):
                return False

            info = result.stdout.strip().split("\n")[1].split(", ")
            diff_lines = 0
            if (len(info) < 3):
                diff_lines = int(info[1].split(" ")[0])
            else:
                diff_lines = max(int(info[1].split(" ")[0]), int(info[2].split(" ")[0]))
            
            ig_whitespace = int(result_ig_whitespace.stdout.strip().split("\n")[0].split(" | ")[1].split(" ")[0])
            whitespace_percentage = (diff_lines - ig_whitespace)/diff_lines
            if (whitespace_percentage > 0.95):
                isEmpty = True

        
        if (isEmpty):
            empty_commits[fileName].append(commit)
        return isEmpty
    except subprocess.CalledProcessError as e:
        print(f"An error occurred while running git diff: {e.stderr}")
        return False

def write_to_csv(data, filename):
    with open(filename, mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(["Name", "TP", "FP", "FN", "Empty commits count"])
        for fileName, stats in data.items():
            writer.writerow([fileName, stats[0], stats[1], stats[2], stats[3]])

def getLogCommits(fileName, repo_path, start_commit, file_path, start_line, end_line, introduction_commit, mappings_file, manual_empty_file):
    repo = git.Repo(repo_path)
    log = repo.git.log(start_commit, '-L', f'{start_line},{end_line}:{file_path}')

    commitIds = []
    process = True
    empty_commits_count = 0
    while process:
        process = False
        for line in log.split('\n'):
            if ('commit ' in line and len(line) == 47):
                _, commitId = line.split(' ')
                if (len(commitId) == 40):
                    # if a file is known to have an "empty" commit in its first set of commits
                    # returned by gitLog
                    if (fileName in mappings_file and commitId in mappings_file[fileName]):
                        info = mappings_file[fileName][commitId]
                        # check if commit is known to be "empty" or if its
                        # a new commit (succeeding the original empty commit) that could be "empty" by running git diff
                        if ((fileName in manual_empty_file and commitId in manual_empty_file[fileName]) or 
                            (is_git_diff_empty(commitId, info['parent_commit_id'], info['element_file_before'], repo_path, fileName))):
                            empty_commits_count += 1
                            sl, el = info['element_name_after'].split('$')[1].split('(')[1].split(')')[0].split('-')
                            log = repo.git.log(info['parent_commit_id'], '-L', f'{sl},{el}:{info["element_file_after"]}')
                            process = True
                            break
                    commitIds.append(commitId)
                    if (commitId == introduction_commit):
                        return commitIds, empty_commits_count

    return commitIds, empty_commits_count

def get_file_names_in_directory(directory_path):
    file_names = []
    for file in os.listdir(directory_path):
        if os.path.isfile(os.path.join(directory_path, file)):
            file_names.append(os.path.basename(file))
    return file_names

def load_json_file(file_path):
    with open(file_path, 'r') as file:
        return json.load(file)
    
def getMethodName(signature):
    return signature.split("#")[1].split("(")[0]

def getFileName(signature):
    splitArray = signature.split("/")
    return splitArray[len(splitArray) -1]

def scanJSON(fileName, data, mappings_file, manual_empty_file):
    print(f"Processing {fileName}")
    start_commit = data.get('startCommitId')
    start_line = data.get('blockStartLine')
    end_line = data.get('blockEndLine')
    file_path = data.get('filePath')

    repo_name = data.get('repositoryWebURL')
    repo_name = repo_name.replace('https://github.com/', '')
    repo_name = repo_name.replace('.git', '')

    introduction_commit = data.get('expectedChanges')[-1].get("commitId")

    repo_path = '../code-tracker/tmp/' + repo_name

    tp = 0
    fp = 0
    fn = 0
    oracleChanges = data.get('expectedChanges', [])
    oracleCommitIds = set()
    for change in oracleChanges:
        oracleCommitIds.add(change.get('commitId'))

    commitIds, empty_commits_count = getLogCommits(fileName, repo_path, start_commit, file_path, start_line, end_line, introduction_commit, mappings_file, manual_empty_file)

    for commitId in commitIds:
        if (commitId in oracleCommitIds):
            tp += 1
        if (not (commitId in oracleCommitIds)):
            fp += 1
    
    commitIdsSet = set(commitIds)
    
    for oracleCommitId in oracleCommitIds:
        if (not (oracleCommitId in commitIdsSet)):
            fn += 1
    
    stats = [tp, fp, fn, empty_commits_count]

    oracle_commits[fileName] = commitIds
    oracle_stats[fileName] = stats
    return True


def processFile(oracle_file, mappings_file, manual_empty_file):
    oracle_file1 = "./oracle/block/training/" + oracle_file

    oracle1 = load_json_file(oracle_file1)
    return scanJSON(oracle_file, oracle1, mappings_file, manual_empty_file)


if __name__ == "__main__":
    commonChanges = defaultdict(int)
    addedChanges = defaultdict(int)
    deletedChanges = defaultdict(int)

    directory_path = "../../src/main/resources/oracle/block/training/"
    file_names = get_file_names_in_directory(directory_path)
    mappings_file = load_json_file("./mappings/training-mappings.json")
    manual_empty_file = load_json_file("./mappings/empty-commits-training.json")

    for file_name in file_names:
        processFile(file_name, mappings_file, manual_empty_file)
    
    write_to_csv(oracle_stats, './stats/gitLog-training-breakdown.csv')
    with open('./stats/gitLog-training.json', 'w') as json_file:
        json.dump(oracle_commits, json_file)
        json_file.write('\n')
    with open('./stats/empty-commits-training.json', 'w') as json_file:
        json.dump(empty_commits, json_file)
        json_file.write('\n')