package fr.avianey.modjo.androidgendrawable;

import java.io.File;


public enum OverrideMode {
    
    always, never, ifModified;
    
    public boolean override(File src, File dest, File ninePatchConfig, boolean isNinePatch) {
        if (!dest.exists() || always.equals(this)) {
            return true;
        } else if (never.equals(this)) {
            return false;
        } else {
            return src.lastModified() > dest.lastModified() 
                    && (!isNinePatch || ninePatchConfig.lastModified() > dest.lastModified());
        }
    }
    
}
