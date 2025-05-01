package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;

import simulate.z2600k.Windows98.R;

public class Printers extends Explorer {
    public Printers(){
        super("打印机", "打印机", R.drawable.directory_printer_4, R.drawable.directory_printer_5, false);
        helpText = defaultHelpText = "该文件夹包含当前打印机的有关信息，也包含可以帮助您安装新打印机的向导。";
        addLink(new Link("添加打印机",
                "“添加打印机向导”将帮助您逐步完成打印机的安装。只需按照每个屏幕上的指令进行操作。",
                getBmp(R.drawable.template_printer_0), parent -> {
                    onOtherTouch();
                    new Wizard("添加打印机向导", true, new Bitmap[]{getBmp(R.drawable.add_printer_1), getBmp(R.drawable.add_printer_2),
                            getBmp(R.drawable.add_printer_3), getBmp(R.drawable.add_printer_4), getBmp(R.drawable.add_printer_5), getBmp(R.drawable.add_printer_6)});
                }));
        linkContainer.updateLinkPositions();
    }
}
