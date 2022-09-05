package org.vincentyeh.audiomerger.ui;

import org.vincentyeh.audiomerger.merger.framework.AudioFileType;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;

public class AudioTypeFileFilter extends javax.swing.filechooser.FileFilter {
    private final FileNameExtensionFilter filter;
    private final AudioFileType type;

    public AudioTypeFileFilter(AudioFileType type){
        this.type = type;
        String extension=type.getExtension();
        filter=new FileNameExtensionFilter("*." + extension, extension);
    }
    @Override
    public boolean accept(File pathname) {
        return filter.accept(pathname);
    }

    @Override
    public String getDescription() {
        return filter.getDescription();
    }

    public String getExtension(){
        return filter.getExtensions()[0];
    }

    public AudioFileType getType() {
        return type;
    }
}
