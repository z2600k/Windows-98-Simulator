package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.CheckBox;
import simulate.z2600k.Windows98.System.DummyWindow;

public class FileProperties extends DummyWindow {
    String filename, type, location, msDosName, size;
    Bitmap fileBmp;
    public FileProperties(Link lnk){
        this(lnk.fullFilename, lnk.helpText, lnk.fullPath,
                lnk.path != null? InternetExplorer.FileDownloadWindow.bytesToString(lnk.path.length()) : "0 字节 (0 字节), 占用 0 字节",
                lnk.icon);
    }

    public FileProperties(String filename, String type, String location, String size, Bitmap fileBmp) {
        super(Link.getSimpleFilename(filename) + " 属性", null, false, getBmp(R.drawable.properties_window),
                new Rect(121, 349, 196, 370), "确定", new Rect(202, 349, 277, 370), "取消");
        this.filename = shortenTextToThreeDots(Link.getSimpleFilename(filename), 256, p);
        this.type = type;
        this.location = location;
        this.msDosName = MsDos.getMsDosFilename(filename);
        this.size = size;
        this.fileBmp = fileBmp;
        addElement(new CheckBox("只读"), 115, 268);
        addElement(new CheckBox("存档"), 115, 286);
        addElement(new CheckBox("隐藏"), 207, 268);
        addElement(new CheckBox("系统"), 207, 286);
        addElement(new CheckBox("启用缩略图查看方式"), 115, 313);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        p.setColor(Color.BLACK);

        canvas.drawBitmap(fileBmp, x + 24, y + 57, null);
        canvas.drawText(filename, x + 85, y + 76, p);
        canvas.drawText(type, x + 85, y + 116, p);
        canvas.drawText(location, x + 85, y + 133, p);
        canvas.drawText(size, x + 85, y + 151, p);
        canvas.drawText(msDosName, x + 115, y + 226, p);
    }
}
