package com.will.increateupdate.util;

/**
 * @author: MyPC
 * @date: 2018/12/26
 * @description:
 */
public class BsPatch {

    public native static void patch(String oldPath, String newPath, String patchPath);

    static {
        System.loadLibrary("bspatch");
    }
}
