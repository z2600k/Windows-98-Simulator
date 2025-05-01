package simulate.z2600k.Windows98.Applications;

import simulate.z2600k.Windows98.R;

public class RecycleBin extends Explorer {
    public RecycleBin(){
        super("回收站", "回收站", R.drawable.recycle_bin_empty_1, R.drawable.recycle_bin_empty_0, true);
        helpText = defaultHelpText = "该文件夹包含已从计算机删除的文件和文件夹";
        linkContainer.updateLinkPositions();
    }
}
