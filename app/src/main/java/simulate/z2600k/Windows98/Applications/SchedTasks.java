package simulate.z2600k.Windows98.Applications;

import android.graphics.Bitmap;

import simulate.z2600k.Windows98.R;

public class SchedTasks extends Explorer {
    public SchedTasks(){
        super("计划任务", "计划任务", R.drawable.directory_sched_tasks_0, R.drawable.directory_sched_tasks_2, false);
        addLink(new Link("添加已计划的任务",
                "“计划任务向导”将帮助您逐步完成添加任务。只需按照每个屏幕上的指令进行操作。",
                getBmp(R.drawable.template_sched_task_3), parent -> new Wizard("计划任务向导", true,
                        new Bitmap[]{getBmp(R.drawable.sched_tasks_wiz1), getBmp(R.drawable.sched_tasks_wiz2), getBmp(R.drawable.sched_tasks_wiz3),
                                getBmp(R.drawable.sched_tasks_wiz4), getBmp(R.drawable.sched_tasks_wiz5)})));
        addLink(new Link("启用 Application Start",
                "计划:\n多次计划",
                getBmp(R.drawable.tune_up_app_start)));
        helpText = defaultHelpText = "该文件夹包含已计划的 Windows 任务。Windows 将按预定时间自动执行每项任务。\n\n例如，可以指定 Windows 在预定时间删除不必要的文件以便清理硬盘。";
        linkContainer.updateLinkPositions();
    }
}
