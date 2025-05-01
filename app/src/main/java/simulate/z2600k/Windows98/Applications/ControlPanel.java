package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;
import android.graphics.Rect;

import simulate.z2600k.Windows98.R;
import simulate.z2600k.Windows98.System.Element;
import simulate.z2600k.Windows98.System.OnClickRunnable;
import simulate.z2600k.Windows98.System.Window;

public class ControlPanel extends Explorer {
    public ControlPanel(){
        super("控制面板", "控制面板", R.drawable.directory_control_panel_5, R.drawable.directory_control_panel_4, false);
        helpText = defaultHelpText = "使用“控制面板”中的设置来个性化计算机。 选定项目可以查看其说明。";
        addLink(new Link("Internet",
                "更改 Internet 设置。",
                getBmp(R.drawable.internet_options_4), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Internet 属性", new Bitmap[]{getBmp(R.drawable.internet_props1), getBmp(R.drawable.internet_props2),
                        getBmp(R.drawable.internet_props3), getBmp(R.drawable.internet_props4), getBmp(R.drawable.internet_props5), getBmp(R.drawable.internet_props6)},
                        new int[]{11, 59, 107, 155, 203, 251, 299},
                        new Rect(188, 415, 263, 436), new Rect(269, 415, 344, 436));
            }
        }));
        addLink(new Link("ODBC 数据源 (32位)",
                "维护 32 位 ODBC 数据源和驱动程序",
                getBmp(R.drawable.odbccp32_1439), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("ODBC 数据源管理器", new Bitmap[]{getBmp(R.drawable.odbc1), getBmp(R.drawable.odbc2), getBmp(R.drawable.odbc3),
                        getBmp(R.drawable.odbc4), getBmp(R.drawable.odbc5), getBmp(R.drawable.odbc6), getBmp(R.drawable.odbc7)},
                        new int[]{11, 71, 131, 191, 257, 304, 353, 401},
                        new Rect(134, 337, 209, 358), new Rect(215, 337, 290, 358));
            }
        }));
        addLink(new Link("打印机",
                "添加、删除及更改打印机的设置。",
                getBmp(R.drawable.directory_printer_shortcut_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                Window window = new Printers();
                window.alignWith(ControlPanel.this);
            }
        }));
        addLink(new Link("电话",
                "配置电话驱动程序与拨号属性",
                getBmp(R.drawable.telephony_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("拨号属性", new Bitmap[]{getBmp(R.drawable.dialing_props_1), getBmp(R.drawable.dialing_props_2)},
                        new int[]{11, 71, 179},
                        new Rect(166, 433, 241, 454), new Rect(247, 433, 322, 454));
            }
        }));
        addLink(new Link("电源管理",
                "更改电源管理设置。",
                getBmp(R.drawable.power_management_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("电源管理 属性", new Bitmap[]{getBmp(R.drawable.power_mng1), getBmp(R.drawable.power_mng2)},
                        new int[]{11, 95, 143},
                        new Rect(158, 386, 233, 407), new Rect(239, 386, 314, 407));
            }
        }));
        addLink(new Link("调制解调器",
                "安装新调制解调器并更改调制解调器的属性。",
                getBmp(R.drawable.modem_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                StartMenu.createDialupNetworking();
            }
        }));
        addLink(new Link("多媒体",
                "更改多媒体设备的设置。",
                getBmp(R.drawable.multimedia_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("多媒体 属性", new Bitmap[]{getBmp(R.drawable.multimedia_props1), getBmp(R.drawable.multimedia_props2), getBmp(R.drawable.multimedia_props3),
                        getBmp(R.drawable.multimedia_props4), getBmp(R.drawable.multimedia_props5)},
                        new int[]{11, 74, 137, 196, 266, 337},
                        new Rect(131, 403, 206, 424), new Rect(212, 403, 287, 424));
            }
        }));
        addLink(new Link("辅助选项",
                "更改系统的辅助选项。",
                getBmp(R.drawable.accessibility_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("辅助选项 属性", new Bitmap[]{getBmp(R.drawable.accessibility_props1), getBmp(R.drawable.accessibility_props2), getBmp(R.drawable.accessibility_props3),
                        getBmp(R.drawable.accessibility_props4), getBmp(R.drawable.accessibility_props5)}, new int[]{11, 59, 107, 155, 203, 251},
                        new Rect(124, 364, 199, 385), new Rect(205, 364, 280, 385));
            }
        }));
        addLink(new Link("键盘",
                "更改键盘设置。",
                getBmp(R.drawable.keyboard_1), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("键盘 属性", new Bitmap[]{getBmp(R.drawable.kbd_props1), getBmp(R.drawable.kbd_props2)},
                        new int[]{11, 59, 107},
                        new Rect(158, 386, 233, 407), new Rect(239, 386, 314, 407));
            }
        }));
        addLink(new Link("密码",
                "更改密码，设置安全性选项。",
                getBmp(R.drawable.keys_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("密码 属性", new Bitmap[]{getBmp(R.drawable.passwords1), getBmp(R.drawable.passwords2)},
                        new int[]{11, 71, 155},
                        new Rect(314, 301, 389, 322), new Rect(395, 301, 470, 322));
            }
        }));
        addLink(new Link("区域设置",
                "修改数字、货币、日期与时间的显示方式。",
                getBmp(R.drawable.world_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("区域设置 属性", new Bitmap[]{getBmp(R.drawable.regional1), getBmp(R.drawable.regional2),
                        getBmp(R.drawable.regional3), getBmp(R.drawable.regional4), getBmp(R.drawable.regional5)},
                        new int[]{11, 71, 119, 167, 215, 263},
                        new Rect(169, 386, 244, 407), new Rect(250, 386, 325, 407));
            }
        }));
        addLink(new Link("日期/时间",
                "更改时间、日期及时区信息。",
                getBmp(R.drawable.time_and_date_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("日期/时间 属性", new Bitmap[]{getBmp(R.drawable.date_time_props)},
                        new int[]{1, 1},
                        new Rect(226, 346, 301, 367), new Rect(307, 346, 382, 367));
            }
        }));
        addLink(new Link("声音",
                "更改系统和程序声音。",
                getBmp(R.drawable.computer_sound_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("声音 属性", new Bitmap[]{getBmp(R.drawable.sounds_props)},
                        new int[]{1, 1},
                        new Rect(121, 386, 196, 407), new Rect(202, 386, 277, 407));
            }
        }));
        addLink(new Link("鼠标",
                "更改鼠标设置。",
                getBmp(R.drawable.mouse_ms_1), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("鼠标 属性", new Bitmap[]{getBmp(R.drawable.mouse_props1), getBmp(R.drawable.mouse_props2), getBmp(R.drawable.mouse_props3)},
                        new int[]{11, 59, 107, 155},
                        new Rect(158, 386, 233, 407), new Rect(239, 386, 314, 407));
            }
        }));
        addLink(new Link("添加/删除程序",
                "安装程序并创建快捷方式。",
                getBmp(R.drawable.appwizard), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("添加/删除程序 属性", new Bitmap[]{getBmp(R.drawable.add_remove_progs1), getBmp(R.drawable.add_remove_progs2), getBmp(R.drawable.add_remove_progs3)},
                        new int[]{11, 77, 185, 233},
                        new Rect(121, 397, 202, 418), new Rect(208, 397, 283, 418));
            }
        }));
        addLink(new Link("添加新硬件",
                "将新硬件添加到系统上。",
                getBmp(R.drawable.hardware_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new Wizard("添加新硬件向导", true, new Bitmap[]{getBmp(R.drawable.hardw_wiz1), getBmp(R.drawable.hardw_wiz2), getBmp(R.drawable.hardw_wiz3), getBmp(R.drawable.hardw_wiz4)});
            }
        }));

        addLink(new Link("网络",
                "配置网络硬件与软件。",
                getBmp(R.drawable.network), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("网络", new Bitmap[]{getBmp(R.drawable.network1), getBmp(R.drawable.network2), getBmp(R.drawable.network3)},
                        new int[]{11, 59, 107, 167},
                        new Rect(205, 385, 280, 406), new Rect(286, 385, 361, 406));
            }
        }));
        addLink(new Link("系统",
                "提供系统信息并更改高级设置。",
                getBmp(R.drawable.computer_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                StartMenu.createSystemProperties();
            }
        }));
        addLink(new Link("显示",
                "更改显示设置。",
                getBmp(R.drawable.display_properties_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new DisplayProperties();
            }
        }));
        addLink(new Link("用户",
                "设置并管理计算机上的多个用户。",
                getBmp(R.drawable.users_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new Wizard("允许多用户设置", true,
                        new Bitmap[]{getBmp(R.drawable.multiuser_1), getBmp(R.drawable.multiuser_2), getBmp(R.drawable.multiuser_3), getBmp(R.drawable.multiuser_4),getBmp(R.drawable.multiuser_5)});
            }
        }));
        addLink(new Link("游戏控制器",
                "添加、删除或更改游戏控制器的设置。",
                getBmp(R.drawable.joystick_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("游戏控制器", new Bitmap[]{getBmp(R.drawable.game_controllers1), getBmp(R.drawable.game_controllers2)},
                        new int[]{11, 59, 125},
                        new Rect(320, 382, 395, 403), null);
            }
        }));
        addLink(new Link("字体",
                "查看、添加及删除计算机中的字体。",
                getBmp(R.drawable.directory_fonts_shortcut_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new Explorer(Explorer.FONTS_INDEX);  // C:\Windows\Fonts
            }
        }));
        linkContainer.updateLinkPositions();
    }
}
