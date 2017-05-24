package com.tutk.P2PCam264.DELUX.Playback;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.AVIOCTRLDEFs.STimeDay;
import com.tutk.IOTC.Camera;
import com.tutk.IOTC.IMonitor;
import com.tutk.IOTC.IRegisterIOTCListener;
import com.tutk.IOTC.LargeDownloadListener;
import com.tutk.IOTC.MediaCodecListener;
import com.tutk.IOTC.MediaCodecMonitor;
import com.tutk.IOTC.Packet;
import com.tutk.P2PCam264.DELUX.Activity.GridViewGalleryActivity;
import com.tutk.P2PCam264.DELUX.EventList.Activity.EventListActivity;
import com.tutk.P2PCam264.DELUX.EventList.Fragment.EventListFragment;
import com.tutk.P2PCam264.DELUX.MultiView.Activity.MultiViewActivity;
import com.tutk.P2PCam264.DELUX.Photo.Fragment.PhotoListFragment;
import com.tutk.P2PCam264.DELUX.Structure.DeviceInfo;
import com.tutk.P2PCam264.MyCamera;
import com.tutk.P2PCam264.R;
import com.tutk.P2PCam264.ui.Custom_OkCancle_Dialog;
import com.tutk.P2PCam264.util.appteam.WifiAdmin;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlaybackActivity extends SherlockActivity implements IRegisterIOTCListener, MediaCodecListener,
        Custom_OkCancle_Dialog.OkCancelDialogListener, LargeDownloadListener {

    private static final int Build_VERSION_CODES_ICE_CREAM_SANDWICH = 14;
    private static final int STS_CHANGE_CHANNEL_STREAMINFO = 99;
    private final boolean SOFTWARE_DECODE = true;
    private final boolean HARDWARE_DECODE = false;

    // private TouchedMonitor monitor = null;
    private IMonitor monitor = null;
    //	private IMonitor mSoftMonitor = null;
//    private IMonitor mHardMonitor = null;
    private MyCamera mCamera = null;
    private SharedPreferences mCodecSettings;


    private RelativeLayout layoutSoftMonitor;
    private RelativeLayout layoutHardMonitor;

    private TextView txtEventType;
    private TextView txtEventTime;
    private TextView txtResolution;
    private TextView txtFrameRate;
    private TextView txtBitRate;
    private TextView txtFrameCount;
    private TextView txtIncompleteFrameCount;

    private String mDevUUID;
    private String mDevNickname;
    private String mViewAcc;
    private String mViewPwd;
    private String mEvtUUID;
    private String mFilePath;

    private int mCameraChannel;
    private int mEvtType;
    private STimeDay mEvtTime2;

    private int mVideoWidth;
    private int mVideoHeight;

    private final int MEDIA_STATE_STOPPED = 0;
    private final int MEDIA_STATE_PLAYING = 1;
    private final int MEDIA_STATE_PAUSED = 2;
    private final int MEDIA_STATE_OPENING = 3;

    private int mPlaybackChannel = -1;
    private int mMediaState = MEDIA_STATE_STOPPED;
    private int mMiniMonitorHeight;

    private BitmapDrawable bg;
    private BitmapDrawable bgSplit;
    private ImageButton btnPlayPause;
    private ImageButton btnDownload;
    private ProgressBar progress;

    private boolean mIsListening = true;
    private boolean unavailable = false;
    private boolean mHasFile = false;

    private enum FrameMode {
        PORTRAIT, LANDSCAPE_ROW_MAJOR, LANDSCAPE_COL_MAJOR
    }

    private FrameMode mFrameMode = FrameMode.PORTRAIT;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playback_portrait);

        bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
        bgSplit = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped_split_img);

        Bundle bundle = this.getIntent().getExtras();
        mDevUUID = bundle != null ? bundle.getString("dev_uuid") : "";
        mDevNickname = bundle != null ? bundle.getString("dev_nickname") : "";
        mCameraChannel = bundle != null ? bundle.getInt("camera_channel") : -1;
        mViewAcc = bundle != null ? bundle.getString("view_acc") : "";
        mViewPwd = bundle != null ? bundle.getString("view_pwd") : "";
        mEvtType = bundle != null ? bundle.getInt("event_type") : -1;
        // mEvtTime = bundle != null ? bundle.getLong("event_time") : -1;
        mEvtUUID = bundle != null ? bundle.getString("event_uuid") : null;
        mEvtTime2 = bundle != null ? new STimeDay(bundle.getByteArray("event_time2")) : null;
        mHasFile = bundle != null ? bundle.getBoolean("has_file") : false;
        mFilePath = bundle != null ? bundle.getString("path") : null;

        for (MyCamera camera : MultiViewActivity.CameraList) {

            if (mDevUUID.equalsIgnoreCase(camera.getUUID())) {
                mCamera = camera;
                mCamera.registerIOTCListener(this);
                mCamera.resetEventCount();
                break;
            }
        }

        mCodecSettings = getSharedPreferences("CodecSettings", 0);
        if (mCodecSettings != null) {
            unavailable = mCodecSettings.getBoolean("unavailable", false);
        }

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getOrientation();

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            setupViewInPortraitLayout(unavailable);
        } else {
            setupViewInLandscapeLayout(unavailable);
        }

        if (mCamera != null) {
            mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
                    AVIOCTRLDEFs.SMsgAVIoctrlPlayRecord.parseContent(mCameraChannel, AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_START, 0,
                            mEvtTime2.toByteArray()));
            mMediaState = MEDIA_STATE_OPENING;

			/* if server no response, close playback function */
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mPlaybackChannel < 0 && mMediaState == MEDIA_STATE_OPENING) {
                        mMediaState = MEDIA_STATE_STOPPED;
                        Toast.makeText(PlaybackActivity.this, getText(R.string.tips_play_record_timeout), Toast.LENGTH_SHORT).show();
                        if (btnPlayPause != null) {
                            btnPlayPause.setBackgroundResource(R.drawable.btn_play);
                        }
                    }
                }
            }, 5000);
        }

        Custom_OkCancle_Dialog.SetDialogListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        quit();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.stopListening(mPlaybackChannel);
            mCamera.stopShow(mPlaybackChannel);
            mCamera.stop(mPlaybackChannel);
            mCamera.unregisterIOTCListener(this);
        }

        if (monitor != null) {
            monitor.deattachCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

//        if (mSoftMonitor != null) {
//            mSoftMonitor.deattachCamera();
//        }
//        if (mHardMonitor != null) {
//            mHardMonitor.deattachCamera();
//        }

        if (monitor != null) {
            monitor.deattachCamera();
        }

        Configuration cfg = getResources().getConfiguration();

        if (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (unavailable) {
                setupViewInLandscapeLayout(SOFTWARE_DECODE);
            } else {
                setupViewInLandscapeLayout(HARDWARE_DECODE);
            }

        } else if (cfg.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (unavailable) {
                setupViewInPortraitLayout(SOFTWARE_DECODE);
            } else {
                setupViewInPortraitLayout(HARDWARE_DECODE);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {

            case KeyEvent.KEYCODE_BACK:

                quit();

                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    private OnClickListener clickPlayPause = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlaybackChannel < 0) {

                if (mCamera != null) {
                    mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
                            AVIOCTRLDEFs.SMsgAVIoctrlPlayRecord.parseContent(mCameraChannel, AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_START, 0,
                                    mEvtTime2.toByteArray()));
                    mMediaState = MEDIA_STATE_OPENING;

					/* if server no response, close playback function */
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mPlaybackChannel < 0 && mMediaState == MEDIA_STATE_OPENING) {
                                mMediaState = MEDIA_STATE_STOPPED;
                                Toast.makeText(PlaybackActivity.this, getText(R.string.tips_play_record_timeout), Toast.LENGTH_SHORT).show();
                                if (btnPlayPause != null) {
                                    btnPlayPause.setBackgroundResource(R.drawable.btn_play);
                                }
                            }
                        }
                    }, 5000);
                }
            } else {
                if (mCamera != null) {
                    mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
                            AVIOCTRLDEFs.SMsgAVIoctrlPlayRecord.parseContent(mCameraChannel, AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_PAUSE, 0,
                                    mEvtTime2.toByteArray()));

                    if (btnPlayPause != null) {
                        btnPlayPause.setBackgroundResource(R.drawable.btn_pause);
                    }
                }
            }
        }
    };

    private void setupViewInPortraitLayout(final boolean runSoftwareDecode) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.titlebar);
        TextView tv = (TextView) this.findViewById(R.id.bar_text);
        tv.setText(getText(R.string.dialog_Playback));

        setContentView(R.layout.playback_portrait);

        if (Build.VERSION.SDK_INT < Build_VERSION_CODES_ICE_CREAM_SANDWICH) {
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }

        txtEventType = (TextView) findViewById(R.id.txtEventType);
        txtEventTime = (TextView) findViewById(R.id.txtEventTime);
        txtResolution = (TextView) findViewById(R.id.txtResolution);
        txtFrameRate = (TextView) findViewById(R.id.txtFrameRate);
        txtBitRate = (TextView) findViewById(R.id.txtBitRate);
        txtFrameCount = (TextView) findViewById(R.id.txtFrameCount);
        txtIncompleteFrameCount = (TextView) findViewById(R.id.txtIncompleteFrameCount);
        btnPlayPause = (ImageButton) findViewById(R.id.btn_playpause);
        btnDownload = (ImageButton) findViewById(R.id.btnDownload);
        progress = (ProgressBar) findViewById(R.id.progressBar);

        btnPlayPause.setOnClickListener(clickPlayPause);
        btnPlayPause.setBackgroundResource(R.drawable.btn_pause);

        txtEventType.setText(MultiViewActivity.getEventType(PlaybackActivity.this, mEvtType, false));
        txtEventTime.setText(mEvtTime2.getLocalTime());

        layoutSoftMonitor = (RelativeLayout) findViewById(R.id.layoutSoftMonitor);
        layoutHardMonitor = (RelativeLayout) findViewById(R.id.layoutHardMonitor);

        if (runSoftwareDecode) {
            layoutSoftMonitor.setVisibility(View.VISIBLE);
            layoutHardMonitor.setVisibility(View.GONE);
            monitor = (IMonitor) findViewById(R.id.softMonitor);
        } else {
            layoutSoftMonitor.setVisibility(View.GONE);
            layoutHardMonitor.setVisibility(View.VISIBLE);
            monitor = (IMonitor) findViewById(R.id.hardMonitor);

            // calculate surface view size
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            final SurfaceView surfaceView = (SurfaceView) monitor;
            surfaceView.getLayoutParams().width = width;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (layoutHardMonitor.getMeasuredHeight() == 0) {
                        handler.postDelayed(this, 200);
                    } else {
                        mMiniMonitorHeight = layoutHardMonitor.getMeasuredHeight();
                        surfaceView.getLayoutParams().height = layoutHardMonitor.getMeasuredHeight();
                        surfaceView.setLayoutParams(surfaceView.getLayoutParams());
                        reScaleMonitor();
                    }
                }
            });
        }

        if (mPlaybackChannel >= 0) {
            monitor.enableDither(mCamera.mEnableDither);
            monitor.attachCamera(mCamera, mPlaybackChannel);
            monitor.setMediaCodecListener(this);
        }

        if (mHasFile) {
            btnDownload.setBackgroundResource(R.drawable.btn_download_h);
        } else {
            btnDownload.setBackgroundResource(R.drawable.btn_download);
        }

        btnDownload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHasFile) {
                    Custom_OkCancle_Dialog dlg = new Custom_OkCancle_Dialog(PlaybackActivity.this, getText(R.string.tips_download_video).toString());
                    dlg.setCanceledOnTouchOutside(false);
                    android.view.Window window = dlg.getWindow();
                    window.setWindowAnimations(R.style.setting_dailog_animstyle);
                    dlg.show();
                }
            }
        });
    }

    private void setupViewInLandscapeLayout(final boolean runSoftwareDecode) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.playback_landscape);

        if (Build.VERSION.SDK_INT < Build_VERSION_CODES_ICE_CREAM_SANDWICH) {
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }

        getActionBar().hide();

        txtEventType = null;
        txtEventTime = null;
        txtResolution = null;
        txtFrameRate = null;
        txtBitRate = null;
        txtFrameCount = null;
        txtIncompleteFrameCount = null;

        layoutSoftMonitor = (RelativeLayout) findViewById(R.id.layoutSoftMonitor);
        layoutHardMonitor = (RelativeLayout) findViewById(R.id.layoutHardMonitor);
        progress = (ProgressBar) findViewById(R.id.progressBar);

        if (runSoftwareDecode) {
            layoutSoftMonitor.setVisibility(View.VISIBLE);
            layoutHardMonitor.setVisibility(View.GONE);
            monitor = (IMonitor) findViewById(R.id.softMonitor);
        } else {
            layoutSoftMonitor.setVisibility(View.GONE);
            layoutHardMonitor.setVisibility(View.VISIBLE);
            monitor = (IMonitor) findViewById(R.id.hardMonitor);
        }

        if (mPlaybackChannel >= 0) {
            monitor.enableDither(mCamera.mEnableDither);
            monitor.attachCamera(mCamera, mPlaybackChannel);
            monitor.setMediaCodecListener(this);
        }
    }

    private void quit() {

        if (monitor != null) {
            monitor.deattachCamera();
        }

        if (mCamera != null) {

            if (mPlaybackChannel >= 0) {

                mCamera.stopListening(mPlaybackChannel);
                mCamera.stopShow(mPlaybackChannel);
                mCamera.stop(mPlaybackChannel);
                mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
                        AVIOCTRLDEFs.SMsgAVIoctrlPlayRecord.parseContent(mCameraChannel, AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_STOP, 0,
                                mEvtTime2.toByteArray()));
                mPlaybackChannel = -1;
            }
        }

        Bundle extras = new Bundle();
        extras.putInt("event_type", mEvtType);
        // extras.putLong("event_time", mEvtTime);
        extras.putByteArray("event_time2", mEvtTime2.toByteArray());
        extras.putString("event_uuid", mEvtUUID);
        extras.putBoolean("has_file", mHasFile);

        Intent intent = new Intent();
        intent.putExtras(extras);
        setResult(RESULT_OK, intent);
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    private void reScaleMonitor() {

        if (mVideoHeight == 0 || mVideoWidth == 0 || mMiniMonitorHeight == 0 || unavailable) {
            return;
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int screenWidth = size.x;
        final int screenHeight = size.y;

        final SurfaceView surfaceView;
        surfaceView = (SurfaceView) monitor;
        if (monitor != null) {
            monitor.callSurfaceChange();
        }


        if (surfaceView == null || layoutHardMonitor == null) {
            return;
        }

        /**
         * portrait mode
         */
        if (screenHeight >= screenWidth) {

            mFrameMode = FrameMode.PORTRAIT;
            surfaceView.getLayoutParams().width = screenWidth;
            surfaceView.getLayoutParams().height = (int) (screenWidth * mVideoHeight / (float) mVideoWidth);

            if (mMiniMonitorHeight < surfaceView.getLayoutParams().height) {
                surfaceView.getLayoutParams().height = mMiniMonitorHeight;
            }

            final int scrollViewHeight = surfaceView.getLayoutParams().height;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    layoutHardMonitor.setPadding(0, (mMiniMonitorHeight - scrollViewHeight) / 2, 0, 0);
                }
            });
        }
        /**
         * landscape mode
         */
        else {

            if (surfaceView.getLayoutParams().width > screenWidth) {
                /**
                 * up down space
                 */
                mFrameMode = FrameMode.LANDSCAPE_COL_MAJOR;

                surfaceView.getLayoutParams().width = screenWidth;
                surfaceView.getLayoutParams().height = (int) (screenWidth * mVideoHeight / (float) mVideoWidth);

                final int scrollViewHeight = surfaceView.getLayoutParams().height;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        int statusbar = 0;
                        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                        if (resourceId > 0) {
                            statusbar = getResources().getDimensionPixelSize(resourceId);
                        }
                        layoutHardMonitor.setPadding(0, (screenHeight - statusbar - scrollViewHeight) / 2, 0, 0);
                    }
                });
            } else {
                /**
                 * left right space
                 */
                mFrameMode = FrameMode.LANDSCAPE_ROW_MAJOR;

                surfaceView.getLayoutParams().height = screenHeight;
                surfaceView.getLayoutParams().width = (int) (screenHeight * mVideoWidth / (float) mVideoHeight);

                final int scrollViewWidth = surfaceView.getLayoutParams().width;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        int statusbar = 0;
                        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                        if (resourceId > 0) {
                            statusbar = getResources().getDimensionPixelSize(resourceId);
                        }
                        layoutHardMonitor.setPadding((screenWidth - scrollViewWidth) / 2, 0, 0, 0);
                    }
                });

            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                surfaceView.setLayoutParams(surfaceView.getLayoutParams());
            }
        });
    }


    @Override
    public void receiveFrameData(final Camera camera, int sessionChannel, Bitmap bmp) {

        if (mCamera == camera && sessionChannel == mPlaybackChannel && bmp != null) {
            mVideoWidth = bmp.getWidth();
            mVideoHeight = bmp.getHeight();
        }
    }

    @Override
    public void receiveSessionInfo(final Camera camera, int resultCode) {
    }

    @Override
    public void receiveChannelInfo(final Camera camera, int sessionChannel, int resultCode) {
    }

    @Override
    public void receiveFrameInfo(final Camera camera, int sessionChannel, long bitRate, int frameRate, int onlineNm, int frameCount,
                                 int incompleteFrameCount) {

        if (mCamera == camera && sessionChannel == mPlaybackChannel) {
            Bundle bundle = new Bundle();
            bundle.putInt("sessionChannel", sessionChannel);
            bundle.putInt("videoFPS", frameRate);
            bundle.putLong("videoBPS", bitRate);
            bundle.putInt("frameCount", frameCount);
            bundle.putInt("inCompleteFrameCount", incompleteFrameCount);

            Message msg = handler.obtainMessage();
            msg.what = STS_CHANGE_CHANNEL_STREAMINFO;
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    @Override
    public void receiveIOCtrlData(final Camera camera, int sessionChannel, int avIOCtrlMsgType, byte[] data) {

        if (mCamera == camera) {
            Bundle bundle = new Bundle();
            bundle.putInt("sessionChannel", sessionChannel);
            bundle.putByteArray("data", data);

            Message msg = new Message();
            msg.what = avIOCtrlMsgType;
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            byte[] data = bundle.getByteArray("data");

            if (msg.what == STS_CHANGE_CHANNEL_STREAMINFO) {

                int videoFPS = bundle.getInt("videoFPS");
                long videoBPS = bundle.getLong("videoBPS");
                int frameCount = bundle.getInt("frameCount");
                int inCompleteFrameCount = bundle.getInt("inCompleteFrameCount");

                if (txtResolution != null) {
                    txtResolution.setText(String.valueOf(mVideoWidth) + "x" + String.valueOf(mVideoHeight));
                }
                if (txtFrameRate != null) {
                    txtFrameRate.setText(String.valueOf(videoFPS));
                }
                if (txtBitRate != null) {
                    txtBitRate.setText(String.valueOf(videoBPS) + "Kb");
                }
                if (txtFrameCount != null) {
                    txtFrameCount.setText(String.valueOf(frameCount));
                }
                if (txtIncompleteFrameCount != null) {
                    txtIncompleteFrameCount.setText(String.valueOf(inCompleteFrameCount));
                }

            } else if (msg.what == AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP) {

                int command = Packet.byteArrayToInt_Little(data, 0);
                int result = Packet.byteArrayToInt_Little(data, 4);

                switch (command) {

                    case AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_START:

                        System.out.println("AVIOCTRL_RECORD_PLAY_START");

                        if (mMediaState == MEDIA_STATE_OPENING) {
                            if (0 <= result && result <= 31) {

                                mPlaybackChannel = result;
                                mMediaState = MEDIA_STATE_PLAYING;

                                if (mCamera != null) {
                                    mCamera.start(mPlaybackChannel, mViewAcc, mViewPwd);
                                    mCamera.startShow(mPlaybackChannel, false, unavailable);
                                    mCamera.startListening(mPlaybackChannel, mIsListening);
                                    monitor.enableDither(mCamera.mEnableDither);
                                    monitor.attachCamera(mCamera, mPlaybackChannel);
                                    monitor.setMediaCodecListener(PlaybackActivity.this);
                                }

                                if (btnPlayPause != null) {
                                    btnPlayPause.setBackgroundResource(R.drawable.btn_pause);
                                }

                            } else {
                                Toast.makeText(PlaybackActivity.this, PlaybackActivity.this.getText(R.string.tips_play_record_failed),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        break;

                    case AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_PAUSE:

                        System.out.println("AVIOCTRL_RECORD_PLAY_PAUSE");

                        if (mPlaybackChannel >= 0 && mCamera != null) {

                            if (mMediaState == MEDIA_STATE_PAUSED) {
                                mMediaState = MEDIA_STATE_PLAYING;
                                if (btnPlayPause != null) {
                                    btnPlayPause.setBackgroundResource(R.drawable.btn_pause);
                                }
                            } else if (mMediaState == MEDIA_STATE_PLAYING) {
                                mMediaState = MEDIA_STATE_PAUSED;
                                if (btnPlayPause != null) {
                                    btnPlayPause.setBackgroundResource(R.drawable.btn_play);
                                }
                            }

                            if (monitor != null) {
                                if (mMediaState == MEDIA_STATE_PAUSED) {
                                    monitor.deattachCamera();
                                } else {
                                    monitor.enableDither(mCamera.mEnableDither);
                                    monitor.attachCamera(mCamera, mPlaybackChannel);
                                }
                            }
                        }

                        break;

                    case AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_STOP:

                        System.out.println("AVIOCTRL_RECORD_PLAY_STOP");

                        if (mPlaybackChannel >= 0 && mCamera != null) {
                            mCamera.stopListening(mPlaybackChannel);
                            mCamera.stopShow(mPlaybackChannel);
                            mCamera.stop(mPlaybackChannel);
                            if (monitor != null) {
                                monitor.deattachCamera();
                            }
                        }

                        mPlaybackChannel = -1;
                        mMediaState = MEDIA_STATE_STOPPED;

                        if (btnPlayPause != null) {
                            btnPlayPause.setBackgroundResource(R.drawable.btn_play);
                        }

                        break;

                    case AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_END:

                        System.out.println("AVIOCTRL_RECORD_PLAY_END");

                        if (mPlaybackChannel >= 0 && mCamera != null) {
                            mCamera.stopListening(mPlaybackChannel);
                            mCamera.stopShow(mPlaybackChannel);
                            mCamera.stop(mPlaybackChannel);
                            if (monitor != null) {
                                monitor.deattachCamera();
                            }

                            mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
                                    AVIOCTRLDEFs.SMsgAVIoctrlPlayRecord.parseContent(mCameraChannel, AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_STOP, 0,
                                            mEvtTime2.toByteArray()));
                        }

                        Toast.makeText(PlaybackActivity.this, getText(R.string.tips_play_record_end), Toast.LENGTH_LONG).show();

                        if (txtFrameRate != null) {
                            txtFrameRate.setText("0");
                        }
                        if (txtBitRate != null) {
                            txtBitRate.setText("0kb");
                        }

                        mPlaybackChannel = -1;
                        mMediaState = MEDIA_STATE_STOPPED;

                        if (btnPlayPause != null) {
                            btnPlayPause.setBackgroundResource(R.drawable.btn_play);
                        }

                        break;

                    case AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_BACKWARD:

                        break;

                    case AVIOCTRLDEFs.AVIOCTRL_RECORD_PLAY_FORWARD:

                        break;
                }
            }

            super.handleMessage(msg);
        }
    };

    @Override
    public void receiveFrameDataForMediaCodec(Camera camera, int avChannel, byte[] buf, int length, int pFrmNo, byte[] pFrmInfoBuf,
                                              boolean isIframe, int codecId) {
        // TODO Auto-generated method stub
        if (monitor != null && monitor.getClass().equals(MediaCodecMonitor.class)) {
            if ((mVideoWidth != ((MediaCodecMonitor) monitor).getVideoWidth() || mVideoHeight != ((MediaCodecMonitor) monitor)
                    .getVideoHeight())) {

                mVideoWidth = ((MediaCodecMonitor) monitor).getVideoWidth();
                mVideoHeight = ((MediaCodecMonitor) monitor).getVideoHeight();

                reScaleMonitor();
            }
        }
    }

    @Override
    public void Unavailable() {
        if (unavailable) {
            return;
        }

        unavailable = true;
        if (mCodecSettings != null) {
            mCodecSettings.edit().putBoolean("unavailable", unavailable).commit();
        }

        if (monitor != null) {
            monitor.deattachCamera();
        }

        Configuration cfg = getResources().getConfiguration();

        if (cfg.orientation == Configuration.ORIENTATION_PORTRAIT) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCamera != null) {

                        mCamera.stopShow(mPlaybackChannel);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mCamera.startShow(mPlaybackChannel, true, SOFTWARE_DECODE);
                                setupViewInPortraitLayout(SOFTWARE_DECODE);
                            }
                        }, 1000);

                    }
                }
            });
        } else if (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCamera != null) {

                        mCamera.stopShow(mPlaybackChannel);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mCamera.startShow(mPlaybackChannel, true, true);
                                setupViewInLandscapeLayout(SOFTWARE_DECODE);
                            }
                        }, 1000);
                    }
                }
            });
        }
    }

    @Override
    public void monitorIsReady() {
        if (progress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void zoomSurface(float scale) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int screenWidth = size.x;
        final int screenHeight = size.y;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceView surfaceView = (SurfaceView) monitor;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) surfaceView.getLayoutParams();

                int paddingLeft = 0;
                int paddingTop = 0;

                if (mFrameMode == FrameMode.LANDSCAPE_COL_MAJOR) {
                    int statusbar = 0;
                    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resourceId > 0) {
                        statusbar = getResources().getDimensionPixelSize(resourceId);
                    }
                    paddingTop = (screenHeight - statusbar - lp.height) / 2;
                    if (paddingTop < 0)
                        paddingTop = 0;
                } else if (mFrameMode == FrameMode.LANDSCAPE_ROW_MAJOR) {
                    paddingLeft = (screenWidth - lp.width) / 2;
                    if (paddingLeft < 0)
                        paddingLeft = 0;
                } else {
                    paddingTop = (mMiniMonitorHeight - lp.height) / 2;
                    if (paddingTop < 0)
                        paddingTop = 0;
                }

                layoutHardMonitor.setPadding(paddingLeft, paddingTop, 0, 0);
            }
        });
    }

    @Override
    public void ok() {
        if (mFilePath.length() > 0 && mCamera != null) {

            String[] filter = mFilePath.split("\\\\");
            int path_end = filter.length;
            File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/");
            if (!rootFolder.exists()) {
                try {
                    rootFolder.mkdir();
                } catch (SecurityException se) {
                }
            }
            File appFolder = new File(rootFolder.getAbsolutePath() + "/KalayCar/");
            if (!appFolder.exists()) {
                try {
                    appFolder.mkdir();
                } catch (SecurityException se) {
                }
            }

            String store_path = appFolder.getAbsolutePath() + "/" + filter[path_end - 1];

            mCamera.initLargeDownloadManager(mCameraChannel);
            mCamera.EnqueueLargeDownloaReqList(mFilePath, store_path);
            mCamera.StartMultiLargeDownload(PlaybackActivity.this);
        }
    }

    @Override
    public void cancel() {

    }

    @Override
    public void getDownload(byte[] buf, int size, boolean start, boolean end) {

    }

    @Override
    public void getFinish(String path) {
        if (path.equals(mFilePath)) {
            if (btnDownload != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnDownload.setBackgroundResource(R.drawable.btn_download_h);
                        mHasFile = true;
                    }
                });
            }
        }
    }

    /**
     * Created by James Huang on 2015/4/3.
     */
    public static class CloudRecordingActivity extends SherlockActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {

            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            TextView tv = (TextView) this.findViewById(R.id.bar_text);
            tv.setText(getText(R.string.txt_cloud_recording));

            super.onCreate(savedInstanceState);

            setContentView(R.layout.cloud_recording_activity);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    return false;
            }

            return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * Created by James Huang on 2015/4/24.
     */
    public static class ViewRemotePhotoActivity extends Activity implements IRegisterIOTCListener, LargeDownloadListener,
            Custom_OkCancle_Dialog.OkCancelDialogListener, GestureDetector.OnGestureListener, View.OnTouchListener {

        private static final int FLING_MIN_DISTANCE = 20;
        private static final int FLING_MIN_VELOCITY = 0;

        private MyCamera mCamera;
        private ByteArrayOutputStream bos;
        private GestureDetector mGestureDetector;

        private ImageView img;
        private ProgressBar progress;
        private ImageButton btnDownload;
        private ImageButton btnRemove;

        private List<PhotoListFragment.PhotoInfo> photo_list = new ArrayList<PhotoListFragment.PhotoInfo>();
        private String mDevUUID;
        private int mCurrentPos;
        private int mCameraChannel;
        private byte[] buff;

        private MODE mMode;

        private enum MODE {
            Download, Remove
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle bundle = getIntent().getExtras();
            mDevUUID = bundle.getString("dev_uuid");
            mCurrentPos = bundle.getInt("pos");
            mCameraChannel = bundle.getInt("channel");
            photo_list = PhotoListFragment.photo_list;


            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            actionBar.show();
            TextView tvTitle = (TextView) findViewById(R.id.bar_text);
            TextView tvTitleSub = (TextView) findViewById(R.id.bar_text_sub);
            STimeDay mTime = photo_list.get(mCurrentPos).EventTime;
            tvTitle.setText(mTime.year + "/" + mTime.month + "/" + mTime.day);
            tvTitleSub.setText(mTime.hour + ":" + mTime.minute);
            tvTitleSub.setVisibility(View.VISIBLE);

            for (MyCamera camera : MultiViewActivity.CameraList) {

                if (mDevUUID.equalsIgnoreCase(camera.getUUID())) {
                    mCamera = camera;
                    mCamera.registerIOTCListener(this);
                    mCamera.initLargeDownloadManager(mCameraChannel);

                    break;
                }
            }

            setContentView(R.layout.view_remote_photo);

            img = (ImageView) findViewById(R.id.img);
            progress = (ProgressBar) findViewById(R.id.progress);
            btnDownload = (ImageButton) findViewById(R.id.btnDownload);
            btnRemove = (ImageButton) findViewById(R.id.btnRemove);

            img.setOnTouchListener(this);
            btnDownload.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMode = MODE.Download;
                    if (!photo_list.get(mCurrentPos).mHasFile) {
                        Custom_OkCancle_Dialog dlg = new Custom_OkCancle_Dialog(ViewRemotePhotoActivity.this,
                                getText(R.string.tips_download_photo).toString());

                        dlg.setCanceledOnTouchOutside(false);
                        Window window = dlg.getWindow();
                        window.setWindowAnimations(R.style.setting_dailog_animstyle);
                        dlg.show();
                    }
                }
            });
            btnRemove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMode = MODE.Remove;

                    Custom_OkCancle_Dialog dlg = new Custom_OkCancle_Dialog(ViewRemotePhotoActivity.this,
                            getText(R.string.dlgAreYouSureToDeleteThisSnapshot).toString());
                    dlg.setCanceledOnTouchOutside(false);
                    Window window = dlg.getWindow();
                    window.setWindowAnimations(R.style.setting_dailog_animstyle);
                    dlg.show();
                }
            });

            getPhoto(photo_list.get(mCurrentPos).path);
            Custom_OkCancle_Dialog.SetDialogListener(this);
            mGestureDetector = new GestureDetector(this);
        }

        private void getPhoto(String path) {

            btnDownload.setEnabled(false);
            btnRemove.setEnabled(false);
            img.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);

            if (photo_list.get(mCurrentPos).mHasFile) {
                btnDownload.setBackgroundResource(R.drawable.btn_download_h);
            } else {
                btnDownload.setBackgroundResource(R.drawable.btn_download);
            }

            if (mCamera != null) {
                mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DOWNLOAD_FILE_REQ,
                        AVIOCTRLDEFs.SMsgAVIoctrlDownloadReq.parseContent(mCameraChannel, path, AVIOCTRLDEFs.AVIOCTRL_VIEW_FILE));
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();

            if (mCamera != null) {
                mCamera.unregisterIOTCListener(this);
                mCamera.unregisterLargeDownload(this);
            }
        }

        @Override
        public void receiveFrameData(Camera camera, int avChannel, Bitmap bmp) {

        }

        @Override
        public void receiveFrameDataForMediaCodec(Camera camera, int avChannel, byte[] buf, int length, int pFrmNo, byte[] pFrmInfoBuf,
                                                  boolean isIframe, int codecId) {

        }

        @Override
        public void receiveFrameInfo(Camera camera, int avChannel, long bitRate, int frameRate, int onlineNm, int frameCount, int incompleteFrameCount) {

        }

        @Override
        public void receiveSessionInfo(Camera camera, int resultCode) {

        }

        @Override
        public void receiveChannelInfo(Camera camera, int avChannel, int resultCode) {

        }

        @Override
        public void receiveIOCtrlData(Camera camera, int avChannel, int avIOCtrlMsgType, byte[] data) {
            if (mCamera == camera) {
                switch (avIOCtrlMsgType) {
                    case AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DOWNLOAD_FILE_RESP:
                        int ch = Packet.byteArrayToInt_Little(data, 4);

                        if (data[8] == AVIOCTRLDEFs.AVIOCTRL_VIEW_FILE) {
                            mCamera.StartLargeDownload(ch, ViewRemotePhotoActivity.this);
                        }
                        break;
                }
            }
        }

        @Override
        public void getDownload(byte[] buf, int size, boolean start, boolean end) {
            if (start) {
                bos = new ByteArrayOutputStream();
                String path = new String(buf, 0, 72);

                bos.write(buf, 72, size - 72);
            } else {
                bos.write(buf, 0, size);
            }

            if (end) {

                buff = bos.toByteArray();
                final Bitmap bmp = BitmapFactory.decodeByteArray(buff, 0, buff.length);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        img.setImageBitmap(bmp);
                        progress.setVisibility(View.GONE);
                        img.setVisibility(View.VISIBLE);
                        Animation anim = AnimationUtils.loadAnimation(ViewRemotePhotoActivity.this, R.anim.fade_in);
                        img.startAnimation(anim);

                        btnDownload.setEnabled(true);
                        btnRemove.setEnabled(true);
                    }
                });

                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void getFinish(String path) {

        }

        @Override
        public void ok() {
            switch (mMode) {
                case Download:
                    String[] filter = photo_list.get(mCurrentPos).path.split("\\\\");
                    int end = filter.length;
                    File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/");
                    if (!rootFolder.exists()) {
                        try {
                            rootFolder.mkdir();
                        } catch (SecurityException se) {
                        }
                    }
                    File appFolder = new File(rootFolder.getAbsolutePath() + "/KalayCar/");
                    if (!appFolder.exists()) {
                        try {
                            appFolder.mkdir();
                        } catch (SecurityException se) {
                        }
                    }


                    File file = new File(appFolder.getAbsolutePath() + "/" + filter[end - 1]);
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                        bos.write(buff);
                        photo_list.get(mCurrentPos).setHasFile(true);
                        btnDownload.setBackgroundResource(R.drawable.btn_download_h);
                        bos.flush();
                        bos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case Remove:
                    String req = "custom=1&cmd=4003&str=" + photo_list.get(mCurrentPos).path;
                    mCamera.sendIOCtrl(Camera.DEFAULT_AV_CHANNEL, AVIOCTRLDEFs.IOTYPE_USER_WIFICMD_REQ,
                            AVIOCTRLDEFs.SMsgAVIoctrlWifiCmdReq.parseContent(mCameraChannel, 0, 0, 4003, 1, 0, req.length(), req));

                    int remove_pos = mCurrentPos;

                    if (photo_list.size() > 1) {
                        if (mCurrentPos == (photo_list.size() - 1)) {
                            mCurrentPos = 0;
                            getPhoto(photo_list.get(mCurrentPos).path);
                        } else {
                            mCurrentPos++;
                            getPhoto(photo_list.get(mCurrentPos).path);
                        }

                        photo_list.remove(remove_pos);

                    } else {
                        photo_list.remove(remove_pos);
                        finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    }


                    break;
            }
        }

        @Override
        public void cancel() {

        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            int mSize = photo_list.size();

            if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                // Fling left
                if (mCurrentPos == (mSize - 1)) {
                    mCurrentPos = 0;
                    getPhoto(photo_list.get(mCurrentPos).path);
                } else {
                    mCurrentPos++;
                    getPhoto(photo_list.get(mCurrentPos).path);
                }
            } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                // Fling right
                if (mCurrentPos == 0) {
                    mCurrentPos = mSize - 1;
                    getPhoto(photo_list.get(mCurrentPos).path);
                } else {
                    mCurrentPos--;
                    getPhoto(photo_list.get(mCurrentPos).path);
                }
            }
            return false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            return true;
        }
    }

    public static class SelectAPActivity extends Activity {

        private ListView lvNickname;
        private SSIDAdapter mAdapter;

        private com.tutk.P2PCam264.util.appteam.WifiAdmin WifiAdmin;
        private List<ScanResult> mWiFiList;
        private String mSSID;
        private String mAP;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            TextView tv = (TextView) this.findViewById(R.id.bar_text);
            tv.setText("Wi-Fi");

            setContentView(R.layout.nickname_list);

            lvNickname = (ListView) findViewById(R.id.lstNickname);

            mSSID = getIntent().getStringExtra("ssid");
            mAP = getIntent().getStringExtra("ap");

            WifiAdmin = new WifiAdmin(this);
            WifiAdmin.startScan();
            mWiFiList = WifiAdmin.getWifiList();
            for (int i = 0; i < mWiFiList.size(); i++) {
                if (mWiFiList.get(i).SSID.equals(mAP)) {
                    mWiFiList.remove(i);
                    break;
                }
            }

            mAdapter = new SSIDAdapter(this);
            lvNickname.setAdapter(mAdapter);
            lvNickname.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mSSID = WifiAdmin.getWifiList().get(position).SSID;
                    String enc = WifiAdmin.getWifiList().get(position).capabilities;
                    int wifi_enc = AVIOCTRLDEFs.AVIOTC_WIFIAPENC_NONE;
                    if (enc.length() != 0) {
                        if (enc.contains("WPA2")) {
                            wifi_enc = AVIOCTRLDEFs.AVIOTC_WIFIAPENC_WPA2_AES;
                        } else {
                            wifi_enc = AVIOCTRLDEFs.AVIOTC_WIFIAPENC_WPA_AES;
                        }
                    }
                    Intent intent = new Intent();
                    intent.putExtra("SSID", mSSID);
                    intent.putExtra("enc", wifi_enc);
                    setResult(RESULT_OK, intent);
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                }
            });

        }

        public class SSIDAdapter extends BaseAdapter {

            private LayoutInflater mInflater;

            public SSIDAdapter(Context context) {
                this.mInflater = LayoutInflater.from(context);
            }

            @Override
            public int getCount() {
                if (WifiAdmin.getWifiList() != null) {
                    return WifiAdmin.getWifiList().size();
                } else {
                    return 0;
                }
            }

            @Override
            public Object getItem(int position) {
                return mWiFiList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return -1;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (mWiFiList == null)
                    return null;

                ViewHolder holder = null;

                if (convertView == null) {

                    convertView = mInflater.inflate(R.layout.item_nickname, null);

                    holder = new ViewHolder();
                    holder.Nickname = (TextView) convertView.findViewById(R.id.txtName);
                    holder.imgCheck = (ImageView) convertView.findViewById(R.id.imgRight);
                    convertView.setTag(holder);

                } else {

                    holder = (ViewHolder) convertView.getTag();
                }

                if (holder != null) {
                    holder.Nickname.setText(mWiFiList.get(position).SSID);
                    if (mSSID.equals(mWiFiList.get(position).SSID))
                        holder.imgCheck.setVisibility(View.VISIBLE);
                    else
                        holder.imgCheck.setVisibility(View.GONE);
                }

                return convertView;
            }

            public class ViewHolder {
                public TextView Nickname;
                public ImageView imgCheck;
            }
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    return false;
            }

            return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * Created by James Huang on 2015/4/3.
     */
    public static class RemoteFileActivity extends SherlockActivity implements Custom_OkCancle_Dialog.OkCancelDialogListener {

        private PhotoListFragment PHOTO_fragment;
        private EventListFragment EVENT_fragment;
        private MyMode mMode = MyMode.PHOTO;

        private Button btnPhoto;
        private Button btnVideo;
        public Button btnEdit;

        private boolean photoList = false;
        private boolean eventList = false;
        private boolean mIsEdit = false;

        public enum MyMode {
            PHOTO, EVENT
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            TextView tv = (TextView) this.findViewById(R.id.bar_text);
            btnEdit = (Button) findViewById(R.id.bar_right_btn);
            RelativeLayout btn_change_mode = (RelativeLayout) findViewById(R.id.bar_gallery);
            btnPhoto = (Button) findViewById(R.id.bar_btn_photo);
            btnVideo = (Button) findViewById(R.id.bar_btn_video);

            tv.setVisibility(View.GONE);
            btnEdit.setText(R.string.txt_edit);
            btnEdit.setTextColor(Color.WHITE);
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setEnabled(false);
            btn_change_mode.setVisibility(View.VISIBLE);
            btnPhoto.setBackgroundResource(R.drawable.btn_tabl_h);
            btnPhoto.setTextColor(Color.BLACK);

            btnEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (mMode) {
                        case PHOTO:
                            PHOTO_fragment.showHideBar();
                            mIsEdit = !mIsEdit;
                            if (mIsEdit) {
                                btnEdit.setText(R.string.cancel);
                            } else {
                                btnEdit.setText(R.string.txt_edit);
                            }
                            break;

                        case EVENT:
                            EVENT_fragment.showHideBar();
                            mIsEdit = !mIsEdit;
                            if (mIsEdit) {
                                btnEdit.setText(R.string.cancel);
                            } else {
                                btnEdit.setText(R.string.txt_edit);
                            }
                            break;
                    }
                }
            });
            btnPhoto.setOnClickListener(ClickTab);
            btnVideo.setOnClickListener(ClickTab);

            super.onCreate(savedInstanceState);

            setContentView(R.layout.remote_file_activity);

            EVENT_fragment = new EventListFragment(this);
            PHOTO_fragment = new PhotoListFragment(this);
            getFragmentManager().beginTransaction().add(R.id.layout_container, EVENT_fragment).add(R.id.layout_container,
                    PHOTO_fragment).hide(EVENT_fragment).commit();

        }

        @Override
        protected void onResume() {
            super.onResume();
            Custom_OkCancle_Dialog.SetDialogListener(this);
        }

        private OnClickListener ClickTab = new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.bar_btn_photo:
                        if (mMode == MyMode.EVENT) {
                            getFragmentManager().beginTransaction().hide(EVENT_fragment).show(PHOTO_fragment).commit();
                            btnVideo.setBackgroundResource(R.drawable.btn_photo);
                            try {
                                btnVideo.setTextColor(ColorStateList.createFromXml(getResources(), getResources().getXml(R.drawable.txt_color_gallery)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            btnPhoto.setBackgroundResource(R.drawable.btn_tabl_h);
                            btnPhoto.setTextColor(Color.BLACK);

                            if (photoList) {
                                btnEdit.setEnabled(true);
                            }

                            mMode = MyMode.PHOTO;

                            if (mIsEdit) {
                                EVENT_fragment.showHideBar();
                                mIsEdit = !mIsEdit;
                                if (mIsEdit) {
                                    btnEdit.setText(R.string.cancel);
                                } else {
                                    btnEdit.setText(R.string.txt_edit);
                                }
                            }
                        }
                        break;

                    case R.id.bar_btn_video:
                        if (mMode == MyMode.PHOTO) {
                            getFragmentManager().beginTransaction().hide(PHOTO_fragment).show(EVENT_fragment).commit();
                            btnPhoto.setBackgroundResource(R.drawable.btn_photo);
                            try {
                                btnPhoto.setTextColor(ColorStateList.createFromXml(getResources(), getResources().getXml(R.drawable.txt_color_gallery)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            btnVideo.setBackgroundResource(R.drawable.btn_tabr_h);
                            btnVideo.setTextColor(Color.BLACK);

                            if (eventList) {
                                btnEdit.setEnabled(true);
                            }

                            mMode = MyMode.EVENT;

                            if (mIsEdit) {
                                PHOTO_fragment.showHideBar();
                                mIsEdit = !mIsEdit;
                                if (mIsEdit) {
                                    btnEdit.setText(R.string.cancel);
                                } else {
                                    btnEdit.setText(R.string.txt_edit);
                                }
                            }
                        }
                        break;
                }
            }
        };

        public void ListFinished(MyMode mode) {
            switch (mode) {
                case PHOTO:
                    photoList = true;
                    if (mMode == MyMode.PHOTO) {
                        btnEdit.setEnabled(true);
                    }
                    break;

                case EVENT:
                    eventList = true;
                    if (mMode == MyMode.EVENT) {
                        btnEdit.setEnabled(true);
                    }
                    break;
            }
        }

        public void cancleEdit() {
            mIsEdit = false;
            btnEdit.setText(R.string.txt_edit);
        }

        public boolean isOnFocus(MyMode mode) {
            if (mode == mMode) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

            if (EVENT_fragment.onKeyDown(keyCode, event)) {
                return super.onKeyDown(keyCode, event);
            } else {
                return false;
            }
        }

        @Override
        public void ok() {
            switch (mMode) {
                case PHOTO:
                    PHOTO_fragment.DialogOK();
                    break;

                case EVENT:
                    EVENT_fragment.DialogOK();
                    break;
            }
        }

        @Override
        public void cancel() {

        }
    }

    public static class PushSettingActivity extends Activity {

        private Switch swPush;
        private SharedPreferences settings;
        private boolean orgSettings;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            TextView tv = (TextView) this.findViewById(R.id.bar_text);
            tv.setText(getText(R.string.txt_Noti_settings));

            setContentView(R.layout.push_setting);

            swPush = (Switch) findViewById(R.id.swPush);

            settings = this.getSharedPreferences("Push Setting", 0);
            orgSettings = settings.getBoolean("settings", false);
            swPush.setChecked(orgSettings);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (orgSettings != swPush.isChecked()) {
                        settings.edit().putBoolean("settings", swPush.isChecked()).commit();

                        Intent intent = new Intent();
                        intent.putExtra("settings", swPush.isChecked());
                        setResult(RESULT_OK, intent);
                        finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    } else {
                        setResult(RESULT_CANCELED);
                        finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    }
                    break;
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    public static class NicknameListActivity extends Activity {

        public static int GALLERY_PHOTO = 0;
        public static int GALLERY_VIDEO = 1;

        private ListView lvNickname;
        //	private RelativeLayout btn_change_mode;
        //	private Button btnPhoto;
        //	private Button btnVideo;

        private NicnameAdapter mAdapter;
        private int mMode;
        private int mGalleryMode = -1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.titlebar);
            TextView tv = (TextView) this.findViewById(R.id.bar_text);
            //		btn_change_mode = (RelativeLayout) findViewById(R.id.bar_gallery);
            //		btnPhoto = (Button) findViewById(R.id.bar_btn_photo);
            //		btnVideo = (Button) findViewById(R.id.bar_btn_video);

            mMode = getIntent().getIntExtra("mode", -1);
            switch (mMode) {
                case MultiViewActivity.MODE_EVENT:
                    tv.setText(getText(R.string.ctxViewEvent));
                    break;
                case MultiViewActivity.MODE_GALLERY:
                    tv.setText(getText(R.string.ctxViewSnapshot));
                    //			btn_change_mode.setVisibility(View.VISIBLE);
                    //			btnPhoto.setBackgroundResource(R.drawable.btn_tabl_h);
                    //			btnPhoto.setTextColor(Color.BLACK);
                    //			btnPhoto.setOnClickListener(mode_change);
                    //			btnVideo.setOnClickListener(mode_change);
                    break;
            }

            setContentView(R.layout.nickname_list);

            lvNickname = (ListView) findViewById(R.id.lstNickname);

            mAdapter = new NicnameAdapter(this);
            lvNickname.setAdapter(mAdapter);
            lvNickname.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switch (mMode) {
                        case MultiViewActivity.MODE_EVENT:
                            Bundle extras = new Bundle();
                            extras.putString("dev_uid", MultiViewActivity.DeviceList.get(position).UID);
                            extras.putString("dev_uuid", MultiViewActivity.DeviceList.get(position).UUID);
                            extras.putString("dev_nickname", MultiViewActivity.DeviceList.get(position).NickName);
                            extras.putString("conn_status", MultiViewActivity.DeviceList.get(position).Status);
                            extras.putString("view_acc", MultiViewActivity.DeviceList.get(position).View_Account);
                            extras.putString("view_pwd", MultiViewActivity.DeviceList.get(position).View_Password);
                            extras.putInt("camera_channel", 0);
                            Intent intent = new Intent();
                            intent.putExtras(extras);
                            intent.setClass(NicknameListActivity.this, EventListActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                            break;

                        case MultiViewActivity.MODE_GALLERY:
                            File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Snapshot/" + MultiViewActivity.DeviceList.get(position).UID);
                            File folder_video = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Record/" + MultiViewActivity.DeviceList.get(position).UID);
                            String[] allFiles = folder.list();
                            String[] allVideos = folder_video.list();
                            Intent intent2 = new Intent(NicknameListActivity.this, GridViewGalleryActivity.class);
                            intent2.putExtra("snap", MultiViewActivity.DeviceList.get(position).UID);
                            intent2.putExtra("images_path", folder.getAbsolutePath());
                            intent2.putExtra("videos_path", folder_video.getAbsolutePath());
                            intent2.putExtra("mode", mGalleryMode);
                            startActivity(intent2);
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                            break;
                    }
                }
            });

        }

        public class NicnameAdapter extends BaseAdapter {

            private LayoutInflater mInflater;

            public NicnameAdapter(Context context) {
                this.mInflater = LayoutInflater.from(context);
            }

            @Override
            public int getCount() {
                return MultiViewActivity.DeviceList.size();
            }

            @Override
            public Object getItem(int position) {
                return MultiViewActivity.DeviceList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return -1;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                DeviceInfo dev = MultiViewActivity.DeviceList.get(position);

                if (dev == null)
                    return null;

                ViewHolder holder = null;

                if (convertView == null) {

                    convertView = mInflater.inflate(R.layout.item_nickname, null);

                    holder = new ViewHolder();
                    holder.Nickname = (TextView) convertView.findViewById(R.id.txtName);
                    convertView.setTag(holder);

                } else {

                    holder = (ViewHolder) convertView.getTag();
                }

                if (holder != null) {
                    holder.Nickname.setText(dev.NickName);
                }

                return convertView;
            }

            public class ViewHolder {
                public TextView Nickname;
            }
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    return false;
            }

            return super.onKeyDown(keyCode, event);
        }
        //	private OnClickListener mode_change = new OnClickListener() {
        //		@Override
        //		public void onClick(View v) {
        //			switch(v.getId()){
        //			case R.id.bar_btn_photo:
        //				mGalleryMode = GALLERY_PHOTO;
        //				mode_change();
        //				break;
        //
        //			case R.id.bar_btn_video:
        //				mGalleryMode = GALLERY_VIDEO;
        //				mode_change();
        //				break;
        //			}
        //		}
        //	};

        //	private void mode_change(){
        //		if (mGalleryMode == GALLERY_VIDEO) {
        //			btnPhoto.setBackgroundResource(R.drawable.btn_photo);
        //			try {
        //				btnPhoto.setTextColor(ColorStateList.createFromXml(getResources(), getResources().getXml(R.drawable.txt_color_gallery)));
        //			} catch (Exception e) {
        //				e.printStackTrace();
        //			}
        //			btnVideo.setBackgroundResource(R.drawable.btn_tabr_h);
        //			btnVideo.setTextColor(Color.BLACK);
        //		} else {
        //			btnVideo.setBackgroundResource(R.drawable.btn_video);
        //			try {
        //				btnVideo.setTextColor(ColorStateList.createFromXml(getResources(), getResources().getXml(R.drawable.txt_color_gallery)));
        //			} catch (Exception e) {
        //				e.printStackTrace();
        //			}
        //			btnPhoto.setBackgroundResource(R.drawable.btn_tabl_h);
        //			btnPhoto.setTextColor(Color.BLACK);
        //		}
        //	}

    }
}