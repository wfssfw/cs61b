package gitlet;

import javax.sound.midi.SysexMessage;
import javax.xml.parsers.SAXParser;
import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECT_DIR = join(GITLET_DIR, "objects");
    public static final File STAGE_DIR = join(GITLET_DIR, "stage");
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    public static final File REF_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REF_DIR, "heads");

    public static final File AddStageFile = join(STAGE_DIR, "add");
    public static final File RemoveStageFile = join(STAGE_DIR, "remove");

    public static Stage addStage = new Stage();
    public static Stage removeStage = new Stage();
    public static Commit curCommit;
    public static String curBranch;

    /******************************** init *****************************************/
    public static void init() {
        // 创建gitlet目录
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        GITLET_DIR.mkdir();
        OBJECT_DIR.mkdir();
        STAGE_DIR.mkdir();
        REF_DIR.mkdir();
        HEADS_DIR.mkdir();

        Commit commit = new Commit();
        curCommit = commit;
        commit.save();

        writeContents(join(HEADS_DIR, "master"), commit.getId());

        writeContents(HEAD_FILE, "master");
        curBranch = "master";
    }

    /********************************* add *****************************************/

    public static void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        Blob blob = new Blob(file);
        String path = file.getPath();
        String id = blob.getId();

        addStage = readFromAddStage();
        removeStage = readFromRemoveStage();
        curCommit = readCurrentCommit();

        if (addStage.isContainsKey(path)) {
            // 和stage area里不一致，复写
            if (!addStage.getPath2BolbId().get(path).equals(id)) {
                addStage.getPath2BolbId().put(path, id);
            }
            // 是否在curcommit里,若存在删去
            if (id.equals(curCommit.getPathblobIDs().get(path))) {
                addStage.getPath2BolbId().remove(path);
            }
        } else {
            addStage.getPath2BolbId().put(path, id);
        }
        saveAddStage();

        if (removeStage.getPath2BolbId().containsValue(id)) {
            removeStage.getPath2BolbId().remove(path);
            saveRemoveStage();
        } else {
            blob.save();
        }
    }

    private static Stage readFromAddStage() {
        if (!AddStageFile.exists()) {
            return new Stage();
        }
        return readObject(AddStageFile, Stage.class);
    }

    private static void saveAddStage() {
        writeContents(AddStageFile, addStage);
    }

    private static Stage readFromRemoveStage() {
        if (!RemoveStageFile.exists()) {
            return new Stage();
        }
        return readObject(RemoveStageFile, Stage.class);
    }

    private static void saveRemoveStage() {
        writeContents(RemoveStageFile, removeStage);
    }

    // 先读当前分支名字(HEAD_FILE), 再从HEADS_DIR找到该分支的当前CommitID
    // 再由ID从OBJECT_DIR中读取COMMIT
    private static Commit readCurrentCommit() {
        String currCommitID = readCurrCommitID();
        File CUR_COMMIT_FILE = join(OBJECT_DIR, currCommitID);
        return readObject(CUR_COMMIT_FILE, Commit.class);
    }

    private static String readCurrCommitID() {
        String curBranch = readCurrBranch();
        File HEAD_FILE = join(HEADS_DIR, curBranch);
        return readContentsAsString(HEAD_FILE);
    }

    private static String readCommitIDByBranch(String branchName) {
        File HEAD_FILE = join(HEADS_DIR, branchName);
        return readContentsAsString(HEAD_FILE);
    }

    private static String readCurrBranch() {
        return readContentsAsString(HEAD_FILE);
    }

    /********************************* commit **************************************/
    public static void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        // Create a new commit
        Map<String, String> addStageMap = new HashMap<>();
        Map<String, String> removeStageMap = new HashMap<>();
        addStageMap.putAll(addStage.getPath2BolbId());
        removeStageMap.putAll(removeStage.getPath2BolbId());

        if (addStageMap.isEmpty() && removeStageMap.isEmpty()) {
            System.out.println("No changes added to the commit.");
        }

        curCommit = readCurrentCommit();
        Map<String, String> finalMap = curCommit.getPathblobIDs();
        for (Map.Entry<String, String> entry : addStageMap.entrySet()) {
            finalMap.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : removeStageMap.entrySet()) {
            finalMap.remove(entry.getKey(), entry.getValue());
        }

        List<String> curParents = new ArrayList<>();
        curParents.add(curCommit.getId());
        Commit newCommit = new Commit(message, finalMap, curParents);

        // save the new commit
        newCommit.save();
        addStage.clear();
        saveAddStage();
        removeStage.clear();
        saveRemoveStage();

        curCommit = newCommit;
        String curBranch = readCurrBranch();
        File HEADS_FILE = join(HEADS_DIR, curBranch);
        writeObject(HEADS_FILE, curCommit.getId());
    }

    /*********************************** rm ****************************************/

    public static void rm(String filename) {
        File file = join(CWD, filename);
        Blob blob = new Blob(file);
        String path = blob.getFilePath();
        String id = blob.getId();

        addStage = readFromAddStage();
        removeStage = readFromRemoveStage();
        curCommit = readCurrentCommit();

        if (addStage.isContainsKey(path)) {
            addStage.getPath2BolbId().remove(path);
            saveAddStage();
        } else if (curCommit.getPathblobIDs().containsKey(path)) {
            removeStage.getPath2BolbId().put(path, id);
            saveRemoveStage();
            if (file.exists()) {
                file.delete();
            }
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /*********************************** log ***************************************/

    public static void log() {
        curCommit = readCurrentCommit();
        List<String> parents = curCommit.getParents();
        while (!parents.isEmpty()) {
            if (parents.size() == 2) {
                printMergeCommit(curCommit);
            } else {
                printCommit(curCommit);
            }
            curCommit = readCommitById(parents.get(0));
            parents = curCommit.getParents();
        }
        printCommit(curCommit);
    }

    private static void printCommit(Commit curCommit) {
        System.out.println("===");
        printCommitID(curCommit);
        printCommitDate(curCommit);
        printCommitMessage(curCommit);
    }

    private static void printMergeCommit(Commit curCommit) {
        System.out.println("===");
        printCommitID(curCommit);
        printMergeMark(curCommit);
        printCommitDate(curCommit);
        printCommitMessage(curCommit);
    }

    private static void printCommitID(Commit curCommmit) {
        System.out.println("commit " + curCommmit.getId());
    }

    private static void printMergeMark(Commit curCommmit) {
        List<String> parentsCommitID = curCommmit.getParents();
        String parent1 = parentsCommitID.get(0);
        String parent2 = parentsCommitID.get(1);
        System.out.println("Merge: " + parent1.substring(0, 7) + " " + parent2.substring(0, 7));
    }

    private static void printCommitDate(Commit currCommmit) {
        System.out.println("Date: " + currCommmit.getDate());
    }

    private static void printCommitMessage(Commit currCommmit) {
        System.out.println(currCommmit.getMessage() + "\n");
    }

    private static Commit readCommitById(String commitId) {
        // 为什么有id长度不等于40的情况?
        File CUR_COMMIT_FILE = join(OBJECT_DIR, commitId);
        if (!CUR_COMMIT_FILE.exists()) {
            return null;
        }
        return readObject(CUR_COMMIT_FILE, Commit.class);
    }

    /******************************* global-log ************************************/

    // commit和blob的id如何区分?
    public static void global_log() {
        List<String> commitIds = plainFilenamesIn(OBJECT_DIR);
        if (commitIds == null) {
            return;
        }
        for (String id : commitIds) {
            try {
                Commit commit = readCommitById(id);
                if (commit.getParents().size() == 2) {
                    printMergeCommit(commit);
                } else {
                    printCommit(commit);
                }
            } catch (Exception ignore) {

            }
        }
    }

    /********************************* find **************************************/

    public static void find(String findMessage) {
        List<String> commitList = plainFilenamesIn(OBJECT_DIR);
        List<String> idList = new ArrayList<>();
        Commit commit;
        for (String id : commitList) {
            try {
                commit = readCommitById(id);
                if (findMessage.equals(commit.getMessage())) {
                    idList.add(id);
                }
            } catch (Exception ignore) {
            }
        }
        printID(idList);
    }

    private static void printID(List<String> idList) {
        if (idList.isEmpty()) {
            System.out.println("Found no commit with that message.");
        } else {
            for (String id : idList) {
                System.out.println(id);
            }
        }
    }

    /******************************** status *************************************/

    public static void status() {
        curCommit = readCurrentCommit();
        addStage = readFromAddStage();
        removeStage = readFromRemoveStage();

        printBranchViews();
        printAddStageFiles();
        printRemoveStageFiles();
        printModifiedNotStagedFiles();
        printUntrackedFiles();
    }

    private static void printBranchViews() {
        String curBranch = readCurrBranch();
        List<String> branches = new ArrayList<>();
        File[] files = HEADS_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                branches.add(file.getName());
            }
        }
        System.out.println("=== Branches ===");
        System.out.println("*" + curBranch);
        for (String branchName : branches) {
            if (!branchName.equals(curBranch)) {
                System.out.println(branchName);
            }
        }
        System.out.println("\n");
    }

    private static void printAddStageFiles() {
        List<Blob> blobList = addStage.getBlobList();
        System.out.println("=== Staged Files ===");
        for (Blob blob : blobList) {
            System.out.println(blob.getFileName().getName());
        }
        System.out.println("\n");
    }

    private static void printRemoveStageFiles() {
        List<Blob> blobList = removeStage.getBlobList();
        System.out.println("=== Staged Files ===");
        for (Blob blob : blobList) {
            System.out.println(blob.getFileName().getName());
        }
        System.out.println("\n");
    }

    private static void printModifiedNotStagedFiles() {
        Map<String, String> trackedMap = curCommit.getPathblobIDs();
        Map<String, String> stagedMap = addStage.getPath2BolbId();
        Map<String, String> removedMap = removeStage.getPath2BolbId();

        // 获取工作区里的文件
        File[] CWDFiles = CWD.listFiles(pathname -> !pathname.equals(GITLET_DIR));
        Map<String, File> workingDirMap = new HashMap<>();
        if (CWDFiles != null) {
            for (File file : CWDFiles) {
                workingDirMap.put(file.getName(), file);
            }
        }

        Set<String> modifiedFiles = new HashSet<>();
        Set<String> removedFiles = new HashSet<>();

        // Tracked in the current commit, changed in the working directory, but not staged.
        // Not staged for removal, but tracked in the current commit and deleted from the working directory.
        for (Map.Entry<String, String> entry : trackedMap.entrySet()) {
            String filePath = entry.getKey(), bolbId = entry.getValue();
            Blob blob = readObject(join(OBJECT_DIR, bolbId), Blob.class);
            if (workingDirMap.containsKey(filePath)) {
                byte[] contents = readContents(workingDirMap.get(filePath));
                if (!Arrays.equals(contents, blob.getContent())) {
                    modifiedFiles.add(blob.getFileName().getName());
                }
            } else {
                if (!removedMap.containsKey(filePath)) {
                    removedFiles.add(blob.getFileName().getName());
                }
            }
        }

        // Staged for addition, but with different contents than in the working directory.
        // Staged for addition, but deleted in the working directory.
        for (Map.Entry<String, String> entry : stagedMap.entrySet()) {
            String filePath = entry.getKey(), bolbId = entry.getValue();
            Blob blob = readObject(join(OBJECT_DIR, bolbId), Blob.class);
            if (workingDirMap.containsKey(filePath)) {
                byte[] contents = readContents(workingDirMap.get(filePath));
                if (!Arrays.equals(contents, blob.getContent())) {
                    modifiedFiles.add(blob.getFileName().getName());
                }
            } else {
                removedFiles.add(blob.getFileName().getName());
            }
        }

        System.out.println("=== Modofications Not Staged For Commit ===");
        for (String fileName : removedFiles) {
            System.out.println(fileName + " (deleted)");
        }
        for (String fileName : modifiedFiles) {
            System.out.println(fileName + " (modified)");
        }
        System.out.println("\n");
    }

    private static void printUntrackedFiles() {
        Map<String, String> trackedMap = curCommit.getPathblobIDs();
        Map<String, String> stagedMap = addStage.getPath2BolbId();
        File[] CWDFiles = CWD.listFiles(pathname -> !pathname.equals(GITLET_DIR));

        System.out.println("=== Untracked Files ===");
        if (CWDFiles != null) {
            for (File file : CWDFiles) {
                if (!trackedMap.containsKey(file.getPath()) && !stagedMap.containsKey(file.getPath())) {
                    System.out.println(file.getName());
                }
            }
        }
        System.out.println("\n");
    }

    /******************************** checkout ************************************/

    public static void checkout(String fileName) {
        curCommit = readCurrentCommit();
        List<String> fileNames = curCommit.getFileNames();
        if (fileNames.contains(fileName)) {
            Blob blob = curCommit.getBolbByFileName(fileName);
            writeBlobToCWD(blob);
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    private static void writeBlobToCWD(Blob blob) {
        File beWritten = blob.getFileName();
        writeContents(beWritten, new String(blob.getContent(), StandardCharsets.UTF_8));
    }

    public static void checkout(String commitId, String fileName) {
        curCommit = readCommitById(commitId);
        if (curCommit == null) {
            System.out.println("No Commit with that id exists.");
            System.exit(0);
        }
        List<String> fileNames = curCommit.getFileNames();
        if (fileNames.contains(fileName)) {
            Blob blob = curCommit.getBolbByFileName(fileName);
            writeBlobToCWD(blob);
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    public static void checkoutBranch(String branchName) {
        // 检查是否check到自己
        curBranch = readCurrBranch();
        if (branchName.equals(curBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        // 检查check到的分支存不存在
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if (branches == null || !branches.contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        Commit checkoutCommit = readCommitByBranchName(branchName);
        curCommit = readCurrentCommit();
        HashSet<String> checkoutFileNames = new HashSet<>(checkoutCommit.getFileNames());
        HashSet<String> trackedFileNames = new HashSet<>(curCommit.getFileNames());
        HashSet<String> curWorkingDirFileNames = new HashSet<>(getCWDFileNames());
        // 只在checkout存在，在cur不存在，在workdir存在，untracked
        for (String fn : curWorkingDirFileNames) {
            if (checkoutFileNames.contains(fn) && !trackedFileNames.contains(fn)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // 1.在cur和checkout commit都存在，复写
        for (String fn : checkoutFileNames) {
            Blob blob = checkoutCommit.getBolbByFileName(fn);
            writeBlobToCWD(blob);
        }

        // 2.只在cur存在，在checkout不存在，删除
        for (String fn : trackedFileNames) {
            if (!checkoutFileNames.contains(fn)) {
                File f = join(CWD, fn);
                restrictedDelete(f);
            }
        }

        // 3.只在checkout存在，在cur不存在，新建 (与1合并)

        writeContents(HEAD_FILE, branchName);

        addStage = readFromAddStage();
        addStage.clear();
        saveAddStage();
        removeStage = readFromRemoveStage();
        removeStage.clear();
        saveRemoveStage();
    }

    private static Commit readCommitByBranchName(String branchName) {
        return readCommitById(readCommitIDByBranch(branchName));
    }

    private static List<String> getCWDFileNames() {
        List<String> CWDFileNames = new ArrayList<>();
        File[] CWDFiles = CWD.listFiles(pathname -> !pathname.equals(GITLET_DIR));
        if (CWDFiles != null) {
            for (File file : CWDFiles) {
                CWDFileNames.add(file.getName());
            }
        }
        return CWDFileNames;
    }

    /******************************** branch *************************************/
    private static void checkBranchExists(String branchName) {
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if (branches.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
    }


    public static void branch(String branchName) {
        checkBranchExists(branchName);

        curCommit = readCurrentCommit();
        writeContents(join(HEADS_DIR, branchName), curCommit.getId());
    }

    /****************************** rm-branch ************************************/

    public static void rmbranch(String branchName) {
        checkBranchExists(branchName);

        curBranch = readCurrBranch();
        if (branchName.equals(curBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        restrictedDelete(join(HEADS_DIR, branchName));
    }

    /********************************* reset *************************************/

    public static void reset(String commitId) {
        File checkedCommit = join(OBJECT_DIR, commitId);
        if (!checkedCommit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit checkoutCommit = readCommitById(commitId);
        curCommit = readCurrentCommit();
        HashSet<String> checkoutFileNames = new HashSet<>(checkoutCommit.getFileNames());
        HashSet<String> trackedFileNames = new HashSet<>(curCommit.getFileNames());
        HashSet<String> curWorkingDirFileNames = new HashSet<>(getCWDFileNames());
        // 只在checkout存在，在cur不存在，在workdir存在，untracked
        for (String fn : curWorkingDirFileNames) {
            if (checkoutFileNames.contains(fn) && !trackedFileNames.contains(fn)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // 1.在cur和checkout commit都存在，复写
        for (String fn : checkoutFileNames) {
            Blob blob = checkoutCommit.getBolbByFileName(fn);
            writeBlobToCWD(blob);
        }

        // 2.只在cur存在，在checkout不存在，删除
        for (String fn : trackedFileNames) {
            if (!checkoutFileNames.contains(fn)) {
                File f = join(CWD, fn);
                restrictedDelete(f);
            }
        }

        // 3.只在checkout存在，在cur不存在，新建 (与1合并)

        addStage = readFromAddStage();
        addStage.clear();
        saveAddStage();
        removeStage = readFromRemoveStage();
        removeStage.clear();
        saveRemoveStage();

        curBranch = readCurrBranch();
        writeContents(join(HEADS_DIR, curBranch), commitId);

    }

    /********************************* merge *************************************/
    public static Commit splitNodeCommit;
    public static Commit givenBranchCommit;
    public static HashSet<String> splitFiles;
    public static HashSet<String> givenFiles;
    public static HashSet<String> currFiles;

    public static void merge(String branchName) {
        addStage = readFromAddStage();
        removeStage = readFromRemoveStage();
        if (!addStage.isEmpty() || !removeStage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if (branches.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        curBranch = readCurrBranch();
        if (curBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        curCommit = readCurrentCommit();
        givenBranchCommit = readCommitByBranchName(branchName);
        splitNodeCommit = findSplitNode(curCommit, givenBranchCommit);

        if (splitNodeCommit.getId().equals(givenBranchCommit.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        } else if (splitNodeCommit.getId().equals(curCommit.getId())) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            System.exit(0);
        }

        splitFiles = new HashSet<>(splitNodeCommit.getFileNames());
        givenFiles = new HashSet<>(givenBranchCommit.getFileNames());
        currFiles = new HashSet<>(curCommit.getFileNames());

        HashSet<String> files = new HashSet<>();
        files.addAll(splitFiles);
        files.addAll(givenFiles);
        files.addAll(currFiles);

        Map<String, String> pathBlobIDs = new HashMap<>(curCommit.getPathblobIDs());
        // 要预先计算，再实际执行
        List<String> writeFiles = new ArrayList<>();
        List<String> deleteFiles = new ArrayList<>();
        List<String> conflictFiles = new ArrayList<>();

        for (String fn : files) {
            if (splitFiles.contains(fn)) {
                // case 1
                if (isFileModifiedIn(fn, givenBranchCommit) && !isFileModifiedIn(fn, curCommit)) {
                    // 修改包括内容不一样和删除
                    if (givenFiles.contains(fn)) {
//                        Blob blob = givenBranchCommit.getBolbByFileName(fn);
//                        writeBlobToCWD(blob);
//                        pathBlobIDs.put(blob.getFilePath(), blob.getId());
                        writeFiles.add(fn);
                    } else {
//                        File f = join(CWD, fn);
//                        restrictedDelete(f);
//                        pathBlobIDs.remove(f.getPath());
                    }
                }

                // case 2
                else if (!isFileModifiedIn(fn, givenBranchCommit) && isFileModifiedIn(fn, curCommit)) {
                    // 不变
//                    if (currFiles.contains(fn)) {
//                        Blob blob = curCommit.getBolbByFileName(fn);
//                        writeBlobToCWD(blob);
//                    } else {
//                        File f = join(CWD, fn);
//                        restrictedDelete(f);
//                    }
                }

                // case 3
                else if (isFileModifiedIn(fn, givenBranchCommit) && isFileModifiedIn(fn, curCommit)) {
                    if (isModifiedInTheSameWay(fn)) {
//                        if (!currFiles.contains(fn)) {
//                            restrictedDelete(join(CWD, fn));
//                        } else {
//                            Blob blob = curCommit.getBolbByFileName(fn);
//                            writeBlobToCWD(blob);
//                        }
                    } else {
//                        System.out.println("Encountered a merge conflict.");
//                        writeConflictContents(fn);
                        conflictFiles.add(fn);
                    }
                }

                // case 6
                else if (!isFileModifiedIn(fn, curCommit) && !givenFiles.contains(fn)) {
//                    restrictedDelete(join(CWD, fn));
//                    pathBlobIDs.remove(curCommit.getBolbByFileName(fn).getFilePath());
                    deleteFiles.add(fn);
                }

                // case 7
                else if (!isFileModifiedIn(fn, givenBranchCommit) && !currFiles.contains(fn)) {
                    // remian removed
                }

            } else {
                // case 4
                if (!givenFiles.contains(fn) && currFiles.contains(fn)) {
//                    Blob blob = curCommit.getBolbByFileName(fn);
//                    writeBlobToCWD(blob);
                }

                // case 5
                if (!currFiles.contains(fn) && givenFiles.contains(fn)) {
//                    Blob blob = givenBranchCommit.getBolbByFileName(fn);
//                    writeBlobToCWD(blob);
//                    pathBlobIDs.put(blob.getFilePath(), blob.getId());
                    writeFiles.add(fn);
                }
            }
        }

        for (String fn : writeFiles) {
            if (!currFiles.contains(fn) && givenFiles.contains(fn)) {
                File f = join(CWD, fn);
                if (f.exists()) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        for (String fn : writeFiles) {
            Blob blob = givenBranchCommit.getBolbByFileName(fn);
            writeBlobToCWD(blob);
            pathBlobIDs.put(blob.getFilePath(), blob.getId());
        }

        for (String fn : deleteFiles) {
            restrictedDelete(join(CWD, fn));
            pathBlobIDs.remove(curCommit.getBolbByFileName(fn).getFilePath());
        }

        String message = "Merged" + branchName + " intp " + curBranch + ".";
        List<String> parents = new ArrayList<>(List.of(curCommit.getId(), givenBranchCommit.getId()));
        Commit mergedCommit = new Commit(message, pathBlobIDs, parents);

        mergedCommit.save();
        addStage = readFromAddStage();
        addStage.clear();
        saveAddStage();
        removeStage = readFromRemoveStage();
        removeStage.clear();
        saveRemoveStage();
        curCommit = mergedCommit;
        writeContents(join(HEADS_DIR, curBranch), curCommit.getId());
    }

    private static Commit findSplitNode(Commit cur, Commit given) {
        HashSet<String> curAncestors = new HashSet<>();
        Commit pc = cur;
        while (!pc.getMessage().equals("init message")) {
            curAncestors.add(pc.getId());
            pc = readCommitById(pc.getParents().get(0));
        }
        Commit lca = null;
        pc = given;
        while (!pc.getMessage().equals("init message")) {
            if (curAncestors.contains(pc.getId())) {
                lca = pc;
                break;
            }
            pc = readCommitById(pc.getParents().get(0));
        }
        return lca;
    }

    private static boolean isFileModifiedIn(String fn, Commit commit) {
        Blob blobInSplit = splitNodeCommit.getBolbByFileName(fn);
        List<String> fns = commit.getFileNames();
        if (!fns.contains(fn)) {
            return true;
        } else {
            Blob blob = commit.getBolbByFileName(fn);
            return Arrays.equals(blobInSplit.getContent(), blob.getContent());
        }
    }

    private static boolean isModifiedInTheSameWay(String fn) {
        if (!currFiles.contains(fn) && !givenFiles.contains(fn)) {
            return true;
        }
        if (currFiles.contains(fn) && givenFiles.contains(fn)) {
            Blob blobInCur = curCommit.getBolbByFileName(fn);
            Blob blobInGiven = givenBranchCommit.getBolbByFileName(fn);
            return Arrays.equals(blobInGiven.getContent(), blobInCur.getContent());
        }
        return false;
    }

    private static void writeConflictContents(String fn) {

        String curContents = "";
        if (currFiles.contains(fn)) {
            Blob b = curCommit.getBolbByFileName(fn);
            curContents = new String(b.getContent(), StandardCharsets.UTF_8);
        }
        String givenContents = "";
        if (givenFiles.contains(fn)) {
            Blob b = givenBranchCommit.getBolbByFileName(fn);
            givenContents = new String(b.getContent(), StandardCharsets.UTF_8);
        }

        String conflictContents = "<<<<<<< HEAD\n" +
                                       curContents +
                                       "=======\n" +
                                     givenContents +
                                      ">>>>>>>\n";
        writeContents(join(CWD, fn), conflictContents);
    }

    public static void checkIfInitialized() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
