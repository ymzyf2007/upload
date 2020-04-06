package com.net.train.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.net.train.util.AnalysisIpa;
import com.net.train.util.FileUtil;
import com.net.train.util.ImageUtil;

@Controller
@RequestMapping("/file")
public class Upload {
	static final String SERVER_URL = "http://115.29.149.156:8080/upload/";
	static final String IOS_FILE_PATH = "upload/temp/patientOL.plist";
	static final String ANDROID_FILE_PATH = "upload/temp/patientOL.apk";
	static final String ROOT_PATH = "upload";
	static final String ORIGINAL_PICTURE_PATH = "/images/op"; // 原图路径
	static final String BIG_PICTURE_PATH = "/images/bp"; // 大图路径
	static final String MIDDLE_PICTURE_PATH = "/images/mp"; // 中图路径
	static final String SMALL_PICTURE_PATH = "/images/sp"; // 小图路径
	static final String AUDIO_PATH = "/audio"; // 音频文件路径
	static final String OTHER_FILE_PATH = "/other"; // 其他类型文件路径
	static final String ALL_TEMP_PATH = "/temp"; // 临时文件路径
	static final String HEAD_PATH = "/head"; // 用户头像目录
	// 图片格式
	static final String[] IMAGE_EXTENSION = { "BMP", "PCX", "TIFF", "GIF", "JPEG", "JPG", "TGA", "EXIF", "FPX", "SVG",
			"PSD", "CDR", "PCD", "DXF", "UFO", "EPS", "AI", "PNG", "HDRI", "RAW", "WEBP" };
	// 音频格式
	static final String[] AUDIO_EXTENSION = { "3GP", "AAC", "AIFF", "ANI", "AMR", "AVI", "AU", "BND", "CD", "MIDI",
			"MID", "MMF", "MP3", "MP4", "RA", "RDI", "RM", "RMN", "RMI", "RMX", "PAL", "VQF", "WAV", "WMA" };
	// 严禁上传的格式
	static final String[] BAN_EXTENSION = { "JSP", "JAVA", "CLASS", "HTML", "HTM", "PHP", "SH", "CMD" };
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

	/**
	 * 文件的上传方法，不对文件做处理
	 * 
	 * @param request
	 * @param response
	 */
	@ResponseBody
	@RequestMapping("/upload")
	public Map<String, Object> upload(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("开始上传");
		String dateStr = sdf.format(new Date());
		Map<String, Object> reMap = new HashMap<String, Object>();
		// 创建一个通用的多部分解析器 ，用于解析SpringMVC的上下文
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
		List<String> fileNameList = null;
		try {
			// 解析request，判断是否为MultipartFile类型数据,即多部分请求
			if (multipartResolver.isMultipart(request)) {
				// 转换成多部分request
				MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
				// 取得request中的所有文件名
				Iterator<String> iter = multiRequest.getFileNames();
				fileNameList = new ArrayList<String>();
				while (iter.hasNext()) {
					// 取得上传文件
					MultipartFile file = multiRequest.getFile(iter.next());
					if (file != null) {
						// 取得当前上传文件的文件名称
						String myFileName = file.getOriginalFilename();
						String realPath = request.getSession().getServletContext().getRealPath("/") + "/";
						String iosPath = realPath + IOS_FILE_PATH;
						String androidPath = realPath + ANDROID_FILE_PATH;
						/** 获取文件的后缀 **/
						String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
						if (suffix != null && !"".equals(suffix)) {
							suffix = suffix.toUpperCase();
							for (String imgSuffix : BAN_EXTENSION) {
								if (imgSuffix.equals(suffix)) {
									return null;
								}
							}
						}
						Integer fileType = getFileType(suffix); // 1:图片文件 2:音频文件 3:其他
						switch (fileType) {
						case 1:
							realPath += ROOT_PATH + ORIGINAL_PICTURE_PATH + "/" + dateStr;
							break;
						case 2:
							realPath += ROOT_PATH + AUDIO_PATH + "/" + dateStr;
							break;
						default:
							realPath += ROOT_PATH + OTHER_FILE_PATH + "/" + dateStr;
							break;
						}
						// 如果名称不为“”,说明该文件存在，否则说明该文件不存在
						if (myFileName.trim() != "") {
							// 重命名上传后的文件名
							String fileName = FileUtil.getMd5(file.getInputStream()) + "." + suffix;
							System.out.println("文件名：" + fileName);
							if (suffix.toLowerCase().equals("ipa")) {
								File fileplist = new File(iosPath);
								InputStream infoIs = new FileInputStream(fileplist);
								Document doc = AnalysisIpa.getDoc(infoIs);
								NodeList nodeList = doc.getElementsByTagName("*");
								for (int i = 0, length = nodeList.getLength(); i < length; i += 2) {
									Node node = nodeList.item(i);
									if (!node.getNodeName().equals("key")) {
										continue;
									}
									// String key = node.getTextContent();
									String value = nodeList.item(i + 1).getTextContent();
									if (StringUtils.isNotBlank(value) && value.length() > 3) {
										if (value.substring(value.length() - 3).toLowerCase().equals("ipa")) {
											String fileUrl = SERVER_URL + ROOT_PATH + OTHER_FILE_PATH + "/" + dateStr
													+ "/" + fileName;
											nodeList.item(i + 1).setTextContent(fileUrl);
										}
									}
								}
								createPlist(doc, iosPath);
								fileNameList.add("upload/temp/patientOL.plist");
							} else if (suffix.toLowerCase().equals("apk")) {
								fileNameList.add(dateStr + "/" + fileName);
								FileUtils.copyInputStreamToFile(file.getInputStream(), new File(androidPath));
							} else {
								if ("AMR".equals(suffix.toUpperCase())) {
									fileNameList.add(dateStr + "/" + FileUtil.getMd5(file.getInputStream()) + ".mp3");
								} else {
									fileNameList.add(dateStr + "/" + fileName);
								}
							}
							// 不必处理IO流关闭的问题，因为FileUtils.copyInputStreamToFile()方法内部会自动把用到的IO流关掉
							File tmpFile = new File(realPath);
							// 如果文件夹不存在则创建
							if (!tmpFile.exists() && !tmpFile.isDirectory()) {
								tmpFile.mkdirs();
							}
							FileUtils.copyInputStreamToFile(file.getInputStream(), new File(realPath, fileName));
						}
					}
				}
			}
			reMap.put("result", "200");
			reMap.put("message", "上传成功");
			reMap.put("fileNames", fileNameList);
			System.out.println("上传成功");
		} catch (Exception e) {
			System.out.println("上传失败，原因：" + e.getMessage());
			reMap.put("result", "500");
			reMap.put("message", "上传失败，原因：" + e.getMessage());
			e.printStackTrace();
		}
		return reMap;
	}

	private static void createPlist(Document document, String filePath) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		DOMSource source = new DOMSource(document);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		PrintWriter pw = new PrintWriter(new FileOutputStream(filePath));
		StreamResult result = new StreamResult(pw);
		transformer.transform(source, result);
		System.out.println("生成XML文件成功!");
	}

	/**
	 * 图片的上传方法，对图片进行压缩组合成三种比例的图片（只处理图片）
	 * 
	 * @param request
	 * @param response
	 */
	@ResponseBody
	@RequestMapping("/uploadImgCtl")
	public Map<String, Object> uploadImgCtl(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> reMap = new HashMap<String, Object>();
		String dateStr = sdf.format(new Date());
		String loadPathBig, loadPathMid, loadPathSma;
		// 创建一个通用的多部分解析器 ，用于解析SpringMVC的上下文
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
		List<String> fileNameList = null;
		try {
			// 解析request，判断是否为MultipartFile类型数据,即多部分请求
			if (multipartResolver.isMultipart(request)) {
				// 转换成多部分request
				MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
				// 取得request中的所有文件名
				Iterator<String> iter = multiRequest.getFileNames();
				fileNameList = new ArrayList<String>();
				while (iter.hasNext()) {
					// 取得上传文件
					MultipartFile file = multiRequest.getFile(iter.next());
					if (file != null) {
						// 取得当前上传文件的文件名称
						String myFileName = file.getOriginalFilename();
						String realPath = request.getSession().getServletContext().getRealPath("/") + "/";
						/** 获取文件的后缀 **/
						String suffix = file.getOriginalFilename()
								.substring(file.getOriginalFilename().lastIndexOf(".") + 1);
						Integer fileType = getFileType(suffix); // 1:图片文件 2:音频文件
																// 3:其他
						switch (fileType) {
						case 1:
							// 除了原图之外，将生成3中比例的图片
							loadPathBig = new String(realPath) + ROOT_PATH + BIG_PICTURE_PATH + "/" + dateStr;
							loadPathMid = new String(realPath) + ROOT_PATH + MIDDLE_PICTURE_PATH + "/" + dateStr;
							loadPathSma = new String(realPath) + ROOT_PATH + SMALL_PICTURE_PATH + "/" + dateStr;
							realPath += ROOT_PATH + ORIGINAL_PICTURE_PATH + "/" + dateStr;
							break;
						default:
							reMap.put("result", "500");
							reMap.put("message", "上传失败，原因：只能处理图片！");
							return reMap;
						}
						// 如果名称不为“”,说明该文件存在，否则说明该文件不存在
						if (myFileName.trim() != "") {
							// 重命名上传后的文件名
							String fileName = FileUtil.getMd5(file.getInputStream()) + "." + suffix;
							System.out.println("文件名：" + fileName);
							fileNameList.add(dateStr + "/" + fileName);
							// 不必处理IO流关闭的问题，因为FileUtils.copyInputStreamToFile()方法内部会自动把用到的IO流关掉
							existsDir(realPath);
							existsDir(loadPathBig);
							existsDir(loadPathMid);
							existsDir(loadPathSma);
							// 保存原图
							FileUtils.copyInputStreamToFile(file.getInputStream(), new File(realPath, fileName));
							FileOutputStream bigFile = new FileOutputStream(loadPathBig + "/" + fileName),
									midFile = new FileOutputStream(loadPathMid + "/" + fileName),
									smaFile = new FileOutputStream(loadPathSma + "/" + fileName);
							try {
								Long imgSize = file.getSize();
								InputStream is = file.getInputStream();
//								BufferedImage prevImage = ImageIO.read(is);
								ImageUtil.resizeImage(is, bigFile, 600, imgSize.intValue());
								is.close();
								is = file.getInputStream();
								ImageUtil.resizeImage(is, midFile, 300, imgSize.intValue());
								is.close();
								is = file.getInputStream();
								ImageUtil.resizeImage(is, smaFile, 130, imgSize.intValue());
								is.close();

								// int width = 0;
								// // 大图暂定尺寸width 700 ，height 随动
								// width = 700;
								// width = width == 0 || width >
								// prevImage.getWidth() ? prevImage.getWidth() :
								// width;
								// ImageTools.cutImage(width, realPath + "/" +
								// fileName, loadPathBig + "/" + fileName);
								// File bigFileTmp = new File(loadPathBig + "/"
								// + fileName);
								// if (bigFileTmp.length() > 350 * 1024) {
								// ImageUtils.Tosmallerpic(loadPathBig + "/" +
								// fileName, loadPathBig + "/" + fileName,
								// width, 0.8F);
								// }
								// // 中图暂定尺寸width 300
								// width = 300;
								// width = width == 0 || width >
								// prevImage.getWidth() ? prevImage.getWidth() :
								// width;
								// ImageUtils.Tosmallerpic(prevImage,
								// loadPathMid + "/" + fileName, width, 0.6F);
								// // 小图暂定尺寸width 130
								// width = 130;
								// width = width == 0 || width >
								// prevImage.getWidth() ? prevImage.getWidth() :
								// width;
								// ImageUtils.Tosmallerpic(prevImage,
								// loadPathSma + "/" + fileName, width, 0.6F);
								//
								// prevImage.flush();
								// is.close();
							} catch (Exception e) {
								e.printStackTrace();
							}

							/*File midFi = */new File(loadPathMid + "/" + fileName);
							/*File smaFi = */new File(loadPathSma + "/" + fileName);
							// ImageUtil.cutMaxCenterImage(midFi, midFi);
							// ImageUtil.cutMaxCenterImage(smaFi, smaFi);
						}
					}
				}
			}
			reMap.put("result", "200");
			reMap.put("message", "上传成功");
			reMap.put("fileNames", fileNameList);
			System.out.println("上传成功");
		} catch (Exception e) {
			System.out.println("上传失败，原因：" + e.getMessage());
			reMap.put("result", "500");
			reMap.put("message", "上传失败，原因：" + e.getMessage());
			e.printStackTrace();
		}
		return reMap;
	}

	/**
	 * 用户头像图片的上传方法，对图片进行压缩组合成三种比例的图片（只处理图片）
	 * 
	 * @param request
	 * @param response
	 */
	@ResponseBody
	@RequestMapping("/uploadHeadImg")
	public Map<String, Object> uploadHeadImg(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> reMap = new HashMap<String, Object>();
		String dateStr = sdf.format(new Date());
		String loadPathBig, loadPathMid, loadPathSma;
		// 创建一个通用的多部分解析器 ，用于解析SpringMVC的上下文
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		List<String> fileNameList = null;
		try {
			// 解析request，判断是否为MultipartFile类型数据,即多部分请求
			if (multipartResolver.isMultipart(request)) {
				// 转换成多部分request
				MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
				// 取得request中的所有文件名
				Iterator<String> iter = multiRequest.getFileNames();
				fileNameList = new ArrayList<String>();
				while (iter.hasNext()) {
					// 取得上传文件
					MultipartFile file = multiRequest.getFile(iter.next());
					if (file != null) {
						// 取得当前上传文件的文件名称
						String myFileName = file.getOriginalFilename();
						String realPath = request.getSession().getServletContext().getRealPath("/") + "/";
						/** 获取文件的后缀 **/
						String suffix = file.getOriginalFilename()
								.substring(file.getOriginalFilename().lastIndexOf(".") + 1);
						Integer fileType = getFileType(suffix); // 1:图片文件 2:音频文件
																// 3:其他
						switch (fileType) {
						case 1:
							// 除了原图之外，将生成3中比例的图片
							loadPathBig = new String(realPath) + ROOT_PATH + HEAD_PATH + BIG_PICTURE_PATH + "/"
									+ dateStr;
							loadPathMid = new String(realPath) + ROOT_PATH + HEAD_PATH + MIDDLE_PICTURE_PATH + "/"
									+ dateStr;
							loadPathSma = new String(realPath) + ROOT_PATH + HEAD_PATH + SMALL_PICTURE_PATH + "/"
									+ dateStr;
							realPath += ROOT_PATH + HEAD_PATH + ORIGINAL_PICTURE_PATH + "/" + dateStr;
							break;
						default:
							reMap.put("result", "500");
							reMap.put("message", "上传失败，原因：只能处理图片！");
							return reMap;
						}
						// 如果名称不为“”,说明该文件存在，否则说明该文件不存在
						if (myFileName.trim() != "") {
							// 重命名上传后的文件名
							String fileName = FileUtil.getMd5(file.getInputStream()) + "." + suffix;
							System.out.println("文件名：" + fileName);
							fileNameList.add(dateStr + "/" + fileName);
							// 不必处理IO流关闭的问题，因为FileUtils.copyInputStreamToFile()方法内部会自动把用到的IO流关掉
							existsDir(realPath);
							existsDir(loadPathBig);
							existsDir(loadPathMid);
							existsDir(loadPathSma);
							FileUtils.copyInputStreamToFile(file.getInputStream(), new File(realPath, fileName));
							FileOutputStream bigFile = new FileOutputStream(loadPathBig + "/" + fileName),
									midFile = new FileOutputStream(loadPathMid + "/" + fileName),
									smaFile = new FileOutputStream(loadPathSma + "/" + fileName);
							try {
								InputStream is = file.getInputStream();
								Long imgSize = file.getSize();
								ImageUtil.resizeImage(is, bigFile, 200, imgSize.intValue());
								is.close();
								is = file.getInputStream();
								ImageUtil.resizeImage(is, midFile, 150, imgSize.intValue());
								is.close();
								is = file.getInputStream();
								ImageUtil.resizeImage(is, smaFile, 100, imgSize.intValue());
								is.close();

								// BufferedImage prevImage = ImageIO.read(is);
								// int width = 0;
								// // 大头像暂定尺寸width 200 ，height 随动
								// width = 200;
								// width = width == 0 || width >
								// prevImage.getWidth() ? prevImage.getWidth() :
								// width;
								// ImageTools.cutImage(width, realPath + "/" +
								// fileName, loadPathBig + "/" + fileName);
								// File bigFileTmp = new File(loadPathBig + "/"
								// + fileName);
								// if (bigFileTmp.length() > 30 * 1024) {
								// ImageUtils.Tosmallerpic(loadPathBig + "/" +
								// fileName, loadPathBig + "/" + fileName,
								// width, 0.8F);
								// }
								// // 中头像暂定尺寸width 150
								// width = 150;
								// width = width == 0 || width >
								// prevImage.getWidth() ? prevImage.getWidth() :
								// width;
								// ImageUtils.Tosmallerpic(prevImage,
								// loadPathMid + "/" + fileName, width, 0.8F);
								// // 小头像暂定尺寸width 100
								// width = 100;
								// width = width == 0 || width >
								// prevImage.getWidth() ? prevImage.getWidth() :
								// width;
								// ImageUtils.Tosmallerpic(prevImage,
								// loadPathSma + "/" + fileName, width, 0.8F);
								//
								// prevImage.flush();
								// is.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
							/*File midFi = */new File(loadPathMid + "/" + fileName);
							/*File smaFi =*/ new File(loadPathSma + "/" + fileName);
							// ImageUtil.cutMaxCenterImage(midFi, midFi);
							// ImageUtil.cutMaxCenterImage(smaFi, smaFi);
						}
					}
				}
			}
			reMap.put("result", "200");
			reMap.put("message", "上传成功");
			reMap.put("fileNames", fileNameList);
			System.out.println("上传成功");
		} catch (Exception e) {
			System.out.println("上传失败，原因：" + e.getMessage());
			reMap.put("result", "500");
			reMap.put("message", "上传失败，原因：" + e.getMessage());
			e.printStackTrace();
		}
		return reMap;
	}

	/**
	 * 根据后缀识别文件类型
	 * 
	 * @param suffix
	 *            文件后缀
	 * @return 1:图片文件 2:音频文件 3:其他
	 */
	public static Integer getFileType(String suffix) {
		if (suffix != null && !"".equals(suffix)) {
			suffix = suffix.toUpperCase();
			for (String imgSuffix : IMAGE_EXTENSION) {
				if (imgSuffix.equals(suffix)) {
					return 1;
				}
			}
			for (String audioSuffix : AUDIO_EXTENSION) {
				if (audioSuffix.equals(suffix)) {
					return 2;
				}
			}
		}
		return 3;
	}

	public static void existsDir(String path) {
		File tmpFile = new File(path);
		// 如果文件夹不存在则创建
		if (!tmpFile.exists() && !tmpFile.isDirectory()) {
			tmpFile.mkdirs();
		}
	}
}