
package com.awell.storage;

import android.content.Context;

import java.util.ArrayList;

public class EnvironmentAwell {

    public EnvironmentAwell(Context ctx) {

    }

    /**
     *  get list of paths of all storage
     *
     * @return return storage paths stored in a string array. eg: "mnt/udisk1", "mnt/ext_sdcard1" ...
     *
     **/
    public String[] getStorageAllPaths() {
        return null;
    }

    /**
     *  get paths of all SD storage
     *
     * @return return sd storage paths stored in a string array. eg : "mnt/ext_sdcard1" ...
     *
     * @deprecated This method was deprecated!
     **/
     @Deprecated
    public String[] getSdAllPaths() {
        return null;
    }

    /**
     *  get paths of all USB storage
     *
     * @return return usb storage paths stored in a string array. eg : "mnt/udisk1" ...
     *
     * @deprecated This method was deprecated!
     **/
     @Deprecated
    public String[] getUsbAllPaths() {
        return null;
    }

    /**
     *  get paths of all mounted storage
     *
     * @return return mounted storage paths stored in a string array. eg: "mnt/udisk1", "mnt/ext_sdcard1" ...
     *
     * @deprecated This method was deprecated!
     **/
     @Deprecated
    public String[] getStorageMountedPaths() {
         return null;
    }

    /**
     *  get paths of all mounted SD storage
     *
     * @return return mounted sd storage paths stored in a string array. eg: "mnt/ext_sdcard1" ...
     *
     * @deprecated This method was deprecated!
     **/
     @Deprecated
    public String[] getSdMountedPaths() {
         return null;
    }

    /**
     *  get paths of all mounted USB storage
     *
     * @return return mounted usb storage paths stored in a string array. eg : "mnt/udisk1" ...
     *
     * @deprecated This method was deprecated!
     **/
     @Deprecated
    public String[] getUsbMountedPaths() {
         return null;
    }

    /**
     *  get paths of all mounted storage
     *
     * @param arrayList    store paths of all mounted storage
     *
     * @deprecated This method was deprecated! 
     **/
     @Deprecated     
    public void getStorageMountedPaths(ArrayList<String> arrayList) {

    }

    /**
     *  get mounted/umounted state of storage device
     *
     * @param mountPoint eg. /storage/ext_sdcard1 
     *
     * @return return string state. eg: "mounted" ...
     *
     * @deprecated This method was deprecated!
     *         
     **/
     @Deprecated     
    public String getStorageState(String mountPoint) {
        return null;
    }

    /**
     *  umount a storage device
     *
     * @param mountPoint eg. /storage/ext_sdcard1
     *
     * @return return true if it is successful; otherwise return false.
     * 
     * @deprecated This method was deprecated!
     **/
    @Deprecated
    public boolean unmount(String mountPoint) {
        return true;
    }

    /**
     *  mount a storage device
     *
     * @param mountPoint eg. /storage/ext_sdcard1
     *
     * @return return true if it is successful; otherwise return false.
     *
     * @deprecated This method was deprecated!          
     **/
    @Deprecated
    public boolean mount(String mountPoint) {
        return true;
    }

    /**
     * report Storage state from usb_state file
     * 
     * @param mountPoint eg. /storage/ext_sdcard1
     *
     * @return storage state 'host' or 'device'
     *
     * @deprecated This method was deprecated!
     * */
     @Deprecated
    public String ReportStorageState(String mountPoint) {
        return "0";
    }

    /**
     * report primary storage size of platform
     *
     * @return the size of primary storage
     *
     * @deprecated This method was deprecated!
     * */
     @Deprecated
    public static long getPrimaryVolumeSize() {
        return 0;
    }

    /**
     *  check whether the disk is supported for setting adoptable storage
     * Parameter:
     *  fsUuid,     the fsUuid of partition(volume) to check
     *
     * return value:
     *  true,        the  partition(volume) is supported for setting adoptable storage
     *  false,   the  partition(volume) is not supported for setting adoptable storage
     */
    public boolean isAdoptableDisk(String fsUuid) {
        return false;
    }
}
