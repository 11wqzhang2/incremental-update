package com.will.bisdiff;

public class BsPatchMain {

	public static void main(String[] args) {
		// 异步执行，得到差分包
		BsDiff.diff(ConstantsWin.OLD_APK_PATH, ConstantsWin.NEW_APK_PATH, ConstantsWin.PATCH_PATH);
	}

}
