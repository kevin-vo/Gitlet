package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Kevin Vo and Christian Lista-Nicoloso
 */
public class Gitlet implements Serializable {
    /**
     * The list of all commits ever made.
     */
    private HashMap<String, Commit> _allCommitsEverMade =
            new HashMap<String, Commit>();

    /**
     * The map of files removed.
     */
    private HashMap<String, String> _rmFiles = new HashMap<String, String>();

    /**
     * The map of all branches.
     */
    private HashMap<String, Commit> _branches = new HashMap<String, Commit>();

    /**
     * The name of the current branch. Acts as a key for _branches.
     */
    private String _currentBranch;

    /**
     * This system's current commit.
     */
    private Commit _head;

    /**
     * The constructor for Gitlet.
     */
    public Gitlet() {
        _head = null;
    }

    /**
     * Runs this Gitlet system by constructing from a serialized file with
     * argument ARGS.
     */
    public static void main(String[] args) {
        Gitlet g = reconstruct();
        interpret(args, g);
        g.serialize();
    }

    /**
     * Execute one statement from the arguments ARGS stream with current GIT.
     */
    static void interpret(String[] args, Gitlet git) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        } else if (!isLegal(args[0], args)) {
            if (args[0].equals("commit") && args.length == 1) {
                System.out.println("Please enter a commit message.");
                return;
            }
            System.out.println("Incorrect operands.");
            return;
        } else if (isLegal(args[0], args) && !isInit()
                && !args[0].equals("init")) {
            System.out.println("Not in an initialized gitlet directory");
            return;
        }
        switch (args[0]) {
        case "init":
            git.initialize();
            return;
        case "add":
            git.add(args[1]);
            return;
        case "commit":
            git.commit(args);
            return;
        case "log":
            git.log();
            return;
        case "status":
            git.status();
            return;
        case "rm":
            git.remove(args[1]);
            return;
        case "branch":
            git.branch(args[1]);
            return;
        case "checkout":
            git.checkout(Arrays.copyOfRange(args, 1, args.length));
            return;
        case "global-log":
            git.globalLog();
            return;
        case "find":
            git.find(args[1]);
            return;
        case "rm-branch":
            git.removeBranch(args[1]);
            return;
        case "reset":
            git.reset(args[1]);
            return;
        case "merge":
            git.merge(args[1]);
            return;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    /**
     * Returns true if COMMAND has a legal amount of ARGS.
     */
    private static boolean isLegal(String command, String[] args) {
        switch (command) {
        case "init":
        case "log":
        case "global-log":
        case "status":
            return args.length == 1;
        case "add":
        case "commit":
        case "rm":
        case "find":
        case "branch":
        case "rm-branch":
        case "reset":
        case "merge":
            return args.length == 2;
        case "checkout":
            return args.length >= 2 && args.length <= 4;
        default:
        }
        return true;
    }

    /**
     * Returns the shared anscestor between COMMIT1 and COMMIT2.
     */
    private Commit getSplitPoint(Commit commit1, Commit commit2) {
        ArrayList<Commit> parents1 = new ArrayList<Commit>();
        Commit pointer = commit1.getParent();
        while (pointer != null) {
            parents1.add(pointer);
            pointer = pointer.getParent();
        }
        pointer = commit2.getParent();
        int i = 0;
        while (pointer != null || i == parents1.size() - 1) {
            if (pointer == commit1) {
                return commit1;
            }
            if (pointer.getValue() == parents1.get(i).getValue()) {
                return pointer;
            }
            pointer = pointer.getParent();
            i += 1;
        }
        return null;
    }

    /**
     * Returns a byte[] that formats conflicting merges from X and Y.
     */
    private byte[] formatMerge(byte[] x, byte[] y) {
        byte[] nL = new byte[] { 10 };
        byte[] head = "<<<<<<< HEAD".getBytes();
        byte[] middle = "=======".getBytes();
        byte[] tail = ">>>>>>>".getBytes();
        byte[][] bytes = null;
        if (x == null) {
            bytes = new byte[][] { head, nL, middle, nL, y,
                tail, nL };
        } else if (y == null) {
            bytes = new byte[][] { head, nL, x, middle, nL,
                tail, nL };
        } else {
            bytes = new byte[][] { head, nL, x, middle, nL, y,
                tail, nL };
        }
        return Utils.concatenate(bytes);
    }

    /**
     * Writes a conflict file into the working directory from FILE1 and FILE2
     * to FILENAME.
     */
    private void writeConflict(String fileName, File file1, File file2) {
        byte[] currB = null;
        byte[] targB = null;
        if (file1 != null) {
            currB = Utils.readContents(file1);
        }
        if (file2 != null) {
            targB = Utils.readContents(file2);
        }
        Utils.writeContents(new File(fileName), formatMerge(currB, targB));
    }

    /**
     * Handles appropriate outputs when merging TARGETBRANCH.
     */
    private void merge(String targetBranch) {
        if (hasUntracked()) {
            System.out.println("There is an untracked file in the way; "
                + "delete it or add it first.");
            return;
        }
        File stage = new File(".gitlet/stage");
        if (stage.listFiles().length > 0 || _rmFiles.size() > 0) {
            System.out.println("You have uncommitted changes.");
        }
        if (!_branches.containsKey(targetBranch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (_currentBranch.equals(targetBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit targetCommit = _branches.get(targetBranch);
        Commit splitPoint = getSplitPoint(_head, targetCommit);
        if (splitPoint == _branches.get(targetCommit)) {
            System.out.println("Given branch is an ancestor of the current "
                + "branch.");
            return;
        }
        if (splitPoint == _head) {
            _head = targetCommit;
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (mergeHelper(targetBranch)) {
            System.out.println("Encountered a merge conflict.");
        } else {
            String log = "Merged " + _currentBranch + " with "
                + targetBranch + ".";
            commit(new String[] {"commit", log});
        }
    }

    /**
     * Merges files from TARGETBRANCH and returns whether there was a conflict.
     */
    private boolean mergeHelper(String targetBranch) {
        Commit targetHead = _branches.get(targetBranch);
        Commit splitPoint = getSplitPoint(_head, targetHead);
        HashMap<String, String> splitFiles = splitPoint.getFiles();
        HashMap<String, String> targetFiles = targetHead.getFiles();
        HashMap<String, String> currentFiles = _head.getFiles();
        boolean conflict = false;
        for (String fileName : targetFiles.keySet()) {
            if (!currentFiles.containsKey(fileName)) {
                if (!splitFiles.containsKey(fileName)) {
                    fileCheckout(targetHead.getValue(), fileName);
                    add(fileName);
                } else if (!splitFiles.get(fileName).equals(targetFiles.get(
                    fileName))) {
                    File targ = new File(".gitlet/commits/"
                        + targetHead.getValue() + "/" + fileName);
                    writeConflict(fileName, null, targ);
                }
            } else if (splitFiles.containsKey(fileName)
                        && currentFiles.containsKey(fileName)) {
                if (splitFiles.get(fileName).equals(
                        currentFiles.get(fileName))) {
                    fileCheckout(targetHead.getValue(), fileName);
                    add(fileName);
                }
            }
        }
        for (String fileName : currentFiles.keySet()) {
            File curr = new File(".gitlet/commits/"
                        + _head.getValue() + "/" + fileName);
            if (splitFiles.containsKey(fileName)
                    && !targetFiles.containsKey(fileName)) {
                if (splitFiles.get(fileName).equals(currentFiles.get(
                        fileName))) {
                    new File(fileName).delete();
                    new File(".gitlet/" + fileName).delete();
                } else {
                    writeConflict(fileName, curr, null);
                    conflict = true;
                }
            } else if ((splitFiles.containsKey(fileName)
                    && targetFiles.containsKey(fileName)
                    && !splitFiles.get(fileName).equals(
                        targetFiles.get(fileName))
                    && !currentFiles.get(fileName).equals(
                            targetFiles.get(fileName))
                    && !splitFiles.get(fileName).equals(
                            currentFiles.get(fileName)))
                    || (!splitFiles.containsKey(fileName)
                    && targetFiles.containsKey(fileName)
                    && !currentFiles.get(fileName).equals(
                            targetFiles.get(fileName)))) {
                File targ = new File(".gitlet/commits/"
                        + targetHead.getValue() + "/" + fileName);
                writeConflict(fileName, curr, targ);
                conflict = true;
            }
        }
        return conflict;
    }

    /**
     * Returns whether a working directory already exists.
     */
    private static boolean isInit() {
        File file = new File(".gitlet");
        return file.exists();
    }

    /**
     * Returns true if a working FILE is untracked in the working directory and
     * would be overwritten by the checkout of COMMIT. Prints out corresponding
     * error message.
     */
    private boolean hasOverwrite(Commit commit) {
        File repo = new File(".");
        String msg = "There is an untracked file in the way; delete it or add"
                + " it first.";
        for (String fileName : repo.list()) {
            if (new File(fileName).isDirectory()) {
                continue;
            }
            if (!new File(".gitlet/" + fileName).exists()
                    && commit.getFiles().containsKey(fileName)
                    && !new File(".gitlet/stage/" + fileName).exists()) {
                String fileVal = Utils.sha1(Utils.readContents(new File(
                        fileName)));
                if (!fileVal.equals(commit.getFiles().get(fileName))) {
                    System.out.println(msg);
                    return true;
                }
            }
            if (!new File(".gitlet/" + fileName).exists()
                    && !commit.getFiles().containsKey(fileName)
                    && !new File(".gitlet/stage/" + fileName).exists()) {
                System.out.println(msg);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there is an untracked and unstaged file in the current
     * commit.
     */
    private boolean hasUntracked() {
        File repo = new File(".");
        for (File file : repo.listFiles()) {
            if (new File(".gitlet/stage/" + file.getName()).exists()) {
                continue;
            }
            if (!file.isDirectory() && !_head.contains(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks out all files tracked by given commit ID and removes tracked files
     * that are not present in the given file.
     */
    private void reset(String id) {
        String fullID = getID(id);
        if (fullID == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit targetCommit = _allCommitsEverMade.get(fullID);
        if (hasOverwrite(targetCommit)) {
            return;
        }
        clearDirectory(".");
        for (File file : new File(".gitlet/stage").listFiles()) {
            Utils.writeContents(new File(file.getName()),
                Utils.readContents(file));
        }
        clearDirectory(".gitlet/stage");
        clearDirectory(".gitlet");
        _rmFiles.clear();
        for (String fileName : targetCommit.getFiles().keySet()) {
            fileCheckout(id, fileName);
        }
        _head = targetCommit;
    }

    /** Removes all files from DIR and ignores directories. */
    private void clearDirectory(String dir) {
        File directory = new File(dir);
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Removes the BRANCH from branches.
     */
    private void removeBranch(String branch) {
        if (!_branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        _branches.remove(branch);
    }

    /**
     * Prints out the log for all commits with LOG.
     */
    private void find(String log) {
        int count = 0;
        for (Commit commit : _allCommitsEverMade.values()) {
            if (commit.getLog().equals(log)) {
                System.out.println(commit.getValue());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Prints out the log for all commits ever made.
     */
    private void globalLog() {
        for (Commit commit : _allCommitsEverMade.values()) {
            commit.printLog();
        }
    }

    /**
     * Switches to branch NAME.
     */
    private void checkout(String[] name) {
        if (name.length == 1) {
            branchCheckout(name[0]);
        } else if (name.length == 2) {
            if (!name[0].equals("--")) {
                System.out.println("Incorrect operands.");
            } else {
                fileCheckout(name[1]);
            }
        } else if (name.length == 3) {
            if (!name[1].equals("--")) {
                System.out.println("Incorrect operands.");
            } else {
                fileCheckout(name[0], name[2]);
            }
        }
    }

    /** Checks out branch NAME. */
    private void branchCheckout(String name) {
        if (!_branches.containsKey(name)) {
            System.out.println("No such branch exists.");
            return;
        } else if (_currentBranch.equals(name)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Commit tempBranch = _branches.get(name);
        if (hasOverwrite(tempBranch)) {
            return;
        }
        clearDirectory(".");
        clearDirectory(".gitlet");
        clearDirectory(".gitlet/stage");
        File commitDirectory = new File(".gitlet/commits/"
                + tempBranch.getValue());
        for (String fileName : commitDirectory.list()) {
            File file = new File(".gitlet/commits/" + tempBranch.getValue()
                    + "/" + fileName);
            byte[] bytes = Utils.readContents(file);
            Utils.writeContents(new File(fileName), bytes);
            Utils.writeContents(new File(".gitlet/" + fileName) , bytes);
        }
        _currentBranch = name;
        _head = tempBranch;
    }

    /**
     * Returns the full id of the commit in the current gitlet system from
     * given ID. Returns null if no commit matches such ID.
     */
    private String getID(String id) {
        if (id.length() < 6) {
            return null;
        }
        for (String full : _allCommitsEverMade.keySet()) {
            if (subIsFull(id, full)) {
                return full;
            }
        }
        return null;
    }

    /** Helper for getID. Returns true if SUB is an abbreviation of FULL.*/
    private boolean subIsFull(String sub, String full) {
        return sub.equals(full.substring(0, sub.length()));
    }

    /** Checks out file NAME. */
    private void fileCheckout(String name) {
        if (!_head.getFiles().containsKey(name)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        byte[] bytes = Utils.readContents(new File(".gitlet/commits/"
                + _head.getValue() + "/" + name));
        Utils.writeContents(new File(name), bytes);
    }

    /** Checks out file NAME fom commit ID. */
    private void fileCheckout(String id, String name) {
        String fullID = getID(id);
        if (fullID == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit targetCommit = _allCommitsEverMade.get(fullID);
        if (!targetCommit.getFiles().containsKey(name)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        byte[] bytes = Utils.readContents(new File(".gitlet/commits/"
                + targetCommit.getValue() + "/" + name));
        Utils.writeContents(new File(name), bytes);

    }

    /**
     * Creates branch NAME at the current _head.
     */
    private void branch(String name) {
        if (_branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
        } else {
            _branches.put(name, _head);
        }
    }

    /**
     * Removes FILE and adds to _rmFiles.
     */
    private void remove(String file) {
        File stageFile = new File(".gitlet/stage/" + file);
        File trackedFile = new File(".gitlet/" + file);
        File repoFile = new File(file);
        boolean valid = false;
        if (stageFile.exists()) {
            stageFile.delete();
            valid = true;
        }
        if (trackedFile.exists()) {
            _rmFiles.put(file, Utils.sha1(Utils.readContents(trackedFile)));
            trackedFile.delete();
            repoFile.delete();
            valid = true;
        }
        if (!valid) {
            System.out.println("No reason to remove the file.");
        }
    }

    /**
     * Gets the gitlet system from the directory. Returns updated gitlet system.
     */
    private static Gitlet reconstruct() {
        Gitlet git;
        if (!(new File(".gitlet/data/data").exists())) {
            return new Gitlet();
        }
        File inFile = new File(".gitlet/data/data");
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(
                    inFile));
            git = (Gitlet) inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            System.out.println("can't read");
            git = null;
        }
        return git;
    }

    /**
     * Serializes the gitlet system and writes it into the file.
     */
    private void serialize() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(new File(".gitlet/data/data")));
            out.writeObject(this);
            out.close();
        } catch (IOException excp) {
            return;
        }
    }

    /**
     * Initialize the system aka create the .gitlet directory. Returns new
     * gitlet system.
     */
    public Gitlet initialize() {
        if (new File(".gitlet/stage").exists()) {
            System.out.println("A gitlet version-control system already exists"
                    + " in the current directory.");
            return null;
        } else {
            createDirectories();
            Commit initial = new Commit("initial commit", null);
            _branches.put("master", initial);
            _allCommitsEverMade.put(initial.getValue(), initial);
            _currentBranch = "master";
            _head = _branches.get("master");
            return this;
        }
    }

    /**
     * Create the directories needed to hold the system.
     */
    private void createDirectories() {
        new File(".gitlet/stage").mkdirs();
        new File(".gitlet/data").mkdirs();
        new File(".gitlet/commits").mkdirs();
    }

    /**
     * Add file FILENAME to the stage.
     */
    public void add(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            if (!file.isDirectory() && _rmFiles.containsKey(fileName)) {
                byte[] bytes = Utils.readContents(file);
                if (!_rmFiles.get(fileName).equals(Utils.sha1(bytes))) {
                    Utils.writeContents(new File(".gitlet/stage/" + fileName),
                        bytes);
                }
                Utils.writeContents(new File(".gitlet/" + fileName),
                    bytes);
                _rmFiles.remove(fileName);
            }
            File repoFile = new File(fileName);
            File trackedFile = new File(".gitlet/" + fileName);
            if (!file.isDirectory()) {
                if (trackedFile.exists()) {
                    String rVal = Utils.sha1(Utils.readContents(repoFile));
                    String tVal = Utils.sha1(Utils.readContents(trackedFile));
                    if (rVal.equals(tVal)) {
                        return;
                    }
                }
                byte[] bytes = Utils.readContents(file);
                Utils.writeContents(new File(".gitlet/stage/" + fileName),
                        bytes);
            }
        } else {
            System.out.println("File does not exist.");
        }
    }

    /**
     * Make a commit with the message ARGS.
     */
    public void commit(String[] args) {
        if (args[1].length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }
        File stage = new File(".gitlet/stage");
        if (stage.list().length == 0 && _rmFiles.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit newCommit = new Commit(args[1], _head);

        _head = newCommit;
        _branches.put(_currentBranch, _head);
        _allCommitsEverMade.put(newCommit.getValue(), newCommit);
        _rmFiles.clear();
    }

    /**
     * Print this system's commit's logs.
     */
    public void log() {
        Commit pointer = _head;
        while (pointer != null) {
            pointer.printLog();
            pointer = pointer.getParent();
        }
    }

    /**
     * Print what branches currently exist, current branch, staged files,
     * untracked files, and unstaged modifications.
     */
    private void status() {
        File stage = new File(".gitlet/stage");
        File[] stageFiles = stage.listFiles();
        Arrays.sort(stageFiles);
        File tracked = new File(".gitlet");
        File[] trackedFiles = tracked.listFiles();
        Arrays.sort(trackedFiles);
        File repo = new File(".");
        File[] repoFiles = repo.listFiles();
        Arrays.sort(repoFiles);
        System.out.println("=== Branches ===");
        printBranches();
        System.out.println("\n=== Staged Files ===");
        for (File file : stageFiles) {
            System.out.println(file.getName());
        }
        System.out.println("\n=== Removed Files ===");
        Object[] removedFiles = _rmFiles.keySet().toArray();
        Arrays.sort(removedFiles);
        for (Object file : removedFiles) {
            System.out.println(file);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (File file : stageFiles) {
            if (!new File(file.getName()).exists()) {
                System.out.println(file.getName() + " (deleted)");
            } else {
                String repoVal = Utils.sha1(Utils.readContents(new File(file
                        .getName())));
                String stageVal = Utils.sha1(Utils.readContents(file));
                if (!repoVal.equals(stageVal)) {
                    System.out.println(file.getName() + " (modified)");
                }
            }
        }
        for (File file : trackedFiles) {
            if (new File(file.getName()).exists()) {
                String repoVal = Utils.sha1(Utils.readContents(new File(file
                        .getName())));
                String stageVal = Utils.sha1(Utils.readContents(file));
                if (!repoVal.equals(stageVal)) {
                    System.out.println(file.getName() + " (modified)");
                }
            } else if (file.isFile()) {
                System.out.println(file.getName() + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (File file : repoFiles) {
            if (file.isDirectory()) {
                continue;
            }
            File curr = new File(".gitlet/" + file.getName());
            File curr2 = new File(".gitlet/stage/" + file.getName());
            if (!curr.exists() && !curr2.exists()) {
                System.out.println(curr.getName());
            }
        }
    }

    /**
     * Prints out existing branches and marks the current one.
     */
    private void printBranches() {
        Object[] branches = _branches.keySet().toArray();
        Arrays.sort(branches);
        for (Object branch : branches) {
            if (branch.equals(_currentBranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
    }

    /**
     * Return the head of this system..
     */
    public Commit getHead() {
        return _head;
    }

    /**
     * Return the current branch.
     */
    public String getCurrentBranch() {
        return _currentBranch;
    }

    /**
     * Return branches this system.
     */
    public HashMap<String, Commit> getBranches() {
        return _branches;
    }

    /**
     * Return files marked for removal on this system.
     */
    public HashMap<String, String> getRmFiles() {
        return _rmFiles;
    }

    /**
     * Return all commits ever made on this system.
     */
    public HashMap<String, Commit> getAllCommitsEverMade() {
        return _allCommitsEverMade;
    }

    /**
     * Return the system in this directory.
     */
    public static Gitlet thisSystem() {
        Gitlet g = reconstruct();
        return g;
    }

}
