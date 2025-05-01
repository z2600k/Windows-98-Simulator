package simulate.z2600k.Windows98.Applications;

import simulate.z2600k.Windows98.MainActivity;
import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.Element;
import simulate.z2600k.Windows98.System.MessageBox;
import simulate.z2600k.Windows98.System.OnClickRunnable;
import simulate.z2600k.Windows98.WindowsView;

public class MyComputer extends Explorer {
    public MyComputer(){
        super("我的电脑", "我的电脑", R.drawable.computer_explorer_0, R.drawable.computer_explorer_2, false);
        helpText = defaultHelpText = "选定项目可以查看其说明。";
        addLink(new Link("3.5 英寸软盘 (A:)", "3.5 寸软盘", getBmp(R.drawable.floppy_drive_3_5), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new MessageBox("我的电脑", "驱动器 A 中的磁盘未被格式化。\n\n想现在格式化吗?",
                        MessageBox.YESNO, MessageBox.WARNING, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if(buttonNumber == YES){  // если пользователь хочет отформатировать дискету в (A:), говорим, что это невозможно
                            WindowsView.handler.postDelayed(() -> {
                                if(MyComputer.this.closed)
                                    return;
                                new MessageBox("我的电脑", "无法格式化驱动器 A 中的磁盘。",
                                        MessageBox.OK, MessageBox.WARNING, null, MyComputer.this);
                                /*new MessageBox("格式化 - 3.5 英寸软盘 (A:)", "Windows 无法格式化磁盘。可能是选择的容量无效，或者不适\n用于这个磁盘，或者磁盘损坏必须更换。",
                                                                        MessageBox.OK, MessageBox.INFO, null, MyComputer.this);*/
                                WindowsView.windowsView.invalidate();
                            }, 500);
                        }
                    }
                }, MyComputer.this);
            }
        }));
        addLink(new Link("(C:)", "本地磁盘", getBmp(R.drawable.hard_disk_drive_0), DriveC.class, this));
        addLink(new Link("Android (D:)", "光盘", getBmp(R.drawable.cd_drive_5), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                context.checkWriteExternalPermission(new MainActivity.PermissionResultListener() {
                    @Override
                    public void onPermissionGranted() {
                        MyDocuments driveD = MyDocuments.createDriveD();
                        driveD.alignWith(MyComputer.this);
                    }

                    @Override
                    public void onPermissionDenied() {
                        MyDocuments.diskNotAccessible(MyComputer.this);
                    }
                });
            }
        }));
        addLink(new Link("打印机", "系统文件夹", getBmp(R.drawable.directory_printer_5), Printers.class, this));
        addLink(new Link("控制面板", "系统文件夹", getBmp(R.drawable.directory_control_panel_4), ControlPanel.class, this));
        addLink(new Link("Web 文件夹", "Web Folders", getBmp(R.drawable.network_drive_world_0), WebFolders.class, this));
        addLink(new Link("拨号网络", "系统文件夹", getBmp(R.drawable.directory_dial_up_networking_0), parent -> StartMenu.createDialupNetworking()));
        addLink(new Link("计划任务", "系统文件夹", getBmp(R.drawable.directory_sched_tasks_2), SchedTasks.class, this));
        linkContainer.updateLinkPositions();
    }
}
