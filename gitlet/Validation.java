package gitlet;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gitlet.Utils.*;

/**
 * A class to store all validation methods used in Main class.
 *
 * @author sychau
 */
public class Validation {
    /**
     * If the gitlet folder existence is opposite to the IS_INITIALIZED input,
     * print the error message and exit the program.
     */
    public static void validateGitletInitialization(boolean isInitialized, String errorMessage) {
        if ((isInitialized && !Repository.GITLET_DIR.exists())
                || (!isInitialized && Repository.GITLET_DIR.exists())) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If a user inputs a command with the wrong number or format of operands,
     * print the error message and exit the program.
     */
    public static void validateNumArgsRange(String[] args, int min, int max, String errorMessage) {
        if (args.length < min || args.length > max) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If a user inputs a file name that is not exist in the working directory,
     * print the error message and exit the program.
     */
    public static void validateFileExists(String fileName, String errorMessage) {
        File target = join(Repository.CWD, fileName);
        if (!target.exists()) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the staging area isEmpty or !isEmpty, print the error message and exit the program.
     */
    public static void validateStagingArea(boolean isEmpty, String errorMessage) {
        long addLength = Repository.STAGING_AREA_ADD.length();
        long delLength = Repository.STAGING_AREA_DEL.length();
        if (isEmpty) {
            // validate staging area is empty
            if (addLength != 0 || delLength != 0) {
                printErrorAndExit(errorMessage);
            }
        } else {
            // validate staging area is not empty
            if (addLength == 0 && delLength == 0) {
                printErrorAndExit(errorMessage);
            }
        }
    }

    /**
     * If the file does not exist in the commit snapshot, print the error message and exit
     * the program.
     */
    public static void validateFileExistInCommit(Commit commit, String fileName,
                                                 String errorMessage) {
        if (!commit.isFileKeyExists(fileName)) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the commit with that ID does not exist, print the error message and exit the program.
     */
    public static void validateCommitExist(String commitID, String errorMessage) {
        File commitBlob = join(Repository.COMMITS_DIR, commitID);
        if (!commitBlob.exists()) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the branch Existence is opposite to the IS_EXIST input, print the error message and
     * exit the program.
     */
    public static void validateBranchExistence(boolean isExist, String branchName,
                                               String errorMessage) {
        File target = join(Repository.BRANCHES_DIR, branchName);
        if ((isExist && !target.exists()) || (!isExist && target.exists())) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the current branch is the same as target branch,
     * print the error message and exit the program
     */
    public static void validateNotOnTargetBranch(String targetBranchName, String errorMessage) {
        String currentBranch = readContentsAsString(Repository.HEAD);
        if (targetBranchName.equals(currentBranch)) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If a working file is untracked in the current branch and would be overwritten by
     * the checkout, print out the error message and exit the program
     */
    public static void validateNoUntrackedFile(String branchName, String errorMessage) {
        Commit branchCommit = Commit.getCommitByBranchName(branchName);
        Map<String, String> branchCommitMap = branchCommit.getFileMap();

        List<String> untrackedFiles = Repository.getUntrackedFiles();
        Set<String> untrackedFilesSet = new HashSet<>(untrackedFiles);

        for (String untrackedFile : untrackedFilesSet) {
            String fileSHA = sha1(readContentsAsString(join(Repository.CWD, untrackedFile)));
            // If the branch does not contain that file or that file in the branch is different
            // from current version
            if (!branchCommitMap.containsKey(untrackedFile)
                    || !branchCommitMap.get(untrackedFile).equals(fileSHA)) {
                printErrorAndExit(errorMessage);
            }
        }
    }

    /**
     * If the commit message is empty print out the error message and exit the program.
     */
    public static void validateMessageNotEmpty(String message, String errorMessage) {
        if (message.equals("")) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the file with that name is neither tracked in the current commit or staged for addition,
     * print out the error message and exit the program
     */
    public static void validateEitherStagedAddOrTracked(String fileName, String errorMessage) {
        Map<String, String> addMap = StagingArea.getFileMapFrom(Repository.STAGING_AREA_ADD);
        Map<String, String> ccMap = Commit.getCurrentCommit().getFileMap();
        if (!ccMap.containsKey(fileName) && !addMap.containsKey(fileName)) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the split point is the same commit as the given branch, then we do nothing; the merge
     * is complete, and the operation ends with the message Given branch is an ancestor of the
     * current branch.
     */
    public static void validateNotAncestorOfCurrent(String branchName, String errorMessage) {
        Commit current = Commit.getCurrentCommit();
        Commit other = Commit.getCommitByBranchName(branchName);
        Commit splitPoint = Repository.findSplitPoint(current, other);
        if (splitPoint.getSHA().equals(other.getSHA())) {
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * If the split point is the current branch, then the effect is to check out the given branch,
     * and the operation ends after printing the error message
     */
    public static void validateSplitPointNotCurrentBranch(String branchName, String errorMessage) {
        Commit current = Commit.getCurrentCommit();
        Commit other = Commit.getCommitByBranchName(branchName);
        Commit splitPoint = Repository.findSplitPoint(current, other);
        if (splitPoint.getSHA().equals(current.getSHA())) {
            Repository.checkoutBranch(branchName);
            printErrorAndExit(errorMessage);
        }
    }

    /**
     * Print the errorMessage and exit the program
     */
    public static void printErrorAndExit(String errorMessage) {
        System.out.println(errorMessage);
        System.exit(0);
    }
}
