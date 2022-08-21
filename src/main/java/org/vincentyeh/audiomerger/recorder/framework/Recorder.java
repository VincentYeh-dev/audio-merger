package org.vincentyeh.audiomerger.recorder.framework;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public interface Recorder extends Closeable {


    void record(int index, int total, File file, long startFrame, long endFrame, float framePerSecond) throws IOException;
}
