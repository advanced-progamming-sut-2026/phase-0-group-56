package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Locates the extracted PvZ asset root without assuming one IDE working directory. */
public final class PvzAssetLocator {
    private PvzAssetLocator() {
    }

    public static FileHandle find() {
        List<FileHandle> candidates = new ArrayList<>();
        String configured = System.getProperty("pvz.assets");
        if (configured != null && !configured.isBlank()) {
            candidates.add(new FileHandle(new File(configured)));
        }

        String[] paths = {
            "Assets",
            "../Assets",
            "../../Assets",
            "pvz-assets",
            "../pvz-assets",
            "../../pvz-assets"
        };
        for (String path : paths) {
            candidates.add(new FileHandle(new File(path)));
        }
        candidates.add(Gdx.files.internal("pvz-assets"));

        for (FileHandle candidate : candidates) {
            FileHandle root = resolve(candidate);
            if (root != null) {
                return root;
            }
        }
        return null;
    }

    private static FileHandle resolve(FileHandle candidate) {
        if (candidate == null || !candidate.exists()) {
            return null;
        }
        if (isRoot(candidate)) {
            return candidate;
        }

        String[] children = {"Base Assets", "base assets", "BaseAssets", "assets"};
        for (String childName : children) {
            FileHandle child = candidate.child(childName);
            if (isRoot(child)) {
                return child;
            }
        }
        return null;
    }

    private static boolean isRoot(FileHandle root) {
        if (root == null || !root.exists()) {
            return false;
        }
        boolean resources = root.child("resources.json").exists()
            || root.child("RESOURCES.json").exists();
        boolean atlases = root.child("atlases").exists()
            || root.child("ATLASES").exists();
        return resources && atlases;
    }
}
