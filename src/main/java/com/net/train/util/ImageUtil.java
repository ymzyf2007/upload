package com.net.train.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

/**
 * 图片处理公共类
 */
public class ImageUtil {

	static Font FONT = new Font("微软雅黑", Font.BOLD, 18);
	static final Color COLOR = Color.WHITE;

	/**
	 * 给图片添加文字水印
	 *
	 * @param pressText
	 *            水印文字
	 * @param srcImageFile
	 *            源图像地址
	 * @param destImageFile
	 *            目标图像地址
	 * @param fontName
	 *            水印的字体名称
	 * @param fontStyle
	 *            水印的字体样式
	 * @param color
	 *            水印的字体颜色
	 * @param fontSize
	 *            水印的字体大小
	 * @param x
	 *            修正值
	 * @param y
	 *            修正值
	 * @param alpha
	 *            透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
	 * @param formatType
	 *            目标格式
	 */
	public final static void pressWatermark(String pressText, InputStream in, OutputStream out, Font font,
			Color color) {
		try {
			if (font == null) {
				font = FONT;
			}
			if (color == null) {
				color = COLOR;
			}
			BufferedImage src = ImageIO.read(in);
			int width = src.getWidth();
			int height = src.getHeight();
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			graphics.drawImage(src, 0, 0, width, height, null);
			graphics.setColor(color);
			graphics.setFont(font);
			// graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,new
			// Float(0.3)));
			FontRenderContext context = graphics.getFontRenderContext();
			Rectangle2D fontRectangle = font.getStringBounds(pressText, context);
			int sw = (int) fontRectangle.getWidth();
			// 在指定坐标绘制水印文字
			graphics.drawString(pressText, image.getWidth() - sw - 6, image.getHeight() - 8);
			graphics.dispose();
			ImageIO.write(image, "JPEG", out);// 输出到文件流
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 改变图片的大小
	 * 
	 * @param is
	 *            上传的图片的输入流
	 * @param os
	 *            改变了图片的大小后，把图片的流输出到目标OutputStream
	 * @param width
	 *            新图片的宽
	 * @param height
	 *            新图片的高
	 * @param format
	 *            新图片的格式
	 * @throws IOException
	 */
	public static void resizeImage(InputStream is, OutputStream os, int width, int height) {
		BufferedImage prevImage;
		try {
			prevImage = ImageIO.read(is);
			width = width == 0 ? prevImage.getWidth() : width;
			height = height == 0 ? prevImage.getHeight() : height;

			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = image.createGraphics();
			graphics.drawImage(prevImage, 0, 0, width, height, null);
			ImageIO.write(image, getFormatInFile(is), os);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 改变图片的大小,等比压缩
	 * 
	 * @param is
	 *            上传的图片的输入流
	 * @param os
	 *            改变了图片的大小后，把图片的流输出到目标OutputStream
	 * @param width
	 *            新图片的宽
	 * @param height
	 *            新图片的高
	 * @param format
	 *            新图片的格式
	 * @throws IOException
	 */
	public static void resizeImage(InputStream is, OutputStream os, int width) {
		BufferedImage prevImage;
		try {
			prevImage = ImageIO.read(is);
			width = width == 0 ? prevImage.getWidth() : width;
			double dob = (double) width / prevImage.getWidth();
			// 获取等比高度
			int height = (int) (prevImage.getHeight() * (dob));

			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = image.createGraphics();
			graphics.drawImage(prevImage, 0, 0, width, height, null);
			ImageIO.write(image, getFormatInFile(is), os);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取图片的格式 获取失败默认JPEG格式
	 * 
	 * @param is
	 * @return
	 */
	public static String getFormatInFile(InputStream is) {
		String formatName = "JPEG";
		try {
			Iterator<ImageReader> iter = ImageIO.getImageReaders(is);
			if (!iter.hasNext()) {
				return formatName;
			}
			formatName = iter.next().getFormatName();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return formatName;
	}

	public static void main(String[] args) throws IOException {
		FileInputStream in = new FileInputStream(new File("D:/test/1.jpg"));
		FileOutputStream out = new FileOutputStream(new File("D:/test/1_test.jpg"));
		try {
			resizeImage(in, out, 5000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out.flush();
			out.close();
			in.close();
		}
	}
}