package com.moonlight.binding;

import android.content.Context;

import com.moonlight.binding.audio.AndroidAudioRenderer;
import com.moonlight.binding.crypto.AndroidCryptoProvider;
import com.moonlight.nvstream.av.audio.AudioRenderer;
import com.moonlight.nvstream.http.LimelightCryptoProvider;

public class PlatformBinding {
    public static LimelightCryptoProvider getCryptoProvider(Context c) {
        return new AndroidCryptoProvider(c);
    }
}
