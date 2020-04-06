package com.net.train.upload;

import java.io.File;

public class MainTest {

	public static void main(String[] args) {
		upload("E:/test.png");
	}
	
	public static void upload(String filePath) {
		String fileName = "";
		File file = new File(filePath);
		if (file.exists()) {
			fileName = file.getName();
		}
		MSHttpManager.upload("http://39.108.149.156:8080/upload/file/upload", filePath, fileName);
	}
	
}
