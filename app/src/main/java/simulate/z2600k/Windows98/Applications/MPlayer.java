package simulate.z2600k.Windows98.Applications;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Tracks;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.google.android.material.snackbar.Snackbar;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.ButtonInList;
import simulate.z2600k.Windows98.System.ButtonList;
import simulate.z2600k.Windows98.System.Cursor;
import simulate.z2600k.Windows98.System.DummyWindow;
import simulate.z2600k.Windows98.System.Element;
import simulate.z2600k.Windows98.System.MessageBox;
import simulate.z2600k.Windows98.System.Separator;
import simulate.z2600k.Windows98.System.Taskbar;
import simulate.z2600k.Windows98.System.TopMenuButton;
import simulate.z2600k.Windows98.System.ViewContainer;
import simulate.z2600k.Windows98.System.WakeLock;
import simulate.z2600k.Windows98.System.Window;
import simulate.z2600k.Windows98.System.Windows98;
import simulate.z2600k.Windows98.WindowsView;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MPlayer extends Window {
    private SelectButton play, pause, stop, mute;
    private VolumeControl volumeControl;
    private SeekBar seekBar;
    private ContentArea contentArea;

    // 替换 MediaPlayer 为 ExoPlayer
    private ExoPlayer player;
    private SurfaceView surfaceView;
    public ViewContainer surfaceViewContainer;
    private Paint coverPaint;

    private boolean hasContent = false;
    private boolean isVideo = false;
    private boolean isFullscreen = false;
    private static final int PLAYING = 0, PAUSED = 1, STOPPED = 2;
    private int state;
    private int contentWidth = 284, contentHeight = 185;
    private Bitmap coverBitmap;          // 音频封面
    private boolean hasCover = false;    // 是否有封面可显示

    private WakeLock wakeLock = new WakeLock();
    private boolean ignoreRepositionElements = false, ignoreUpdateContentSize = false;
    private Bitmap inactiveButtonsBmp = getBmp(R.drawable.mplayer_buttons),
            winBmp = getBmp(R.drawable.mplayer_win), stereo = getBmp(R.drawable.mplayer_stereo);
    private static List<String> audioFormats = Arrays.asList("mp3", "aac", "flac", "ogg", "wav", "wma", "mid", "m4a");
    private static List<String> videoFormats = Arrays.asList("mp4", "3gp", "wmv", "webm", "avi", "mkv", "flv", "mov");
    static String[] supportedFormats;

    static {
        supportedFormats = new String[audioFormats.size() + videoFormats.size()];
        for (int i = 0; i < audioFormats.size(); i++)
            supportedFormats[i] = audioFormats.get(i);
        for (int i = 0; i < videoFormats.size(); i++)
            supportedFormats[i + audioFormats.size()] = videoFormats.get(i);
    }

    {
        coverPaint = new Paint();
        coverPaint.setFilterBitmap(true);  // 开启双线性滤波，实现抗锯齿
        coverPaint.setDither(true);        // 可选，改善颜色过渡
    }

    @OptIn(markerClass = UnstableApi.class) private boolean isHdrFormat(Format format) {
        if (format == null || format.colorInfo == null) {
            return false;
        }
        ColorInfo colorInfo = format.colorInfo;

        int colorTransfer = colorInfo.colorTransfer; // 检查色彩传输函数是否为 HDR 相关

        if (colorTransfer == C.COLOR_TRANSFER_ST2084 ||
                colorTransfer == C.COLOR_TRANSFER_HLG) {
            return true;
        }   // HDR10: ST2084 (PQ), HLG: HLG

        /*
        部分 Dolby Vision 或 HDR10+ 可能用其他字段标识，可通过 mimeType 和 codec 辅助判断
        例如 Dolby Vision profile 在 codecs 字符串中包含 "dvh1" 或 "dvhe"
        */
        String codecs = format.codecs;
        return codecs != null && (codecs.contains("dvh1") || codecs.contains("dvhe"));
    }

    @RequiresApi(api = Build.VERSION_CODES_FULL.N)
    private boolean hasHdrDecoder() {
        String[] hdrMimeTypes = {
                MediaFormat.MIMETYPE_VIDEO_HEVC,   // HEVC Main10 profile
                MediaFormat.MIMETYPE_VIDEO_VP9     // VP9 Profile 2
        };  // 常见的 HDR 视频编码格式

        for (String mime : hdrMimeTypes) {
            MediaCodecList codecList;
            codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
                if (codecInfo.isEncoder()) continue;

                String[] supportedTypes = codecInfo.getSupportedTypes();
                for (String type : supportedTypes) {
                    if (type.equalsIgnoreCase(mime)) {
                        MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType(type);
                        if (caps == null) continue;

                        MediaCodecInfo.CodecProfileLevel[] profileLevels = caps.profileLevels;  // 检查 Profile 数组
                        if (profileLevels == null) continue;

                        for (MediaCodecInfo.CodecProfileLevel profileLevel : profileLevels) {
                            int profile = profileLevel.profile;
                            if (mime.equals(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                                if (profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                                        profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                                        profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus) {
                                    return true;
                                }   // HEVC Main10 及相关 HDR profile
                            } else if (mime.equals(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                                if (profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2 ||
                                        profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR ||
                                        profile == MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR) {
                                    return true;
                                }   // VP9 Profile 2 及相关 HDR profile
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                double oldPos = seekBar.realPos;
                seekBar.update();
                if (oldPos != seekBar.realPos)
                    updateWindow();
            }
            WindowsView.handler.postDelayed(this, 250);
        }
    };

    public MPlayer(File file) {
        super("Windows Media Player", StartMenu.mPlayer, 294, 305, true, true, false);
        volumeControl = new VolumeControl();
        volumeControl.realPos = 1;
        volumeControl.updateThumbPos();
        addElement(volumeControl);
        seekBar = new SeekBar();
        addElement(seekBar);

        play = new SelectButton(getBmp(R.drawable.wmp_play), () -> {
            state = PLAYING;
            player.play();
            pause.disabled = false;
            seekBar.update();
        });
        pause = new SelectButton(getBmp(R.drawable.wmp_pause), () -> {
            state = PAUSED;
            player.pause();
            seekBar.update();
        });
        stop = new SelectButton(getBmp(R.drawable.wmp_stop), () -> {
            state = STOPPED;
            player.pause();
            player.seekTo(0);
            pause.disabled = true;
            seekBar.realPos = 0;
            seekBar.updateThumbPos();
        });
        SelectButton[] group = {play, pause, stop};
        play.group = pause.group = stop.group = group;
        addElement(play);
        addElement(pause);
        addElement(stop);

        mute = new SelectButton(getBmp(R.drawable.mute), this::updateVolume);
        mute.activeBmp = getBmp(R.drawable.mute_active);
        addElement(mute);
        contentArea = new ContentArea();
        addElement(contentArea);
        setupTopMenu();
        repositionElements();

        initializePlayer(); // 初始化 ExoPlayer

        surfaceView = new SurfaceView(context); // 创建 SurfaceView 替换 TextureView
        configureSurfaceViewForHdr();
        surfaceViewContainer = new ViewContainer(surfaceView);
        addElement(surfaceViewContainer);

        setHasContent(false);
        WindowsView.handler.post(progressUpdateRunnable);

        if (file != null)
            getOpenFileAction().openFile(file);
        else {
            SharedPreferences sharedPreferences = getSharedPreferences();
            final String key = "mpcTutorialShowedTimes";
            int showedTimes = sharedPreferences.getInt(key, 0);
            if (showedTimes < 6) {
                makeSnackbar(R.string.mpc_tutorial, Snackbar.LENGTH_LONG);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(key, showedTimes + 1);
                editor.apply();
            }
        }
        updateMouseOver();
    }

    public MPlayer() {
        this(null);
    }

    @OptIn(markerClass = UnstableApi.class) private void initializePlayer() {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context); // 使用 DefaultTrackSelector 以便后续检测 HDR
        player = new ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    state = STOPPED;
                    play.active = false;
                    pause.disabled = true;
                    stop.active = true;
                    seekBar.realPos = 1;
                    seekBar.updateThumbPos();
                    updateWindow();
                }
            }  // 监听播放状态

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                mplayerError();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                wakeLock.setScreenOn(isPlaying && playingVideo());
            }
        });
    }

    /**
     * 配置 SurfaceView 以支持 HDR 色彩空间
     */
    private void configureSurfaceViewForHdr() {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.O) {
                    holder.getSurface();    // 当 Surface 创建时，如果视频是 HDR 且系统版本支持，设置色彩空间
                }
                if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.N) {
                    holder.setFormat(PixelFormat.RGBA_8888);    // 设置像素格式以支持 10-bit（可选）
                }
                player.setVideoSurface(holder.getSurface());    // 将 Surface 设置给播放器
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                player.setVideoSurface(null);
            }
        });
    }

    /**
     * 检测设备是否支持 HDR 播放
     */
    private boolean isHdrSupported() {
        if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.N) {
            return false;
        }
        Display display = context.getWindowManager().getDefaultDisplay();
        Display.HdrCapabilities hdrCapabilities = display.getHdrCapabilities();
        assert hdrCapabilities != null;
        int[] types = hdrCapabilities.getSupportedHdrTypes();
        for (int type : types) {
            if (type == Display.HdrCapabilities.HDR_TYPE_HDR10 ||
                type == Display.HdrCapabilities.HDR_TYPE_HLG ||
                type == Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION ||
                type == Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        if (!isFullscreen) {
            super.onNewDraw(canvas, x, y);
            canvas.drawBitmap(inactiveButtonsBmp, x + 76, y + height - 53, null);
            drawVerySimpleFrameRect(canvas, x + 4, y + height - 28, x + width - 4, y + height - 4);
            p.setColor(Color.BLACK);
            canvas.drawRect(x + 5, y + height - 27, x + width - 5, y + height - 5, p);
            if (hasContent) {
                canvas.drawBitmap(stereo, x + width - 31, y + height - 22, null);
                p.setColor(Color.WHITE);
                String status;
                if (state == PLAYING)
                    status = "正在播放";
                else if (state == PAUSED)
                    status = "已暂停";
                else
                    status = "已停止";
                canvas.drawText(status, x + 33, y + height - 11, p);
                long curPos = player.getCurrentPosition();
                long duration = player.getDuration();
                boolean includeHours = duration >= 3600000;
                String timeString = formatTime((int) (curPos / 1000), includeHours) +
                        " / " + formatTime((int) (duration / 1000), includeHours);
                p.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(timeString, x + width - 43, y + height - 11, p);
                p.setTextAlign(Paint.Align.LEFT);
            }
        }

        wakeLock.setScreenOn(playingVideo());

        if (isFullscreen) {
            x = 0;
            y = 0;
        }
        int left = x + contentArea.x;
        int top = y + contentArea.y;
        int right = x + contentArea.x + contentArea.width;
        int bottom = y + contentArea.y + contentArea.height;
        drawVerySimpleFrameRect(canvas, left - 1, top - 1, right + 1, bottom + 1, true);
        if (!playingVideo() || (contentWidth != contentArea.width || contentHeight != contentArea.height)) {
            p.setColor(Color.BLACK);
            canvas.drawRect(left, top, right, bottom, p);
        }

        if (playingVideo() && surfaceViewContainer.getView().getVisibility() == View.VISIBLE) {
            int videoX = (left + right) / 2 - contentWidth / 2;
            int videoY = (top + bottom) / 2 - contentHeight / 2;
            drawHole(canvas, videoX, videoY, videoX + contentWidth, videoY + contentHeight);
        } else if (hasCover && coverBitmap != null && !coverBitmap.isRecycled()) {
            // 绘制音频封面，居中并按比例缩放
            int coverDrawWidth = contentWidth;
            int coverDrawHeight = contentHeight;
            int drawX = (left + right) / 2 - coverDrawWidth / 2;
            int drawY = (top + bottom) / 2 - coverDrawHeight / 2;
            // 使用带抗锯齿的 Paint 绘制
            canvas.drawBitmap(coverBitmap, null,
                    new Rect(drawX, drawY, drawX + coverDrawWidth, drawY + coverDrawHeight),
                    coverPaint);
        } else if (contentArea.width >= winBmp.getWidth() && contentArea.height >= winBmp.getHeight()) {
            int drawX = (left + right) / 2 - winBmp.getWidth() / 2;
            int drawY = (top + bottom) / 2 - winBmp.getHeight() / 2;
            canvas.drawBitmap(winBmp, drawX, drawY, null);
        }
    }

    private String formatTime(int time, boolean includeHours) {
        int sec = time % 60, min = (time / 60) % 60, hour = time / 3600;
        if (includeHours)
            return String.format(Locale.US, "%02d:%02d:%02d", hour, min, sec);
        else
            return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    @Override
    public void prepareForDelete() {
        super.prepareForDelete();
        WindowsView.handler.removeCallbacks(progressUpdateRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
        if (coverBitmap != null && !coverBitmap.isRecycled()) {
            coverBitmap.recycle();
            coverBitmap = null;
        }
        wakeLock.setScreenOn(false);
    }

    @Override
    public void repositionElements() {
        if (ignoreRepositionElements)
            return;
        if (surfaceView != null && !ignoreUpdateContentSize) {
            ignoreRepositionElements = true;
            updateContentSize();
            ignoreRepositionElements = false;
        }
        play.x = 6;
        pause.x = 28;
        stop.x = 50;
        mute.x = width - 78;
        play.y = pause.y = stop.y = mute.y = height - 53;
        volumeControl.x = width - 56;
        volumeControl.y = height - 53;
        seekBar.x = 6;
        seekBar.y = height - 72;
        seekBar.width = width - 6 - 7;
        seekBar.height = 15;
        seekBar.updateThumbPos();
        int left, top, right, bottom;
        if (!isFullscreen) {
            left = 5;
            top = 43;
            right = width - 5;
            bottom = height - 77;
        } else {
            left = 0;
            top = 0;
            right = Windows98.SCREEN_WIDTH;
            bottom = Windows98.SCREEN_HEIGHT;
        }
        contentArea.setBounds(left, top, right, bottom);
        if (surfaceViewContainer != null) {
            int x = (left + right) / 2 - contentWidth / 2;
            int y = (top + bottom) / 2 - contentHeight / 2;
            surfaceViewContainer.setBounds(x, y, x + contentWidth, y + contentHeight);
            surfaceViewContainer.updateViewPosition();
            // surfaceViewContainer.updateViewSize(contentWidth, contentHeight);
        }
    }

    private static class SelectButton extends Element {
        public boolean disabled = false;
        private Runnable action;
        private DitherPainter ditherPainter = new DitherPainter(Color.parseColor("#C0C0C0"), Color.WHITE);
        SelectButton[] group;
        private Bitmap bmp, disabledBmp;
        private Bitmap activeBmp = null;
        private boolean mouseOver = false, active = false;

        public SelectButton(Bitmap bmp, Runnable action) {
            this.bmp = bmp;
            this.action = action;
            disabledBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            int grey = Color.parseColor("#808080");
            for (int i = 0; i < disabledBmp.getWidth(); i++) {
                for (int j = 0; j < disabledBmp.getHeight(); j++) {
                    if (disabledBmp.getPixel(i, j) == Color.BLACK)
                        disabledBmp.setPixel(i, j, grey);
                }
            }
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            if (disabled)
                active = false;
            if (active)
                drawDitherRect(canvas, x, y, x + 21, y + 21, ditherPainter);
            if (active || isPressed())
                drawVerySimpleFrameRect(canvas, x, y, x + 21, y + 21);
            else if (mouseOver)
                drawVerySimpleFrameRect(canvas, x, y, x + 21, y + 21, false);
            Bitmap drawBmp = disabled ? disabledBmp : (active && activeBmp != null ? activeBmp : this.bmp);
            if (active || isPressed())
                canvas.drawBitmap(drawBmp, x + 1, y + 1, null);
            else
                canvas.drawBitmap(drawBmp, x, y, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if (disabled)
                return false;
            mouseOver = (0 <= x && x < 21 && 0 <= y && y < 21);
            return mouseOver;
        }

        @Override
        public void onMouseLeave() {
            mouseOver = false;
        }

        @Override
        public void onClick(int x, int y) {
            if (group != null) {
                if (active)
                    return;
                for (SelectButton button : group) {
                    button.active = false;
                }
                active = true;
            } else {
                active = !active;
            }
            if (action != null)
                action.run();
        }

        public void makeActive() {
            onClick(0, 0);
        }
    }

    private class VolumeControl extends Slider {
        private Bitmap volumeBmp = getBmp(R.drawable.volume);

        public VolumeControl() {
            super(49, 21, 10);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            canvas.drawBitmap(volumeBmp, x, y + 3, null);
            drawFrameRect(canvas, x + thumbPos, y, x + thumbPos + 10, y + height, true, true);
        }

        @Override
        public void updateThumbPos() {
            super.updateThumbPos();
            updateVolume();
        }

        @Override
        protected void setThumbPos(int x) {
            super.setThumbPos(x);
            updateVolume();
        }
    }

    private class SeekBar extends Slider {
        private Bitmap thumbBmp = getBmp(R.drawable.seekbar_thumb);

        public SeekBar() {
            super(0, 0, 13);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            p.setColor(Color.WHITE);
            canvas.drawRect(x + 6, y + 5, x + width - 5, y + height - 4, p);
            p.setColor(Color.parseColor("#808080"));
            canvas.drawRect(x + 5, y + 4, x + width - 6, y + 5, p);
            canvas.drawRect(x + 5, y + 4, x + 6, y + 10, p);
            canvas.drawBitmap(thumbBmp, x + thumbPos, y, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if (!super.onMouseOver(x, y, touch))
                return false;
            if (isPressed()) {
                setState(PAUSED);
                mpSeekTo();
            }
            return true;
        }

        @Override
        public void onClick(int x, int y) {
            super.onClick(x, y);
            mpSeekTo();
            setState(PLAYING);
        }

        private void mpSeekTo() {
            if (!hasContent)
                return;
            long ms = Math.round(realPos * player.getDuration());
            player.seekTo(ms);
        }

        public void update() {
            long duration = player.getDuration();
            if (duration > 0) {
                realPos = (double) player.getCurrentPosition() / duration;
                if (realPos > 1)
                    realPos = 1;
                updateThumbPos();
            }
        }
    }

    private abstract static class Slider extends Element {
        double realPos = 0;
        int thumbPos = 0;
        private int thumbWidth;

        public Slider(int width, int height, int thumbWidth) {
            this.width = width;
            this.height = height;
            this.thumbWidth = thumbWidth;
            captureMouse = true;
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            boolean mouseOver = (0 <= x && x < width && 0 <= y && y < height);
            if (touch) {
                if (mouseOver) {
                    parent.startTouch = this;
                    setThumbPos(x);
                }
                return mouseOver;
            } else if (!isPressed())
                return mouseOver;
            else {
                setThumbPos(x);
                return true;
            }
        }

        protected void setThumbPos(int x) {
            thumbPos = x - thumbWidth / 2;
            if (thumbPos < 0)
                thumbPos = 0;
            if (thumbPos > width - thumbWidth)
                thumbPos = width - thumbWidth;
            realPos = ((double) thumbPos) / (width - thumbWidth);
        }

        public void updateThumbPos() {
            thumbPos = (int) Math.round((width - thumbWidth) * realPos);
        }
    }

    private class ContentArea extends Element {
        Cursor hand = new Cursor(getBmp(R.drawable.hand), 6, 0);

        @Override
        public void onDraw(Canvas canvas, int x, int y) {}

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if (0 <= x && x < width && 0 <= y && y < height) {
                if (!isFullscreen && hasContent)
                    Windows98.setCursor(hand);
                return true;
            }
            return false;
        }

        @Override
        public void onClick(int x, int y) {
            if (state != PLAYING)
                setState(PLAYING);
            else
                setState(PAUSED);
        }

        @Override
        public void onDoubleClick(int x, int y) {
            toggleFullscreen();
            if (isFullscreen)
                removeCustomCursor();
            onClick(0, 0);
        }

        @Override
        public void onMouseLeave() {
            removeCustomCursor();
        }

        private void removeCustomCursor() {
            if (Windows98.windows98.getCursor() == hand)
                Windows98.setDefaultCursor();
        }
    }

    private FileDialog.OnResultListener getOpenFileAction() {
        return new FileDialog.OnResultListener() {
            @Override
            void openFile(final File file) {
                if (!file.exists()) {
                    new MessageBox("Windows Media Player 错误",
                            "无法打开 '" + MyDocuments.getFullPath(file) + "'。\n请检查路径和文件名是否正确，然后再试一次。",
                            MessageBox.OK, MessageBox.WARNING, null, MPlayer.this);
                    return;
                }
                startPlayback(file);    // 正常播放流程
            }
        };
    }

    private void showHdrNotSupportedDialog() {
        new MessageBox(
                "Windows Media Player",
                "此设备完全不支持 HDR 显示。\r\n\r\nWindows Media Player 拒绝播放。",
                MessageBox.OK,
                MessageBox.ERROR,
                buttonNumber -> {
                    player.stop();
                    player.clearMediaItems();
                    setHasContent(false);
                    setTitle("Windows Media Player");
                },
                this
        );
    }

    private void showSdkNotSupportedDialog() {
        new MessageBox(
                "Windows Media Player",
                "要播放 HDR 视频，请先升级您的 Android 版本。",
                MessageBox.OK,
                MessageBox.ERROR,
                buttonNumber -> {
                    player.stop();
                    player.clearMediaItems();
                    setHasContent(false);
                    setTitle("Windows Media Player");
                },
                this
        );
    }

    private void showHdrContinueDialog() {
        new MessageBox(
                "Windows Media Player",
                "此设备不支持 HDR 显示，但拥有支持的解码器。\r\n\r\nWindows Media Player 将强制以标准模式显示视频轨。\r\n\r\n要继续播放吗？",
                MessageBox.YESNO,
                MessageBox.QUESTION,
                buttonNumber -> {
                    if (buttonNumber == MessageBox.MsgResultListener.YES) {
                        player.play();
                        onPrepared();  // 直接继续，不再重置
                    } else {
                        // 用户拒绝，关闭当前内容
                        player.stop();
                        player.clearMediaItems();
                        setHasContent(false);
                        setTitle("Windows Media Player");
                    }
                },
                this
        );
    }

    /**
     * 尝试从音频文件中提取内嵌封面
     */
    private Bitmap extractCoverFromAudio(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            byte[] picture = retriever.getEmbeddedPicture();
            if (picture != null) {
                return BitmapFactory.decodeByteArray(picture, 0, picture.length);
            }
        } catch (Exception e) {
            // 忽略提取失败，返回 null
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void startPlayback(File file) {
        setHasContent(false);
        player.stop();
        player.clearMediaItems();
        // 释放旧封面
        if (coverBitmap != null && !coverBitmap.isRecycled()) {
            coverBitmap.recycle();
            coverBitmap = null;
        }
        hasCover = false;
        Uri uri = Uri.fromFile(file);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);

        setTitle(Link.getSimpleFilename(file.getName()) + " - Windows Media Player");
        // 如果是音频文件，预先尝试提取封面
        String fileName = file.getName().toLowerCase();
        boolean isAudioFile = false;
        for (String ext : audioFormats) {
            if (fileName.endsWith("." + ext)) {
                isAudioFile = true;
                break;
            }
        }

        if (isAudioFile) {
            // 异步提取封面，避免阻塞 UI
            new Thread(() -> {
                Bitmap extracted = extractCoverFromAudio(file.getAbsolutePath());
                WindowsView.handler.post(() -> {
                    if (extracted != null) {
                        coverBitmap = extracted;
                        hasCover = true;
                        // 更新内容尺寸为封面原始尺寸
                        contentWidth = coverBitmap.getWidth();
                        contentHeight = coverBitmap.getHeight();
                    }
                    // 继续准备播放
                    player.prepare();
                });
            }).start();
        } else {
            // 视频文件直接准备
            player.prepare();
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    onPrepared();
                    player.removeListener(this);
                    checkHdrAndProceed();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                mplayerError();
                player.removeListener(this);
            }
        });
    }

    private void checkHdrAndProceed() {
        Format videoFormat = null;  // 获取当前播放列表中的视频 Format（仅取第一个视频轨道）
        for (int groupIndex = 0; groupIndex < player.getCurrentTracks().getGroups().size(); groupIndex++) {
            Tracks.Group group = player.getCurrentTracks().getGroups().get(groupIndex);
            if (group.getType() == C.TRACK_TYPE_VIDEO && group.length > 0) {
                videoFormat = group.getTrackFormat(0);
                break;
            }
        }

        boolean isHdr = isHdrFormat(videoFormat);
        isVideo = (videoFormat != null);    // 修正 isVideo 标志

        if (!isHdr) {
            onPrepared();
            return;
        }   // 如果不是 HDR，直接进入准备完成阶段
        player.pause();

        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.N) {
            if (!hasHdrDecoder()) {
                showHdrNotSupportedDialog();
                return;
            }
        }
        else {
            showSdkNotSupportedDialog();
            return;
        }   // 第一层：检查 Android 版本，并检查是否有 HDR 解码器

        if (!isHdrSupported()) {
            showHdrContinueDialog();
            return;
        }   // 第二层：检查显示器是否支持 HDR

        player.play();  // 完全支持，正常播放
        onPrepared();
    }

    private void mplayerError() {
        setHasContent(false);
        if (childMessagebox == null)
            new MessageBox("Windows Media Player 错误", "无法下载对应的解码程序。",
                    MessageBox.OK, MessageBox.WARNING, null, this);
    }

    private void setHasContent(boolean hasContent) {
        this.hasContent = hasContent;
        if (!hasContent) {
            if (isFullscreen)
                toggleFullscreen();
        }
        play.disabled = pause.disabled = stop.disabled = !hasContent;
    }

    private void onPrepared() {
        setHasContent(true);
        updateContentSize();
        if (hasContent)
            setState(PLAYING);
        updateWindow();
        SurfaceHolder holder = surfaceView.getHolder();
        if (holder.getSurface() != null && holder.getSurface().isValid()) {
            player.setVideoSurface(holder.getSurface());
        }
        updateMouseOver();
    }

    private void updateContentSize() {
        if (playingVideo() || (hasCover && coverBitmap != null)) {
            if (player.getVideoSize().width > 0) {
                contentWidth = player.getVideoSize().width;
                contentHeight = player.getVideoSize().height;
            }   // 获取视频宽高
        }else {
            contentWidth = contentHeight = 0;
            ViewContainer.setSize(surfaceView, 1, 1);
        }
        if (!hasContent)
            return;

        if (maximized || isFullscreen) {
            int fieldWidth, fieldHeight;
            if (!isFullscreen) {
                fieldWidth = Windows98.SCREEN_WIDTH - 2;
                fieldHeight = Windows98.SCREEN_HEIGHT - 140;
            } else {
                fieldWidth = Windows98.SCREEN_WIDTH;
                fieldHeight = Windows98.SCREEN_HEIGHT;
            }
            double scalingFactor = Math.min((double) fieldWidth / contentWidth, (double) fieldHeight / contentHeight);
            contentWidth = (int) Math.round(contentWidth * scalingFactor);
            contentHeight = (int) Math.round(contentHeight * scalingFactor);
        } else {
            final int maxWidth = Windows98.SCREEN_WIDTH - 10, maxHeight = Windows98.TASKBAR_Y - 120;
            if (contentWidth > maxWidth || contentHeight > maxHeight) {
                double scalingFactor = Math.min((double) maxWidth / contentWidth, (double) maxHeight / contentHeight);
                contentWidth = (int) (contentWidth * scalingFactor);
                contentHeight = (int) (contentHeight * scalingFactor);
            }
        }
        if (!maximized && !isFullscreen) {
            final int minWidth = 294, minHeight = 120;
            width = normal_width = Math.max(contentWidth + 10, minWidth);
            height = normal_height = Math.max(contentHeight + 120, minHeight);
            if (x + width > Windows98.SCREEN_WIDTH) {
                x = x_old = Windows98.SCREEN_WIDTH - width;
            }
            if (y + height > Windows98.TASKBAR_Y)
                y = y_old = Windows98.TASKBAR_Y - height;
        } else if (isFullscreen) {
            if (!maximized) {
                x_old = x;
                y_old = y;
            }
            x = y = 0;
            width = Windows98.SCREEN_WIDTH;
            height = Windows98.SCREEN_HEIGHT;
        }
        ignoreUpdateContentSize = true;
        repositionEverything(true);
        ignoreUpdateContentSize = false;
        if (surfaceView != null) {
            surfaceView.requestLayout();
            surfaceView.invalidate();
        }
    }

    private void setState(int state) {
        if (!hasContent)
            return;
        if (state == PLAYING)
            play.makeActive();
        else if (state == PAUSED)
            pause.makeActive();
        else
            stop.makeActive();
    }

    private void updateVolume() {
        if (!hasContent)
            return;
        float volume = mute.active ? 0 : (float) volumeControl.realPos;
        player.setVolume(volume);
    }

    private void toggleFullscreen() {
        if (!hasContent || !isVideo)
            return;
        isFullscreen = !isFullscreen;
        if (isFullscreen)
            updateContentSize();
        else {
            if (maximized) {
                x = x_old;
                y = y_old;
                maximize();
            } else {
                restore();
            }
        }
        topMenu.visible = !isFullscreen;
        Taskbar.taskbar.visible = !isFullscreen;
        Windows98.windows98.links.visible = !isFullscreen;
    }

    private boolean playingVideo() {
        return hasContent && isVideo;
    }

    @Override
    public void makeActive() {
        super.makeActive();
        if (surfaceViewContainer != null) {
            surfaceViewContainer.onMakeActive();
        }
    }

    @Override
    public void minimize() {
        super.minimize();
        surfaceViewContainer.onMinimize();
    }

    @Override
    public void onClick(int x, int y) {
        int oldX = this.x, oldY = this.y;
        super.onClick(x, y);
        if (oldX != this.x || oldY != this.y) {
            surfaceViewContainer.updateViewPosition();
        }
    }

    private void setupTopMenu() {
        ButtonList file = new ButtonList();
        file.elements.add(new ButtonInList("打开...", "Ctrl+O", parent -> {
            new FileDialog(true, supportedFormats,
                    "媒体文件(所有类型)", MPlayer.this, getOpenFileAction());
            makeSnackbar(R.string.mpc_no_files, 5000);
        }));
        file.elements.add(new ButtonInList("关闭", parent -> {
            player.stop();
            player.clearMediaItems();
            seekBar.realPos = 0;
            seekBar.updateThumbPos();
            setHasContent(false);
            setTitle("Windows Media Player");
        }));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("另存为...", "Ctrl+S"));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("属性"));
        file.elements.add(new ButtonInList("详细信息..."));
        file.elements.add(new Separator());
        for (int i = 3; i < file.elements.size(); i++)
            ((ButtonInList) file.elements.get(i)).disabled = true;
        file.elements.add(new ButtonInList("退出", parent -> close()));

        ButtonList view = new ButtonList();
        ButtonInList standard = new ButtonInList("标准", "Ctrl+1");
        ButtonInList compact = new ButtonInList("精简", "Ctrl+2");
        ButtonInList minimal = new ButtonInList("最小", "Ctrl+3");
        standard.radioButtonGroup = compact.radioButtonGroup = minimal.radioButtonGroup =
                Arrays.asList(standard, compact, minimal);
        compact.checkActive = true;
        view.elements.add(standard);
        view.elements.add(compact);
        view.elements.add(minimal);
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("全屏幕", "Alt+Enter", parent -> toggleFullscreen()));
        ButtonInList zoom = new ButtonInList("缩放...");
        zoom.disabled = true;
        view.elements.add(zoom);
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("统计信息"));
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("字幕"));
        view.elements.add(new ButtonInList("总在最前", "Ctrl+T"));
        view.elements.add(new ButtonInList("选项..."));

        ButtonList playMenu = new ButtonList();
        playMenu.elements.add(new ButtonInList("播放/暂停", "空格键", parent -> {
            if (state != PLAYING)
                setState(PLAYING);
            else
                setState(PAUSED);
        }));
        playMenu.elements.add(new ButtonInList("停止", "句号键", parent -> setState(STOPPED)));
        playMenu.elements.add(new Separator());
        playMenu.elements.add(new ButtonInList("向后跳进", "Page Up"));
        playMenu.elements.add(new ButtonInList("向前跳进", "Page Down"));
        playMenu.elements.add(new ButtonInList("快退", "Ctrl+←"));
        playMenu.elements.add(new ButtonInList("快进", "Ctrl+→"));
        playMenu.elements.add(new Separator());
        playMenu.elements.add(new ButtonInList("预览", "Ctrl+V"));
        playMenu.elements.add(new ButtonInList("转到...", "Ctrl+G"));
        playMenu.elements.add(new Separator());
        playMenu.elements.add(new ButtonInList("语言..."));
        final ButtonList volume = new ButtonList();
        volume.elements.add(new ButtonInList("增大", "↑", parent -> {
            volumeControl.realPos += 0.1;
            if (volumeControl.realPos > 1)
                volumeControl.realPos = 1;
            volumeControl.updateThumbPos();
        }));
        volume.elements.add(new ButtonInList("减小", "↓", parent -> {
            volumeControl.realPos -= 0.1;
            if (volumeControl.realPos < 0)
                volumeControl.realPos = 0;
            volumeControl.updateThumbPos();
        }));
        volume.elements.add(new ButtonInList("静音", "Ctrl+M", parent -> mute.makeActive()));
        for (int i = 3; i < playMenu.elements.size(); i++)
            ((ButtonInList) playMenu.elements.get(i)).disabled = true;
        playMenu.elements.add(new ButtonInList("音量", volume));
        ButtonList favorites = StartMenu.getFavoritesMenu();
        ButtonList go = new ButtonList();
        go.elements.add(new ButtonInList("后退", "Alt+←"));
        go.elements.add(new ButtonInList("前进", "Alt+→"));
        go.elements.add(new Separator());
        go.elements.add(new ButtonInList("Web 事件", "Alt+S"));
        go.elements.add(new ButtonInList("Windows Media Player 主页"));
        ButtonList help = new ButtonList();
        help.elements.add(new ButtonInList("帮助主题"));
        help.elements.add(new Separator());
        help.elements.add(new ButtonInList("检查播放机的升级版本"));
        help.elements.add(new ButtonInList("关于 Windows Media Player", parent -> {
            DummyWindow about = new DummyWindow("关于 Windows Media Player", null, false,
                    getBmp(R.drawable.about_wmp), new Rect(225, 84, 285, 105), "确定");
            about.centerWindowOnScreen();
        }));
        topMenu.elements.add(new TopMenuButton("文件", file));
        topMenu.elements.add(new TopMenuButton("查看", view));
        topMenu.elements.add(new TopMenuButton("播放", playMenu));
        topMenu.elements.add(new TopMenuButton("收藏", favorites));
        topMenu.elements.add(new TopMenuButton("转到", go));
        topMenu.elements.add(new TopMenuButton("帮助", help));
    }
}
