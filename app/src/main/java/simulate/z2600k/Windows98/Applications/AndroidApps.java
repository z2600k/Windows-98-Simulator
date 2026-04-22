package simulate.z2600k.Windows98.Applications;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;

import simulate.z2600k.Windows98.System.Windows98;
import simulate.z2600k.Windows98.WindowsView;
import simulate.z2600k.Windows98.System.AndroidIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AndroidApps extends Explorer {
    private static final List<AppInfo> apps = new ArrayList<>();  // список приложений, может обновлятся
    private Map<Link, AppInfo> linkAppInfo = new HashMap<>();
    private final static Set<AndroidApps> activeWindows = new HashSet<>();
    private final static AndroidIcon androidIcon=new AndroidIcon();

    public AndroidApps(){
        super("Android 应用", "Android 应用",androidIcon.GetAndroidLogo("Small"), androidIcon.GetAndroidLogo("Normal"), false);
        helpText = defaultHelpText = "选定项目可以启动应用程序。";
        synchronized (apps) {
            for (AppInfo appInfo : apps) {
                addLink(appInfo);
            }
        }
        linkContainer.updateLinkPositions();
        synchronized (activeWindows) {
            activeWindows.add(this);
        }
    }

    private void addLink(final AppInfo appInfo) {
        Link appLink = new Link(appInfo.name, "应用程序", appInfo.icon, parent -> {
            try {
                if (appInfo.launchIntent != null)
                    context.startActivity(appInfo.launchIntent);
            }
            catch (ActivityNotFoundException ignored) {}
        });
        linkAppInfo.put(appLink, appInfo);
        addLink(appLink);
    }

    public static boolean weAreLauncher(){
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null && resolveInfo.activityInfo != null &&
                context.getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    public void deleteApp(Link link){
        Uri packageURI = Uri.parse("package:" + Objects.requireNonNull(linkAppInfo.get(link)).packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        context.startActivity(uninstallIntent);
    }

    @Override
    public void prepareForDelete() {
        synchronized (activeWindows) {
            activeWindows.remove(this);
        }
        super.prepareForDelete();
    }

    private static void loadAppInfo() {
        //Log.d(TAG, "Loading app info!");
        synchronized (apps) {
            apps.clear();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> allApps = packageManager.queryIntentActivities(intent, 0);
            Set<String> packages = new HashSet<>();  // так как приложения могут повторяться, если у них несколько activities
            String win98package = context.getPackageName();

            for (ResolveInfo resolveInfo : allApps) {
                String name = resolveInfo.loadLabel(packageManager).toString();
                String packageName = resolveInfo.activityInfo.packageName;
                if (packages.contains(packageName))
                    continue;
                packages.add(packageName);
                Bitmap bitmap;
                // загружаем bitmap
                try {
                    Drawable drawable = resolveInfo.loadIcon(packageManager);
                    bitmap = createBitmap(32, 32, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.O && drawable instanceof AdaptiveIconDrawable adaptive && !packageName.equals("com.android.chrome")) {
                        // у хрома пусть будет круглая иконка
                        adaptive.setBounds(0, 0, 32, 32);
                        adaptive.draw(canvas);
                    } else {
                        drawable.setBounds(0, 0, 32, 32);
                        drawable.draw(canvas);
                    }
                } catch (Exception e) {
                    continue;
                }
                Intent launchIntent = null;
                if (!win98package.equals(packageName)) {
                    launchIntent = new Intent();
                    launchIntent.setClassName(packageName, resolveInfo.activityInfo.name);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                apps.add(new AppInfo(name, packageName, bitmap, launchIntent));
            }

            Collections.sort(apps, (a, b) -> {
                int result = a.name.compareTo(b.name);
                if (result != 0)
                    return result;
                else
                    return a.packageName.compareTo(b.packageName);
            });
        }
    }

    private void updateApps(){  // обновить ярлыки программ в данном окне
        Set<AppInfo> oldApps = new HashSet<>(linkAppInfo.values());
        synchronized (apps) {
            boolean needRebuild = false;
            if (apps.size() != oldApps.size()) {
                needRebuild = true;
            } else {
                for (AppInfo appInfo : apps) {
                    AppInfo old = findOldAppInfo(oldApps, appInfo);
                    if (old == null || !old.icon.sameAs(appInfo.icon)) {
                        needRebuild = true;
                        break;
                    }
                }
            }
            if (!needRebuild) return;

            // 完全重建链接
            for (Map.Entry<Link, AppInfo> entry : new HashMap<>(linkAppInfo).entrySet()) {
                entry.getKey().removeFromParent();
            }
            linkAppInfo.clear();
            for (AppInfo appInfo : apps) {
                addLink(appInfo);
            }
        }
        linkContainer.updateLinkPositions();
        updateWindow();
    }

    // 辅助方法
    private AppInfo findOldAppInfo(Set<AppInfo> oldApps, AppInfo target) {
        for (AppInfo old : oldApps) {
            if (old.packageName.equals(target.packageName)) return old;
        }
        return null;
    }

    private static class AppInfo {  // так как Link содержит ссылку на parent, и garbage collection не получится
        String name, packageName;
        Bitmap icon;
        Intent launchIntent;

        public AppInfo(String name, String packageName, Bitmap icon, Intent launchIntent) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
            this.launchIntent = launchIntent;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if(!(obj instanceof AppInfo))
                return false;
            return packageName.equals(((AppInfo) obj).packageName);
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
        }
    }

    private static volatile boolean loadingAppInfo = false;

    private static synchronized void updateAllAndroidApps(){  // загрузить новую информацию об установленных приложениях, обновить все окна
        if(Windows98.state != Windows98.WORKING)
            return;
        Thread updateAppsThread = new Thread(){
            @Override
            public void run() {
                if(loadingAppInfo)
                    return;
                loadingAppInfo = true;
                loadAppInfo();
                // обновлять окна надо в UI thread
                WindowsView.handler.post(() -> {
                    synchronized (activeWindows) {
                        for (AndroidApps window : activeWindows) {
                            if(!window.closed)
                                window.updateApps();
                        }
                    }
                });
                loadingAppInfo = false;
            }
        };
        updateAppsThread.start();
    }

    private static boolean monitoringStarted = false;
    private static int sequenceNumber = 0;

    public static void startInstallMonitoring(){
        if(monitoringStarted)
            return;
        monitoringStarted = true;
        loadAppInfo();
        if(Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.O){
            BroadcastReceiver installReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateAllAndroidApps();
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addDataScheme("package");
            context.registerReceiver(installReceiver, intentFilter);
        }
        else{
            // Android 8.0+ 使用 ChangedPackages 监控安装/卸载
            Runnable checkInstalls = () -> {
                if (context == null) return;
                PackageManager pm = context.getPackageManager();
                ChangedPackages changedPackages = pm.getChangedPackages(sequenceNumber);
                if (changedPackages != null) {
                    sequenceNumber = changedPackages.getSequenceNumber();
                    if (!changedPackages.getPackageNames().isEmpty())
                        updateAllAndroidApps();
                }
            };
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleWithFixedDelay(checkInstalls, 5, 5, TimeUnit.SECONDS);

            // 添加配置变更监听（用于捕获图标形状变化）
            BroadcastReceiver configChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
                        updateAllAndroidApps();
                    }
                }
            };
            IntentFilter configFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
            context.registerReceiver(configChangeReceiver, configFilter);
            ScheduledExecutorService shapeChecker = Executors.newScheduledThreadPool(1);
            // 5 秒后备检查：强制刷新（如果图标实际无变化，updateAllAndroidApps 内部会快速返回）
            shapeChecker.scheduleWithFixedDelay(AndroidApps::updateAllAndroidApps, 5, 5, TimeUnit.SECONDS);
        }
    }
}
