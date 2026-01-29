package com.example.virtualcamera;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {
    private static final String TAG = "VirtualCameraDecoder";
    private boolean isRunning = false;

    public interface FrameCallback {
        void onFrameAvailable(byte[] data);
    }

    public void startDecoding(final String filePath, final FrameCallback callback) {
        if (isRunning) return;
        isRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                decodeLoop(filePath, callback);
            }
        }).start();
    }

    public void stopDecoding() {
        isRunning = false;
    }

    private void decodeLoop(String filePath, FrameCallback callback) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;

        try {
            extractor.setDataSource(filePath);
            int trackIndex = selectVideoTrack(extractor);
            if (trackIndex < 0) {
                Log.e(TAG, "No video track found in " + filePath);
                return;
            }
            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);

            decoder = MediaCodec.createDecoderByType(mime);
            // Configure with null surface to receive ByteBuffers
            decoder.configure(format, null, null, 0);
            decoder.start();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            
            while (isRunning) {
                if (!inputDone) {
                    int inputIndex = decoder.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            // End of stream, loop back
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            inputDone = false; // Continue looping
                            // Or send EOS: decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            // inputDone = true;
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputIndex >= 0) {
                    ByteBuffer outputBuffer = decoder.getOutputBuffer(outputIndex);
                    
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        
                        byte[] frameData = new byte[bufferInfo.size];
                        outputBuffer.get(frameData);
                        
                        callback.onFrameAvailable(frameData);
                    }
                    
                    decoder.releaseOutputBuffer(outputIndex, false);
                    
                    // Simple frame rate control (very rough)
                    try {
                        Thread.sleep(30); // ~30 FPS
                    } catch (InterruptedException e) {
                        break;
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Format changed
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // No output yet
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Decoder error", e);
        } finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
            extractor.release();
        }
    }

    private int selectVideoTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }
}
