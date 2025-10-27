package simulate.z2600k.Windows98.Applications;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MPlayer extends Window {
    private SelectButton play, pause, stop, mute;
    private VolumeControl volumeControl;
    private SeekBar seekBar;
    private ContentArea contentArea;

    private MediaPlayer mediaPlayer;
    public MyTextureView textureView;
    private ViewContainer textureViewContainer;

    private boolean hasContent = false;
    private boolean isVideo = false;
    private boolean isFullscreen = false;
    private static final int PLAYING = 0, PAUSED = 1, STOPPED = 2;
    private int state;
    private int contentWidth = 284, contentHeight = 185;  // размер прямоугольника с видео (не самого видео!) (БЕЗ полос по краям)

    private WakeLock wakeLock = new WakeLock();
    // для избежания рекурсии
    private boolean ignoreRepositionElements = false, ignoreUpdateContentSize = false;
    private Bitmap inactiveButtonsBmp = getBmp(R.drawable.mplayer_buttons),
            winBmp = getBmp(R.drawable.mplayer_win), stereo = getBmp(R.drawable.mplayer_stereo);
    private static List<String> audioFormats = Arrays.asList("mp3", "aac", "flac", "ogg", "wav", "wma", "mid");  // при изменении изменять и копии
    private static List<String> videoFormats = Arrays.asList("mp4", "3gp", "wmv", "webm", "avi", "mkv", "flv", "mov");
    static String[] supportedFormats = new String[audioFormats.size() + videoFormats.size()];
    static {
        for(int i = 0; i < audioFormats.size(); i++)
            supportedFormats[i] = audioFormats.get(i);
        for(int i = 0; i < videoFormats.size(); i++)
            supportedFormats[i + audioFormats.size()] = videoFormats.get(i);
    }

    private Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if(mediaPlayer.isPlaying()) {
                double oldPos = seekBar.realPos;
                seekBar.update();
                if(oldPos != seekBar.realPos)
                    updateWindow();
            }
            WindowsView.handler.postDelayed(this, 250);
        }
    };

    public MPlayer(File file){
        super("Windows Media Player", StartMenu.mPlayer, 294, 305, true, true, false);
        volumeControl = new VolumeControl();
        volumeControl.realPos = 1;
        volumeControl.updateThumbPos();
        addElement(volumeControl);
        seekBar = new SeekBar();
        addElement(seekBar);

        play = new SelectButton(getBmp(R.drawable.wmp_play), () -> {
            state = PLAYING;
            mediaPlayer.start();
            pause.disabled = false;
            seekBar.update();
        });
        pause = new SelectButton(getBmp(R.drawable.wmp_pause), () -> {
            state = PAUSED;
            mediaPlayer.pause();
            seekBar.update();
        });
        stop = new SelectButton(getBmp(R.drawable.wmp_stop), () -> {
            // mediaPlayer.stop();
            state = STOPPED;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
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

        mediaPlayer = new MediaPlayer();
        //Windows98.setDesiredVolume(mediaPlayer, 1);
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            mplayerError();
            return true;
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            //setState(STOPPED); - отматывает в начало. нам этого не надо
            play.active = false;
            pause.disabled = true;
            stop.active = true;
            state = STOPPED;
            seekBar.realPos = 1;
            seekBar.updateThumbPos();
            updateWindow();
        });

        textureView = new MyTextureView();
        textureViewContainer = new ViewContainer(textureView);
        addElement(textureViewContainer);

        setHasContent(false);
        WindowsView.handler.post(progressUpdateRunnable);

        if(file != null)
            getOpenFileAction().openFile(file);
        else {
            // tutorial
            SharedPreferences sharedPreferences = getSharedPreferences();
            final String key = "mpcTutorialShowedTimes";
            int showedTimes = sharedPreferences.getInt(key, 0);
            if (showedTimes < 6) {  // 6 раз показываем
                makeSnackbar(R.string.mpc_tutorial, Snackbar.LENGTH_LONG);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(key, showedTimes + 1);
                editor.apply();
            }
        }
        updateMouseOver();
    }

    public MPlayer(){
        this(null);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        if(!isFullscreen) {
            super.onNewDraw(canvas, x, y);
            canvas.drawBitmap(inactiveButtonsBmp, x + 76, y + height - 53, null);
            drawVerySimpleFrameRect(canvas, x + 4, y + height - 28, x + width - 4, y + height - 4);
			p.setColor(Color.BLACK);
            canvas.drawRect(x + 5, y + height - 27, x + width - 5, y + height - 5, p);
            // status bar
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
                int curPos = mediaPlayer.getCurrentPosition() / 1000, duration = mediaPlayer.getDuration() / 1000;
                boolean includeHours = duration >= 60 * 60;
                String timeString = formatTime(curPos, includeHours) + " / " + formatTime(duration, includeHours);
                p.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(timeString, x + width - 43, y + height - 11, p);
                p.setTextAlign(Paint.Align.LEFT);
            }
        }

        wakeLock.setScreenOn(playingVideo());

        if(isFullscreen){
            x = 0;
            y = 0;
        }
        int left = x + contentArea.x;
        int top = y + contentArea.y;
        int right = x + contentArea.x + contentArea.width;
        int bottom = y + contentArea.y + contentArea.height;
        drawVerySimpleFrameRect(canvas, left - 1, top - 1, right + 1, bottom + 1, true);
        if(!playingVideo() || (contentWidth != contentArea.width || contentHeight != contentArea.height)){  // рисуем черные полосы по бокам
            p.setColor(Color.BLACK);
            canvas.drawRect(left, top, right, bottom, p);
        }

        if(playingVideo() && textureView.surfaceTextureAvailable && textureView.hasBeenUpdated){
            int videoX = (left + right) / 2 - contentWidth / 2;
            int videoY = (top + bottom) / 2 - contentHeight / 2;
            drawHole(canvas, videoX, videoY, videoX + contentWidth, videoY + contentHeight);
        }
        else if(contentArea.width >= winBmp.getWidth() && contentArea.height >= winBmp.getHeight()) {
            int drawX = (left + right) / 2 - winBmp.getWidth() / 2;
            int drawY = (top + bottom) / 2 - winBmp.getHeight() / 2;
            canvas.drawBitmap(winBmp, drawX, drawY, null);
        }
    }

    private String formatTime(int time, boolean includeHours){
        int sec = time % 60, min = (time / 60) % 60, hour = time / 3600;
        if(includeHours)
            return String.format(Locale.US, "%02d:%02d:%02d", hour, min, sec);
        else
            return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    private class MyTextureView extends TextureView implements TextureView.SurfaceTextureListener {
        boolean surfaceTextureAvailable = false;
        boolean startVideoOnSurfaceAvailable = false;  // если мы ждём доступности surfaceTexture
        boolean hasBeenUpdated = false;  // рисовали ли на textureview

        public MyTextureView(){
            super(context);
            setSurfaceTextureListener(this);
            setOpaque(false);
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            surfaceTextureAvailable = true;
            mediaPlayer.setSurface(new Surface(surface));
            if (isVideo && startVideoOnSurfaceAvailable) {
                startVideoOnSurfaceAvailable = false;
                onPrepared();
            }
        }

        public void setSize(int width, int height){
            ViewContainer.setSize(this, width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            hasBeenUpdated = true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
    }

    @Override
    public void prepareForDelete() {
        super.prepareForDelete();
        WindowsView.handler.removeCallbacks(progressUpdateRunnable);
        if(mediaPlayer.isPlaying())
            mediaPlayer.stop();
        textureView.startVideoOnSurfaceAvailable = false;
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
        wakeLock.setScreenOn(false);
    }

    @Override
    public void repositionElements(){
        if(ignoreRepositionElements)
            return;
        if(textureView != null && !ignoreUpdateContentSize) {  // не вызываемся из конструктора
            ignoreRepositionElements = true;  // чтобы избежать рекурсии
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
        if(!isFullscreen){
            left = 5; top = 43; right = width - 5; bottom = height - 77;
        }
        else{
            left = 0; top = 0; right = Windows98.SCREEN_WIDTH; bottom = Windows98.SCREEN_HEIGHT;
        }
        contentArea.setBounds(left, top, right, bottom);
        if(textureView != null) {
            int x = (left + right) / 2 - contentWidth / 2;
            int y = (top + bottom) / 2 - contentHeight / 2;
            textureViewContainer.setBounds(x, y, x + contentWidth, y + contentHeight);
        }
    }


    private static class SelectButton extends Element {  // Play, Pause, Stop
        public boolean disabled = false;
        private Runnable action;
        private DitherPainter ditherPainter = new DitherPainter(Color.parseColor("#C0C0C0"), Color.WHITE);
        SelectButton[] group;
        private Bitmap bmp, disabledBmp;
        private Bitmap activeBmp = null;
        private boolean mouseOver = false, active = false;

        public SelectButton(Bitmap bmp, Runnable action){
            this.bmp = bmp;
            this.action = action;
            // создаём disabledBitmap
            disabledBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            int grey = Color.parseColor("#808080");
            for(int i = 0; i < disabledBmp.getWidth(); i++){
                for(int j = 0; j < disabledBmp.getHeight(); j++){
                    if(disabledBmp.getPixel(i, j) == Color.BLACK)
                        disabledBmp.setPixel(i, j, grey);
                }
            }
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            if(disabled)
                active = false;
            if(active)
                drawDitherRect(canvas, x, y, x + 21, y + 21, ditherPainter);
            if(active || isPressed())
                drawVerySimpleFrameRect(canvas, x, y, x + 21, y + 21);
            else if(mouseOver)
                drawVerySimpleFrameRect(canvas, x, y, x + 21, y + 21, false);
            Bitmap bmp = disabled? disabledBmp :
                    (active && activeBmp != null? activeBmp : this.bmp);
            if(active || isPressed())
                canvas.drawBitmap(bmp, x + 1, y + 1, null);
            else
                canvas.drawBitmap(bmp, x, y, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(disabled)
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
            if(group != null) {
                if(active)
                    return;
                for (SelectButton button : group) {
                    button.active = false;
                }
                active = true;
                if(action != null)
                    action.run();
            }
            else {
                active = !active;
                if(action != null)
                    action.run();
            }
        }

        public void makeActive(){
            onClick(0, 0);
        }
    }

    private class VolumeControl extends Slider {
        private Bitmap volumeBmp = getBmp(R.drawable.volume);

        public VolumeControl(){
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

    private class SeekBar extends Slider{
        private Bitmap thumbBmp = getBmp(R.drawable.seekbar_thumb);
        public SeekBar(){
            super(0, 0, 13);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            p.setColor(Color.WHITE);
            canvas.drawRect(x + 6, y + 5, x + width - 5, y + height - 4, p);
            p.setColor(Color.parseColor("#808080"));  // серый
            canvas.drawRect(x + 5, y + 4, x + width - 6, y + 5, p);
            canvas.drawRect(x + 5, y + 4, x + 6, y + 10, p);
            canvas.drawBitmap(thumbBmp, x + thumbPos, y, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(!super.onMouseOver(x, y, touch))
                return false;
            if(isPressed()) {
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

        private void mpSeekTo(){  // промотать до нужного момента
            if(!hasContent)
                return;
            long ms = Math.round(realPos * mediaPlayer.getDuration());
            if(Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.O)
                mediaPlayer.seekTo(ms, MediaPlayer.SEEK_CLOSEST);
            else
                mediaPlayer.seekTo((int) ms);
        }

        public void update(){
            realPos = (double) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration();
            if(realPos > 1)
                realPos = 1;
            updateThumbPos();
        }
    }

    private abstract static class Slider extends Element {  // VolumeControl или SeekBar
        double realPos = 0;
        int thumbPos = 0;  // от 0 до width - thumbWidth
        private int thumbWidth;

        public Slider(int width, int height, int thumbWidth){
            this.width = width;
            this.height = height;
            this.thumbWidth = thumbWidth;
            captureMouse = true;
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            boolean mouseOver = (0 <= x && x < width && 0 <= y && y < height);
            if(touch){
                if(mouseOver) {
                    parent.startTouch = this;
                    setThumbPos(x);
                }
                return mouseOver;
            }
            else if(!isPressed())
                return mouseOver;
            else {  // мы нажаты, мышь перемещается
                setThumbPos(x);
                return true;
            }
        }

        protected void setThumbPos(int x){
            thumbPos = x - thumbWidth / 2;
            if(thumbPos < 0)
                thumbPos = 0;
            if(thumbPos > width - thumbWidth)
                thumbPos = width - thumbWidth;
            realPos = ((double) thumbPos) / (width - thumbWidth);
        }

        public void updateThumbPos(){
            thumbPos = (int) Math.round((width - thumbWidth) * realPos);
        }
    }

    private class ContentArea extends Element {  // чтобы удобнее было обрабатывать клик и дабл клик на видео
        Cursor hand = new Cursor(getBmp(R.drawable.hand), 6, 0);

        @Override
        public void onDraw(Canvas canvas, int x, int y) {}

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(0 <= x && x < width && 0 <= y && y < height) {
                if(!isFullscreen && hasContent)
                    Windows98.setCursor(hand);
                return true;
            }
            else
                return false;
        }

        @Override
        public void onClick(int x, int y) {
            if(state != PLAYING)
                setState(PLAYING);
            else
                setState(PAUSED);
        }

        @Override
        public void onDoubleClick(int x, int y) {
            toggleFullscreen();
            if(isFullscreen)  // не показывается в полном экране
                removeCustomCursor();
            onClick(0, 0);  // если mediaPlayer играл, он был поставлен на паузу первым кликом, и наоборот
        }

        @Override
        public void onMouseLeave() {
            removeCustomCursor();
        }

        private void removeCustomCursor(){
            if(Windows98.windows98.getCursor() == hand)
                Windows98.setDefaultCursor();
        }
    }

    private FileDialog.OnResultListener getOpenFileAction(){
        return new FileDialog.OnResultListener() {
            @Override
            void openFile(final File file) {
                if(!file.exists()){
                    new MessageBox("Windows Media Player 错误", "无法打开 '" + MyDocuments.getFullPath(file) +
                            "'.\n请检查路径和文件名是否正确，然后再试一次。",
                            MessageBox.OK, MessageBox.WARNING, null, MPlayer.this);
                    return;
                }
                setHasContent(false);
                //mediaPlayer.stop();
                mediaPlayer.reset();
                try {
                    mediaPlayer.setDataSource(context, Uri.parse("file://" + file.getAbsolutePath()));
                }
                catch (IOException e){
                    mplayerError();
                    return;
                }
                isVideo = videoFormats.contains(Link.getExtension(file.getName()));
                setTitle(Link.getSimpleFilename(file.getName()) + " - Windows Media Player");
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if(!textureView.surfaceTextureAvailable)
                            textureView.startVideoOnSurfaceAvailable = true;
                        else
                            MPlayer.this.onPrepared();
                    }
                });
                mediaPlayer.prepareAsync();
            }
        };
    }

    private void mplayerError(){
        setHasContent(false);
        if(childMessagebox == null)
            new MessageBox("Windows Media Player 错误", "无法下载对应的解码程序。",
                MessageBox.OK, MessageBox.WARNING, null, this);
    }

    private void setHasContent(boolean hasContent){
        this.hasContent = hasContent;
        if(!hasContent) {
            if(isFullscreen)
                toggleFullscreen();
            textureView.startVideoOnSurfaceAvailable = false;
        }
        play.disabled = pause.disabled = stop.disabled = !hasContent;
    }

    private void onPrepared(){
        setHasContent(true);
        updateContentSize();
        if(hasContent)
            setState(PLAYING);
        updateWindow();
        updateMouseOver();
    }

    private void updateContentSize(){  // размер контента изменился, меняем размер окна и т. д.
        if(playingVideo()) {
            contentWidth = mediaPlayer.getVideoWidth();
            contentHeight = mediaPlayer.getVideoHeight();
            if(contentWidth == 0 || contentHeight == 0)
                setHasContent(false);
        }
        else if(!isVideo) {
            contentWidth = contentHeight = 0;
            textureView.setSize(1, 1);
        }
        if(!hasContent)
            return;

        if(maximized || isFullscreen){
            int fieldWidth, fieldHeight;  // доступное пространство для видео
            if(!isFullscreen){
                fieldWidth = Windows98.SCREEN_WIDTH - 2;
                fieldHeight = Windows98.SCREEN_HEIGHT - 140;
            }
            else{
                fieldWidth = Windows98.SCREEN_WIDTH;
                fieldHeight = Windows98.SCREEN_HEIGHT;
            }
            double scalingFactor = Math.min((double) fieldWidth / contentWidth, (double) fieldHeight / contentHeight);
            contentWidth = (int) Math.round(contentWidth * scalingFactor);
            contentHeight = (int) Math.round(contentHeight * scalingFactor);
        }
        else {  // minimized - мы меняем размер окна
            final int maxWidth = Windows98.SCREEN_WIDTH - 10, maxHeight = Windows98.TASKBAR_Y - 120;
            if(contentWidth > maxWidth || contentHeight > maxHeight) {
                double scalingFactor = Math.min((double) maxWidth / contentWidth, (double) maxHeight / contentHeight);
                contentWidth = (int) (contentWidth * scalingFactor);  // floor
                contentHeight = (int) (contentHeight * scalingFactor);
            }
            /*if(playingVideo){
                contentWidth = Math.max(1, contentWidth);  // чтобы textureView не считал себя невидимым
                contentHeight = Math.max(1, contentHeight);
            }*/
        }
        if(!maximized && !isFullscreen) {
            final int minWidth = 294, minHeight = 120;  // минимальные размеры окна
            width = normal_width = Math.max(contentWidth + 10, minWidth);
            height = normal_height = Math.max(contentHeight + 120, minHeight);
            if (x + width > Windows98.SCREEN_WIDTH) {
                x = x_old = Windows98.SCREEN_WIDTH - width;
            }
            if (y + height > Windows98.TASKBAR_Y)
                y = y_old = Windows98.TASKBAR_Y - height;
        }
        else if(isFullscreen){
            if(!maximized) {
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
    }

    private void setState(int state){
        if(!hasContent)
            return;
        if(state == PLAYING)
            play.makeActive();
        else if(state == PAUSED)
            pause.makeActive();
        else
            stop.makeActive();
    }

    private void updateVolume(){
        if(!hasContent)
            return;
        float volume = mute.active? 0 : (float) volumeControl.realPos;
        mediaPlayer.setVolume(volume, volume);
        //Windows98.setDesiredVolume(mediaPlayer, volume);
    }

    private void toggleFullscreen(){
        if(!hasContent)
            return;
        if(!isVideo)
            return;
        isFullscreen = !isFullscreen;
        if(isFullscreen)
            updateContentSize();
        else{  // т. к. после fullscreen координаты окна сбиваются
            if(maximized) {
                x = x_old;
                y = y_old;
                maximize();
            }
            else {
                restore();
            }
        }
        topMenu.visible = !isFullscreen;
        Taskbar.taskbar.visible = !isFullscreen;
        Windows98.windows98.links.visible = !isFullscreen;
    }

    private boolean playingVideo(){
        return hasContent && isVideo;
    }

    // работа с положением textureView
    @Override
    public void makeActive() {
        super.makeActive();
        if(textureViewContainer != null) {
            textureViewContainer.onMakeActive();
        }
    }

    @Override
    public void minimize() {
        super.minimize();
        textureViewContainer.onMinimize();
    }

    @Override
    public void onClick(int x, int y) {
        // проверяем, переместилось ли окно, если да - перемещаем surfaceView
        int oldX = this.x, oldY = this.y;
        super.onClick(x, y);
        if(oldX != this.x || oldY != this.y){
            textureViewContainer.updateViewPosition();
        }
    }

    private void setupTopMenu(){
        ButtonList file = new ButtonList();
        file.elements.add(new ButtonInList("打开...", "Ctrl+O", parent -> {
            FileDialog fileDialog = new FileDialog(true, supportedFormats,
                    "媒体文件(所有类型)", MPlayer.this, getOpenFileAction());
            makeSnackbar(R.string.mpc_no_files, 5000);
        }));
        file.elements.add(new ButtonInList("关闭", parent -> {
            mediaPlayer.stop();
            mediaPlayer.reset();
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
        for(int i = 3; i < file.elements.size(); i++)
            ((ButtonInList) file.elements.get(i)).disabled = true;
        file.elements.add(new ButtonInList("退出", parent -> close()));

        ButtonList view = new ButtonList();
        ButtonInList standart = new ButtonInList("标准", "Ctrl+1");
        ButtonInList compact = new ButtonInList("精简", "Ctrl+2");
        ButtonInList minimal = new ButtonInList("最小", "Ctrl+3");
        standart.radioButtonGroup = compact.radioButtonGroup = minimal.radioButtonGroup =
                Arrays.asList(standart, compact, minimal);
        compact.checkActive = true;
        view.elements.add(standart);
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

        ButtonList play = new ButtonList();
        play.elements.add(new ButtonInList("播放/暂停", "空格键", parent -> {
            if(state != PLAYING)
                setState(PLAYING);
            else
                setState(PAUSED);
        }));
        play.elements.add(new ButtonInList("停止", "句号键", parent -> setState(STOPPED)));
        play.elements.add(new Separator());
        play.elements.add(new ButtonInList("向后跳进", "Page Up"));
        play.elements.add(new ButtonInList("向前跳进", "Page Down"));
        play.elements.add(new ButtonInList("快退", "Ctrl+←"));
        play.elements.add(new ButtonInList("快进", "Ctrl+→"));
        play.elements.add(new Separator());
        play.elements.add(new ButtonInList("预览", "Ctrl+V"));
        play.elements.add(new ButtonInList("转到...", "Ctrl+G"));
        play.elements.add(new Separator());
        play.elements.add(new ButtonInList("语言..."));
        final ButtonList volume = new ButtonList();
        volume.elements.add(new ButtonInList("增大", "↑", parent -> {
            volumeControl.realPos += 0.1;
            if(volumeControl.realPos > 1)
                volumeControl.realPos = 1;
            volumeControl.updateThumbPos();
        }));
        volume.elements.add(new ButtonInList("减小", "↓", parent -> {
            volumeControl.realPos -= 0.1;
            if(volumeControl.realPos < 0)
                volumeControl.realPos = 0;
            volumeControl.updateThumbPos();
        }));
        volume.elements.add(new ButtonInList("静音", "Ctrl+M", parent -> mute.makeActive()));
        for(int i = 3; i < play.elements.size(); i++)
            ((ButtonInList) play.elements.get(i)).disabled = true;
        play.elements.add(new ButtonInList("音量", volume));
        ButtonList favotites = StartMenu.getFavoritesMenu();
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
        topMenu.elements.add(new TopMenuButton("播放", play));
        topMenu.elements.add(new TopMenuButton("收藏", favotites));
        topMenu.elements.add(new TopMenuButton("转到", go));
        topMenu.elements.add(new TopMenuButton("帮助", help));
    }
}
