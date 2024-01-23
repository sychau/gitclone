package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author sychau
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // java gitlet.Main init
                Validation.validateGitletInitialization(false,
                        "A Gitlet version-control system already exists in the current directory.");

                Repository.init();
                break;

            case "add":
                // java gitlet.Main add [file]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2,
                        "Incorrect operands.");

                String fileName = args[1];
                Validation.validateFileExists(fileName, "File does not exist.");
                Repository.add(fileName);
                break;

            case "commit":
                // java gitlet.Main commit [message]
                String message = args[1];
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2,
                        "Incorrect operands.");
                Validation.validateMessageNotEmpty(message,
                        "Please enter a commit message.");
                Validation.validateStagingArea(false,
                        "No changes added to the commit.");

                Repository.commit(message, null);
                break;

            case "rm":
                // java gitlet.Main rm [file name]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2,
                        "Incorrect operands.");
                String rmFileName = args[1];
                Validation.validateEitherStagedAddOrTracked(rmFileName,
                        "No reason to remove the file.");
                Repository.rm(rmFileName);
                break;

            case "log":
                // java gitlet.Main log
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 1, 1,
                        "Incorrect operands.");
                Repository.log();
                break;

            case "global-log":
                // java gitlet.Main global-log
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 1, 1,
                        "Incorrect operands.");
                Repository.globalLog();
                break;

            case "find":
                // java gitlet.Main find [commit message]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2,
                        "Incorrect operands.");
                String targetMessage = args[1];
                Repository.find(targetMessage);
                break;

            case "status":
                // java gitlet.Main status
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 1, 1,
                        "Incorrect operands.");
                Repository.status();
                break;

            case "checkout":
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 4,
                        "Incorrect operands.");

                String checkoutFileName;
                String commitID;
                String checkoutBranchName;
                if (args.length == 3) {
                    // java gitlet.Main checkout -- [file name]
                    if (!args[1].equals("--")) {
                        Validation.printErrorAndExit("Incorrect operands.");
                    }
                    checkoutFileName = args[2];
                    Commit current = Commit.getCurrentCommit();
                    Validation.validateFileExistInCommit(current, checkoutFileName,
                            "File does not exist in that commit.");
                    Repository.checkoutFile(current, checkoutFileName);

                } else if (args.length == 4) {
                    // java gitlet.Main checkout [commit id] -- [file name]
                    if (!args[2].equals("--")) {
                        Validation.printErrorAndExit("Incorrect operands.");
                    }
                    commitID = args[1];
                    if (commitID.length() < 40) {
                        commitID = Repository.getFullSHA(Repository.COMMITS_DIR, commitID);
                    }
                    checkoutFileName = args[3];

                    Validation.validateCommitExist(commitID,
                            "No commit with that id exists.");
                    Commit targetCommit = Commit.getCommitByID(commitID);
                    assert targetCommit != null;
                    Validation.validateFileExistInCommit(targetCommit, checkoutFileName,
                            "File does not exist in that commit.");

                    Repository.checkoutFile(targetCommit, checkoutFileName);
                } else {
                    // java gitlet.Main checkout [branch name]
                    checkoutBranchName = args[1];
                    Validation.validateBranchExistence(true, checkoutBranchName,
                            "No such branch exists.");
                    Validation.validateNotOnTargetBranch(checkoutBranchName,
                            "No need to checkout the current branch.");
                    Validation.validateNoUntrackedFile(checkoutBranchName,
                            "There is an untracked file in the way; delete it, " +
                                    "or add and commit it first.");
                    Repository.checkoutBranch(checkoutBranchName);
                }
                break;

            case "branch":
                // java gitlet.Main branch [branch name]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2, "Incorrect operands.");
                String branchName = args[1];
                Validation.validateBranchExistence(false, branchName,
                        "A branch with that name already exists.");
                Repository.branch(branchName);
                break;

            case "rm-branch":
                // java gitlet.Main rm-branch [branch name]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2, "Incorrect operands.");
                String rmBranchName = args[1];
                Validation.validateBranchExistence(true, rmBranchName,
                        "A branch with that name does not exist.");
                Validation.validateNotOnTargetBranch(rmBranchName,
                        "Cannot remove the current branch.");
                Repository.rmBranch(rmBranchName);
                break;

            case "reset":
                // java gitlet.Main reset [commit id]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2, "Incorrect operands.");

                String resetCommitId = args[1];
                if (resetCommitId.length() < 40) {
                    resetCommitId = Repository.getFullSHA(Repository.COMMITS_DIR, resetCommitId);
                }
                Validation.validateCommitExist(resetCommitId,
                        "No commit with that id exists.");
                String currentBranchName = Utils.readContentsAsString(Repository.HEAD);
                Validation.validateNoUntrackedFile(currentBranchName,
                        "There is an untracked file in the way; delete it, " +
                                "or add and commit it first.");
                Repository.reset(resetCommitId);
                break;

            case "merge":
                // java gitlet.Main merge [branch name]
                Validation.validateGitletInitialization(true,
                        "Not in an initialized Gitlet directory.");
                Validation.validateNumArgsRange(args, 2, 2, "Incorrect operands.");

                String mergeBranchName = args[1];
                Validation.validateStagingArea(true, "You have uncommitted changes.");
                Validation.validateBranchExistence(true, mergeBranchName,
                        "A branch with that name does not exist.");
                Validation.validateNotOnTargetBranch(mergeBranchName,
                        "Cannot merge a branch with itself");
                Validation.validateNoUntrackedFile(mergeBranchName,
                        "There is an untracked file in the way; delete it, or add and commit it first.");
                Validation.validateNotAncestorOfCurrent(mergeBranchName,
                        "Given branch is an ancestor of the current branch.");

                // This validation has a side effect, it will check out the branch
                // if condition is met
                Validation.validateSplitPointNotCurrentBranch(mergeBranchName,
                        "Current branch fast-forwarded.");
                Repository.merge(mergeBranchName);
                break;

            default:
                Validation.printErrorAndExit("No command with that name exists.");
        }
    }
}
