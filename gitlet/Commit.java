package gitlet;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.io.Serializable;

/**
 * Commit objects that will be chained together, representing the history of the
 * files.
 *
 * @author Kevin Vo and Christian Lista-Nicoloso
 */
public class Commit implements Serializable {

    /**
     * Create a new Commit Object with the message LOG and that points to
     * PARENT.
     */
    public Commit(String log, Commit parent) {
        if (log.isEmpty()) {
            System.out.println("Please enter a commit message.");
        }
        _log = log;
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.now();
        _date = dateTime.format(formatter);
        _parent = parent;
        _value = Utils.sha1(_log, _date);
        _commitDirectory = new File(".gitlet/commits/" + _value);
        _commitDirectory.mkdirs();
        updateFiles();
    }

    /** The unique directory for this commit. */
    private File _commitDirectory;

    /**
     * Sets the files of this commit to the correct version of the file, taking
     * from the last commit or the stage as necessary.
     */
    private void updateFiles() {
        copyStage();
        if (_parent != null) {
            for (File file : new File(".gitlet").listFiles()) {
                if (file.isFile()) {
                    _files.put(file.getName(),
                            Utils.sha1(Utils.readContents(file)));
                    Utils.writeContents(new File(_commitDirectory.getPath()
                            + "/" + file.getName()), Utils.readContents(file));
                }
            }
        } else {
            _files = new HashMap<String, String>();
        }
    }

    /**
     * Copies the stage directory and at the same time, clears it. Also sends
     * files to .gitlet for tracking.
     */
    private void copyStage() {
        File stage = new File(".gitlet/stage");
        for (File file : stage.listFiles()) {
            File copy = new File(".gitlet/" + file.getName());
            Utils.writeContents(copy, Utils.readContents(file));
            file.delete();
        }
    }

    /**
     * Returns true if FILE is contained in this Commit.
     */
    public boolean contains(File file) {
        if (_files == null) {
            return false;
        }
        String key = file.getName();
        return _files.containsKey(key)
                && _files.get(key).equals(Utils.sha1(Utils.readContents(file)));
    }

    /**
     * Return the files this commit.
     */
    public HashMap<String, String> getFiles() {
        return _files;
    }

    /**
     * Prints out the log for this commit.
     */
    public void printLog() {
        System.out.println("===");
        System.out.printf("Commit %s", _value);
        System.out.println("");
        System.out.println(_date);
        System.out.println(_log);
        System.out.println("");
    }

    /**
     * Return the log message of this commit.
     */
    public String getLog() {
        return _log;
    }

    /**
     * Return the parent of this commit.
     */
    public Commit getParent() {
        return _parent;
    }

    /**
     * Return the Date of this commit.
     */
    public String getDate() {
        return _date;
    }

    /**
     * Return the value of this commit.
     */
    public String getValue() {
        return _value;
    }

    /**
     * The value of this commit.
     */
    private String _value;

    /**
     * The message associated with this commit.
     */
    private String _log;

    /**
     * The date this commit was made.
     */
    private String _date;

    /**
     * The date this commit was made.
     */
    private HashMap<String, String> _files = new HashMap<String, String>();

    /**
     * This commit's parent.
     */
    private Commit _parent;
}
