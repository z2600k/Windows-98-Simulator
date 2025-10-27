package simulate.z2600k.Windows98.System;

import android.os.Build;

import java.util.Objects;

import simulate.z2600k.Windows98.R;

public class AndroidIcon {
    public int androidLogo,androidLogoSmall;
    public int GetAndroidLogo(String Size){
        int sdkVersion= Build.VERSION.SDK_INT_FULL;
        switch(sdkVersion){
            case 1600000,1700000,1800000:
                androidLogo= R.drawable.android_sdk16;
                androidLogoSmall=R.drawable.android_sdk16_small;
                break;
            case 1900000,2000000:
                androidLogo=R.drawable.android_sdk19;
                androidLogoSmall=R.drawable.android_sdk19_small;
                break;
            case 2100000,2200000:
                androidLogo=R.drawable.android_sdk21;
                androidLogoSmall=R.drawable.android_sdk21_small;
                break;
            case 2300000:
                androidLogo=R.drawable.android_sdk23;
                androidLogoSmall=R.drawable.android_sdk23_small;
                break;
            case 2400000,2500000:
                androidLogo=R.drawable.android_sdk24;
                androidLogoSmall=R.drawable.android_sdk24_small;
                break;
            case 2600000:
                androidLogo=R.drawable.android_sdk26;
                androidLogoSmall=R.drawable.android_sdk26_small;
                break;
            case 2700000:
                androidLogo=R.drawable.android_sdk27;
                androidLogoSmall=R.drawable.android_sdk27_small;
                break;
            case 2800000:
                androidLogo=R.drawable.android_sdk28;
                androidLogoSmall=R.drawable.android_sdk28_small;
                break;
            case 2900000:
                androidLogo=R.drawable.android_sdk29;
                androidLogoSmall=R.drawable.android_sdk29_small;
                break;
            case 3000000:
                androidLogo=R.drawable.android_sdk30;
                androidLogoSmall=R.drawable.android_sdk30_small;
                break;
            case 3100000,3200000:
                androidLogo=R.drawable.android_sdk31;
                androidLogoSmall=R.drawable.android_sdk31_small;
                break;
            case 3300000:
                androidLogo=R.drawable.android_sdk33;
                androidLogoSmall=R.drawable.android_sdk33_small;
                break;
            case 3400000:
                androidLogo=R.drawable.android_sdk34;
                androidLogoSmall=R.drawable.android_sdk34_small;
                break;
            case 3500000:
                androidLogo=R.drawable.android_sdk35;
                androidLogoSmall=R.drawable.android_sdk35_small;
                break;
            case 3600000,3600001:
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
