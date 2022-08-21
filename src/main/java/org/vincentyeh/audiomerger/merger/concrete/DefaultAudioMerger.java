package org.vincentyeh.audiomerger.merger.concrete;

import org.vincentyeh.audiomerger.merger.framework.AudioFileType;
import org.vincentyeh.audiomerger.merger.framework.AudioMerger;
import org.vincentyeh.audiomerger.recorder.framework.Recorder;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import static java.lang.String.format;

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
        merge(audioFiles, destination,null,null);
    }

    @Override
    public void merge(File[] audioFiles, File destination) throws IOException, UnsupportedAudioFileException {
        merge(audioFiles, destination, null,null);
    }

    @Override
    public void merge(List<File> audioFiles, File destination, Listener listener, Recorder recorder) throws IOException, UnsupportedAudioFileException {
        merge(audioFiles.toArray(new File[0]), destination, listener,recorder);
    }

    @Override
    public void merge(File[] audioFiles, File destination, AudioMerger.Listener listener,Recorder recorder) throws IOException, UnsupportedAudioFileException {
        var audioInputStreams = new AudioInputStream[audioFiles.length];
        long totalLength = 0;
        for (int i = 0; i < audioFiles.length; i++) {
            audioInputStreams[i] = AudioSystem.getAudioInputStream(audioFiles[i]);
            var frameLength = audioInputStreams[i].getFrameLength();
            var framePerSecond = audioInputStreams[i].getFormat().getFrameRate();

            var startFrame=totalLength+1;
            var endFrame=totalLength+frameLength;
            if(recorder!=null)
                recorder.record(i,audioFiles.length,audioFiles[i],startFrame,endFrame,framePerSecond);

            totalLength += frameLength;
        }
        if(recorder!=null)
            recorder.close();

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


    private int[] getFormattedTime(long frame, float framePerSecond) {
        var durationInSecond = frame / framePerSecond;

        var hours = durationInSecond / 3600;
        var minutes = (durationInSecond % 3600) / 60;
        var seconds = durationInSecond % 60;
        var millis = (seconds - ((int) seconds)) * 1000;
        return new int[]{(int) hours, (int) minutes, (int) seconds, (int) millis};
    }
}
