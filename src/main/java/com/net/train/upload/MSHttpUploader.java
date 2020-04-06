package com.net.train.upload;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.UUID;

public class MSHttpUploader {

    private static final int TIME_OUT = 10 * 10000000; // 超时时间  
    private static final String CHARSET = "utf-8"; // 设置编码  
  
    protected static String uploadFile(String uploadUrl, File file) {  
        String BOUNDARY = UUID.randomUUID().toString(); // 边界标识 随机生成  
        String PREFIX = "--", LINE_END = "\r\n";  
        String CONTENT_TYPE = "multipart/form-data"; // 内容类型  
        try {  
        	URL url = new URL(uploadUrl);  
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
            conn.setReadTimeout(TIME_OUT);  
            conn.setConnectTimeout(TIME_OUT);  
            conn.setDoInput(true); // 允许输入流  
            conn.setDoOutput(true); // 允许输出流  
            conn.setUseCaches(false); // 不允许使用缓存  
            conn.setRequestMethod("POST"); // 请求方式  
            conn.setRequestProperty("Charset", CHARSET); // 设置编码  
            conn.setRequestProperty("connection", "keep-alive");  
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary="  
                    + BOUNDARY); 
            conn.setRequestProperty("User-Agent", "patient-client/Android");
            
            if (file != null) {  
                /** 
                 * 当文件不为空，把文件包装并且上传 
                 */  
                OutputStream outputSteam = conn.getOutputStream();  
  
                DataOutputStream dos = new DataOutputStream(outputSteam);  
                StringBuffer sb = new StringBuffer();  
                sb.append(PREFIX);  
                sb.append(BOUNDARY);  
                sb.append(LINE_END);  
                /** 
                 * 这里重点注意： name里面的值为服务器端需要key 只有这个key 才可以得到对应的文件 
                 * filename是文件的名字，包含后缀名的 比如:abc.png 
                 */  
  
                sb.append("Content-Disposition: form-data; name=\"img\"; filename=\""  
                        + file.getName() + "\"" + LINE_END);  
                sb.append("Content-Type: application/octet-stream; charset="  
                        + CHARSET + LINE_END);  
                sb.append(LINE_END);  
                dos.write(sb.toString().getBytes());  
                InputStream is = new FileInputStream(file);  
                byte[] bytes = new byte[1024];  
                int len = 0;  
                while ((len = is.read(bytes)) != -1) {  
                    dos.write(bytes, 0, len);  
                }  
                is.close();  
                dos.write(LINE_END.getBytes());  
                byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END)  
                        .getBytes();  
                dos.write(end_data);  
                dos.flush();  
                /** 
                 * 获取响应码 200=成功 当响应成功，获取响应的流 
                 */  
                int res = conn.getResponseCode();  
                // 开始读取从服务端传过来的信息
                InputStream iss = conn.getInputStream();
                InputStreamReader isr = new InputStreamReader(iss, "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String result = "";
                String line;
                while ((line = br.readLine()) != null) {
                	result = result + line;
                }
                iss.close();
                
                if (res == 200) {  
                	result = URLDecoder.decode(result, "UTF-8");
                	return result;
                } 
                 
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
        
        return "网络错误，上传失败";
    }  
	
}