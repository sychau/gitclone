package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *
 * All operations related to the internal structure of .gitlet is stored here
 *
 * @author sychau
 */
public class Repository {
    /** List all instance variables of the Repository class here with a useful
     *  comment above them describing what that variable represents and how that
     *  variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The .gitlet/objects directory.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /**
     * The .gitlet/objects/commits directory, which stores commits
     */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");
    /**
     * The .gitlet/objects/blobs directory, which stores file content
     */
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /**
     * The HEAD file, which stores a UID of commit of current HEAD
     */
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    /**
     * The .gitlet/branches directory, which stores branches
     */
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    /**
     * The master file, which stores a UID of commit of master branch
     */
    public static final File MASTER = join(BRANCHES_DIR, "master");
    /**
     * The staging area file for addition, which pairs of file name and BlobUID
     */
    public static final File STAGING_AREA_ADD = join(GITLET_DIR, "stage_add");
    /**
     * The staging area file for removal, which pairs of file name and BlobUID
     */
    public static final File STAGING_AREA_DEL = join(GITLET_DIR, "stage_del");

    /**
     * A command to initialize gitlet repository:
     * 1. Create a .gitlet directory and related subdirectory
     * 2. Create empty staging areas
     * 3. Create master branch and HEAD and store UID of initial commit
     * 4. Create an initial commit and persist the commit
     */
    public static void init() {
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        try {
            HEAD.createNewFile();
            MASTER.createNewFile();
            STAGING_AREA_ADD.createNewFile();
            STAGING_AREA_DEL.createNewFile();
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        Commit ic = new Commit(); // initial commit
        String icSHA1 = ic.getSHA(); // initial commit UID

        writeContents(HEAD, MASTER.getName());
        writeContents(MASTER, icSHA1);
        ic.createCommitBlob();
    }

    /**
     * A command to add a file to a staging area:
     * 1. If a file is already staged, overwrite it with new contents.
     * 2. If the current working version of the file is identical to the version in the
     * current commit,do not stage it to be added, and remove it from the staging area
     * if it is already there (as can happen when a file is changed, added, and then
     * changed back to its original version)
     */
    public static void add(String fileName) {
        File currentFile = join(CWD, fileName); // current working version of the file
        String currentFileContent = readContentsAsString(currentFile);
        String currentFileID = sha1(currentFileContent);

        Commit currentCommit = Commit.getCurrentCommit();
        Map<String, String> ccMap = currentCommit.getFileMap();
        Map<String, String> addMap = StagingArea.getFileMapFrom(STAGING_AREA_ADD);
        Map<String, String> delMap = StagingArea.getFileMapFrom(STAGING_AREA_DEL);

        // If the current working version of the file is identical to the version in
        // the current commit, do not stage it to be added, and remove it from the
        // staging area if it is already there.
        if (ccMap.containsKey(fileName)
                && currentFileID.equals(ccMap.get(fileName))) {
            addMap.remove(fileName);
            delMap.remove(fileName);
            StagingArea.overwriteFromMap(STAGING_AREA_ADD, addMap);
            StagingArea.overwriteFromMap(STAGING_AREA_DEL, delMap);
            return;
        }

        // If a file is already staged, overwrite it with new contents.
        if (addMap.containsKey(fileName)
                && !currentFileID.equals(addMap.get(fileName))) {
            String stagedUID = addMap.replace(fileName, currentFileID);
        }

        addMap.put(fileName, currentFileID);
        writeContents(join(BLOBS_DIR, currentFileID), currentFileContent);
        StagingArea.overwriteFromMap(STAGING_AREA_ADD, addMap);
    }

    /**
     * A command to saves a snapshot of tracked files in the current commit and
     * staging area, so they can be restored at a later time
     */
    public static void commit(String message, String secondParentID) {
        // Get current commit and create next commit
        Commit current = Commit.getCurrentCommit();
        String currentID = current.getSHA();
        Commit next = new Commit(message, currentID, secondParentID);

        // Get file Map of next commit
        Map<String, String> nextMap = next.getFileMap();

        // Handle add section
        Map<String, String> addMap = StagingArea.getFileMapFrom(STAGING_AREA_ADD);
        nextMap.putAll(addMap);

        // Handle del section
        Map<String, String> delMap = StagingArea.getFileMapFrom(STAGING_AREA_DEL);
        for (Map.Entry<String, String> entry : delMap.entrySet()) {
            nextMap.remove(entry.getKey());
        }

        // Create commit blob
        next.createCommitBlob();

        // Reassign HEAD and branch pointer
        File targetBranch = join(BRANCHES_DIR, readContentsAsString(HEAD));
        String nextID = next.getSHA();
        writeContents(targetBranch, nextID);

        // Clear staging area
        StagingArea.clear(STAGING_AREA_ADD);
        StagingArea.clear(STAGING_AREA_DEL);
    }

    /**
     * A command to remove
     */
    public static void rm(String fileName) {
        // Un-stage the file if it is currently staged for addition
        Map<String, String> addMap = StagingArea.getFileMapFrom(STAGING_AREA_ADD);
        addMap.remove(fileName);
        StagingArea.overwriteFromMap(STAGING_AREA_ADD, addMap);

        // If the file is tracked in the current commit, stage it for removal
        // and remove the file from the working directory if the user has not already done so
        // (do not remove it unless it is tracked in the current commit).
        Commit currentCommit = Commit.getCurrentCommit();
        Map<String, String> currentMap = currentCommit.getFileMap();
        Map<String, String> delMap = StagingArea.getFileMapFrom(STAGING_AREA_DEL);
        if (currentMap.containsKey(fileName)) {
            delMap.put(fileName, currentMap.get(fileName));
            StagingArea.overwriteFromMap(STAGING_AREA_DEL, delMap);
            // remove the file from the working directory if the user has not already done so
            File targetFile = join(CWD, fileName);
            restrictedDelete(targetFile);
        }
    }

    /**
     * A command print out information of commit history
     */
    public static void log() {
        Commit cmt = Commit.getCurrentCommit();
        while (cmt != null) {
            cmt.printLog();
            // Traverse the commit history
            cmt = Commit.getCommitByID(cmt.getParent());
        }
    }

    /**
     * A command to print all commit history in any order
     */
    public static void globalLog() {
        List<String> commitList = plainFilenamesIn(COMMITS_DIR);
        if (commitList != null) {
            for (String commitID : commitList) {
                Commit cmt = Commit.getCommitByID(commitID);
                cmt.printLog();
            }
        }
    }

    /**
     * A command to print all the commit IDs if that commits' message match the TARGET_MESSAGE
     */
    public static void find(String targetMessage) {
        // The string builder to print out commit IDs one per line
        StringBuilder output = new StringBuilder();

        // Iterate through the list of commits inside the COMMITS_DIR directory
        List<String> commitList = plainFilenamesIn(COMMITS_DIR);
        if (commitList != null) {
            for (String commitID : commitList) {
                Commit cmt = Commit.getCommitByID(commitID);
                String msg = cmt.getMessage();
                if (msg.equals(targetMessage)) {
                    output.append(commitID);
                    output.append("\n");
                }
            }
        }
        // Failure if no commit with TARGET_MESSAGE found
        if (output.toString().equals("")) {
            Validation.printErrorAndExit("Found no commit with that message.");
        }
        System.out.println(output);
    }

    /**
     * A command to print out the current status of the repository
     */
    public static void status() {
        StringBuilder sb = new StringBuilder();
        List<String> branchNameList = plainFilenamesIn(BRANCHES_DIR);
        Map<String, String> addMap = StagingArea.getFileMapFrom(STAGING_AREA_ADD);
        List<String> addMapList = new ArrayList<>(addMap.keySet());
        Map<String, String> delMap = StagingArea.getFileMapFrom(STAGING_AREA_DEL);
        List<String> delMapList = new ArrayList<>(delMap.keySet());

        // Displays what branches currently exist, and marks the current branch with a *
        sb.append("=== Branches ===\n");
        if (branchNameList != null) {
            Collections.sort(branchNameList);
            for (String branchName : branchNameList) {
                String currentBranchName = readContentsAsString(HEAD);
                if (branchName.equals(currentBranchName)) {
                    sb.append(String.format("*%s\n", branchName));
                } else {
                    sb.append(String.format("%s\n", branchName));
                }
            }
        }
        sb.append("\n");

        sb.append("=== Staged Files ===\n");
        Collections.sort(addMapList);
        for (String fileName : addMapList) {
            sb.append(String.format("%s\n", fileName));
        }
        sb.append("\n");

        sb.append("=== Removed Files ===\n");
        Collections.sort(delMapList);
        for (String fileName : delMapList) {
            sb.append(String.format("%s\n", fileName));
        }
        sb.append("\n");

        sb.append("=== Modifications Not Staged For Commit ===\n");
        // A file in the working directory is "modified but not staged" if it is
        // 1. Tracked in the current commit, changed in the working directory, but not staged; or
        // 2. Staged for addition, but with different contents than in the working directory; or
        // 3. Staged for addition, but deleted in the working directory; or
        // 4. Not staged for removal, but tracked in the current commit and deleted from the
        //    working directory.
        List<String> modifiedButNotStagedList = new ArrayList<>(getModifiedButNotStagedFile());
        Collections.sort(modifiedButNotStagedList);
        for (String fileName : modifiedButNotStagedList) {
            sb.append(String.format("%s\n", fileName));
        }
        sb.append("\n");

        sb.append("=== Untracked Files ===\n");
        // The final category ("Untracked Files") is for files present in the working directory
        // but neither staged for addition nor tracked. This includes files that have been staged
        // for removal, but then re-created without Gitlet's knowledge. Ignore any subdirectories
        // that may have been introduced, since Gitlet does not deal with them.
        List<String> untrackedFilesList = getUntrackedFiles();

        Collections.sort(untrackedFilesList);
        for (String fileName : untrackedFilesList) {
            sb.append(String.format("%s\n", fileName));
        }
        sb.append("\n");

        // print the result
        System.out.println(sb);
    }

    /**
     * A command to overwrite the file named FILENAME with the snapshot version stored in CMT
     */
    public static void checkoutFile(Commit cmt, String fileName) {
        Map<String, String> fileMap = cmt.getFileMap();
        File target = join(Repository.CWD, fileName);
        File blob = join(Repository.BLOBS_DIR, fileMap.get(fileName));
        String storedContent = readContentsAsString(blob);
        writeContents(target, storedContent);
    }

    /**
     * A command to check out branch, takes all files in the commit at the head of the given
     * branch, and puts them in the working directory,overwriting the versions of the files that
     * are already there if they exist. Also, at the end of this command, the given branch ill now
     * be considered the current branch (HEAD). Any files that are tracked in the current branch
     * but are not present in the checked-out branch are deleted. The staging area is cleared,
     * unless the checked-out branch is the current branch (see Failure cases below).
     */
    public static void checkoutBranch(String branchName) {
        // Clean the CWD first
        List<String> cwdFileList = plainFilenamesIn(CWD);
        if (cwdFileList != null) {
            for (String fileName : cwdFileList) {
                File f = join(CWD, fileName);
                restrictedDelete(f);
            }
        }

        // Iterate through the branch commit, create and write the tracked files
        Commit branchCommit = Commit.getCommitByBranchName(branchName);
        Map<String, String> bcMap = branchCommit.getFileMap();
        for (Map.Entry<String, String> e : bcMap.entrySet()) {
            File f = join(CWD, e.getKey());
            String content = readContentsAsString(join(BLOBS_DIR, e.getValue()));
            writeContents(f, content);
        }

        // Clear staging area;
        StagingArea.clear(STAGING_AREA_ADD);
        StagingArea.clear(STAGING_AREA_DEL);

        // Reassign HEAD to the checked-out branch
        writeContents(HEAD, branchName);
    }

    /**
     * A command to creates a new branch with the given name, and points it at the current
     * head commit
     */
    public static void branch(String branchName) {
        File newBranch = join(BRANCHES_DIR, branchName);
        writeContents(newBranch, Commit.getCurrentCommit().getSHA());
    }

    /**
     * A command to remove the branch with the given name
     */
    public static void rmBranch(String branchName) {
        // delete the pointer file that point to that branch,
        // the commits of the d (real git use garbage collection to recycle)
        File target = join(BRANCHES_DIR, branchName);
        target.delete();
    }

    /**
     * A command to reset the current working directory into the commit specified by the COMMIT_ID
     */
    public static void reset(String commitID) {
        // move the branch pointer to the commit specified by commitID
        String currentBranch = readContentsAsString(HEAD);
        writeContents(join(BRANCHES_DIR, currentBranch), commitID);
        // checkout branch again
        checkoutBranch(currentBranch);
    }

    //** merge current branch with other branch */
    public static void merge(String otherBranchName) {
        Commit splitPoint = findSplitPoint(Commit.getCurrentCommit(),
                Commit.getCommitByBranchName(otherBranchName)); // Find the latest common ancestor
        Commit current = Commit.getCurrentCommit();
        Commit other = Commit.getCommitByBranchName(otherBranchName);

        assert splitPoint != null;
        Map<String, String> splitPointMap = splitPoint.getFileMap();
        Map<String, String> currentMap = current.getFileMap();
        Map<String, String> otherMap = other.getFileMap();

        Set<String> allFile = new HashSet<>();
        allFile.addAll(splitPointMap.keySet());
        allFile.addAll(currentMap.keySet());
        allFile.addAll(otherMap.keySet());

        // Iterate through allFile and decide what action to take to the file (8 cases in total)
        for (String fileName : allFile) {
            handleMergeFile(splitPointMap, currentMap, otherMap,
                    current, other, fileName);
        }
        // Commit all the changes
        Validation.validateStagingArea(false, "No changes added to the commit.");
        commit(String.format("Merged %s into %s.", otherBranchName, readContentsAsString(HEAD)),
                other.getSHA());
    }

    /**
     * A helper method to return a List of names of the untracked files
     */
    public static List<String> getUntrackedFiles() {
        // Set of files staged for addition
        Map<String, String> addMap = StagingArea.getFileMapFrom(STAGING_AREA_ADD);
        Set<String> addSet = addMap.keySet();

        // Set of files staged for deletion
        Map<String, String> delMap = StagingArea.getFileMapFrom(STAGING_AREA_DEL);
        Set<String> delSet = delMap.keySet();

        // Set of files in CWD
        List<String> cwdFileList = plainFilenamesIn(CWD);
        Set<String> cwdSet = new HashSet<>();
        if (cwdFileList != null) {
            cwdSet.addAll(cwdFileList);
        }

        // Set of files tracked in current commit
        Map<String, String> ccMap = Commit.getCurrentCommit().getFileMap();
        Set<String> ccSet = ccMap.keySet();

        // File that are staged for removal but recreated in CWD
        Set<String> delSetIntersectCWDSet = new HashSet<>(delSet);
        delSetIntersectCWDSet.retainAll(cwdSet);
        List<String> untrackedFiles = new ArrayList<>(delSetIntersectCWDSet);

        // Files that are presented in CWD but not tracked by current commit or staged for addition
        Set<String> ccSetCombineAddSET = new HashSet<>(ccSet);
        ccSetCombineAddSET.addAll(addSet);
        Set<String> cwdSetRemoveCcSetCombineAddSET = new HashSet<>(cwdSet);
        cwdSetRemoveCcSetCombineAddSET.removeAll(ccSetCombineAddSET);
        untrackedFiles.addAll(cwdSetRemoveCcSetCombineAddSET);
        return untrackedFiles;
    }

    /**
     * Get a set of files that is modified but not staged
     */
    public static Set<String> getModifiedButNotStagedFile() {
        Map<String, String> addMap = StagingArea.getFileMapFrom(STAGING_AREA_ADD);
        Map<String, String> delMap = StagingArea.getFileMapFrom(STAGING_AREA_DEL);
        Set<String> modifiedButNotStaged = new HashSet<>();

        Commit currentCommit = Commit.getCurrentCommit();
        Map<String, String> currentFileMap = currentCommit.getFileMap();

        List<String> cwdFileList = plainFilenamesIn(CWD);
        Set<String> cwdSet;
        if (cwdFileList != null) {
            cwdSet = new HashSet<>(cwdFileList);
        } else {
            cwdSet = new HashSet<>();
        }

        Set<String> ccSet = currentFileMap.keySet();
        Set<String> addSet = addMap.keySet();
        Set<String> delSet = delMap.keySet();

        // Tracked in the current commit, changed in the working directory, but not staged
        Set<String> cwdSetIntersectCcSetDelAddSet = new HashSet<>(cwdSet);
        cwdSetIntersectCcSetDelAddSet.retainAll(ccSet);
        cwdSetIntersectCcSetDelAddSet.removeAll(addSet);

        for (String fileName : cwdSetIntersectCcSetDelAddSet) {
            String fileContent = readContentsAsString(join(CWD, fileName));
            // check if content is changed (compare SHA)
            if (!sha1(fileContent).equals(currentFileMap.get(fileName))) {
                String fileNameModified = fileName + " (modified)";
                modifiedButNotStaged.add(fileNameModified);
            }
        }
        // Staged for addition, but with different contents than in the working directory
        Set<String> addSetIntersectCWDSet = new HashSet<>(addSet);
        addSetIntersectCWDSet.retainAll(cwdSet);
        for (String fileName : addSetIntersectCWDSet) {
            String fileContent = readContentsAsString(join(CWD, fileName));
            // check if content is changed (compare SHA)
            if (!sha1(fileContent).equals(addMap.get(fileName))) {
                String fileNameModified = fileName + " (modified)";
                modifiedButNotStaged.add(fileNameModified);
            }
        }
        // Staged for addition, but deleted in the working directory
        for (String fileName : addSet) {
            // check if file is deleted from CWD
            if (!cwdSet.contains(fileName)) {
                String fileNameDeleted = fileName + " (deleted)";
                modifiedButNotStaged.add(fileNameDeleted);
            }
        }
        // Not staged for removal, but tracked in the current commit and deleted from
        // the working directory.
        Set<String> ccSetRemoveDelSet = new HashSet<>(ccSet);
        ccSetRemoveDelSet.removeAll(delSet);
        for (String fileName : ccSetRemoveDelSet) {
            // check if file is deleted from CWD
            if (!cwdSet.contains(fileName)) {
                String fileNameDeleted = fileName + " (deleted)";
                modifiedButNotStaged.add(fileNameDeleted);
            }
        }

        return modifiedButNotStaged;
    }

    /**
     * Given a six hex-digit ABV_SHA and DIRECTORY for searching, return the full SHA
     */
    public static String getFullSHA(File directory, String abvSHA) {
        List<String> shaList = plainFilenamesIn(directory);
        if (shaList != null) {
            for (String sha : shaList) {
                String shortSHA = sha.substring(0, abvSHA.length());
                if (shortSHA.equals(abvSHA)) {
                    return sha;
                }
            }
        }
        return null;
    }

    /**
     * Get merge conflict message String
     */
    public static String getConflictMessage(String fileName, Commit current, Commit other) {
        String currentContent = current.getFileContent(fileName);
        String otherContent = other.getFileContent(fileName);
        currentContent = currentContent == null ? "" : currentContent;
        otherContent = otherContent == null ? "" : otherContent;
        String conflict = "<<<<<<< HEAD\n"
                + currentContent
                + "=======\n"
                + otherContent
                + ">>>>>>>\n";
        return conflict;
    }

    /**
     * A helper method of merge, to decide to add or remove or do nothing to a file
     */
    public static void handleMergeFile(Map<String, String> splitPointMap,
                                       Map<String, String> currentMap,
                                       Map<String, String> otherMap,
                                       Commit current, Commit other, String fileName) {
        boolean presentInSplitPoint = splitPointMap.containsKey(fileName);
        boolean presentInCurrent = currentMap.containsKey(fileName);
        boolean presentInOther = otherMap.containsKey(fileName);

        boolean modifiedInCurrent = (presentInSplitPoint && presentInCurrent
                && !splitPointMap.get(fileName).equals(currentMap.get(fileName)))
                || (presentInSplitPoint && !presentInCurrent);

        boolean modifiedInOther = (presentInSplitPoint && presentInOther
                && !splitPointMap.get(fileName).equals(otherMap.get(fileName)))
                || (presentInSplitPoint && !presentInOther);

        if (!modifiedInCurrent && modifiedInOther) {
            // Modified in OTHER but not CURRENT -> OTHER
            if (!otherMap.containsKey(fileName)) {
                rm(fileName);
            } else {
                // overwrite the file with the version in OTHER branch
                writeContents(join(CWD, fileName), other.getFileContent(fileName));
                add(fileName);
            }
        } else if (modifiedInCurrent && !modifiedInOther) {
            // Modified in CURRENT but not OTHER -> CURRENT
            assert true;
        } else if (modifiedInCurrent && modifiedInOther) {
            // Modified in both CURRENT and OTHER ->  ...

            // Modified in same way -> CURRENT/OTHER
            if ((!presentInCurrent && !presentInOther)
                    || currentMap.get(fileName).equals(otherMap.get(fileName))) {
                assert true;
            } else {
                // Modified in different way -> *** MERGE CONFLICT ***
                System.out.println("Encountered a merge conflict.");
                // overwrite the file with conflict message, then stage it for addition
                writeContents(join(CWD, fileName),
                        getConflictMessage(fileName, current, other));
                add(fileName);
            }
        } else if (!presentInSplitPoint && presentInCurrent && !presentInOther) {
            // Not in SPLIT_POINT nor OTHER but in CURRENT -> CURRENT
            assert true;
        } else if (!presentInSplitPoint && !presentInCurrent && presentInOther) {
            // Not in SPLIT_POINT nor CURRENT but in OTHER -> OTHER
            if (!otherMap.containsKey(fileName)) {
                rm(fileName);
            } else {
                writeContents(join(CWD, fileName), other.getFileContent(fileName));
                add(fileName);
            }
        } else if (!modifiedInCurrent && !presentInOther) {
            // Unmodified in CURRENT but not present in OTHER -> DELETE FILE

            // remove the file and stage it for removal
            rm(fileName);
        } else if (!modifiedInOther && !presentInCurrent) {
            // Unmodified in OTHER but not present in CURRENT -> REMAIN DELETED
            assert true;
        }
    }

    /**
     * Find split point, which is the latest common ancestor of the current and other branch
     */
    public static Commit findSplitPoint(Commit current, Commit other) {
        Set<String> s = new HashSet<>();
        Queue<String> q = new LinkedList<>();

        String currentSHA = current.getSHA();
        String otherSHA = other.getSHA();
        q.add(currentSHA);
        q.add(otherSHA);

        while (!q.isEmpty()) {
            Commit cmt = Commit.getCommitByID(q.remove());
            boolean success = s.add(cmt.getSHA());
            if (!success) {
                return cmt;
            }
            for (String parentSHA : cmt.adj()) {
                q.add(parentSHA);
            }
        }
        return null;
    }
}
