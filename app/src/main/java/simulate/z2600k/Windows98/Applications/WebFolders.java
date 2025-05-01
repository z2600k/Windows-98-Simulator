package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;

import simulate.z2600k.Windows98.R;

public class WebFolders extends Explorer {
    public WebFolders(){
        super("Web 文件夹", "Web 文件夹", R.drawable.network_drive_world_1, R.drawable.network_drive_world_0, false);
        addLink(new Link("添加“Web 文件夹”",
                "“添加‘Web 文件夹’”向导可指导您逐步创建一个“Web 文件夹”，您只需按屏幕提示进行操作。",
                getBmp(R.drawable.template_directory_net_web_0), parent ->
                new Wizard("添加“Web 文件夹”", false, new Bitmap[]{getBmp(R.drawable.web_folder_wiz)})));
        helpText = defaultHelpText = "";
        linkContainer.updateLinkPositions();
    }
}
