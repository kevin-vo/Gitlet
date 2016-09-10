package gitlet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.Test;
import ucb.junit.textui;
/**
 * Tests of the Gitlet system.
 * @author Kevin Vo & Christian Lista Nicoloso
 */
public class GitletTest {
    @Test
    public void testInitialize() {
        clearDirectory(".gitlet");
        new File(".gitlet").delete();
        assertTrue(!(new File(".gitlet").exists()));
        command("java", "gitlet.Main", "init");
        assertTrue(new File(".gitlet/stage").exists());
        assertTrue(new File(".gitlet/data").exists());
        assertTrue(new File(".gitlet/commits").exists());
        assertEquals(
                "A gitlet version-control system already exists in the"
                + " current directory.\n",
                command("java", "gitlet.Main", "init"));
        clearDirectory(".gitlet");
        new File(".gitlet").delete();
    }

    @Test
    public void testAddAndCommitMessages() {
        clearDirectory(".gitlet");
        new File(".gitlet").delete();
        byte[] contentsTest1 = "Small Step".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest1);
        assertTrue(new File("test1.txt").exists());
        command("java", "gitlet.Main", "init");
        command("java", "gitlet.Main", "add", "test1.txt");
        assertTrue(new File(".gitlet/stage/test1.txt").exists());
        assertEquals("Please enter a commit message.\n", command("java",
                "gitlet.Main", "commit", ""));
        command("java", "gitlet.Main", "commit", "commit1");
        assertTrue(new File(".gitlet/stage").list().length == 0);
        Gitlet g = Gitlet.thisSystem();
        assertTrue((g.getHead().contains(new File("test1.txt"))));
        String commitID = g.getHead().getValue();
        assertTrue(new File(".gitlet/commits/" + commitID + "/test1.txt")
                .exists());
        assertEquals("No changes added to the commit.\n", command("java",
                "gitlet.Main", "commit", "commit 2"));
        byte[] contentsTest2 = "Small Step for man".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest2);
        command("java", "gitlet.Main", "commit", "commit2");
        command("java", "gitlet.Main", "add", "test1.txt");
        assertEquals("", command("java", "gitlet.Main", "commit", "commit 2"));
        (new File("test1.txt")).delete();
        clearDirectory(".gitlet");
        new File(".gitlet").delete();
    }

    @Test
    public void testAddCase() throws IOException {
        clearDirectory(".gitlet");
        clearWorkingDirectory();
        new File(".gitlet").delete();
        byte[] babyComeBack = "Blame it all on me".getBytes();
        Utils.writeContents(new File("test1.txt"), babyComeBack);
        command("java", "gitlet.Main", "init");
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit1");
        command("java", "gitlet.Main", "rm", "test1.txt");
        File whereAreYou = new File("test1.txt");
        File tracked = new File(".gitlet/test1.txt");
        assertTrue(!whereAreYou.exists());
        assertTrue(!tracked.exists());
        Utils.writeContents(new File("test1.txt"), babyComeBack);
        command("java", "gitlet.Main", "add", "test1.txt");
        assertTrue(tracked.exists());
        File noNeedToStage = new File(".gitlet/stage/test1.txt");
        assertTrue(!noNeedToStage.exists());
        whereAreYou.delete();
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    @Test
    public void testRemove() throws IOException {
        clearWorkingDirectory();
        clearDirectory(".gitlet");
        new File(".gitlet").delete();
        byte[] contentsTest1 = "Small step for man".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest1);
        command("java", "gitlet.Main", "init");
        assertEquals("No reason to remove the file.\n", command("java",
                "gitlet.Main", "rm", "test1.txt"));
        command("java", "gitlet.Main", "add", "test1.txt");
        assertTrue(new File(".gitlet/stage/test1.txt").exists());
        command("java", "gitlet.Main", "rm", "test1.txt");
        assertTrue(!new File(".gitlet/stage/test1.txt").exists());
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit1");
        Gitlet f = Gitlet.thisSystem();
        assertTrue(new File(".gitlet/commits/" + f.getHead().getValue()
                + "/test1.txt").exists());
        command("java", "gitlet.Main", "rm", "test1.txt");
        command("java", "gitlet.Main", "commit", "removed file");
        Gitlet g = Gitlet.thisSystem();
        assertEquals(0, new File(".gitlet/commits/" + g.getHead().getValue())
                .listFiles().length);
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    @Test
    public void testMerge() throws IOException {
        clearDirectory(".gitlet");
        clearWorkingDirectory();
        new File(".gitlet").delete();
        byte[] contentsTest1 = "Small step for man".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest1);
        byte[] contentsFiller = "only useful for second commit".getBytes();
        Utils.writeContents(new File("ignoreme.txt"), contentsFiller);
        command("java", "gitlet.Main", "init");
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit1");
        command("java", "gitlet.Main", "branch", "newBranch");
        command("java", "gitlet.Main", "add", "ignoreme.txt");
        command("java", "gitlet.Main", "commit", "commit2");
        command("java", "gitlet.Main", "checkout", "newBranch");
        byte[] contentsTest2 = "I like burgers".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest2);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit3");
        command("java", "gitlet.Main", "merge", "master");
        byte[] unchanged = Utils.readContents(new File("test1.txt"));
        assertArrayEquals(contentsTest2, unchanged);
        command("java", "gitlet.Main", "checkout", "master");
        byte[] revert = Utils.readContents(new File("test1.txt"));
        assertArrayEquals(contentsTest1, revert);
        new File(".gitlet").delete();
        new File("test1.txt").delete();
        new File("ignoreme.txt").delete();
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    @Test
    public void testMerge2() throws IOException {
        clearDirectory(".gitlet");
        clearWorkingDirectory();
        new File(".gitlet").delete();
        byte[] contentsTest1 = "I am hungry.".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest1);
        byte[] contentsTest2 = "Oh so very hungry.".getBytes();
        Utils.writeContents(new File("test2.txt"), contentsTest2);
        command("java", "gitlet.Main", "init");
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "add", "test2.txt");
        command("java", "gitlet.Main", "commit", "two files");
        command("java", "gitlet.Main", "branch", "other");
        byte[] contentsTest3 = "I like burgers".getBytes();
        Utils.writeContents(new File("test3.txt"), contentsTest3);
        command("java", "gitlet.Main", "add", "test3.txt");
        command("java", "gitlet.Main", "rm", "test2.txt");
        command("java", "gitlet.Main", "commit", "add 3, rm 2");
        command("java", "gitlet.Main", "checkout", "other");
        command("java", "gitlet.Main", "rm", "test1.txt");
        byte[] contentsTest4 = "I also like pizza".getBytes();
        Utils.writeContents(new File("test4.txt"), contentsTest4);
        command("java", "gitlet.Main", "add", "test4.txt");
        command("java", "gitlet.Main", "commit", "add 4, rm 1");
        command("java", "gitlet.Main", "checkout", "master");
        command("java", "gitlet.Main", "merge", "other");
        assertTrue(!new File("test1.txt").exists());
        assertTrue(!new File("test2.txt").exists());
        assertArrayEquals(contentsTest3, Utils.readContents(
                new File("test3.txt")));
        assertArrayEquals(contentsTest4, Utils.readContents(
                new File("test4.txt")));
        new File("test3.txt").delete();
        new File("test4.txt").delete();
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    @Test
    public void testMergeWithRm() throws IOException {
        clearDirectory(".gitlet");
        clearWorkingDirectory();
        new File(".gitlet").delete();
        byte[] contentsTest1 = "Small step for man".getBytes();
        Utils.writeContents(new File("test1.txt"), contentsTest1);
        command("java", "gitlet.Main", "init");
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit1");
        command("java", "gitlet.Main", "branch", "newBranch");
        command("java", "gitlet.Main", "rm", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit2");
        command("java", "gitlet.Main", "checkout", "newBranch");
        byte[] contentsFiller = "only useful for third commit".getBytes();
        Utils.writeContents(new File("ignoreme.txt"), contentsFiller);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "add", "ignoreme.txt");
        command("java", "gitlet.Main", "commit", "commit3");
        command("java", "gitlet.Main", "merge", "master");
        File deleted = new File("test1.txt");
        File stayed = new File("ignoreme.txt");
        assertTrue(!deleted.exists());
        assertTrue(stayed.exists());
        stayed.delete();
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    @Test
    public void testMergeConflict() throws IOException {
        clearDirectory(".gitlet");
        clearWorkingDirectory();
        new File(".gitlet").delete();
        byte[] contentsTest1 = "Small step for man\n".getBytes();
        byte[] contentsTest2 = "One giant leap\n".getBytes();
        byte[] contentsTest3 = "for mankind.\n".getBytes();
        byte[] output = ("<<<<<<< HEAD\nfor mankind.\n======="
                + "\nOne giant leap\n>>>>>>>\n").getBytes();
        command("java", "gitlet.Main", "init");
        Utils.writeContents(new File("test1.txt"), contentsTest1);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit1");
        command("java", "gitlet.Main", "branch", "newBranch");
        Utils.writeContents(new File("test1.txt"), contentsTest2);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit2");
        command("java", "gitlet.Main", "checkout", "newBranch");
        Utils.writeContents(new File("test1.txt"), contentsTest3);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit3");
        command("java", "gitlet.Main", "merge", "master");
        assertArrayEquals(output, Utils.readContents(new File("test1.txt")));
        new File("test1.txt").delete();
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    @Test
    public void testMergeConflict2() throws IOException {
        clearDirectory(".gitlet");
        clearWorkingDirectory();
        new File(".gitlet").delete();
        byte[] filler = "dummy file for commit\n".getBytes();
        byte[] contentsTest2 = "One giant leap\n".getBytes();
        byte[] contentsTest3 = "for mankind.\n".getBytes();
        byte[] output = ("<<<<<<< HEAD\nfor mankind.\n======="
                + "\nOne giant leap\n>>>>>>>\n").getBytes();
        command("java", "gitlet.Main", "init");
        Utils.writeContents(new File("ignoreMe.txt"), filler);
        command("java", "gitlet.Main", "add", "ignoreMe.txt");
        command("java", "gitlet.Main", "commit", "commit1");
        command("java", "gitlet.Main", "branch", "newBranch");
        Utils.writeContents(new File("test1.txt"), contentsTest2);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit2");
        command("java", "gitlet.Main", "checkout", "newBranch");
        Utils.writeContents(new File("test1.txt"), contentsTest3);
        command("java", "gitlet.Main", "add", "test1.txt");
        command("java", "gitlet.Main", "commit", "commit3");
        command("java", "gitlet.Main", "merge", "master");
        assertArrayEquals(output, Utils.readContents(new File("test1.txt")));
        new File("test1.txt").delete();
        new File("ignoreMe.txt").delete();
        clearDirectory(".gitlet");
        restoreWorkingDirectory();
    }

    /**
     * Processes command from ARGS.
     */
    private static String command(String... args) {
        try {
            StringBuilder results = new StringBuilder();
            Process p = Runtime.getRuntime().exec(args);
            p.waitFor();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p
                    .getInputStream()));) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    results.append(line).append(System.lineSeparator());
                }
                return results.toString();
            }
        } catch (IOException e) {
            return e.getMessage();
        } catch (InterruptedException e) {
            return e.getMessage();
        }
    }

    /**
     * Deletes every file located in directory PATH INCLUDING sub-directories.
     * DANGEROUS!!!
     */
    private static void clearDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            return;
        }
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                file.delete();
            } else {
                clearDirectory(file.getPath());
            }
        }
        if (directory.isDirectory()) {
            directory.delete();
        }
    }

    /**
     * Clear working directory and moves contents to a folder "backup".
     */
    private static void clearWorkingDirectory() {
        File backup = new File("backup");
        backup.mkdir();
        File repo = new File(".");
        for (File file : repo.listFiles()) {
            if (file.isFile()) {
                byte[] bytes = Utils.readContents(file);
                Utils.writeContents(new File("backup/" + file.getName()),
                        bytes);
                file.delete();
            }
        }
    }

    /**
     * Restores the working directory from files in "backup". Deletes the
     * "backup" directory afterwards.
     */
    private static void restoreWorkingDirectory() {
        File backup = new File("backup");
        if (!backup.exists()) {
            return;
        }
        for (File file : backup.listFiles()) {
            byte[] bytes = Utils.readContents(file);
            Utils.writeContents(new File(file.getName()), bytes);
            file.delete();
        }
        backup.delete();
    }


    public static void main(String... args) {
        System.exit(textui.runClasses(GitletTest.class));
    }
}
