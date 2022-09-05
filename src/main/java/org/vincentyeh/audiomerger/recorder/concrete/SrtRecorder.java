package org.vincentyeh.audiomerger.recorder.concrete;

import org.vincentyeh.audiomerger.recorder.framework.Recorder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static java.lang.String.format;

public abstract class SrtRecorder implements Recorder {

    protected abstract String getSubtitle(File file);

    private final BufferedWriter writer;

    public SrtRecorder(File recordDestination) throws IOException {
        this(recordDestination, StandardCharsets.UTF_8);
    }

    public SrtRecorder(File recordDestination, Charset charset) throws IOException {
//        this.recordDestination=recordDestination;
        writer = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(recordDestination.toPath()),
                charset));
    }

    private int[] getFormattedTime(long frame, float framePerSecond) {
        float durationInSecond = frame / framePerSecond;

        float hours = durationInSecond / 3600;
        float minutes = (durationInSecond % 3600) / 60;
        float seconds = durationInSecond % 60;
        float millis = (seconds - ((int) seconds)) * 1000;
        return new int[]{(int) hours, (int) minutes, (int) seconds, (int) millis};
    }

    @Override
    public void record(int index, int total, File file, long startFrame, long endFrame, float framePerSecond) throws IOException {
        int[] start = getFormattedTime(startFrame, framePerSecond);
        int[] end = getFormattedTime(endFrame, framePerSecond);

        writer.write(index + 1 + "\n");
        writer.write(format("%02d:%02d:%02d,%03d --> %02d:%02d:%02d,%03d\n",
                start[0], start[1], start[2], start[3],
                end[0], end[1], end[2], end[3]
        ));
        writer.write(getSubtitle(file) + "\n\n");
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
