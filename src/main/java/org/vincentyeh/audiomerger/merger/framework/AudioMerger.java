package org.vincentyeh.audiomerger.merger.framework;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface AudioMerger {

    interface Listener{
        void onSingleAudioMergeComplete(int index,int total);
    }

    void merge(List<File> audioFiles, File destination, Listener listener) throws IOException, UnsupportedAudioFileException;

    void merge(File[] audioFiles, File destination, Listener listener) throws UnsupportedAudioFileException, IOException;
    void merge(List<File> audioFiles,File destination) throws UnsupportedAudioFileException, IOException;
    void merge(File[] audioFiles,File destination) throws UnsupportedAudioFileException, IOException;

}
