package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.Button;
import simulate.z2600k.Windows98.System.RadioButton;
import simulate.z2600k.Windows98.System.Window;
import simulate.z2600k.Windows98.System.Windows98;

public class ShutDownWindow extends Window {
    private Bitmap iconBmp;
    private DitherPainter ditherPainter;
    private boolean logoff;

    public ShutDownWindow(boolean logoff){
        super(logoff? "注销 Windows" : "关闭 Windows", null,
                logoff? 288 : 323, logoff? 123 : 173, false, false, true);
        this.logoff = logoff;
        ditherPainter = new DitherPainter(Color.TRANSPARENT, Color.BLACK);
        ditherPainter.ignorePosition = true;
        Windows98.windows98.topElement = this;

        if(!logoff) {
            centerWindowHorizontally();
            y = 100;
            iconBmp = getBmp(R.drawable.shut_down_with_computer_0);

            final RadioButton shutDown = new RadioButton("关闭计算机?");
            final RadioButton restart = new RadioButton("重新启动计算机?");
            RadioButton restartDos = new RadioButton("重新启动计算机并切换到 MS-DOS 方式?");
            shutDown.active = true;
            RadioButton.createGroup(shutDown, restart, restartDos);
            addElement(shutDown, 61, 67);
            addElement(restart, 61, 87);
            addElement(restartDos, 61, 106);

            Button ok = new Button("确定", new Rect(62, 139, 140, 162), parent -> {
                if(shutDown.active)
                    Windows98.windows98.shutdown();
                else if(restart.active)
                    Windows98.windows98.restart();
                else
                    Windows98.windows98.restartInMsDosMode();
            });
            ok.coolActive = true;
            defaultButton = ok;
            addElement(ok);

            addCloseButton(new Rect(146, 139, 224, 162), "取消");
            addElement(new Button("帮助", new Rect(230, 139, 308, 162), null));
        }
        else{
            iconBmp = getBmp(R.drawable.logoff_key_big);
            Button yes = new Button("是", new Rect(77, 84, 142, 107),
                    parent -> Windows98.windows98.shutdown());
            yes.coolActive = true;
            defaultButton = yes;
            addElement(yes);
            addCloseButton(new Rect(152, 84, 217, 107), "否");
            centerWindowOnScreen();
        }
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        drawDitherRect(canvas, 0, 0, Windows98.SCREEN_WIDTH, Windows98.SCREEN_HEIGHT, ditherPainter);
        super.onNewDraw(canvas, x, y);
        canvas.drawBitmap(iconBmp, x + (logoff? 24 : 14), y + 40, null);
        p.setColor(Color.BLACK);
        if(!logoff)
            canvas.drawText("确实要：", x + 60, y + 51, p);
        else
            canvas.drawText("确实要注销吗?", x + 77, y + 57, p);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        super.onMouseOver(x, y, touch);
        return true;
    }

    @Override
    public void onSelfOtherTouch() {
        topElement = this;  // на случай, если будет создан MessageBox или типа того
    }
}
