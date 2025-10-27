package simulate.z2600k.Windows98.Applications;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Movie;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedImageDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;

import androidx.core.content.ContextCompat;

import simulate.z2600k.Windows98.MainActivity;
import simulate.z2600k.Windows98.MyWebView;
import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.BigTopButtons;
import simulate.z2600k.Windows98.System.Button;
import simulate.z2600k.Windows98.System.ButtonInList;
import simulate.z2600k.Windows98.System.ButtonList;
import simulate.z2600k.Windows98.System.CheckBox;
import simulate.z2600k.Windows98.System.DummyWindow;
import simulate.z2600k.Windows98.System.MessageBox;
import simulate.z2600k.Windows98.System.OnClickRunnable;
import simulate.z2600k.Windows98.System.ScrollBar;
import simulate.z2600k.Windows98.System.Separator;
import simulate.z2600k.Windows98.System.TextBox;
import simulate.z2600k.Windows98.System.TopMenu;
import simulate.z2600k.Windows98.System.TopMenuButton;
import simulate.z2600k.Windows98.System.Window;
import simulate.z2600k.Windows98.System.Windows98;
import simulate.z2600k.Windows98.WindowsView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InternetExplorer extends DummyWindow {
    public WebViewContainer webViewContainer;
    public TextBox urlAddressEdit;
    private boolean goActive = false;  // кнопка Go
    private Bitmap goBmp = getBmp(R.drawable.ie_go);

    public InternetExplorer(){
        this(true);
    }

    public InternetExplorer(boolean loadHomePage){
        super("Google - Microsoft Internet Explorer", getBmp(R.drawable.html_0), true,
                getBmp(R.drawable.ie1), getBmp(Windows98.WIDESCREEN? R.drawable.ie2w : R.drawable.ie2));
        final int[] topButtonsIds = {R.drawable.back_ie, R.drawable.stop, R.drawable.refresh, R.drawable.home, R.drawable.search, R.drawable.favorites, R.drawable.history, R.drawable.mail, R.drawable.print};
        Bitmap[] topButtons = new Bitmap[9];
        for(int i = 0; i < 9; i++)
            topButtons[i] = getBmp(topButtonsIds[i]);
        BigTopButtons bigTopButtons = new BigTopButtons(topButtons,
                new int[][]{{15, 72}, {129, 173}, {173, 217}, {217, 261}, {267, 311}, {311, 355}, {355, 399}, {405, 449}, {449, 493}}, 8, new BigTopButtons.OnButtonPressListener() {
            @Override
            public void onButtonPress(int buttonNumber) {
                MyWebView webView = webViewContainer.webView;
                if(buttonNumber == 0) {
                    if(webView.canGoBack()) {
                        WebBackForwardList backForwardList = webView.copyBackForwardList();
                        WebHistoryItem previousPage = backForwardList.getItemAtIndex(backForwardList.getCurrentIndex() - 1);
                        webView.updateUrl(previousPage.getUrl());
                        setWebPageTitle(previousPage.getTitle(), previousPage.getUrl());
                        webView.goBack();
                    }
                }
                else if(buttonNumber == 1){
                    webView.stopLoading();
                }
                else if(buttonNumber == 2){
                    webView.reload();
                }
                else if(buttonNumber == 3){
                    webView.loadUrl("about:blank");
                }
            }
        });
        bigTopButtons.y = 49;
        addElement(bigTopButtons);

        // поле ввода url
        urlAddressEdit = new TextBox(new RelativeBounds(79, 92, -134, 110),
                6, 14, p, new Rect(-1, -11, 0, 2));
        urlAddressEdit.parent = this;
        urlAddressEdit.setActive(false);
        urlAddressEdit.deleteLongText = true;
        urlAddressEdit.selectOnActive = true;
        urlAddressEdit.enterRunnable = this::go;
        addElement(urlAddressEdit);
        // собственно, интернет
        webViewContainer = new WebViewContainer(new RelativeBounds(6, 119, -24, -44), this);
        addElement(webViewContainer);
        webViewContainer.verticalScrollBar = new ScrollBar(webViewContainer, new RelativeBounds(-24, 119, -6, -44), true);
        webViewContainer.horizontalScrollBar = new ScrollBar(webViewContainer, new RelativeBounds(6, -44, -24, -26), false);
        addElement(webViewContainer.verticalScrollBar);
        addElement(webViewContainer.horizontalScrollBar);
        if(loadHomePage) {
            //webViewContainer.webView.loadUrl("chrome://crash");
            //webViewContainer.webView.loadUrl("https://yadi.sk/d/_uWMbu594SsYow");
            //webViewContainer.webView.loadUrl("https://www.microsoft.com/en-us/download/confirmation.aspx?id=35");
            //webViewContainer.webView.loadUrl("https://gvyoutube.com/watch?v=Zv7UUKRyCis");
            webViewContainer.webView.loadUrl("about:blank");  // указываем страницу загрузки
        }
        setupTopMenu();
    }

    private RelativeBounds progressBarBounds = new RelativeBounds(-314, -22, -216, -4);
    private RelativeBounds goBounds = new RelativeBounds(-112, 90, -61, 112);

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        Rect bounds = progressBarBounds.getRect(maximized);
        bounds.offset(x, y);
        drawProgressBar(canvas, bounds);
        bounds.offset(-x, -y);  // т. к. всё по ссылке

        if(goActive) {
            Rect goBounds = this.goBounds.getRect(maximized);
            canvas.drawBitmap(goBmp, x + goBounds.left, y + goBounds.top, null);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!super.onMouseOver(x, y, touch))
            return false;
        goActive = goBounds.getRect(maximized).contains(x, y);
        if(touch && goActive){
            go();
        }
        return true;
    }

    @Override
    public void onOtherTouch() {
        super.onOtherTouch();
        goActive = false;
    }

    private void go(){  // перейти по ссылке, набранной в textBox
        String url = urlAddressEdit.text.trim();
        if(url.contains(" ") || !url.contains(".")){  // делаем из этого поисковый запрос
            try {
                String query = URLEncoder.encode(url, "utf-8");
                url = "https://www.baidu.com/s?&wd=" + query;
            }
            catch (UnsupportedEncodingException ignored){}
        }
        else if(!url.startsWith("http://") && !url.startsWith("https://"))
            url = "http://" + url; //"http://www." + url;
        urlAddressEdit.setText(url);
        webViewContainer.webView.loadUrl(url);
    }

    private void drawProgressBar(Canvas canvas, Rect rect){
        int x1 = rect.left, y1 = rect.top, x2 = rect.right, y2 = rect.bottom;
        int progress = webViewContainer.webView.getProgress();
        if(progress == 100)
            return;
        drawVerySimpleFrameRect(canvas, x1, y1, x2, y2);
        // надо ещё дорисовать, т. к. на самом деле мы рисуем поверх уже такого же simpleFrameRect
        p.setColor(Color.parseColor("#C0C0C0"));
        canvas.drawRect(x1 - 2, y1, x1, y2, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(x1 - 3, y1, x1 - 2, y2, p);
        int progressWidth = Math.round((x2 - x1 - 6) * progress * 0.01f);
        p.setColor(Color.parseColor("#000080"));
        canvas.drawRect(x1 + 3, y1 + 3, x1 + 3 + progressWidth, y2 - 3, p);
    }

    public void setWebPageTitle(String webPageTitle, String url){
        if(webPageTitle == null || url == null){  // в Play Console есть такой crash. На всякий случай.
            setTitle("Microsoft Internet Explorer");
            return;
        }
        //if(!isAsciiString(webPageTitle))
        //    webPageTitle = url;
        if(!webPageTitle.isEmpty()) {  // && isAsciiString(webPageTitle)) {
            setTitle(webPageTitle + " - Microsoft Internet Explorer");
        }
        else
            setTitle("Microsoft Internet Explorer");
    }

    // работа с положением webView
    @Override
    public void makeActive() {
        super.makeActive();
        if(webViewContainer != null) {  // не вызываемся из super конструктора
            webViewContainer.onMakeActive();
            if(WebViewContainer.customView != null)
                WebViewContainer.customView.bringToFront();
        }
    }

    @Override
    public void minimize() {
        super.minimize();
        webViewContainer.onMinimize();
    }

    @Override
    public void onClick(int x, int y) {
        // проверяем, переместилось ли окно, если да - перемещаем webView
        int oldX = this.x, oldY = this.y;
        super.onClick(x, y);
        if(oldX != this.x || oldY != this.y){
            webViewContainer.updateViewPosition();
        }
    }

    // ================ FILE DOWNLOAD WINDOW =======================================

    public static class FileDownloadWindow extends Window {
        // для GIF анимации загрузки используется Movie для API < 28, AnimatedImageDrawable для API >= 28
        Movie oldMovie;
        AnimatedImageDrawable newMovie;
        Bitmap movieBitmap = createBitmap(272, 60, Bitmap.Config.ARGB_8888);
        Canvas movieCanvas = new Canvas(movieBitmap);  // потому что Movie глючный (???)
        long downloadStartTime = -1;
        Button open, openFolder;
        CheckBox closeOnDownloadComplete;
        float progress = 0;
        boolean downloadComplete = false, downloadFailed = false;
        long downloadId;
        long lastTextUpdate = -1;
        long updateRate = 1000;  // обновляем текст раз в 1000 миллисекунд
        File file;
        String url;
        String savingFrom = "", timeLeft = "", downloadTo = "", transferRate = "";
        BroadcastReceiver onDownloadComplete;
        ContentObserver contentObserver;
        boolean isContentObserverWorking = false;  // может быть, contentObserver ничего не получит - это недокументированная функция
        Bitmap downloadCompleteBmp = getBmp(R.drawable.download_complete);

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            if(!downloadComplete) {
                movieCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.P) {
                    if (downloadStartTime != -1)
                        oldMovie.setTime((int) (System.currentTimeMillis() - downloadStartTime) % oldMovie.duration());
                    oldMovie.draw(movieCanvas, 0, 0); //, x + 14, y + 22);
                }
                else
                    newMovie.draw(movieCanvas);
                canvas.drawBitmap(movieBitmap, x + 14, y + 22, null);
            }
            else
                canvas.drawBitmap(downloadCompleteBmp, x + 14, y + 38, null);
            updateStrings();
            p.setColor(Color.BLACK);
            canvas.drawText("保存：", x + 14, y + 96, p);
            canvas.drawText(savingFrom, x + 14, y + 110, p);
            canvas.drawText(downloadComplete? "下载：" : "估计剩余时间：", x + 14, y + 146, p);
            canvas.drawText(timeLeft, x + 115, y + 146, p);
            canvas.drawText("下载到", x + 14, y + 161, p);
            canvas.drawText(downloadTo, x + 108, y + 160, p);
            canvas.drawText("传输速度：", x + 14, y + 176, p);
            canvas.drawText(transferRate, x + 108, y + 176, p);
            // progress bar
            p.setColor(Color.parseColor("#808080"));  // серый
            canvas.drawRect(x + 14, y + 117, x + 360, y + 118, p);
            canvas.drawRect(x + 14, y + 117, x + 15, y + 128, p);
            p.setColor(Color.WHITE);
            canvas.drawRect(x + 14, y + 128, x + 361, y + 129, p);
            canvas.drawRect(x + 360, y + 117, x + 361, y + 129, p);
            int progressSquares = Math.round(progress * 49);
            p.setColor(Color.rgb(0, 0, 128));  // синий
            for(int i = 0; i < progressSquares; i++){
                canvas.drawRect(x + 16 + 7 * i, y + 119, x + 21 + 7 * i, y + 127, p);
            }
        }

        private void updateStrings(){
            updateStrings(false);
        }

        private void updateStrings(boolean isCalledByContentObserver){  // обновляет строки, которые выводятся на экран, и вообще состояние всего окна
            if(downloadComplete || downloadFailed)
                return;
            if(isContentObserverWorking && !isCalledByContentObserver)
                return;
            if(!isContentObserverWorking && System.currentTimeMillis() - lastTextUpdate < updateRate)
                return;
            checkDownloadStatus();
            if(downloadFailed)
                return;
            //if(file != null) {
            //    if (file.exists())
            //        fileObserver.startWatching();
            //}

            savingFrom = shortenString(getFilename() + " 来自 " + url, 270);
            downloadTo = shortenString(MyDocuments.getFullPath(file), 250);

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).query(query);
            if(!c.moveToFirst()) {  // база данных пустая (?)
                c.close();
                return;
            }
            long size = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            long downloadedBytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            c.close();
            //if(size == -1)
            //    return;
            progress = (float) downloadedBytes / size;
            if(progress < 0)
                progress = 0;
            int progressPercent = Math.round(progress * 100);
            setTitle(progressPercent + "% 已完成 (" + getFilename() + ")");
            //float speed = (downloadedBytes - lastDownloadedBytes) / (System.currentTimeMillis() - lastTextUpdate);
            float downloadTime = (System.currentTimeMillis() - downloadStartTime) / 1000f;
            float speed = downloadedBytes / downloadTime;
            int timeToFinish = Math.round((size - downloadedBytes) / speed);  // в секундах
            String timeString = secondsToString(timeToFinish);
            if(size == -1)
                timeString = "未知 ";
            timeLeft = timeString + "(已复制 " + bytesToString(downloadedBytes) + "，共" + bytesToString(size) + ")";
            timeLeft = shortenTextToThreeDots(timeLeft, 250, p);
            transferRate = bytesToString(speed) + "/秒";

            lastTextUpdate = System.currentTimeMillis();
        }

        private String getFilename(){
            return file != null? file.getName() : "";
        }

        private void checkDownloadStatus(){
            if(downloadComplete || downloadFailed)
                return;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).query(query);
            if(!c.moveToFirst()) {  // база данных пустая (?)
                c.close();
                return;
            }
            int downloadStatus = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            /*int downloadedBytes = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int size = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            if(downloadStatus == DownloadManager.STATUS_SUCCESSFUL || downloadedBytes == size){
                Log.d(TAG, "STATUS SUCCESS!!!!!!");
            }
            else */
            if(downloadStatus == DownloadManager.STATUS_FAILED){
                int reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                /*if(reason == DownloadManager.ERROR_CANNOT_RESUME){
                    File file = new File(MyDocuments.getFilesDir().getAbsolutePath() + File.separator + filename);
                    if(file.exists()){
                        Log.d(TAG, "this file exists, and size is " + file.length());
                    }
                    else
                        Log.d(TAG, "sadly, it no more exists");
                }
                Log.d(TAG, "shit ass mother fuckers!", new Exception());*/
                setDownloadFailed(reason);
            }
            c.close();
            //Log.d(TAG, "downloaded vs size: " + downloadedBytes + " " + size);
        }

        private void setDownloadFailed(int reason){
            downloadFailed = true;
            stopAnimation();
            String text = "下载失败";
            if(reason == DownloadManager.ERROR_INSUFFICIENT_SPACE)
                text += "没有足够的可用磁盘空间。请删除部分文件以释放磁盘空间，然后再试。";
            else if(reason == DownloadManager.ERROR_CANNOT_RESUME)
                text += "不支持断点续传。";
            else if(reason == DownloadManager.ERROR_DEVICE_NOT_FOUND)
                text += "无法访问磁盘。";
            else if(reason == DownloadManager.ERROR_FILE_ERROR)
                text += "磁盘写入错误。";
            //Log.d(TAG, "reason: " + reason);
            new MessageBox("文件下载", text, MessageBox.OK, MessageBox.ERROR, new MessageBox.MsgResultListener() {
                @Override
                public void onMsgResult(int buttonNumber) {
                    close();
                }
            }, this);
        }

        private String secondsToString(int time){  // строка с пробелом на конце!
            int seconds = time % 60;
            int minutes = (time / 60) % 60;
            int hours = (time / (60 * 60)) % 24;
            int days = (time / (60 * 60 * 24));
            String timeString = "";
            if(days != 0)
                timeString += days + (" 天 ");
            if(hours != 0)
                timeString += hours + (" 小时 ");
            if(minutes != 0)
                timeString += minutes + (" 分钟 ");
            timeString += seconds + (" 秒");
            return timeString;
        }
        private String shortenString(String text, int maxWidth){
            if (measureText(text, p) > maxWidth) {
                int countDrawingSymbols = 0;
                // copypaste из shortenTextToThreeDots
                while (measureText("..." + text.substring(text.length() - (countDrawingSymbols + 1)), p) <= maxWidth)
                    countDrawingSymbols++;
                return "..." + text.substring(text.length() - countDrawingSymbols);
            } else
                return text;
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            updateStrings();
            if(!closed)
                return super.onMouseOver(x, y, touch);
            else
                return false;
        }

        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                WindowsView.handler.postDelayed(this, 70);
                FileDownloadWindow.this.updateWindow();
            }
        };

        @SuppressWarnings("ResourceType")
        public FileDownloadWindow(final String url, final String userAgent, final String contentDisposition, final String mimeType){
            super("文件下载", null, 374, 261, false, false, false);
            // инициализируем элементы
            unableToMaximize = true;
            if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.P) {
                oldMovie = Movie.decodeStream(resources.openRawResource(R.drawable.tshell32_170));
            }
            else{
                try {
                    ImageDecoder.Source source = ImageDecoder.createSource(resources, R.drawable.tshell32_170);
                    newMovie = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(source);
                }
                catch (Exception e){
                    throw new RuntimeException("AnimatedImageDrawable loading failed");
                }
                newMovie.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            }

            open = new Button("打开", new Rect(98, 210, 184, 231), parent -> {
                close();
                OnClickRunnable r = new Link(file, FileDownloadWindow.this).action;
                if(r != null)
                    r.run(null);
            });

            open.disabled = true;
            addElement(open);
            openFolder = new Button("打开文件夹", new Rect(188, 210, 274, 231), parent -> {
                close();
                new MyDocuments(file.getParentFile());
            });
            openFolder.disabled = true;
            addElement(openFolder);
            defaultButton = new Button("取消", new Rect(278, 210, 364, 231), parent -> close());
            defaultButton.coolActive = true;
            addElement(defaultButton);
            closeOnDownloadComplete = new CheckBox("下载完毕后关闭该对话框");
            closeOnDownloadComplete.x = 17;
            closeOnDownloadComplete.y = 191;
            addElement(closeOnDownloadComplete);
            centerWindowOnScreen();
            this.url = url.substring(url.indexOf('/') + 2);  // http(s)://
            if(this.url.contains("/"))
                this.url = this.url.substring(0, this.url.indexOf('/'));
            // создаём FileDialog
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            String name, extension;
            if(filename.contains(".")) {
                int dotIndex = filename.lastIndexOf('.');
                name = filename.substring(0, dotIndex);
                extension = filename.substring(dotIndex + 1);
            }
            else {
                name = filename;
                extension = "bin";
            }
            //if(!isAsciiString(name))
            //    name = "downloadfile";
            //filename = name + "." + extension;
            String extensionDescription = extension.toUpperCase() + " 文件";
            extensionDescription = shortenTextToThreeDots(extensionDescription, 205, p);
            new FileDialog(false, extension, extensionDescription, this, new FileDialog.OnResultListener() {
                @Override
                public void writeToFile(final File file) {
                    if(Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.Q) {
                        context.checkWriteExternalPermission(new MainActivity.PermissionResultListener() {
                             @Override
                             public void onPermissionGranted() {
                                 FileDownloadWindow.this.onPermissionGranted(file, url, mimeType, userAgent);
                             }

                             @Override
                             public void onPermissionDenied() {
                                 setDownloadFailed(DownloadManager.ERROR_DEVICE_NOT_FOUND);
                                 updateWindow();
                             }
                        });
                    }
                    else
                        onPermissionGranted(file, url, mimeType, userAgent);
                }
                @Override
                void onCancel() {
                    close();
                }
            }, name, false);
        }

        private void onPermissionGranted(File file, String url, String mimeType, String userAgent) {
            if (file.exists())  // DownloadManager не умеет заменять файлы
                file.delete();
            FileDownloadWindow.this.file = file;
            String filename = file.getName();
            startAnimation();
            // загружаем файл
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("Cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Windows 98 Downloads");
            request.setTitle(filename);
            if(Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.Q) {
                request.allowScanningByMediaScanner();
                request.setVisibleInDownloadsUi(false);
            }
            //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalFilesDir(context, null, MyDocuments.getRelativePath(file));
            DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(request);
            // конец загрузки
            onDownloadComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Fetching the download id received with the broadcast
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    //Checking if the received broadcast is for our enqueued download by matching download id
                    if (id == downloadId) {
                        checkDownloadStatus();
                        if (downloadFailed) {
                            return;
                        }
                        downloadComplete = true;
                        progress = 1;

                        if (closeOnDownloadComplete.checked) {
                            close();
                            return;
                        }
                        setTitle("下载完成");
                        closeOnDownloadComplete.visible = false;
                        open.disabled = false;
                        openFolder.disabled = false;
                        defaultButton.text = "关闭";  // Cancel -> Close
                        float downloadTime = (System.currentTimeMillis() - downloadStartTime) / 1000f;

                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        Cursor c = ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).query(query);
                        long size = 0;
                        if(c.moveToFirst())
                            size = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        c.close();
                        // переиспользуем те же строки
                        timeLeft = bytesToString(size) + "(共 " + secondsToString(Math.round(downloadTime));
                        transferRate = bytesToString(size / downloadTime) + "/秒";
                        stopAnimation();
                        updateWindow();
                    }
                }
            };
            ContextCompat.registerReceiver(context, onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);

            Uri myDownloads = Uri.parse("content://downloads/my_downloads");
            contentObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    String uriStr = uri.toString();
                    if(uriStr.substring(uriStr.lastIndexOf('/') + 1).equals(String.valueOf(downloadId))) {
                        //Log.d(TAG, "content observer update");
                        isContentObserverWorking = true;
                        updateStrings(true);
                    }
                    updateWindow();
                }
            };
            context.getContentResolver().registerContentObserver(myDownloads, true, contentObserver);
            updateWindow();
        }

        private void startAnimation(){
            WindowsView.handler.postDelayed(updateRunnable, 70);
            if(Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.P)
                newMovie.start();
            downloadStartTime = System.currentTimeMillis();
        }

        private void stopAnimation(){
            WindowsView.handler.removeCallbacks(updateRunnable);
            if(Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.P)
                newMovie.stop();
        }

        @Override
        public void close(boolean activateNextWindow) {
            if(!downloadComplete)
                ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).remove(downloadId);  // удаляем нашу загрузку
            super.close(activateNextWindow);
        }

        @Override
        public void prepareForDelete() {
            super.prepareForDelete();
            stopAnimation();
            try{
                context.unregisterReceiver(onDownloadComplete);
            }
            catch (Exception ignored){}
            try{
                context.getContentResolver().unregisterContentObserver(contentObserver);
            }
            catch (Exception ignored){}
        }

        private final static String[] units = {"字节", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"};
        public static String bytesToString(double bytes){
            if(bytes < 0)
                return "(未知)";
            int curIndex = 0;  // индекс в массиве units
            while(bytes > 1024 && curIndex < units.length - 1){
                bytes /= 1024;
                curIndex++;
            }
            // округляем до 3 значащих цифр
            String numberFormatted;
            if(bytes < 10)  // 1.23
                numberFormatted = String.format(Locale.CHINA, "%.2f", bytes);
            else if(bytes < 100)  // 12.3
                numberFormatted = String.format(Locale.CHINA, "%.1f", bytes);
            else // 123
                numberFormatted = String.format(Locale.CHINA, "%.0f", bytes);
            return numberFormatted + " " + units[curIndex];
        }
    }

    private void setupTopMenu(){
        ButtonList file = new ButtonList();
        ButtonList new_ = new ButtonList();
        new_.elements.add(new ButtonInList("窗口", "Ctrl+N"));
        new_.elements.add(new Separator());
        new_.elements.add(new ButtonInList("邮件"));
        new_.elements.add(new ButtonInList("发布信息"));
        new_.elements.add(new ButtonInList("联系人"));
        new_.elements.add(new ButtonInList("Internet 呼叫"));
        file.elements.add(new ButtonInList("新建", new_));
        file.elements.add(new ButtonInList("打开...", "Ctrl+O"));
        ButtonInList editButton = new ButtonInList("编辑");
        editButton.disabled = true;
        file.elements.add(editButton);
        ButtonInList save = new ButtonInList("保存", "Ctrl+S");
        save.disabled = true;
        file.elements.add(save);
        file.elements.add(new ButtonInList("另存为..."));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("页面设置..."));
        file.elements.add(new ButtonInList("打印...", "Ctrl+P"));
        file.elements.add(new Separator());
        ButtonList send = new ButtonList();
        send.elements.add(new ButtonInList("电子邮件页面..."));
        send.elements.add(new ButtonInList("电子邮件链接..."));
        send.elements.add(new ButtonInList("桌面快捷方式"));
        file.elements.add(new ButtonInList("发送", send));
        file.elements.add(new ButtonInList("导入和导出..."));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("属性"));
        file.elements.add(new ButtonInList("脱机工作"));
        ButtonInList close = new ButtonInList("关闭");
        close.action = parent -> close();
        file.elements.add(close);

        ButtonList edit = new ButtonList();
        ButtonInList cut = new ButtonInList("剪切", "Ctrl+X");
        cut.disabled = true;
        edit.elements.add(cut);
        ButtonInList copy = new ButtonInList("复制", "Ctrl+C");
        copy.disabled = true;
        edit.elements.add(copy);
        ButtonInList paste = new ButtonInList("粘贴", "Ctrl+V");
        paste.disabled = true;
        edit.elements.add(paste);
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("全选", "Ctrl+A"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("查找(在当前页)...", "Ctrl+F"));

        ButtonList view = new ButtonList();
        ButtonList toolbars = new ButtonList();
        ButtonInList standartButtons = new ButtonInList("标准按钮");
        standartButtons.check = true;
        standartButtons.checkActive = true;
        toolbars.elements.add(standartButtons);
        ButtonInList addressBar = new ButtonInList("地址栏");
        addressBar.check = true;
        addressBar.checkActive = true;
        toolbars.elements.add(addressBar);
        ButtonInList links = new ButtonInList("链接");
        links.check = true;
        links.checkActive = true;
        toolbars.elements.add(links);
        ButtonInList radio = new ButtonInList("电台");
        radio.check = true;
        radio.checkActive = false;
        toolbars.elements.add(radio);
        toolbars.elements.add(new Separator());
        toolbars.elements.add(new ButtonInList("自定义..."));
        view.elements.add(new ButtonInList("工具栏", toolbars));
        ButtonInList statusBar = new ButtonInList("状态栏");
        statusBar.check = true;
        statusBar.checkActive = true;
        view.elements.add(statusBar);
        ButtonList explorerBar = new ButtonList();
        explorerBar.elements.add(new ButtonInList("搜索", "Ctrl+E"));
        explorerBar.elements.add(new ButtonInList("收藏夹", "Ctrl+I"));
        explorerBar.elements.add(new ButtonInList("文件夹", "Ctrl+H"));
        explorerBar.elements.add(new ButtonInList("Folders"));
        explorerBar.elements.add(new Separator());
        explorerBar.elements.add(new ButtonInList("每日提示"));
        view.elements.add(new ButtonInList("浏览栏", explorerBar));
        view.elements.add(new Separator());
        ButtonList goTo = new ButtonList();
        goTo.elements.add(new ButtonInList("后退", "Alt+←"));
        goTo.elements.add(new ButtonInList("前进", "Alt+→"));
        goTo.elements.add(new Separator());
        goTo.elements.add(new ButtonInList("主页", "Alt+Home"));
        goTo.elements.add(new Separator());
        goTo.elements.add(new ButtonInList("找不到服务器"));
        view.elements.add(new ButtonInList("转到", goTo));
        view.elements.add(new ButtonInList("停止", "Esc"));
        view.elements.add(new ButtonInList("刷新", "F5"));
        view.elements.add(new Separator());
        ButtonList textSize = new ButtonList();
        List<ButtonInList> buttonGroup = new ArrayList<>();
        buttonGroup.add(new ButtonInList("最大"));
        buttonGroup.add(new ButtonInList("较大"));
        buttonGroup.add(new ButtonInList("中"));
        buttonGroup.add(new ButtonInList("较小"));
        buttonGroup.add(new ButtonInList("最小"));
        buttonGroup.get(2).checkActive = true;  // Medium
        for(ButtonInList button : buttonGroup){
            button.radioButtonGroup = buttonGroup;
            textSize.elements.add(button);
        }
        view.elements.add(new ButtonInList("文字大小", textSize));
        ButtonList encoding = new ButtonList();
        ButtonInList autoSelect = new ButtonInList("自动选择");
        autoSelect.check = true;
        encoding.elements.add(autoSelect);
        encoding.elements.add(new Separator());
        ButtonInList westernEuropean = new ButtonInList("简体中文 (GB2312)");
        List<ButtonInList> buttonGroup2 = new ArrayList<>();
        buttonGroup2.add(westernEuropean);
        westernEuropean.radioButtonGroup = buttonGroup2;
        westernEuropean.checkActive = true;
        encoding.elements.add(westernEuropean);
        encoding.elements.add(new ButtonInList("其他..."));
        view.elements.add(new ButtonInList("编码", encoding));
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("源文件"));
        view.elements.add(new ButtonInList("全屏", "F11"));

        ButtonList favorites = StartMenu.getFavoritesMenu();
        favorites.elements.add(0, new ButtonInList("添加到收藏夹..."));
        favorites.elements.add(1, new ButtonInList("整理收藏夹..."));
        favorites.elements.add(2, new Separator());

        ButtonList tools = new ButtonList();
        ButtonList mailAndNews = new ButtonList();
        mailAndNews.elements.add(new ButtonInList("阅读邮件"));
        mailAndNews.elements.add(new ButtonInList("新建邮件..."));
        mailAndNews.elements.add(new ButtonInList("发送链接..."));
        mailAndNews.elements.add(new ButtonInList("发送网页..."));
        mailAndNews.elements.add(new Separator());
        mailAndNews.elements.add(new ButtonInList("阅读新闻"));
        tools.elements.add(new ButtonInList("邮件和新闻", mailAndNews));
        tools.elements.add(new ButtonInList("同步"));
        tools.elements.add(new ButtonInList("Windows Update"));
        tools.elements.add(new Separator());
        tools.elements.add(new ButtonInList("显示相关站点"));
        tools.elements.add(new Separator());
        tools.elements.add(new ButtonInList("Internet 选项..."));

        ButtonList help = new ButtonList();
        help.elements.add(new ButtonInList("目录和索引"));
        help.elements.add(new ButtonInList("每日提示"));
        help.elements.add(new ButtonInList("Netscape 用户"));
        help.elements.add(new ButtonInList("教程"));
        help.elements.add(new ButtonInList("联机支持"));
        help.elements.add(new ButtonInList("发送反馈意见"));
        help.elements.add(new Separator());
        ButtonInList aboutIE = new ButtonInList("关于 Internet Explorer");
        aboutIE.action = parent -> {
            new DummyWindow("关于 Internet Explorer", null, false, getBmp(R.drawable.about_ie), new Rect(346, 303, 430, 329), "确定");
            onOtherTouch();
        };
        help.elements.add(aboutIE);

        TopMenu topMenu = new TopMenu();
        topMenu.buttonsSize = 2;
        topMenu.elements.add(new TopMenuButton("文件", file));
        topMenu.elements.add(new TopMenuButton("编辑", edit));
        topMenu.elements.add(new TopMenuButton("视图", view));
        topMenu.elements.add(new TopMenuButton("收藏夹", favorites));
        topMenu.elements.add(new TopMenuButton("工具", tools));
        topMenu.elements.add(new TopMenuButton("帮助", help));
        topMenu.x = 15;
        topMenu.y = 25;
        repositionTopMenu = false;
        setTopMenu(topMenu);
    }
}
