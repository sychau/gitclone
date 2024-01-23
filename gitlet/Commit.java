package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *
 *  A commit will have at most 2 parents because gitlet only allow merging with 2 branches
 *  Same commits refers to having the same metadata, the same mapping of names to reference,
 *  and the same parent reference. Therefore, commits are content addressable.
 *  Commit is immutable. They should not be changed after creation (put entry into fileMap).
 *
 *  @author sychau
 */

public class Commit implements Serializable, Dumpable {
    /** List all instance variables of the Commit class here with a useful
     *  comment above them describing what that variable represents and how that
     *  variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** The timestamp to record when this Commit is made. */
    private Date timestamp;

    /** A mapping of file names to blob references (UID). */
    private Map<String, String> fileMap;

    /** A String containing UID of the parent of this Commit. */
    private String parent;

    /** A String containing UID of the second parent of this Commit (merging). */
    private String secondParent;


    /** Initial Commit constructor*/
    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0); // The Unix epoch
        // Tree map is sorted map, insertion order does not affect serialization and SHA-1.
        this.fileMap = new TreeMap<>();
        this.parent = null;
        this.secondParent = null;
    }

    /** Regular Commit constructor */
    public Commit(String message, String parent, String secondParent) {
        this.message = message;
        this.timestamp = new Date(); // Get current time

        this.fileMap = getCommitByID(parent).getFileMapCopy();
        this.parent = parent;
        this.secondParent = secondParent;
    }

    /** Return file map stored in this commit */
    public Map<String, String> getFileMap() {
        return fileMap;
    }

    /** Return a deep copy of fileMap */
    public Map<String, String> getFileMapCopy() {
        return new TreeMap<>(this.fileMap);
    }

    /** Return SHA-1 of the commit */
    public String getSHA() {
        return sha1((Object) serialize(this));
    }

    /** Return parent of the commit*/
    public String getParent() {
        return this.parent;
    }

    /** Return parent of the message*/
    public String getMessage() {
        return this.message;
    }

    /** Return content of the file named FILENAME tracked by THIS commit */
    public String getFileContent(String fileName) {
        String blobID = fileMap.get(fileName);
        if (blobID == null) {
            return null;
        }
        File target = join(Repository.BLOBS_DIR, blobID);
        return readContentsAsString(target);
    }

    /** Print commit log */
    public void printLog() {
        SimpleDateFormat sdf = new SimpleDateFormat("E MMM d HH:mm:ss y Z", Locale.US);

        String cmtLog = "===\n"
                + String.format("commit %s\n", this.getSHA())
                + String.format("Date: %s\n", sdf.format(this.timestamp))
                + String.format("%s\n", this.message);

        System.out.println(cmtLog);
    }
    /** Return whether a file is stored in the commit snapshot */
    public boolean isFileKeyExists(String fileName) {
        return this.fileMap.containsKey(fileName);
    }

    /** Create a commit blob file inside .gitlet/objects/commits directory,
     *  The blob file is named by SHA1 of the commit, contains serialized commit: THIS */
    public void createCommitBlob() {
        String sha = this.getSHA();
        File blob = join(Repository.COMMITS_DIR, sha);
        writeObject(blob, this);
    }

    /** Return an iterable list including all parents (if exist) */
    public Iterable<String> adj() {
        LinkedList<String> L = new LinkedList<>();
        if (parent != null) {
            L.add(parent);
        }
        if (secondParent != null) {
            L.add(secondParent);
        }
        return L;
    }

    /** Return current commit, which is indicated by HEAD pointer */
    public static Commit getCurrentCommit() {
        String currBranchName = readContentsAsString(Repository.HEAD);
        File currBranch = join(Repository.BRANCHES_DIR, currBranchName);
        String currCommitSHA = readContentsAsString(currBranch);
        File currCommit = join(Repository.COMMITS_DIR, currCommitSHA);
        return readObject(currCommit, Commit.class);
    }

    /** Return commit by ID, assume the commitID exists */
    public static Commit getCommitByID(String commitID) {
        if (commitID == null) {
            return null;
        }
        File commit = join(Repository.COMMITS_DIR, commitID);
        return readObject(commit, Commit.class);
    }

    /** Return the latest commit by that branch name */
    public static Commit getCommitByBranchName(String branchName) {
        File branchFile = join(Repository.BRANCHES_DIR, branchName);
        String commitSHA = readContentsAsString(branchFile);
        File commitFile = join(Repository.COMMITS_DIR, commitSHA);
        return readObject(commitFile, Commit.class);
    }
    /** Dumpable interface for debugging */
    @Override
    public void dump() {
        System.out.printf("message: %s%ntimestamp: %s%nmap: %s%nparent: %s%n",
                message, timestamp, fileMap, parent);
    }
}
