package com.limelight;


import com.limelight.binding.PlatformBinding;
import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.capture.InputCaptureManager;
import com.limelight.binding.input.capture.InputCaptureProvider;
import com.limelight.binding.input.touch.AbsoluteTouchContext;
import com.limelight.binding.input.touch.RelativeTouchContext;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.binding.input.touch.TouchContext;
import com.limelight.binding.input.touch.TrackpadContext;
import com.limelight.binding.input.virtual_controller.VirtualController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardLayoutController;
import com.limelight.binding.video.CrashListener;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.GameGestures;
import com.limelight.ui.StreamView;
import com.limelight.utils.Dialog;
import com.limelight.utils.PanZoomHandler;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Rational;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.limelight.binding.input.virtual_controller.GamepadLayoutManager;
import android.util.DisplayMetrics;
import com.limelight.binding.input.virtual_controller.VirtualControllerConfigurationLoader;

public class Game extends Activity implements SurfaceHolder.Callback,
        OnGenericMotionListener, OnTouchListener, NvConnectionListener, EvdevListener,
        OnSystemUiVisibilityChangeListener, GameGestures, StreamView.InputCallbacks,
        PerfOverlayListener, UsbDriverService.UsbDriverStateListener, View.OnKeyListener{
    public static Game instance;

    private int lastButtonState = 0;

    // Only 2 touches are supported
    private final TouchContext[] touchContextMap = new TouchContext[2];
    private final TouchContext[] trackpadContextMap = new TouchContext[2];
    private PanZoomHandler panZoomHandler;
    private long threeFingerDownTime = 0;
    private long fourFingerDownTime = 0;

    private static final int REFERENCE_HORIZ_RES = 1280;
    private static final int REFERENCE_VERT_RES = 720;

    private static final int STYLUS_DOWN_DEAD_ZONE_DELAY = 100;
    private static final int STYLUS_DOWN_DEAD_ZONE_RADIUS = 20;

    private static final int STYLUS_UP_DEAD_ZONE_DELAY = 150;
    private static final int STYLUS_UP_DEAD_ZONE_RADIUS = 50;

    private static final int THREE_FINGER_TAP_THRESHOLD = 300;
    private static final int FOUR_FINGER_TAP_THRESHOLD = 300;

    private ControllerHandler controllerHandler;
    private KeyboardTranslator keyboardTranslator;
    private VirtualController virtualController;

    private KeyBoardController keyBoardController;

    private KeyBoardLayoutController keyBoardLayoutController;

    private PreferenceConfiguration prefConfig;
    private SharedPreferences tombstonePrefs;

    private int displayWidth;
    private int displayHeight;
    private int currentOrientation;

    private NvConnection conn;
    private SpinnerDialog spinner;
    private boolean displayedFailureDialog = false;
    private boolean connecting = false;
    public boolean connected = false;
    private boolean autoEnterPip = false;
    private boolean surfaceCreated = false;
    private boolean attemptedConnection = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;
    private NvApp app;
    private float desiredRefreshRate;

    private InputCaptureProvider inputCaptureProvider;
    private int modifierFlags = 0;
    private boolean grabbedInput = true;
    private boolean cursorVisible = false;
    private boolean isPanZoomMode = false;
    private boolean synthClickPending = false;
    private boolean pointerSwiping = false;
    private boolean waitingForAllModifiersUp = false;
    private int specialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private StreamView streamView;
    private long synthTouchDownTime = 0;

    private boolean pendingDrag = false;
    private boolean isDragging = false;
    private float lastTouchDownX, lastTouchDownY;

    private long lastAbsTouchUpTime = 0;
    private long lastAbsTouchDownTime = 0;
    private float lastAbsTouchUpX, lastAbsTouchUpY;
    private float lastAbsTouchDownX, lastAbsTouchDownY;

    private boolean quitOnStop = false;
    private boolean isHidingOverlays;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private View performanceOverlayView;

    private TextView performanceOverlayLite;

    private TextView performanceOverlayBig;

    private MediaCodecDecoderRenderer decoderRenderer;
    private boolean reportedCrash;

    private WifiManager.WifiLock highPerfWifiLock;
    private WifiManager.WifiLock lowLatencyWifiLock;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
            binder.setListener(controllerHandler);
            binder.setStateListener(Game.this);
            binder.start();
            connectedToUsbDriverService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectedToUsbDriverService = false;
        }
    };

    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_PORT = "Port";
    public static final String EXTRA_HTTPS_PORT = "HttpsPort";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_ID = "AppId";
    public static final String EXTRA_UNIQUEID = "UniqueId";
    public static final String EXTRA_PC_UUID = "UUID";
    public static final String EXTRA_PC_NAME = "PcName";
    public static final String EXTRA_APP_HDR = "HDR";
    public static final String EXTRA_SERVER_CERT = "ServerCert";
    public static final String EXTRA_VDISPLAY = "VirtualDisplay";
    public static final String EXTRA_SERVER_COMMANDS = "ServerCommands";

    public static final String CLIPBOARD_IDENTIFIER = "ArtemisStreaming";

    private String host;
    private int port;
    private int httpsPort;
    private int appId;
    private String uniqueId;
    private X509Certificate serverCert;
    private boolean vDisplay;
    private ArrayList<String> serverCommands;

    private ViewParent rootView;
    private ClipboardManager clipboardManager;
    private boolean clipboardSyncRunning = false;

    private NvHTTP httpConn;

    public interface GameMenuCallbacks {
        void showMenu(GameInputDevice devic);
        void hideMenu();
        boolean isMenuOpen();
    }

    public GameMenuCallbacks gameMenuCallbacks;

    // Floating menu button
    private ImageButton floatingMenuButton;
    private float dX, dY;
    private boolean isMovingButton = false;
    private static final float CLICK_ACTION_THRESHOLD = 5;
    private float startX, startY;

    // Variables for gamepad layout management
    private String currentEditingLayout = null;
    private boolean isEditingLayout = false;
    private GamepadLayoutManager gamepadLayoutManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set static instance for access from GamepadLayoutActivity
        instance = this;

        UiHelper.setLocale(this);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Full-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // If we're going to use immersive mode, we want to have
        // the entire screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Listen for UI visibility events
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        // Change volume button behavior
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Inflate the content
        setContentView(R.layout.activity_game);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // Start the spinner
        spinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.conn_establishing_title),
                getResources().getString(R.string.conn_establishing_msg), true);

        // Read the stream preferences
        prefConfig = PreferenceConfiguration.readPreferences(this);
        tombstonePrefs = Game.this.getSharedPreferences("DecoderTombstone", 0);

        if (prefConfig.autoOrientation) {
            currentOrientation = getResources().getConfiguration().orientation;
        } else {
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
        }

        boolean portraitMode = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
        boolean shouldInvertDecoderResolution = portraitMode && prefConfig.autoInvertVideoResolution;

        displayWidth = shouldInvertDecoderResolution ? prefConfig.height : prefConfig.width;
        displayHeight = shouldInvertDecoderResolution ? prefConfig.width : prefConfig.height;

        // Enter landscape unless we're on a square screen
        setPreferredOrientationForCurrentDisplay();

        if (
                prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH ||
                        shouldIgnoreInsetsForResolution(displayWidth, displayHeight)
        ) {
            // Allow the activity to layout under notches if the fill-screen option
            // was turned on by the user or it's a full-screen native resolution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        }
        // Listen for non-touch events on the game surface
        streamView = findViewById(R.id.surfaceView);
        streamView.setOnGenericMotionListener(this);
        streamView.setOnKeyListener(this);
        streamView.setInputCallbacks(this);

        //光标是否显示
        cursorVisible = prefConfig.enableMouseLocalCursor;

        //串流画面 顶部居中显示
        if(prefConfig.alignDisplayTopCenter){
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) streamView.getLayoutParams();
            params.gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;
        }
        // Listen for touch events on the background touch view to enable trackpad mode
        // to work on areas outside of the StreamView itself. We use a separate View
        // for this rather than just handling it at the Activity level, because that
        // allows proper touch splitting, which the OSC relies upon.
        View backgroundTouchView = findViewById(R.id.backgroundTouchView);
        backgroundTouchView.setOnTouchListener(this);

        rootView = streamView.getParent();

        panZoomHandler = new PanZoomHandler(
                getApplicationContext(),
                this,
                streamView,
                prefConfig
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Request unbuffered input event dispatching for all input classes we handle here.
            // Without this, input events are buffered to be delivered in lock-step with VBlank,
            // artificially increasing input latency while streaming.
            streamView.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                            InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                            InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                            InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                            InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
            backgroundTouchView.requestUnbufferedDispatch(
                    InputDevice.SOURCE_CLASS_BUTTON | // Keyboards
                            InputDevice.SOURCE_CLASS_JOYSTICK | // Gamepads
                            InputDevice.SOURCE_CLASS_POINTER | // Touchscreens and mice (w/o pointer capture)
                            InputDevice.SOURCE_CLASS_POSITION | // Touchpads
                            InputDevice.SOURCE_CLASS_TRACKBALL // Mice (pointer capture)
            );
        }

        notificationOverlayView = findViewById(R.id.notificationOverlay);

        performanceOverlayView = findViewById(R.id.performanceOverlay);

        performanceOverlayLite = findViewById(R.id.performanceOverlayLite);

        performanceOverlayBig = findViewById(R.id.performanceOverlayBig);

        inputCaptureProvider = InputCaptureManager.getInputCaptureProvider(this, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            streamView.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
//                    LimeLog.info("onCapturedPointer="+motionEvent.toString());
//                    LimeLog.info("onCapturedPointer-Device="+motionEvent.getDevice().toString());
                    return handleMotionEvent(view, motionEvent);
                }
            });
        }

        // Warn the user if they're on a metered connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isMetered = connMgr.isActiveNetworkMetered();
        if (isMetered) {
            displayTransientMessage(getResources().getString(R.string.conn_metered));
        }

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            highPerfWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Moonlight High Perf Lock");
            highPerfWifiLock.setReferenceCounted(false);
            highPerfWifiLock.acquire();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lowLatencyWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Moonlight Low Latency Lock");
                lowLatencyWifiLock.setReferenceCounted(false);
                lowLatencyWifiLock.acquire();
            }
        } catch (SecurityException e) {
            // Some Samsung Galaxy S10+/S10e devices throw a SecurityException from
            // WifiLock.acquire() even though we have android.permission.WAKE_LOCK in our manifest.
            e.printStackTrace();
        }

        appName = Game.this.getIntent().getStringExtra(EXTRA_APP_NAME);
        pcName = Game.this.getIntent().getStringExtra(EXTRA_PC_NAME);

        host = Game.this.getIntent().getStringExtra(EXTRA_HOST);
        port = Game.this.getIntent().getIntExtra(EXTRA_PORT, NvHTTP.DEFAULT_HTTP_PORT);
        httpsPort = Game.this.getIntent().getIntExtra(EXTRA_HTTPS_PORT, 0); // 0 is treated as unknown
        appId = Game.this.getIntent().getIntExtra(EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);
        uniqueId = Game.this.getIntent().getStringExtra(EXTRA_UNIQUEID);
        vDisplay = Game.this.getIntent().getBooleanExtra(EXTRA_VDISPLAY, false);
        serverCommands = Game.this.getIntent().getStringArrayListExtra(EXTRA_SERVER_COMMANDS);
        boolean appSupportsHdr = Game.this.getIntent().getBooleanExtra(EXTRA_APP_HDR, false);
        byte[] derCertData = Game.this.getIntent().getByteArrayExtra(EXTRA_SERVER_CERT);

        app = new NvApp(appName != null ? appName : "app", appId, appSupportsHdr);

        try {
            if (derCertData != null) {
                serverCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(derCertData));

                httpConn = new NvHTTP(new ComputerDetails.AddressTuple(host, port), httpsPort, uniqueId, serverCert, PlatformBinding.getCryptoProvider(this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (appId == StreamConfiguration.INVALID_APP_ID) {
            finish();
            return;
        }

        // Initialize the MediaCodec helper before creating the decoder
        GlPreferences glPrefs = GlPreferences.readPreferences(this);
        MediaCodecHelper.initialize(this, glPrefs.glRenderer);

        // Check if the user has enabled HDR
        boolean willStreamHdr = false;
        if (prefConfig.enableHdr) {
            // Start our HDR checklist
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Display display = getWindowManager().getDefaultDisplay();
                Display.HdrCapabilities hdrCaps = display.getHdrCapabilities();

                // We must now ensure our display is compatible with HDR10
                if (hdrCaps != null) {
                    // getHdrCapabilities() returns null on Lenovo Lenovo Mirage Solo (vega), Android 8.0
                    for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                        if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                            willStreamHdr = true;
                            break;
                        }
                    }
                }

                if (!willStreamHdr) {
                    // Nope, no HDR for us :(
                    Toast.makeText(this, "Display does not support HDR10", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(this, "HDR requires Android 7.0 or later", Toast.LENGTH_LONG).show();
            }
        }

        // Check if the user has enabled performance stats overlay
        if (prefConfig.enablePerfOverlay) {
            performanceOverlayView.setVisibility(View.VISIBLE);
            if(prefConfig.enablePerfOverlayLite){
                performanceOverlayLite.setVisibility(View.VISIBLE);
                if(prefConfig.enablePerfOverlayLiteDialog){
                    performanceOverlayLite.setOnClickListener(v -> showGameMenu(null));
                }
            }else{
                performanceOverlayBig.setVisibility(View.VISIBLE);
            }
        }

        decoderRenderer = new MediaCodecDecoderRenderer(
                this,
                prefConfig,
                new CrashListener() {
                    @Override
                    public void notifyCrash(Exception e) {
                        // The MediaCodec instance is going down due to a crash
                        // let's tell the user something when they open the app again

                        // We must use commit because the app will crash when we return from this function
                        tombstonePrefs.edit().putInt("CrashCount", tombstonePrefs.getInt("CrashCount", 0) + 1).commit();
                        reportedCrash = true;
                    }
                },
                tombstonePrefs.getInt("CrashCount", 0),
                connMgr.isActiveNetworkMetered(),
                willStreamHdr,
                shouldInvertDecoderResolution,
                glPrefs.glRenderer,
                this);

        // Don't stream HDR if the decoder can't support it
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported() && !decoderRenderer.isAv1Main10Supported()) {
            willStreamHdr = false;
            Toast.makeText(this, "Decoder does not support HDR10 profile", Toast.LENGTH_LONG).show();
        }

        // Display a message to the user if HEVC was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_HEVC && !decoderRenderer.isHevcSupported()) {
            Toast.makeText(this, "No HEVC decoder found", Toast.LENGTH_LONG).show();
        }

        // Display a message to the user if AV1 was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_AV1 && !decoderRenderer.isAv1Supported()) {
            Toast.makeText(this, "No AV1 decoder found", Toast.LENGTH_LONG).show();
        }

        // H.264 is always supported
        int supportedVideoFormats = MoonBridge.VIDEO_FORMAT_H264;
        if (decoderRenderer.isHevcSupported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265;
            if (willStreamHdr && decoderRenderer.isHevcMain10Hdr10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_H265_MAIN10;
            }
        }
        if (decoderRenderer.isAv1Supported()) {
            supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;
            if (willStreamHdr && decoderRenderer.isAv1Main10Supported()) {
                supportedVideoFormats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN10;
            }
        }

        int gamepadMask = ControllerHandler.getAttachedControllerMask(this);
        if (!prefConfig.multiController) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            gamepadMask = 1;
        }
        if (prefConfig.onscreenController) {
            // If we're using OSC, always set at least gamepad 1.
            gamepadMask |= 1;
        }

        // Set to the optimal mode for streaming
        float displayRefreshRate = prepareDisplayForRendering();
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        int roundedRefreshRate = Math.round(displayRefreshRate);
        float chosenFrameRate = prefConfig.fps;
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    // Use frame drops when rendering above the screen frame rate
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Using drop mode for FPS > Hz");
                } else if (roundedRefreshRate <= 49) {
                    // Let's avoid clearly bogus refresh rates and fall back to legacy rendering
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Bogus refresh rate: " + roundedRefreshRate);
                }
                else {
                    chosenFrameRate = roundedRefreshRate - 1;
                    LimeLog.info("Adjusting FPS target for screen to " + chosenFrameRate);
                }
            }
        }

        if (prefConfig.framePacingWarpFactor > 0) {
            chosenFrameRate *= prefConfig.framePacingWarpFactor;
        }

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(
                        displayWidth,
                        displayHeight
                )
                .setLaunchRefreshRate(prefConfig.fps)
                .setRefreshRate(chosenFrameRate)
                .setVirtualDisplay(vDisplay)
                .setResolutionScaleFactor(prefConfig.resolutionScaleFactor)
                .setApp(app)
                .setBitrate(isMetered ? prefConfig.meteredBitrate: prefConfig.bitrate)
                .setEnableSops(prefConfig.enableSops)
                .enableLocalAudioPlayback(prefConfig.playHostAudio)
                .setMaxPacketSize(1392)
                .setRemoteConfiguration(StreamConfiguration.STREAM_CFG_AUTO) // NvConnection will perform LAN and VPN detection
                .setSupportedVideoFormats(supportedVideoFormats)
                .setAttachedGamepadMask(gamepadMask)
                .setClientRefreshRateX100((int)(displayRefreshRate * 100))
                .setAudioConfiguration(prefConfig.audioConfiguration)
                .setColorSpace(decoderRenderer.getPreferredColorSpace())
                .setColorRange(decoderRenderer.getPreferredColorRange())
                .setPersistGamepadsAfterDisconnect(!prefConfig.multiController)
                .build();

        // Initialize the connection
        conn = new NvConnection(getApplicationContext(),
                new ComputerDetails.AddressTuple(host, port),
                httpsPort, uniqueId, config,
                PlatformBinding.getCryptoProvider(this), serverCert);
        controllerHandler = new ControllerHandler(this, conn, this, prefConfig);
        keyboardTranslator = new KeyboardTranslator(prefConfig);

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(keyboardTranslator, null);

        // Initialize touch contexts
        String mouseMode = PreferenceManager.getDefaultSharedPreferences(this).getString("mouse_mode_list", "0");
        applyMouseMode(Integer.parseInt(mouseMode));

        // Initialize trackpad contexts
        for (int i = 0; i < trackpadContextMap.length; i++) {
            trackpadContextMap[i] = new TrackpadContext(conn, i, prefConfig.trackpadSwapAxis, prefConfig.trackpadSensitivityX, prefConfig.trackpadSensitivityY);
        }

        if (prefConfig.onscreenController) {
            // create virtual onscreen controller
            if (prefConfig.hideOSCWhenHasGamepad) {
                if (!controllerHandler.hasController()) {
                    initVirtualController();
                }
            } else {
                initVirtualController();
            }
        }

        //特殊按键屏幕布局
        if(prefConfig.enableKeyboard){
            initKeyboardController();
        }

        if (!decoderRenderer.isAvcSupported()) {
            if (spinner != null) {
                spinner.dismiss();
                spinner = null;
            }

            // If we can't find an AVC decoder, we can't proceed
            Dialog.displayDialog(this, getResources().getString(R.string.conn_error_title),
                    "This device or ROM doesn't support hardware accelerated H.264 playback.", true);
            return;
        }

        // The connection will be started when the surface gets created
        streamView.getHolder().addCallback(this);

        //外接显示器模式
        if(prefConfig.enableExDisplay){
            showSecondScreen();
        }

        gameMenuCallbacks = new GameMenu(this, conn);

        // Setup floating menu button
        floatingMenuButton = findViewById(R.id.floatingMenuButton);
        updateFloatingButtonVisibility();
        
        // Touch listener for drag and click
        floatingMenuButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        isMovingButton = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        
                        // Check if it's a move or just a tap
                        if (Math.abs(event.getRawX() - startX) > CLICK_ACTION_THRESHOLD ||
                                Math.abs(event.getRawY() - startY) > CLICK_ACTION_THRESHOLD) {
                            isMovingButton = true;
                        }
                        
                        // Ensure the button stays within screen bounds
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX > getWindow().getDecorView().getWidth() - view.getWidth()) {
                            newX = getWindow().getDecorView().getWidth() - view.getWidth();
                        }
                        if (newY > getWindow().getDecorView().getHeight() - view.getHeight()) {
                            newY = getWindow().getDecorView().getHeight() - view.getHeight();
                        }
                        
                        view.setX(newX);
                        view.setY(newY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isMovingButton) {
                            // It's a click event, show menu
                            showGameMenu(null);
                        }
                        isMovingButton = false;
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void initKeyboardController(){
        keyBoardController = new KeyBoardController(conn,(FrameLayout)rootView, this);
        keyBoardController.refreshLayout();
        keyBoardController.show();
    }


    private void initVirtualController(){
        if(virtualController == null){
            try {
                LimeLog.info("Initializing virtual controller");
                
                // Get the root content view which is guaranteed to exist
                FrameLayout containerLayout = (FrameLayout)findViewById(android.R.id.content);
                
                if (containerLayout == null) {
                    LimeLog.severe("Failed to find content frame layout");
                    Toast.makeText(this, "Failed to initialize virtual controller: Layout not found", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                LimeLog.info("Found container layout: " + containerLayout);
                
                // Create and initialize the virtual controller
                virtualController = new VirtualController(controllerHandler, containerLayout, this);
                virtualController.refreshLayout();
                
                // Initialize GamepadLayoutManager if it doesn't exist yet
                if (gamepadLayoutManager == null) {
                    gamepadLayoutManager = new GamepadLayoutManager(this, virtualController);
                    
                    // Load the most recently used layout if available
                    try {
                        SharedPreferences prefs = getSharedPreferences("controller_layouts", MODE_PRIVATE);
                        String lastLayout = prefs.getString("last_layout", GamepadLayoutManager.DEFAULT_LAYOUT_NAME);
                        if (lastLayout != null && !lastLayout.isEmpty()) {
                            gamepadLayoutManager.loadLayout(lastLayout);
                            LimeLog.info("Loaded last used layout: " + lastLayout);
                        }
                    } catch (Exception e) {
                        LimeLog.warning("Failed to load last layout: " + e.getMessage());
                    }
                }
                
                // Set visibility based on preference
                if (prefConfig != null && prefConfig.onscreenController) {
                    virtualController.show();
                } else {
                    virtualController.hide();
                }
                
                LimeLog.info("Virtual controller initialized successfully");
            } catch (Exception e) {
                LimeLog.severe("Error initializing virtual controller: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Failed to initialize virtual controller: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initkeyBoardLayoutController(){
        keyBoardLayoutController = new KeyBoardLayoutController((FrameLayout)rootView, this, prefConfig);
        keyBoardLayoutController.refreshLayout();
        keyBoardLayoutController.show();
    }

    //显示隐藏虚拟特殊按键
    public void showHideKeyboardController(){
        if(keyBoardController==null){
            initKeyboardController();
            return;
        }
        keyBoardController.toggleVisibility();
    }

    public void showHidekeyBoardLayoutController(){
        if(keyBoardLayoutController==null){
            initkeyBoardLayoutController();
            return;
        }
        keyBoardLayoutController.toggleVisibility();
    }

    //显示隐藏虚拟手柄控制器
    public void showHideVirtualController(){
        if (virtualController == null) {
            initVirtualController();
            // Make sure to apply the current saved layout
            if (gamepadLayoutManager != null) {
                gamepadLayoutManager.loadLayout(gamepadLayoutManager.getCurrentLayoutName());
            }
            virtualController.show();
            prefConfig.onscreenController = true;
            Toast.makeText(this, R.string.virtual_controller_shown, Toast.LENGTH_SHORT).show();
        } else {
            int result = virtualController.switchShowHide();
            prefConfig.onscreenController = (result != 0);
            if (result == 0) {
                Toast.makeText(this, R.string.virtual_controller_hidden, Toast.LENGTH_SHORT).show();
            } else {
                // Khi hiển thị lại, hãy đảm bảo áp dụng layout hiện tại
                if (gamepadLayoutManager != null) {
                    gamepadLayoutManager.loadLayout(gamepadLayoutManager.getCurrentLayoutName());
                }
                Toast.makeText(this, R.string.virtual_controller_shown, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setPreferredOrientationForCurrentDisplay() {
        Display display = getWindowManager().getDefaultDisplay();

        // For semi-square displays, we use more complex logic to determine which orientation to use (if any)
        if (PreferenceConfiguration.isSquarishScreen(display)) {
            int desiredOrientation = Configuration.ORIENTATION_UNDEFINED;

            // OSC doesn't properly support portrait displays, so don't use it in portrait mode by default
            if (prefConfig.onscreenController) {
                desiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
            }

            // For native resolution, we will lock the orientation to the one that matches the specified resolution
            if (PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height)) {
                if (displayWidth > displayHeight) {
                    desiredOrientation = Configuration.ORIENTATION_LANDSCAPE;
                }
                else {
                    desiredOrientation = Configuration.ORIENTATION_PORTRAIT;
                }
            }

            if (desiredOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            }
            else if (desiredOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            }
            else {
                // If we don't have a reason to lock to portrait or landscape, allow any orientation
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            }
        }
        else {
            // Lock to current orientation
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Set requested orientation for possible new screen size
        setPreferredOrientationForCurrentDisplay();
        
        // Save virtual controller state if exists
        boolean wasVCVisible = false;
        boolean wasInEditMode = isEditingLayout;
        String editingLayout = currentEditingLayout;
        
        if (virtualController != null) {
            // Check if the controller was visible
            wasVCVisible = virtualController.isVisible();
            
            // Refresh layout of OSC for possible new screen size
            virtualController.refreshLayout();
            
            // Reload current layout
            if (gamepadLayoutManager != null) {
                gamepadLayoutManager.loadLayout(gamepadLayoutManager.getCurrentLayoutName());
            }
            
            // Restore edit mode if needed
            if (wasInEditMode && editingLayout != null) {
                startEditingLayout(editingLayout);
            } else if (wasVCVisible) {
                virtualController.show();
            }
        }

        if(keyBoardController != null){
            keyBoardController.refreshLayout();
        }

        if(keyBoardLayoutController != null){
            keyBoardLayoutController.refreshLayout();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                isHidingOverlays = true;

                if (virtualController != null) {
                    virtualController.hide();
                }

                if (keyBoardController != null && keyBoardController.shown) {
                    keyBoardController.hide(true);
                }

                if (keyBoardLayoutController!=null && keyBoardLayoutController.shown) {
                    keyBoardLayoutController.hide(true);
                }

                hideGameMenu();

                performanceOverlayView.setVisibility(View.GONE);
                notificationOverlayView.setVisibility(View.GONE);

                // Disable sensors while in PiP mode
                controllerHandler.disableSensors();

                // Update GameManager state to indicate we're in PiP (still gaming, but interruptible)
                UiHelper.notifyStreamEnteringPiP(this);
            }
            else {
                isHidingOverlays = false;
                
                // Restore overlays to previous state when leaving PiP
                if (virtualController != null && wasVCVisible && !isEditingLayout) {
                    virtualController.show();
                }

                if (keyBoardController != null && keyBoardController.shown) {
                    keyBoardController.show();
                }
                
                if(keyBoardLayoutController!=null && keyBoardLayoutController.shown){
                    keyBoardLayoutController.show();
                }

                if (prefConfig.enablePerfOverlay) {
                    performanceOverlayView.setVisibility(View.VISIBLE);
                }

                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);

                // Enable sensors again after exiting PiP
                controllerHandler.enableSensors();

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private PictureInPictureParams getPictureInPictureParams(boolean autoEnter) {
        View view;
        Rect hint;
        if (prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FIT && streamView.getScaleX() == 1) {
            view = streamView;
        } else {
            view = (View)rootView;
        }

        int[] viewLocation = new int[2];

        view.getLocationOnScreen(viewLocation);

        int left = viewLocation[0];
        int top = viewLocation[1];
        int width = view.getWidth();
        int height = view.getHeight();
        Rational aspectRatio = new Rational(width, height);
        hint = new Rect(left, top, left + width, top + height);

        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .setSourceRectHint(hint);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter);
            builder.setSeamlessResizeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName);
                if (pcName != null) {
                    builder.setSubtitle(pcName);
                }
            }
            else if (pcName != null) {
                builder.setTitle(pcName);
            }
        }

        return builder.build();
    }

    public void updatePipAutoEnter() {
        if (!prefConfig.enablePip) {
            return;
        }

        boolean autoEnter = connected && suppressPipRefCount == 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(getPictureInPictureParams(autoEnter));
        }
        else {
            autoEnterPip = autoEnter;
        }
    }

    public void setMetaKeyCaptureState(boolean enabled) {
        // This uses custom APIs present on some Samsung devices to allow capture of
        // meta key events while streaming.
        try {
            Class<?> semWindowManager = Class.forName("com.samsung.android.view.SemWindowManager");
            Method getInstanceMethod = semWindowManager.getMethod("getInstance");
            Object manager = getInstanceMethod.invoke(null);

            if (manager != null) {
                Class<?>[] parameterTypes = new Class<?>[2];
                parameterTypes[0] = ComponentName.class;
                parameterTypes[1] = boolean.class;
                Method requestMetaKeyEventMethod = semWindowManager.getDeclaredMethod("requestMetaKeyEvent", parameterTypes);
                requestMetaKeyEventMethod.invoke(manager, this.getComponentName(), enabled);
            }
            else {
                LimeLog.warning("SemWindowManager.getInstance() returned null");
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // PiP is only supported on Oreo and later, and we don't need to manually enter PiP on
        // Android S and later. On Android R, we will use onPictureInPictureRequested() instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (autoEnterPip) {
                try {
                    // This has thrown all sorts of weird exceptions on Samsung devices
                    // running Oreo. Just eat them and close gracefully on leave, rather
                    // than crashing.
                    enterPictureInPictureMode(getPictureInPictureParams(false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        // Enter PiP when requested unless we're on Android 12 which supports auto-enter.
        if (autoEnterPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureMode(getPictureInPictureParams(false));
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // We can't guarantee the state of modifiers keys which may have
        // lifted while focus was not on us. Clear the modifier state.
        this.modifierFlags = 0;

        // With Android native pointer capture, capture is lost when focus is lost,
        // so it must be requested again when focus is regained.
        inputCaptureProvider.onWindowFocusChanged(hasFocus);
    }

    private boolean isRefreshRateEqualMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                refreshRate <= prefConfig.fps + 3;
    }

    private boolean isRefreshRateGoodMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                Math.round(refreshRate) % prefConfig.fps <= 3;
    }

    private boolean shouldIgnoreInsetsForResolution(int width, int height) {
        // Never ignore insets for non-native resolutions
        if (!PreferenceConfiguration.isNativeResolution(width, height)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display display = getWindowManager().getDefaultDisplay();
            for (Display.Mode candidate : display.getSupportedModes()) {
                // Ignore insets if this is an exact match for the display resolution
                if ((width == candidate.getPhysicalWidth() && height == candidate.getPhysicalHeight()) ||
                        (height == candidate.getPhysicalWidth() && width == candidate.getPhysicalHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean mayReduceRefreshRate() {
        return prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS ||
                prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS ||
                (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED && prefConfig.reduceRefreshRate);
    }

    private float prepareDisplayForRendering() {
        Display display = getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams windowLayoutParams = getWindow().getAttributes();
        float displayRefreshRate;

        // On M, we can explicitly set the optimal display mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode bestMode = display.getMode();
            boolean isNativeResolutionStream = PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height);
            boolean refreshRateIsGood = isRefreshRateGoodMatch(bestMode.getRefreshRate());
            boolean refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.getRefreshRate());

            LimeLog.info("Current display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            for (Display.Mode candidate : display.getSupportedModes()) {
                boolean refreshRateReduced = candidate.getRefreshRate() < bestMode.getRefreshRate();
                boolean resolutionReduced = candidate.getPhysicalWidth() < bestMode.getPhysicalWidth() ||
                        candidate.getPhysicalHeight() < bestMode.getPhysicalHeight();
                boolean resolutionFitsStream = candidate.getPhysicalWidth() >= prefConfig.width &&
                        candidate.getPhysicalHeight() >= prefConfig.height;

                LimeLog.info("Examining display mode: "+candidate.getPhysicalWidth()+"x"+
                        candidate.getPhysicalHeight()+"x"+candidate.getRefreshRate());

                if (candidate.getPhysicalWidth() > 4096 && prefConfig.width <= 4096) {
                    // Avoid resolutions options above 4K to be safe
                    continue;
                }

                // On non-4K streams, we force the resolution to never change unless it's above
                // 60 FPS, which may require a resolution reduction due to HDMI bandwidth limitations,
                // or it's a native resolution stream.
                if (prefConfig.width < 3840 && prefConfig.fps <= 60 && !isNativeResolutionStream) {
                    if (display.getMode().getPhysicalWidth() != candidate.getPhysicalWidth() ||
                            display.getMode().getPhysicalHeight() != candidate.getPhysicalHeight()) {
                        continue;
                    }
                }

                // Make sure the resolution doesn't regress unless if it's over 60 FPS
                // where we may need to reduce resolution to achieve the desired refresh rate.
                if (resolutionReduced && !(prefConfig.fps > 60 && resolutionFitsStream)) {
                    continue;
                }

                if (mayReduceRefreshRate() && refreshRateIsEqual && !isRefreshRateEqualMatch(candidate.getRefreshRate())) {
                    // If we had an equal refresh rate and this one is not, skip it. In min latency
                    // mode, we want to always prefer the highest frame rate even though it may cause
                    // microstuttering.
                    continue;
                }
                else if (refreshRateIsGood) {
                    // We've already got a good match, so if this one isn't also good, it's not
                    // worth considering at all.
                    if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                        continue;
                    }

                    if (mayReduceRefreshRate()) {
                        // User asked for the lowest possible refresh rate, so don't raise it if we
                        // have a good match already
                        if (candidate.getRefreshRate() > bestMode.getRefreshRate()) {
                            continue;
                        }
                    }
                    else {
                        // User asked for the highest possible refresh rate, so don't reduce it if we
                        // have a good match already
                        if (refreshRateReduced) {
                            continue;
                        }
                    }
                }
                else if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                    // We didn't have a good match and this match isn't good either, so just don't
                    // reduce the refresh rate.
                    if (refreshRateReduced) {
                        continue;
                    }
                } else {
                    // We didn't have a good match and this match is good. Prefer this refresh rate
                    // even if it reduces the refresh rate. Lowering the refresh rate can be beneficial
                    // when streaming a 60 FPS stream on a 90 Hz device. We want to select 60 Hz to
                    // match the frame rate even if the active display mode is 90 Hz.
                }

                bestMode = candidate;
                refreshRateIsGood = isRefreshRateGoodMatch(candidate.getRefreshRate());
                refreshRateIsEqual = isRefreshRateEqualMatch(candidate.getRefreshRate());
            }

            LimeLog.info("Best display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());

            // Only apply new window layout parameters if we've actually changed the display mode
            if (display.getMode().getModeId() != bestMode.getModeId()) {
                // If we only changed refresh rate and we're on an OS that supports Surface.setFrameRate()
                // use that instead of using preferredDisplayModeId to avoid the possibility of triggering
                // bugs that can cause the system to switch from 4K60 to 4K24 on Chromecast 4K.
                if (prefConfig.enforceDisplayMode ||
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        display.getMode().getPhysicalWidth() != bestMode.getPhysicalWidth() ||
                        display.getMode().getPhysicalHeight() != bestMode.getPhysicalHeight()) {
                    // Apply the display mode change
                    windowLayoutParams.preferredDisplayModeId = bestMode.getModeId();
                    getWindow().setAttributes(windowLayoutParams);
                }
                else {
                    LimeLog.info("Using setFrameRate() instead of preferredDisplayModeId due to matching resolution");
                }
            }
            else {
                LimeLog.info("Current display mode is already the best display mode");
            }

            displayRefreshRate = bestMode.getRefreshRate();
        }
        // On L, we can at least tell the OS that we want a refresh rate
        else {
            float bestRefreshRate = display.getRefreshRate();
            for (float candidate : display.getSupportedRefreshRates()) {
                LimeLog.info("Examining refresh rate: "+candidate);

                if (candidate > bestRefreshRate) {
                    // Ensure the frame rate stays around 60 Hz for <= 60 FPS streams
                    if (prefConfig.fps <= 60) {
                        if (candidate >= 63) {
                            continue;
                        }
                    }

                    bestRefreshRate = candidate;
                }
            }

            LimeLog.info("Selected refresh rate: "+bestRefreshRate);
            windowLayoutParams.preferredRefreshRate = bestRefreshRate;
            displayRefreshRate = bestRefreshRate;

            // Apply the refresh rate change
            getWindow().setAttributes(windowLayoutParams);
        }

        // Until Marshmallow, we can't ask for a 4K display mode, so we'll
        // need to hint the OS to provide one.
        boolean aspectRatioMatch = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // We'll calculate whether we need to scale by aspect ratio. If not, we'll use
            // setFixedSize so we can handle 4K properly. The only known devices that have
            // >= 4K screens have exactly 4K screens, so we'll be able to hit this good path
            // on these devices. On Marshmallow, we can start changing to 4K manually but no
            // 4K devices run 6.0 at the moment.
            Point screenSize = new Point(0, 0);
            display.getSize(screenSize);

            double screenAspectRatio = ((double)screenSize.y) / screenSize.x;
            double streamAspectRatio = ((double)displayHeight) / displayWidth;
            if (Math.abs(screenAspectRatio - streamAspectRatio) < 0.001) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        // Don't do setFixedSize since it might not update the view dimensions correctly when entering PiP mode
        if (!(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH || aspectRatioMatch)) {
            // Set the surface to scale based on the aspect ratio of the stream
            streamView.setDesiredAspectRatio((double)displayWidth / (double)displayHeight);
            streamView.setFillDisplay(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FILL);
            LimeLog.info("surfaceChanged-->"+(double)displayWidth / (double)displayHeight);
            LimeLog.info("scaleMode-->"+prefConfig.videoScaleMode);
        }

        // Set the desired refresh rate that will get passed into setFrameRate() later
        desiredRefreshRate = displayRefreshRate;

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // TVs may take a few moments to switch refresh rates, and we can probably assume
            // it will be eventually activated.
            // TODO: Improve this
            return displayRefreshRate;
        }
        else {
            // Use the lower of the current refresh rate and the selected refresh rate.
            // The preferred refresh rate may not actually be applied (ex: Battery Saver mode).
            return Math.min(getWindowManager().getDefaultDisplay().getRefreshRate(), displayRefreshRate);
        }
    }

    @SuppressLint("InlinedApi")
    private final Runnable hideSystemUi = new Runnable() {
        @Override
        public void run() {
            // TODO: Do we want to use WindowInsetsController here on R+ instead of
            // SYSTEM_UI_FLAG_IMMERSIVE_STICKY? They seem to do the same thing as of S...

            // In multi-window mode on N+, we need to drop our layout flags or we'll
            // be drawing underneath the system UI.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                Game.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            else {
                // Use immersive mode
                Game.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    };
    
    // Additional methods and code would go here
    
} // This is the closing brace for the Game class
