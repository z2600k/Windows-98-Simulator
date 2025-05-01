package simulate.z2600k.Windows98.Applications;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.google.android.material.snackbar.Snackbar;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.Button;
import simulate.z2600k.Windows98.System.Element;
import simulate.z2600k.Windows98.System.MessageBox;
import simulate.z2600k.Windows98.System.Window;
import simulate.z2600k.Windows98.WindowsView;

import java.util.StringTokenizer;

public class Run extends BaseNotepad {
    public Run(){
        super("运行", false,
                new Rect(70, 80, 323, 96), 2, 12, p, new Rect(-1, -11, 0, 3),
                R.drawable.run_window, new Rect(167, 117, 242, 138), "取消");
        ((Button) elements.get(0)).coolActive = false;  // так как уже есть другая кнопка, неактивный OK
        Button ok = new Button("确定", 75, 21, 68, 117, Color.BLACK, true, parent -> {
            if(runAction())
                close();
        });
        ok.coolActive = true;
        ok.disabled = true;  // потому, что textBox пустой
        defaultButton = ok;
        addElement(ok);
        addElement(new Button("浏览...", 75, 21, 266, 117, Color.BLACK, true, null));
    }

    private boolean runAction(){  // возвращает успех выполнения
        String text = textBox.text;

        Class<? extends Window> windowClass = getWindowClass(text);
        if(windowClass != null) {
            try {
                windowClass.newInstance();
            }
            catch (IllegalAccessException ignored) {}
            catch (InstantiationException ignored) {}

            return true;
        }

        if((text.contains("/") || text.contains("\\"))
                && (text.contains("con") || text.contains("prn") || text.contains("nul") || text.contains("aux") || text.contains("lpt"))){
            WindowsView.windowsView.slowBSOD();
            return false;
        }

        new MessageBox(text, "找不到文件 '" + text + "' (或它的组件之一)。请确定路径和文件名是否正确，而且所需的库文件均可用。",
                MessageBox.OK, MessageBox.ERROR, null, this);
        return false;
    }

    public static Class<? extends Window> getWindowClass(String text){
        if(text.isEmpty())
            return null;
        StringTokenizer stringTokenizer = new StringTokenizer(text, " ");
        if(!stringTokenizer.hasMoreElements())
            return null;
        text = stringTokenizer.nextToken();  // если есть пробелы - берем первое слово
        text = text.toLowerCase();

        if(text.equals("command.com") || text.equals("command")){
            return MsDos.class;
        }
        if(text.endsWith(".exe"))
            text = text.substring(0, text.length() - 4);

        switch (text){
            case "mspaint": case "paint":
                return PaintBrush.class;
            case "explorer":
                return DriveC.class;
            case "calculator": case "calc":
                return Calculator.class;
            case "solitaire": case "sol":
                return Solitaire.class;
            case "minesweeper": case "winmine":
                return Minesweeper.class;
            case "iexplore":
                return InternetExplorer.class;
            case "wordpad":
                return WordPad.class;
            case "notepad":
                return Notepad.class;
            case "freecell":
                return FreeCell.class;
            case "mplayer2":
                return MPlayer.class;
            case "control":
                return ControlPanel.class;
            case "cmd":
                return MsDos.class;
        }
        return null;
    }

    @Override
    public void onKeyPress(String key) {
        if(key.equals("\n")) {
            if(runAction())
                close();
        }
        super.onKeyPress(key);
        defaultButton.disabled = textBox.text.isEmpty(); // если текст пустой, то кнопка OK серая
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        if(active) {
            boolean containsActiveButton = false;  // костыль, чтобы кнопка ОК не теряла обводку при нажатии на TextBox
            for (Element element : elements) {
                if (element instanceof Button && ((Button) element).coolActive) {
                    containsActiveButton = true;
                    break;
                }
            }
            if (!containsActiveButton)
                defaultButton.coolActive = true;
        }
        super.onNewDraw(canvas, x, y);
    }
}
