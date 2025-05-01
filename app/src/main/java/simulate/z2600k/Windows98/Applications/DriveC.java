package simulate.z2600k.Windows98.Applications;

import simulate.z2600k.Windows98.R;

public class DriveC extends Explorer {
    public DriveC(boolean isMsDos){
        super(0, R.drawable.hard_disk_drive_1, R.drawable.hard_disk_drive_0, isMsDos);

        if(isMsDos)
            return;

        Link myDocs = new Link("My Documents", "我的文档", getBmp(R.drawable.directory_open_file_mydocs_0), parent -> {
            MyDocuments myDocuments = new MyDocuments();
            myDocuments.alignWith(DriveC.this);
        });
        linkContainer.elements.add(0, myDocs);
        linkContainer.updateLinkPositions();
    }

    public DriveC(){
        this(false);
    }
}
