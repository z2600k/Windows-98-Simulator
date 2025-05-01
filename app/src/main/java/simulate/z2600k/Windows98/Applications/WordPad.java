package simulate.z2600k.Windows98.Applications;

import android.graphics.Rect;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.AboutWindow;
import simulate.z2600k.Windows98.System.ButtonInList;
import simulate.z2600k.Windows98.System.ButtonList;
import simulate.z2600k.Windows98.System.Element;
import simulate.z2600k.Windows98.System.HelpTopics;
import simulate.z2600k.Windows98.System.OnClickRunnable;
import simulate.z2600k.Windows98.System.Separator;
import simulate.z2600k.Windows98.System.TopMenu;
import simulate.z2600k.Windows98.System.TopMenuButton;
import simulate.z2600k.Windows98.System.Windows98;

public class WordPad extends BaseNotepad {
    public WordPad(){
        super("文档 - 写字板",
                R.drawable.write_wordpad_0,  R.drawable.wordpad1, Windows98.WIDESCREEN? R.drawable.wordpad2w : R.drawable.wordpad2);
        initTextAndScrollBar(new RelativeBounds(6, 131, -22, -24), new RelativeBounds(-22, 131, -6, -24),
                15, 15, p, new Rect(0, -12, 1, 3));
        appTitle = "写字板";
        // top menu
        ButtonList file = new ButtonList();
        ButtonInList new_ = new ButtonInList("新建...", "Ctrl+N");
        new_.action = parent -> {
            performActionWithSaveCheck(() -> {
                textBox.setText("");
                textChanged = false;
                openedFile = null;
                setTitle("文档 - 写字板");
                textBox.parent.inputFocus = textBox;
                textBox.setActive(true);
            });
        };
        file.elements.add(new_);
        ButtonInList open = new ButtonInList("打开...", "Ctrl+O");
        open.action = parent -> open();
        file.elements.add(open);
        OnClickRunnable saveRunnable = parent -> save();
        ButtonInList save = new ButtonInList("保存", "Ctrl+S");
        save.action = saveRunnable;
        file.elements.add(save);
        ButtonInList saveAs = new ButtonInList("另存为...");
        saveAs.action = saveRunnable;
        file.elements.add(saveAs);
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("打印...", "Ctrl+P"));
        file.elements.add(new ButtonInList("打印预览"));
        file.elements.add(new ButtonInList("页面设置..."));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("发送..."));
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
        edit.elements.add(new ButtonInList("选择性粘贴..."));
        edit.elements.add(new ButtonInList("清除", "Del"));
        edit.elements.add(new ButtonInList("全选", "Ctrl+A"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("查找...", "Ctrl+F"));
        edit.elements.add(new ButtonInList("查找下一个", "F3"));
        edit.elements.add(new ButtonInList("替换...", "Ctrl+H"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("链接..."));
        edit.elements.add(new ButtonInList("对象属性", "Alt+Enter"));
        edit.elements.add(new ButtonInList("对象"));
        ButtonList view = new ButtonList();
        view.elements.add(new ButtonInList("工具栏"));
        view.elements.add(new ButtonInList("格式栏"));
        view.elements.add(new ButtonInList("标尺"));
        view.elements.add(new ButtonInList("状态栏"));
        for(Element element : view.elements){
            ButtonInList button = (ButtonInList) element;
            button.check = button.checkActive = true;
        }
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("选项..."));
        ButtonList insert = new ButtonList();
        insert.elements.add(new ButtonInList("日期和时间..."));
        insert.elements.add(new ButtonInList("对象..."));
        ButtonList format = new ButtonList();
        format.elements.add(new ButtonInList("字体..."));
        format.elements.add(new ButtonInList("项目符号类型"));
        format.elements.add(new ButtonInList("段落..."));
        format.elements.add(new ButtonInList("制表符..."));
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("帮助主题");
        helpTopics.action = parent -> new HelpTopics("写字板帮助", true,
                new int[]{R.drawable.wordpad_help1, R.drawable.wordpad_help2, R.drawable.wordpad_help3});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("关于写字板");
        about.action = parent -> {
            ((ButtonInList) parent).closeMenu();
            new AboutWindow(WordPad.this, "写字板", getBmp(R.drawable.write_wordpad_1));
        };
        help.elements.add(about);

        TopMenu topMenu = new TopMenu();
        topMenu.elements.add(new TopMenuButton("文件", file));
        topMenu.elements.add(new TopMenuButton("编辑", edit));
        topMenu.elements.add(new TopMenuButton("查看", view));
        topMenu.elements.add(new TopMenuButton("插入", insert));
        topMenu.elements.add(new TopMenuButton("格式", format));
        topMenu.elements.add(new TopMenuButton("帮助", help));
        setTopMenu(topMenu);
        restore();
    }

    /* Задел на будущее
    private static class RichString {
        private static final int ALIGN_LEFT = -1, ALIGN_CENTER = 0, ALIGN_RIGHT = 1;

        private static class RichChar {
            char c;
            boolean bold, italic;

            public RichChar(char c, boolean bold, boolean italic) {
                this.c = c;
                this.bold = bold;
                this.italic = italic;
            }
        }

        private static class Part {
            String str;
            boolean bold, italic;
            int align;

            public Part(String str, boolean bold, boolean italic, int align) {
                this.str = str;
                this.bold = bold;
                this.italic = italic;
                this.align = align;
            }

            public RichChar charAt(int index){
                return new RichChar(str.charAt(index), bold, italic);
            }

            public int length(){
                return str.length();
            }
        }

        List<Part> parts = new ArrayList<>();

        public RichChar charAt(int index) {
            for(Part part : parts){
                if(part.length() < index)
                    return part.charAt(index);
                else
                    index -= part.length();
            }
            return null;
        }

        
    }
    */
}
