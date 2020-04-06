package com.net.train.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Stack;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 文件处理工具类
 */
public class FileUtil {
	/**
	 * @description: 建立路径中缺失的目录
	 * @param: file
	 *             一个不存在的文件
	 * @return:
	 * @throws:
	 */
	public static void createMissDir(File file) {
		if (file.exists())
			return;
		Stack<File> stack = new Stack<File>();
		File curretn = file.getParentFile();
		// 把缺失的目录放到栈里
		while (curretn != null && !curretn.exists()) {
			stack.push(curretn);
			curretn = curretn.getParentFile();
		}
		// 建立缺失的目录
		while (!stack.isEmpty()) {
			stack.pop().mkdir();
		}
	}

	/**
	 * @description: 拷贝文件
	 * @param: src
	 *             源文件
	 * @param: dst
	 *             目标文件，文件夹不存在也无所谓
	 * @return:
	 * @throws:
	 */
	public static void copy(File src, File dst) {
		// 建立缺失目录
		createMissDir(dst);
		// 交换数组大小
		int bufferSize = 512;
		int len;
		try {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = new BufferedInputStream(new FileInputStream(src), bufferSize);
				out = new BufferedOutputStream(new FileOutputStream(dst), bufferSize);
				byte[] buffer = new byte[bufferSize];
				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}
			} finally {
				if (null != in) {
					in.close();
				}
				if (null != out) {
					out.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @description: 获取某jsp执行结果的流
	 * @param jspFile
	 *            jsp文件
	 * @param outputStream
	 *            静态文件流
	 * @param varMap
	 *            要注入jsp的变量，通过request.setAttribute()注入
	 * @param request
	 *            当前请求
	 * @param response
	 *            当前响应
	 * @return:
	 * @throws:
	 */
	public static void createHtmlFromJsp(String jspFile, OutputStream outputStream, Map<String, Object> varMap,
			HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		RequestDispatcher dispatcher = request.getRequestDispatcher(jspFile);
		// 包装getAttribute()方法，使它能取到varMap的变量
		HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request) {
			private Map<String, Object> varMap;
			private HttpServletRequest oldRequest;

			public HttpServletRequestWrapper init(Map<String, Object> varMap, HttpServletRequest request) {
				this.varMap = varMap;
				this.oldRequest = request;
				return this;
			}

			public Object getAttribute(String key) {
				Object value = varMap != null ? varMap.get(key) : null;
				if (value == null) {
					value = oldRequest.getAttribute(key);
				}
				return value;
			}
		}.init(varMap, request);

		// OutputStreamWriter ow = new OutputStreamWriter(outputStream,"UTF-8");
		// PrintWriter servletWriter = new PrintWriter(ow);

		// 包装write方法，把结果流诱导到一个StringBuffer里
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		StringBuffer isTomcatBuffer = new StringBuffer();
		HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
			private PrintWriter servletWriter;
			private ServletOutputStream servletOutputStream;
			private ByteArrayOutputStream bos;
			private StringBuffer isTomcatBuffer;

			public PrintWriter getWriter() {
				if (servletWriter == null) {
					isTomcatBuffer.append("1");
					try {
						servletWriter = new PrintWriter(getOutputStream());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return servletWriter;
			}

			public HttpServletResponseWrapper init(ByteArrayOutputStream bos, StringBuffer isTomcatBuffer) {
				this.bos = bos;
				this.isTomcatBuffer = isTomcatBuffer;
				return this;
			}

			public void flushBuffer() throws IOException {
				if (servletWriter != null) {
					servletWriter.flush();
				}
				if (servletOutputStream != null) {
					servletOutputStream.flush();
				}
			}

			public ServletOutputStream getOutputStream() throws IOException {
				if (servletOutputStream == null) {
					servletOutputStream = new ServletOutputStream() {
						public void write(int arg0) throws IOException {
							bos.write(arg0);
						}

						public void close() throws IOException {
							bos.flush();
						}

						public void flush() throws IOException {
							bos.close();
						}
					};
				}
				return servletOutputStream;
			}
		}.init(bos, isTomcatBuffer);
		responseWrapper.setCharacterEncoding("UTF-8");
		dispatcher.include(requestWrapper, responseWrapper);
		// tomcat
		if (isTomcatBuffer.length() > 0) {
			OutputStreamWriter utf8Writer = new OutputStreamWriter(outputStream, "UTF-8");
			responseWrapper.flushBuffer();
			bos.flush();
			byte[] bs = bos.toByteArray();
			utf8Writer.write(new String(bs));
			utf8Writer.flush();
			utf8Writer.close();
			outputStream.flush();
			outputStream.close();
		} else {
			responseWrapper.flushBuffer();
			bos.flush();
			outputStream.write(bos.toByteArray());
			outputStream.flush();
			outputStream.close();
		}
	}

	/**
	 * @description: 获取某jsp执行结果的流
	 * @param jspFile
	 *            jsp文件
	 * @param htmlFile
	 *            静态文件
	 * @param varMap
	 *            要注入jsp的变量，通过request.setAttribute()注入
	 * @param request
	 *            当前请求
	 * @param response
	 *            当前响应
	 * @return:
	 * @throws:
	 */
	public static void createHtmlFromJsp(String jspFile, File htmlFile, Map<String, Object> varMap,
			HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// 预防文件夹不存在
		createMissDir(htmlFile);
		createHtmlFromJsp(jspFile, new FileOutputStream(htmlFile), varMap, request, response);
	}

	/**
	 * 获取文件的md5编码
	 * 
	 * @description: <方法描述>
	 * @param:
	 * @return:
	 * @throws:
	 */
	public static String getMd5(File file) throws IOException {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		InputStream is = new FileInputStream(file);

		byte buffer[] = new byte[1024];
		int len = 0;
		while ((len = is.read(buffer)) != -1) {
			md.update(buffer, 0, len);
		}
		is.close();
		byte[] digest = md.digest();
		// 用来将字节转换成 16 进制表示的字符
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		char digestChar[] = new char[32];
		int k = 0;
		// 从第一个字节开始，对 MD5 的每一个字节转换成 16 进制字符的转换
		for (int i = 0; i < 16; i++) {
			byte d = digest[i]; // 取第 i 个字节
			digestChar[k++] = hexDigits[d >>> 4 & 0xf]; // >>>
			// 为逻辑右移，将符号位一起右移，取字节中高
			// 4 位的数字转换,
			digestChar[k++] = hexDigits[d & 0xf]; // 取字节中低 4 位的数字转换
		}
		return new String(digestChar);
	}

	/**
	 * 获取文件的md5编码
	 * 
	 * @description: <方法描述>
	 * @param:
	 * @return:
	 * @throws:
	 */
	public static String getMd5(InputStream is) throws IOException {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte buffer[] = new byte[1024];
		int len = 0;
		while ((len = is.read(buffer)) != -1) {
			md.update(buffer, 0, len);
		}
		byte[] digest = md.digest();
		// 用来将字节转换成 16 进制表示的字符
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		char digestChar[] = new char[32];
		int k = 0;
		// 从第一个字节开始，对 MD5 的每一个字节转换成 16 进制字符的转换
		for (int i = 0; i < 16; i++) {
			byte d = digest[i]; // 取第 i 个字节
			digestChar[k++] = hexDigits[d >>> 4 & 0xf]; // >>>
			// 为逻辑右移，将符号位一起右移，取字节中高
			// 4 位的数字转换,
			digestChar[k++] = hexDigits[d & 0xf]; // 取字节中低 4 位的数字转换
		}
		return new String(digestChar);
	}

	public static void main(String args[]) {
		try {
			System.out.println(FileUtil.getMd5(new File("E:/develop/doc/ptksite.sql")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}