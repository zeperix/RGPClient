package com.rgpclient.binding;

import android.content.Context;

import com.rgpclient.binding.audio.AndroidAudioRenderer;
import com.rgpclient.binding.crypto.AndroidCryptoProvider;
import com.rgpclient.nvstream.av.audio.AudioRenderer;
import com.rgpclient.nvstream.http.LimelightCryptoProvider;

public class PlatformBinding {
    public static LimelightCryptoProvider getCryptoProvider(Context c) {
        return new AndroidCryptoProvider(c);
    }
}
