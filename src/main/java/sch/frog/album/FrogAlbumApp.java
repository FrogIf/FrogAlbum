package sch.frog.album;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

/**
 * 批量对照片按照日期进行重命名
 */
public class FrogAlbumApp {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private static final HashMap<String, Integer> dateToIndex = new HashMap<>();

    public static void main(String[] args){
        System.out.println("album rename program start");
        if(args.length < 1){
            System.err.println("no album directory, like : --dir=xxx");
            return;
        }

        // 获取路径
        String directory = args[0];
        System.out.println("scan dir : " + directory);
        try{
            directory = parseKV(directory)[1];
        }catch (IllegalArgumentException e){
            System.out.println(e.getMessage());
            return;
        }

        // 操作确认
        Scanner sc = new Scanner(System.in);
        System.out.print("are you sure to rename picture? (yes/on):");
        String next = sc.next();
        if(!"yes".equals(next)){
            System.out.println("no picture was rename, program exit.");
            return;
        }

        // 目录检查
        File dir = new File(directory);
        if(!dir.exists()){
            System.err.println(directory + " is not exist.");
            return;
        }
        if(!dir.isDirectory()){
            System.err.println(directory + " is not directory");
            return;
        }

        retrieveAndRename(dir);

        System.out.println("album rename program end");
    }

    /**
     * 递归检索, 并重命名文件
     * @param dir 浏览的目录
     */
    private static void retrieveAndRename(File dir) {
        File[] files = dir.listFiles();
        if(files != null && files.length > 0){
            for (File file : files) {
                if(file.isDirectory()){
                    retrieveAndRename(file);
                }else{
                    Date date = null;
                    try{
                        date = readDate(file);
                    } catch (JpegProcessingException | IOException | ParseException e) {
                        System.err.println(e.getMessage());
                    }
                    String originName = file.getName();
                    if(date != null){
                        String dateStr = sdf.format(date);
                        Integer index = dateToIndex.getOrDefault(dateStr, 1);
                        dateToIndex.put(dateStr, index + 1);
                        String fileName = "FROG_" + dateStr + "_" + index + "." + getSuffix(originName);
                        if(file.renameTo(new File(file.getParentFile().getAbsolutePath() + File.separator + fileName))){
                            System.out.println("success for " + originName);
                        }else{
                            System.out.println("rename fail for " + originName);
                        }
                    }else{
                        System.out.println("get date fail for " + originName);
                    }
                }
            }
        }
    }


    /**
     * jpeg图片文件日期格式
     */
    private static final SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    /**
     * 获取相片日期
     * @param file 照片文件
     * @return 日期
     */
    private static Date readDate(File file) throws JpegProcessingException, IOException, ParseException {
        String fileName = file.getName();
        String suffix = getSuffix(fileName);
        if("jpg".equalsIgnoreCase(suffix) || "jpeg".equalsIgnoreCase(suffix)){
            Metadata metadata;
            metadata = JpegMetadataReader.readMetadata(file);
            for (Directory d : metadata.getDirectories()) {
                if (!"Exif IFD0".equals(d.getName())) {
                    continue;
                }
                for (Tag tag : d.getTags()) {
                    if ("Date/Time".equalsIgnoreCase(tag.getTagName())) {
                        return exifDateFormat.parse(tag.getDescription());
                    }
                }
            }
        }
        long lastModified = file.lastModified();
        return new Date(lastModified);
    }

    /**
     * 获取文件后缀
     * @param name 文件名
     * @return 后缀
     */
    private static String getSuffix(String name){
        int pos = name.lastIndexOf('.');
        if(pos < 0){
            return "";
        }else{
            return name.substring(pos + 1);
        }
    }

    /**
     * 解析入参
     * @param content 入参, 格式: --key=value
     * @return { key, value }
     */
    private static String[] parseKV(String content){
        if(!content.startsWith("--")){
            throw new IllegalArgumentException("argument format not right : " + content);
        }
        content = content.substring(2);
        int pos = content.indexOf('=');
        if(pos < 1){
            throw new IllegalArgumentException("argument format not right : " + content);
        }
        return new String[]{
                content.substring(0, pos),
                content.substring(pos + 1)
        };
    }

}
