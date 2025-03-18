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
 * Utility class for file operations.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    
    // Set of common compressed file extensions
    private static final Set<String> COMPRESSED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "zip", "gz", "gzip", "bz2", "bzip2", "tar.gz", "tgz", "tar.bz2", "tbz2",
        "7z", "rar", "jar", "war", "xz", "lzma", "lz", "z"
    ));
    
    // Extensions supported by our compressor
    private static final Set<String> SUPPORTED_COMPRESS_EXTENSIONS = new HashSet<>(Arrays.asList(
        "zip", "gz", "bz2"
    ));
    
    /**
     * Gets file information for the specified file.
     * 
     * @param file The file
     * @return The file information
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
     * Calculates the total size of a directory.
     * 
     * @param directory The directory
     * @return The total size in bytes
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
    
    /**
     * Gets the file type or extension.
     * 
     * @param file The file
     * @return The file type
     */
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
     * Formats a file size in bytes to a human-readable string.
     * 
     * @param size The file size in bytes
     * @return The formatted file size
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
    
    /**
     * Checks if a file is a compressed file based on its extension.
     * 
     * @param file The file to check
     * @return true if the file is a compressed file, false otherwise
     */
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
     * Checks if a file is compressible by our application.
     * Some file formats are already highly compressed (images, videos, etc.)
     * and don't benefit much from additional compression.
     * 
     * @param file The file to check
     * @return true if the file is likely to benefit from compression
     */
    public static boolean isCompressibleFile(File file) {
        if (file == null || !file.isFile()) {
            return true; // Directories are always compressible
        }
        
        String ext = getExtension(file).toLowerCase();
        
        // List of extensions that are typically already compressed
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
    
    /**
     * Gets the file extension without the dot.
     * 
     * @param file The file
     * @return The extension or empty string if none
     */
    public static String getExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1);
        }
        
        return "";
    }
    
    /**
     * Determines if a compression algorithm is supported for the given file.
     * 
     * @param file The file to check
     * @param algorithm The algorithm to check
     * @return true if the file can be compressed with the algorithm
     */
    public static boolean isSupportedForCompression(File file, CompressionAlgorithm algorithm) {
        // Directories can only be compressed with ZIP
        if (file.isDirectory() && algorithm != CompressionAlgorithm.ZIP) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Determines the compression algorithm for a compressed file based on its extension.
     * 
     * @param file The compressed file
     * @return The compression algorithm
     */
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
     * Gets the output path for decompression based on the compressed file name.
     * 
     * @param compressedFile The compressed file
     * @return The output path
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