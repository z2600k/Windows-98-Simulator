package simulate.z2600k.Windows98.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public class AboutWindow extends DialogWindow {
    Bitmap bigIcon;
    String appTitle;
    String additionalText;  // Copyright (C) 1981-1998 Microsoft
    public int systemResources = (int)(Math.random() * (94 - 86 + 1)) + 86;  // 86 - 94%
    public AboutWindow(Window parentWindow, String appTitle, Bitmap bigIcon){
        this(parentWindow, appTitle, bigIcon, "");
    }
    public AboutWindow(Window parentWindow, String appTitle, Bitmap bigIcon, String additionalText){
        super("关于" + appTitle, 351, 315, parentWindow);
        this.appTitle = appTitle;
        this.bigIcon = bigIcon;
        this.additionalText = additionalText;
        Button ok = new Button("确定", 68, 23, 245, 271, Color.BLACK, true, parent -> close());
        ok.coolActive = true;
        defaultButton = ok;
        elements.add(ok);
        alignWithParent(35, 74);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        canvas.drawBitmap(bigIcon, x + 13, y + 32, null);
        p.setColor(Color.BLACK);
        canvas.drawText("Microsoft (R) " + appTitle, x + 81, y + 42, p);
        canvas.drawText("Windows 98", x + 81, y + 64, p);
        canvas.drawText("版权所有 (C) 1981-1998 Microsoft Corp.", x + 81, y + 83, p);
        canvas.drawText(additionalText, x + 81, y + 100, p);
        canvas.drawText("本产品的使用权属于:", x + 81, y + 167, p);
        canvas.drawText("Microsoft User", x + 81, y + 181, p);
        canvas.drawText("Microsoft Corporation", x + 81, y + 201, p);
        // рисуем separator
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 81, y + 207, x + 331, y + 209, p);
        p.setColor(Color.parseColor("#808080"));
        canvas.drawRect(x + 81, y + 207, x + 330, y + 208, p);
        // продолжение текста
        p.setColor(Color.BLACK);
        canvas.drawText("Windows 的可用物理内存:", x + 81, y + 230, p);
        canvas.drawText("130,512 KB", x + 249, y + 230, p);
        canvas.drawText("系统资源:", x + 81, y + 254, p);
        canvas.drawText( systemResources + "% 可用空间", x + 249, y + 254, p);
        //drawTopLayer(canvas, shift_x, shift_y);
    }
}
