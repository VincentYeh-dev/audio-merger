package org.vincentyeh.audiomerger.recorder.concrete;

public enum RecorderType {
    Srt("srt");

    private final String extension;

    RecorderType(String extension) {

        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
