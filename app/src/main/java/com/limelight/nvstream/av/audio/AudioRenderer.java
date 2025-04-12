package com.moonlight.nvstream.av.audio;

import com.moonlight.nvstream.jni.MoonBridge;

public interface AudioRenderer {
    int setup(MoonBridge.AudioConfiguration audioConfiguration, int sampleRate, int samplesPerFrame);

    void start();

    void stop();
    
    void playDecodedAudio(short[] audioData);
    
    void cleanup();
}
