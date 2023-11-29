package gitlet;

import java.io.Serializable;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.File;

import static gitlet.Utils.*;
import static gitlet.Repository.OBJECT_DIR;

public class Blob implements Serializable {
    private String id;           // blob的id
    private byte[] content;      // 存储的内容，二进制
    private File fileName;       // 对应的工作区里的文件名
    private String filePath;     // 对应的工作区里的文件路径
    private File blobSave;       // blob object保存路径

    public Blob(File fileName) {
        this.content = readContents(fileName);
        this.fileName = fileName;
        this.filePath = fileName.getPath();
        this.id = genBolbID(this.content, this.fileName);
        this.blobSave = join(OBJECT_DIR, id);
    }

    private String genBolbID(byte[] content, File fileName) {
        return sha1(content, fileName);
    }

    public void save() {
        writeContents(this.blobSave, this);
    }

    public String getId() {
        return this.id;
    }

    public String getFilePath() { return this.filePath; }

    public File getFileName() { return this.fileName; }

    public byte[] getContent() { return this.content; }
}