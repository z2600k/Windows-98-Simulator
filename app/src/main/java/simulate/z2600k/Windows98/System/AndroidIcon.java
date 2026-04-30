package simulate.z2600k.Windows98.System;

import android.os.Build;

import java.util.Objects;

import simulate.z2600k.Windows98.R;

public class AndroidIcon {
    public int androidLogo,androidLogoSmall;
    public int GetAndroidLogo(String Size){
        switch(Build.VERSION.SDK_INT_FULL){
            case 1600000,1700000,1800000 -> {
                androidLogo= R.drawable.android_sdk16;
                androidLogoSmall=R.drawable.android_sdk16_small;
            }
            case 1900000,2000000 -> {
                androidLogo=R.drawable.android_sdk19;
                androidLogoSmall=R.drawable.android_sdk19_small;
            }
            case 2100000,2200000 -> {
                androidLogo=R.drawable.android_sdk21;
                androidLogoSmall=R.drawable.android_sdk21_small;
            }
            case 2300000 -> {
                androidLogo=R.drawable.android_sdk23;
                androidLogoSmall=R.drawable.android_sdk23_small;
            }
            case 2400000,2500000 -> {
                androidLogo=R.drawable.android_sdk24;
                androidLogoSmall=R.drawable.android_sdk24_small;
            }
            case 2600000 -> {
                androidLogo=R.drawable.android_sdk26;
                androidLogoSmall=R.drawable.android_sdk26_small;
            }
            case 2700000 -> {
                androidLogo=R.drawable.android_sdk27;
                androidLogoSmall=R.drawable.android_sdk27_small;
            }
            case 2800000 -> {
                androidLogo=R.drawable.android_sdk28;
                androidLogoSmall=R.drawable.android_sdk28_small;
            }
            case 2900000 -> {
                androidLogo=R.drawable.android_sdk29;
                androidLogoSmall=R.drawable.android_sdk29_small;
            }
            case 3000000 -> {
                androidLogo=R.drawable.android_sdk30;
                androidLogoSmall=R.drawable.android_sdk30_small;
            }
            case 3100000,3200000 -> {
                androidLogo=R.drawable.android_sdk31;
                androidLogoSmall=R.drawable.android_sdk31_small;
            }
            case 3300000 -> {
                androidLogo=R.drawable.android_sdk33;
                androidLogoSmall=R.drawable.android_sdk33_small;
            }
            case 3400000 -> {
                androidLogo=R.drawable.android_sdk34;
                androidLogoSmall=R.drawable.android_sdk34_small;
            }
            case 3500000 -> {
                androidLogo=R.drawable.android_sdk35;
                androidLogoSmall=R.drawable.android_sdk35_small;
            }
            case 3600000,3600001 -> {
                androidLogo=R.drawable.android_sdk36;
                androidLogoSmall=R.drawable.android_sdk36_small;
            }
            case 3700000,3700001 -> {
                androidLogo=R.drawable.android_sdk37;
                androidLogoSmall=R.drawable.android_sdk37_small;
            }
            default -> {
                androidLogo=R.drawable.android;
                androidLogoSmall=R.drawable.android_small;
            }
        }
        return (Objects.equals(Size, "Normal") ?androidLogo:
                (Objects.equals(Size, "Small") ?androidLogoSmall:R.drawable.folder));
    }
}
