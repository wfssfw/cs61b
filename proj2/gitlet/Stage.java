package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.text.*;
import static gitlet.Utils.join;
import static gitlet.Repository.OBJECT_DIR;
import static gitlet.Utils.readObject;

public class Stage implements Serializable {
    private Map<String, String> path2BolbId;     // add stage

    public Stage() {
        this.path2BolbId = new HashMap<>();
    }

    public Stage(Map<String, String> path2BolbId) {
        this.path2BolbId = path2BolbId;
    }

    public Map<String, String> getPath2BolbId() {
        return this.path2BolbId;
    }

    public boolean isContainsValue(String bolbId) {
        return this.path2BolbId.containsValue(bolbId);
    }

    public boolean isContainsKey(String bolbPath) {
        return this.path2BolbId.containsKey(bolbPath);
    }

    public void clear() {
        path2BolbId.clear();
    }

    public List<Blob> getBlobList() {
        List<Blob> blobList = new ArrayList<>();
        for (String id : path2BolbId.values()) {
            File blobSaveFile = join(OBJECT_DIR, id);
            Blob blob = readObject(blobSaveFile, Blob.class);
            blobList.add(blob);
        }
        return blobList;
    }

    public boolean isEmpty() {
        return this.path2BolbId.isEmpty();
    }

}
