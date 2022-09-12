package org.vincentyeh.audiomerger.codec;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.common.*;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.*;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.muxer.CodecMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ImageToH264MP4Encoder {
    private final SeekableByteChannel ch;

    private final Transform transform;
    private H264Encoder encoder;


    // Encoder extra data ( SPS, PPS ) to be stored in a special place of
    // MP4
    private final ArrayList<ByteBuffer> spsList = new ArrayList<>();
    private final ArrayList<ByteBuffer> ppsList = new ArrayList<>();
    private final CodecMP4MuxerTrack outVideoTrack;
    private final PCMMP4MuxerTrack outAudioTrack;
    private ByteBuffer _out;
    private int frameNo;
    private final MP4Muxer muxer;

    public ImageToH264MP4Encoder(SeekableByteChannel ch, AudioFormat af) throws IOException {
        this.ch = ch;
        // Muxer that will store the encoded frames
        muxer = MP4Muxer.createMP4Muxer(ch, Brand.MP4);

        VideoCodecMeta meta = VideoCodecMeta.createSimpleVideoCodecMeta(new Size(1080, 720), encoder.getSupportedColorSpaces()[0]);
        // Add video track to muxer
        outVideoTrack = (CodecMP4MuxerTrack) muxer.addVideoTrack(Codec.H264, meta);
        outAudioTrack = muxer.addPCMAudioTrack(af);

        // Create an instance of encoder
        encoder = new H264Encoder(new H264FixedRateControl(1024));

        // Transform to convert between RGB and YUV
        transform = ColorUtil.getTransform(ColorSpace.RGB, encoder.getSupportedColorSpaces()[0]);
    }

    public void addFrame(Picture pic) throws IOException {
        Picture toEncode = Picture.create(pic.getWidth(), pic.getHeight(), encoder.getSupportedColorSpaces()[0]);

        if (_out == null) {
            // Allocate a buffer big enough to hold output frames
            _out = ByteBuffer.allocate(pic.getWidth() * pic.getHeight() * 6);

        }

        // Perform conversion
        transform.transform(pic, toEncode);

        // Encode image into H.264 frame, the result is stored in '_out' buffer
        _out.clear();
        VideoEncoder.EncodedFrame frame = encoder.encodeFrame(toEncode, _out);

        // Based on the frame above form correct MP4 packet
        spsList.clear();
        ppsList.clear();
        H264Utils.wipePS(_out, _out, spsList, ppsList);
        _out = H264Utils.encodeMOVPacket(_out);

        // Add packet to video track
//        outVideoTrack.addFrame(
//                new MP4Packet(out, frameNo, 25, 1, frameNo, true, null, frameNo, 0));

        MP4Packet packet = MP4Packet.createMP4Packet(_out,
                frameNo, 25, 1, frameNo,
                frame.isKeyFrame() ? Packet.FrameType.KEY : Packet.FrameType.INTER, TapeTimecode.ZERO_TAPE_TIMECODE,
                frameNo,0, 0);

        outVideoTrack.addFrame(packet);
        frameNo++;
    }

    public void addAudio(ByteBuffer buffer) throws IOException {
        outAudioTrack.addSamples(buffer);
    }

    public void finish() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        outVideoTrack.addSampleEntry(H264Utils.createMOVSampleEntryFromSpsPpsList(spsList, ppsList, 4));
//        outAudioTrack.addSampleEntry(new AudioSampleEntry(new Header()));
//        audioTrack.addSampleEntry(MP4Muxer.audioSampleEntry(af));

        // Write MP4 header and finalize recording
        muxer.finish();
        NIOUtils.closeQuietly(ch);
    }
}