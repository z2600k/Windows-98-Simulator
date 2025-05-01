package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import java.util.Objects;

import simulate.z2600k.Windows98.System.Button;
import simulate.z2600k.Windows98.System.Window;

public class Wizard extends Window {  // мастер по установке
    Bitmap[] pages;
    int curPage = 0;
    Button back, next, finish;
    boolean ableToFinish;
    public Wizard(String windowTitle, boolean ableToFinish, Bitmap[] pages){
        super(windowTitle, null, pages[0].getWidth(), pages[0].getHeight(), false, false, true);
        fillWindow = false;
        this.pages = pages;
        this.ableToFinish = ableToFinish;
        if(Objects.equals(windowTitle, "计划任务向导") || Objects.equals(windowTitle,"添加“Web 文件夹”")){
            back = new Button("< 上一步", 75, 21, 193, height - 33, Color.BLACK, true, parent -> {
                curPage--;
                updateButtons();
            });
            addElement(back);

            next = new Button("下一步 >", 75, 21, 268, height - 33, Color.BLACK, true, parent -> {
                curPage++;
                updateButtons();
            });
            addElement(next);

            addElement(new Button("取消", 75, 21, 353, height - 33, Color.BLACK, true, parent -> close()));

            finish = new Button("完成", 75, 21, 268, height - 33, Color.BLACK, true, parent -> close());
            addElement(finish);

            updateButtons();
        }
        else if(Objects.equals(windowTitle, "允许多用户设置")){
            back = new Button("< 上一步", 75, 21, 210, height - 33, Color.BLACK, true, parent -> {
                curPage--;
                updateButtons();
            });
            addElement(back);

            next = new Button("下一步 >", 75, 21, 285, height - 33, Color.BLACK, true, parent -> {
                curPage++;
                updateButtons();
            });
            addElement(next);

            addElement(new Button("取消", 75, 21, 370, height - 33, Color.BLACK, true, parent -> close()));

            finish = new Button("完成", 75, 21, 285, height - 33, Color.BLACK, true, parent -> close());
            addElement(finish);

            updateButtons();
        }
        else {
            back = new Button("< 上一步", 75, 21, 205, height - 38, Color.BLACK, true, parent -> {
                curPage--;
                updateButtons();
            });
            addElement(back);

            next = new Button("下一步 >", 75, 21, 280, height - 38, Color.BLACK, true, parent -> {
                curPage++;
                updateButtons();
            });
            addElement(next);

            addElement(new Button("取消", 75, 21, 365, height - 38, Color.BLACK, true, parent -> close()));

            finish = new Button("完成", 75, 21, 280, height - 38, Color.BLACK, true, parent -> close());
            addElement(finish);

            updateButtons();
        }
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(pages[curPage], x, y, null);
        super.onNewDraw(canvas, x, y);
    }

    private void updateButtons(){  // мы перешли на другую страницу, какие-то кнопки могут поменять своё состояние
        back.disabled = (curPage == 0);
        next.disabled = (curPage == pages.length - 1);
        finish.visible = (curPage == pages.length - 1 && ableToFinish);  // finish будет перекрывать next
        if(curPage == pages.length - 1){  // последняя страница
            if(ableToFinish) {
                finish.coolActive = true;
                defaultButton = finish;
            }
            else{
                back.coolActive = true;
                defaultButton = back;
            }
        }
        else{
            next.coolActive = true;
            back.coolActive = false;
            defaultButton = next;
        }
    }
}
