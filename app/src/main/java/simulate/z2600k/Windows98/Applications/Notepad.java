package simulate.z2600k.Windows98.Applications;

import android.graphics.Rect;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.AboutWindow;
import simulate.z2600k.Windows98.System.ButtonInList;
import simulate.z2600k.Windows98.System.ButtonList;
import simulate.z2600k.Windows98.System.Element;
import simulate.z2600k.Windows98.System.HelpTopics;
import simulate.z2600k.Windows98.System.Separator;
import simulate.z2600k.Windows98.System.TopMenu;
import simulate.z2600k.Windows98.System.TopMenuButton;
import simulate.z2600k.Windows98.System.Windows98;

import java.io.File;

public class Notepad extends BaseNotepad {
    public Notepad(){
        this("无标题");
    }
    public Notepad(String openFilename){
        this(openFilename, null);
    }
    public Notepad(File path){
        this(path.getName(), path);
    }
    public Notepad(final String openFilename, File path){
        super(openFilename + " - 记事本",
                R.drawable.notepad_0, R.drawable.notepad1, Windows98.WIDESCREEN? R.drawable.notepad2w : R.drawable.notepad2);
        initTextAndScrollBar(new RelativeBounds(6, 44, -24, -24), new RelativeBounds(-24, 44, -6, -24),
                1, 13, p_fixedsys, new Rect(0, -12, 2, 3));
        appTitle = "记事本";
        if(path != null){
            getTxtWriter().openFile(path);
        }
        // top menu
        ButtonList file = new ButtonList();
        ButtonInList new_ = new ButtonInList("新建");
        new_.action = parent -> performActionWithSaveCheck(
                () -> {
                    textBox.setText("");
                    textChanged = false;
                    openedFile = null;
                    setTitle("无标题 - 记事本");
                    textBox.parent.inputFocus = textBox;
                    textBox.setActive(true);
                });
        file.elements.add(new_);
        ButtonInList open = new ButtonInList("打开...");
        open.action = parent -> open();
        file.elements.add(open);
        ButtonInList save = new ButtonInList("保存");
        save.action = parent -> save();
        file.elements.add(save);
        ButtonInList saveAs = new ButtonInList("另存为...");
        saveAs.action = parent -> saveAs();
        file.elements.add(saveAs);
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("页面设置..."));
        ButtonInList print = new ButtonInList("打印");
        print.disabled = true;
        file.elements.add(print);
        file.elements.add(new Separator());
        ButtonInList exit = new ButtonInList("退出");
        exit.action = parent -> close();
        file.elements.add(exit);

        ButtonList edit = new ButtonList();
        edit.elements.add(new ButtonInList("撤销", "Ctrl+Z"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("剪切", "Ctrl+X"));
        edit.elements.add(new ButtonInList("复制", "Ctrl+C"));
        edit.elements.add(new ButtonInList("粘贴", "Ctrl+V"));
        edit.elements.add(new ButtonInList("删除", "Del"));
        edit.elements.add(new Separator());
        for(Element el : edit.elements)
            ((ButtonInList) el).disabled = true;
        edit.elements.add(new ButtonInList("全选"));
        edit.elements.add(new ButtonInList("时间/日期", "F5"));
        edit.elements.add(new Separator());
        ButtonInList wordWrap = new ButtonInList("自动换行");
        wordWrap.check = wordWrap.checkActive = true;
        edit.elements.add(wordWrap);
        edit.elements.add(new ButtonInList("设置字体..."));

        ButtonList search = new ButtonList();
        search.elements.add(new ButtonInList("查找"));
        search.elements.add(new ButtonInList("查找下一个", "F3"));

        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("帮助主题");
        helpTopics.action = parent -> new HelpTopics("记事本帮助", true,
                new int[]{R.drawable.notepad_help1, R.drawable.notepad_help2, R.drawable.notepad_help3});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("关于记事本");
        about.action = parent -> new AboutWindow(Notepad.this, "记事本", getBmp(R.drawable.notepad_1));
        help.elements.add(about);

        TopMenu topMenu = new TopMenu();
        topMenu.elements.add(new TopMenuButton("文件", file));
        topMenu.elements.add(new TopMenuButton("编辑", edit));
        topMenu.elements.add(new TopMenuButton("搜索", search));
        topMenu.elements.add(new TopMenuButton("帮助", help));
        setTopMenu(topMenu);
        restore();
        makeActive();
    }
}
