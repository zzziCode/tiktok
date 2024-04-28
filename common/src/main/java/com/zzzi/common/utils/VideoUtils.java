package com.zzzi.common.utils;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author zzzi
 * @date 2024/3/23 21:32
 * 根据传递而来的视频抽取一阵作为封面并返回
 */

public class VideoUtils {

    /**
     * @author zzzi
     * @date 2024/3/24 10:30
     * 抓取视频帧作为封面，保存到本地
     * 返回对应的封面文件
     */
    public static File fetchPic(File file, String frameFile) {
        try {
            FFmpegFrameGrabber ff = new FFmpegFrameGrabber(file);
            ff.start();
            int length = ff.getLengthInFrames();

            File targetFile = new File(frameFile);
            int i = 0;
            Frame frame = null;
            while (i < length) {
                // 过滤前5帧，避免出现全黑的图片，依自己情况而定

                frame = ff.grabFrame();
                //找到了合适的帧
                if ((i > 5) && (frame.image != null)) {
                    break;
                }
                i++;
            }

            //获取文件格式：.jpg、.png
            String imgSuffix = "jpg";
            if (frameFile.indexOf('.') != -1) {
                String[] arr = frameFile.split("\\.");
                if (arr.length >= 2) {
                    imgSuffix = arr[1];
                }
            }

            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage srcBi = converter.getBufferedImage(frame);
            int owidth = srcBi.getWidth();
            int oheight = srcBi.getHeight();
            // 对截取的帧进行等比例缩放
            int width = 800;
            int height = (int) (((double) width / owidth) * oheight);
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            bi.getGraphics().drawImage(srcBi.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            try {
                ImageIO.write(bi, imgSuffix, targetFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ff.stop();
            return targetFile;
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}