package com.will.bisdiff;

public class BsDiff {
	
	/**
	 * 
	 * @param oldFile
	 * @param newFile
	 * @param patchFile
	 */
	public native static void diff(String oldFile, String newFile, String patchFile);
	

	static {
		System.loadLibrary("bsdiff");
	}
}
