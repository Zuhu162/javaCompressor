package com.javacompressor.util;

import com.javacompressor.compression.CompressionAlgorithm;
import com.javacompressor.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for working with files
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    
    // These are the extensions that we know are already compressed
    private static final Set<String> COMPRESSED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "zip", "gz", "gzip", "bz2", "bzip2", "tar.gz", "tgz", "tar.bz2", "tbz2",
        "7z", "rar", "jar", "war", "xz", "lzma", "lz", "z"
    ));
    
    // Only supporting these compression types for now
    // TODO: Add support for 7z in a future version
    private static final Set<String> SUPPORTED_COMPRESS_EXTENSIONS = new HashSet<>(Arrays.asList(
        "zip", "gz", "bz2"
    ));
    
    /**
     * Gets information about a file (size, type, etc)
     */
    public static FileInfo getFileInfo(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }
        
        String name = file.getName();
        long size = file.isDirectory() ? calculateDirectorySize(file) : file.length();
        String type = getFileType(file);
        
        return new FileInfo(file, name, size, type);
    }
    
    /**
     * Adds up the size of all files in a directory
     */
    private static long calculateDirectorySize(File directory) {
        if (!directory.isDirectory()) {
            return 0;
        }
        
        long size = 0;
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += calculateDirectorySize(file);
                }
            }
        }
        
        return size;
    }
    
    // Figure out if this is a directory or what kind of file it is
    public static String getFileType(File file) {
        if (file.isDirectory()) {
            return "Directory";
        }
        
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1).toUpperCase();
        }
        
        return "Unknown";
    }
    
    /**
     * Converts bytes into human-readable sizes like "4.2 MB"
     */
    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        
        DecimalFormat df = new DecimalFormat("#,##0.##");
        return df.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    // Checks if a file is already compressed based on its extension
    public static boolean isCompressedFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        
        // Check simple extensions
        for (String ext : COMPRESSED_EXTENSIONS) {
            if (name.endsWith("." + ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if we should compress this file
     * Some files are already compressed and we'd just waste CPU time
     */
    public static boolean isCompressibleFile(File file) {
        if (file == null || !file.isFile()) {
            return true; // Directories are always compressible
        }
        
        String ext = getExtension(file).toLowerCase();
        
        // List of extensions that are typically already compressed
        // Probably missing some, but these are the common ones
        Set<String> alreadyCompressedTypes = new HashSet<>(Arrays.asList(
            // Images
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff", "heic",
            // Audio
            "mp3", "aac", "ogg", "flac", "wav",
            // Video
            "mp4", "avi", "mkv", "mov", "webm", "flv",
            // Archives (already compressed)
            "zip", "rar", "7z", "gz", "bz2", "tar", "tgz"
        ));
        
        return !alreadyCompressedTypes.contains(ext);
    }
    
    // Gets just the extension part of a filename
    public static String getExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1);
        }
        
        return "";
    }
    
    /**
     * Checks if we can use a particular algorithm on this file
     * Currently only ZIP supports directories
     */
    public static boolean isSupportedForCompression(File file, CompressionAlgorithm algorithm) {
        // Directories can only be compressed with ZIP
        if (file.isDirectory() && algorithm != CompressionAlgorithm.ZIP) {
            return false;
        }
        
        return true;
    }
    
    // Figures out what algorithm to use for decompression based on the file extension
    public static CompressionAlgorithm determineAlgorithm(File file) {
        if (file == null || !file.isFile()) {
            return CompressionAlgorithm.ZIP;
        }
        
        String name = file.getName().toLowerCase();
        
        if (name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".war")) {
            return CompressionAlgorithm.ZIP;
        } else if (name.endsWith(".gz") || name.endsWith(".gzip") || name.endsWith(".tgz") || name.endsWith(".tar.gz")) {
            return CompressionAlgorithm.GZIP;
        } else if (name.endsWith(".bz2") || name.endsWith(".bzip2") || name.endsWith(".tbz2") || name.endsWith(".tar.bz2")) {
            return CompressionAlgorithm.BZIP2;
        }
        
        // Default to ZIP
        return CompressionAlgorithm.ZIP;
    }
    
    /**
     * Figures out where to put the decompressed file(s)
     * This gets tricky with tar.gz and other double extensions
     */
    public static String getDecompressionOutputPath(File compressedFile) {
        if (compressedFile == null) {
            throw new IllegalArgumentException("Compressed file cannot be null");
        }
        
        String fileName = compressedFile.getName();
        String parentPath = compressedFile.getParent();
        String baseName = fileName;
        
        // Handle different compression formats
        if (fileName.toLowerCase().endsWith(".zip") || 
            fileName.toLowerCase().endsWith(".jar") || 
            fileName.toLowerCase().endsWith(".war")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        } else if (fileName.toLowerCase().endsWith(".gz") || fileName.toLowerCase().endsWith(".gzip")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        } else if (fileName.toLowerCase().endsWith(".bz2") || fileName.toLowerCase().endsWith(".bzip2")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        } else if (fileName.toLowerCase().endsWith(".tar.gz") || fileName.toLowerCase().endsWith(".tgz")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            if (baseName.toLowerCase().endsWith(".tar")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
        } else if (fileName.toLowerCase().endsWith(".tar.bz2") || fileName.toLowerCase().endsWith(".tbz2")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            if (baseName.toLowerCase().endsWith(".tar")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
        }
        
        // If it's a ZIP file, we'll decompress to a directory
        if (fileName.toLowerCase().endsWith(".zip") || 
            fileName.toLowerCase().endsWith(".jar") || 
            fileName.toLowerCase().endsWith(".war")) {
            return (parentPath != null ? parentPath + File.separator : "") + baseName;
        } else {
            // For GZIP and BZIP2, we'll decompress to a file
            return (parentPath != null ? parentPath + File.separator : "") + baseName;
        }
    }
}