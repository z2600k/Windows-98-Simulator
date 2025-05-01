package simulate.z2600k.Windows98;

import android.os.Build;

import java.util.Objects;

public class AndroidIcon {
    public int androidLogo,androidLogoSmall;
    public int GetAndroidLogo(String Size){
        int sdkVersion= Build.VERSION.SDK_INT;
        switch(sdkVersion){
            case 16,17,18:
                androidLogo=R.drawable.android_sdk16;
                androidLogoSmall=R.drawable.android_sdk16_small;
                break;
            case 19,20:
                androidLogo=R.drawable.android_sdk19;
                androidLogoSmall=R.drawable.android_sdk19_small;
                break;
            case 21,22:
                androidLogo=R.drawable.android_sdk21;
                androidLogoSmall=R.drawable.android_sdk21_small;
                break;
            case 23:
                androidLogo=R.drawable.android_sdk23;
                androidLogoSmall=R.drawable.android_sdk23_small;
                break;
            case 24,25:
                androidLogo=R.drawable.android_sdk24;
                androidLogoSmall=R.drawable.android_sdk24_small;
                break;
            case 26:
                androidLogo=R.drawable.android_sdk26;
                androidLogoSmall=R.drawable.android_sdk26_small;
                break;
            case 27:
                androidLogo=R.drawable.android_sdk27;
                androidLogoSmall=R.drawable.android_sdk27_small;
                break;
            case 28:
                androidLogo=R.drawable.android_sdk28;
                androidLogoSmall=R.drawable.android_sdk28_small;
                break;
            case 29:
                androidLogo=R.drawable.android_sdk29;
                androidLogoSmall=R.drawable.android_sdk29_small;
                break;
            case 30:
                androidLogo=R.drawable.android_sdk30;
                androidLogoSmall=R.drawable.android_sdk30_small;
                break;
            case 31,32:
                androidLogo=R.drawable.android_sdk31;
                androidLogoSmall=R.drawable.android_sdk31_small;
                break;
            case 33:
                androidLogo=R.drawable.android_sdk33;
                androidLogoSmall=R.drawable.android_sdk33_small;
                break;
            case 34:
                androidLogo=R.drawable.android_sdk34;
                androidLogoSmall=R.drawable.android_sdk34_small;
                break;
            case 35:
                androidLogo=R.drawable.android_sdk35;
                androidLogoSmall=R.drawable.android_sdk35_small;
                break;
            case 36:
                androidLogo=R.drawable.android_sdk36;
                androidLogoSmall=R.drawable.android_sdk36_small;
                break;
            default:
                androidLogo=R.drawable.android;
                androidLogoSmall=R.drawable.android_small;
                break;
        }
        return (Objects.equals(Size, "Normal") ?androidLogo:
                (Objects.equals(Size, "Small") ?androidLogoSmall:R.drawable.folder));
    }
}
