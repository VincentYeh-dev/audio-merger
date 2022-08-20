package org.vincentyeh.audiomerger.merger.concrete;

import org.vincentyeh.audiomerger.merger.framework.AudioFileType;
import org.vincentyeh.audiomerger.merger.framework.AudioMerger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.List;

public class DefaultAudioMerger implements AudioMerger {

    private final AudioFileFormat.Type outputFormat;

    public DefaultAudioMerger(AudioFileType type) {
        if (type == AudioFileType.AU)
            outputFormat = AudioFileFormat.Type.AU;
        else if (type == AudioFileType.AIFF)
            outputFormat = AudioFileFormat.Type.AIFF;
        else if (type == AudioFileType.WAV)
            outputFormat = AudioFileFormat.Type.WAVE;
        else
            throw new IllegalStateException("WTF is that??");
    }

    @Override
    public void merge(List<File> audioFiles, File destination) throws IOException, UnsupportedAudioFileException {
        merge(audioFiles, destination);
    }

    @Override
    public void merge(File[] audioFiles, File destination) throws IOException, UnsupportedAudioFileException {
        merge(audioFiles, destination, null);
    }

    @Override
    public void merge(List<File> audioFiles, File destination, AudioMerger.Listener listener) throws IOException, UnsupportedAudioFileException {
        merge(audioFiles.toArray(new File[0]), destination, listener);
    }

    @Override
    public void merge(File[] audioFiles, File destination, AudioMerger.Listener listener) throws IOException, UnsupportedAudioFileException {
        var audioInputStreams = new AudioInputStream[audioFiles.length];
        long totalLength = 0;
        for (int i = 0; i < audioFiles.length; i++) {
            audioInputStreams[i] = AudioSystem.getAudioInputStream(audioFiles[i]);
            totalLength += audioInputStreams[i].getFrameLength();
        }

        var appendInputStream = new SequenceInputStream(new Enumeration<>() {
            int index = 0;

            @Override
            public boolean hasMoreElements() {
                if (listener != null && index > 0)
                    listener.onSingleAudioMergeComplete(index - 1, audioInputStreams.length);
                return index < audioInputStreams.length;
            }

            @Override
            public InputStream nextElement() {
                return audioInputStreams[index++];
            }
        });

        var appendedFiles = new AudioInputStream(appendInputStream, audioInputStreams[0].getFormat(), totalLength);
        AudioSystem.write(appendedFiles, outputFormat, destination);
        appendedFiles.close();
        appendInputStream.close();
    }
}
