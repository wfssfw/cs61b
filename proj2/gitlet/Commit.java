package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.*; // TODO: You'll likely use this in this class
import java.text.SimpleDateFormat;

import static gitlet.Repository.CWD;
import static gitlet.Utils.*; // 注意要导入的是不是静态成员/方法
import static gitlet.Repository.OBJECT_DIR;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;       // commit时添加的message
    private Map<String, String> pathblobIDs;   // 该commit对应的blobs的id(hashcode)
    private List<String> parents; // 当前commits的parents的hashcode
    private Date curTime;         // 时间戳，init commit为Date(0)
    private String id;            // commit的id
    private File saveCommitPath;  // commit的保存文件

    public Commit(String message, Map<String, String> blobs, List<String> parents) {
        this.message = message;
        this.pathblobIDs = blobs;
        this.parents = parents;
        this.curTime = new Date();
        this.id = genCommitID(message, blobs, parents, dateToTimestamp(this.curTime));
        this.saveCommitPath = genSaveCommitPath();
    }

    // init commit的构造函数
    public Commit() {
        this.message = "init commit";
        this.pathblobIDs = new HashMap<>();
        this.parents = new ArrayList<>();
        this.curTime = new Date(0);
        this.id = genCommitID(this.message, this.pathblobIDs, this.parents, dateToTimestamp(this.curTime));
        this.saveCommitPath = genSaveCommitPath();
    }

    private String dateToTimestamp(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return formatter.format(date);
    }

    private String genCommitID(String message, Map<String, String> blobs, List<String> parents, String timeStamp) {
        String id = sha1(message, blobs, parents, timeStamp);
        return id;
    }

    private File genSaveCommitPath() {
        return join(OBJECT_DIR, this.id);
    }

    public String getId() {
        return this.id;
    }

    public void save() {
        writeContents(this.saveCommitPath, this);
    }

    public File getSaveCommitPath() { return this.saveCommitPath; }

    public Map<String, String> getPathblobIDs() { return this.pathblobIDs; }

    public List<String> getParents() { return this.parents; }

    public String getDate() { return dateToTimestamp(this.curTime); }

    public String getMessage() { return this.message; }

    public List<Blob> getBlobList() {
        List<Blob> blobList = new ArrayList<>();
        for (String id : pathblobIDs.values()) {
            File blobSaveFile = join(OBJECT_DIR, id);
            Blob blob = readObject(blobSaveFile, Blob.class);
            blobList.add(blob);
        }
        return blobList;
    }

    public List<String> getFileNames() {
        List<String> fileNames = new ArrayList<>();
        for (String id : pathblobIDs.values()) {
            File blobSaveFile = join(OBJECT_DIR, id);
            Blob blob = readObject(blobSaveFile, Blob.class);
            fileNames.add(blob.getFileName().getName());
        }
        return fileNames;
    }

    public Blob getBolbByFileName(String fileName) {
        File file = join(CWD, fileName);
        String bolbId = pathblobIDs.get(file.getPath());
        File blobSaveFile = join(OBJECT_DIR, bolbId);
        return readObject(blobSaveFile, Blob.class);
    }
}
