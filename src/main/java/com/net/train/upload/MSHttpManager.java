package com.net.train.upload;

import java.io.File;

public class MSHttpManager {
	/**
	 * 文件上传
	 * @param requestUrl
	 * @param inputStream
	 * @return
	 */
	public static String upload(String requestUrl, String filepath, String fileName){
		File file = new File(filepath);
		return MSHttpUploader.uploadFile(requestUrl, file);
	}

}