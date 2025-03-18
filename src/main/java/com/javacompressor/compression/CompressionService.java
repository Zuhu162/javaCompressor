package com.javacompressor.compression;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service class that handles file compression and decompression operations.
 */
public class CompressionService {
    private static final Logger logger = LoggerFactory.getLogger(CompressionService.class);
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Compresses a file using the specified algorithm.
     * 
     * @param sourceFile The file to compress
     * @param outputFile The output file
     * @param algorithm The compression algorithm to use
     * @param compressionLevel The compression level (1-9)
     * @param preserveStructure Whether to preserve directory structure for directories
     * @param progressCallback Callback for progress updates
     * @return The compression ratio (output size / input size)
     * @throws IOException If an I/O error occurs
     */
    public double compressFile(File sourceFile, File outputFile, CompressionAlgorithm algorithm,
                              int compressionLevel, boolean preserveStructure,
                              Consumer<Double> progressCallback) throws IOException {
        logger.info("Compressing file: {} to {} using {}", sourceFile.getAbsolutePath(), 
                   outputFile.getAbsolutePath(), algorithm);
        
        // Make sure the compression level is valid
        int level = Math.max(1, Math.min(9, compressionLevel));
        
        // Create parent directories if needed
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        
        long inputSize = calculateTotalSize(sourceFile);
        
        // Choose the appropriate compression method
        switch (algorithm) {
            case ZIP:
                return compressWithZip(sourceFile, outputFile, level, preserveStructure, inputSize, progressCallback);
            case GZIP:
                return compressWithGzip(sourceFile, outputFile, level, inputSize, progressCallback);
            case BZIP2:
                return compressWithBzip2(sourceFile, outputFile, level, inputSize, progressCallback);
            default:
                throw new IllegalArgumentException("Unsupported compression algorithm: " + algorithm);
        }
    }
    
    /**
     * Decompresses a file.
     * 
     * @param sourceFile The compressed file
     * @param outputPath The output directory or file
     * @param algorithm The compression algorithm
     * @param progressCallback Callback for progress updates
     * @return The decompression ratio (output size / input size)
     * @throws IOException If an I/O error occurs
     */
    public double decompressFile(File sourceFile, File outputPath, CompressionAlgorithm algorithm,
                                Consumer<Double> progressCallback) throws IOException {
        logger.info("Decompressing file: {} to {} using {}", sourceFile.getAbsolutePath(), 
                   outputPath.getAbsolutePath(), algorithm);
        
        // Create output directory if needed
        if (outputPath.getParentFile() != null) {
            outputPath.getParentFile().mkdirs();
        }
        
        long inputSize = sourceFile.length();
        
        // Choose the appropriate decompression method
        switch (algorithm) {
            case ZIP:
                return decompressZip(sourceFile, outputPath, inputSize, progressCallback);
            case GZIP:
                return decompressGzip(sourceFile, outputPath, inputSize, progressCallback);
            case BZIP2:
                return decompressBzip2(sourceFile, outputPath, inputSize, progressCallback);
            default:
                throw new IllegalArgumentException("Unsupported compression algorithm: " + algorithm);
        }
    }
    
    /**
     * Calculates the total size of a file or directory.
     * 
     * @param file The file or directory
     * @return The total size in bytes
     */
    private long calculateTotalSize(File file) {
        if (file.isFile()) {
            return file.length();
        } else if (file.isDirectory()) {
            long size = 0;
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    size += calculateTotalSize(f);
                }
            }
            return size;
        }
        return 0;
    }
    
    /**
     * Compresses a file or directory using ZIP compression.
     */
    private double compressWithZip(File sourceFile, File outputFile, int level, boolean preserveStructure,
                                   long totalSize, Consumer<Double> progressCallback) throws IOException {
        long bytesProcessed = 0;
        
        try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(outputFile)) {
            zipOutput.setLevel(level);
            
            if (sourceFile.isFile()) {
                // Compress a single file
                bytesProcessed = compressSingleFileToZip(sourceFile, zipOutput, "", totalSize, progressCallback);
            } else if (sourceFile.isDirectory()) {
                // Compress a directory
                String basePath = preserveStructure ? sourceFile.getName() + File.separator : "";
                bytesProcessed = compressDirectoryToZip(sourceFile, zipOutput, basePath, totalSize, progressCallback);
            }
        }
        
        // Calculate compression ratio
        double compressionRatio = (double) outputFile.length() / totalSize;
        logger.info("Compression complete. Compression ratio: {}", compressionRatio);
        
        return compressionRatio;
    }
    
    /**
     * Compresses a single file into a ZIP archive.
     */
    private long compressSingleFileToZip(File file, ZipArchiveOutputStream zipOutput, String basePath,
                                        long totalSize, Consumer<Double> progressCallback) throws IOException {
        if (file == null || !file.exists()) {
            return 0;
        }
        
        String entryName = basePath + file.getName();
        ZipArchiveEntry entry = new ZipArchiveEntry(file, entryName);
        zipOutput.putArchiveEntry(entry);
        
        long bytesProcessed = 0;
        
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            
            while ((read = bis.read(buffer)) != -1) {
                zipOutput.write(buffer, 0, read);
                bytesProcessed += read;
                
                if (progressCallback != null && totalSize > 0) {
                    progressCallback.accept((double) bytesProcessed / totalSize);
                }
            }
        }
        
        zipOutput.closeArchiveEntry();
        return bytesProcessed;
    }
    
    /**
     * Recursively compresses a directory into a ZIP archive.
     */
    private long compressDirectoryToZip(File directory, ZipArchiveOutputStream zipOutput, String basePath,
                                       long totalSize, Consumer<Double> progressCallback) throws IOException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return 0;
        }
        
        long bytesProcessed = 0;
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    bytesProcessed += compressSingleFileToZip(file, zipOutput, basePath, totalSize, progressCallback);
                } else if (file.isDirectory()) {
                    String newBasePath = basePath + file.getName() + File.separator;
                    bytesProcessed += compressDirectoryToZip(file, zipOutput, newBasePath, totalSize, progressCallback);
                }
            }
        }
        
        return bytesProcessed;
    }
    
    /**
     * Compresses a file using GZIP compression.
     */
    private double compressWithGzip(File sourceFile, File outputFile, int level, long totalSize,
                                   Consumer<Double> progressCallback) throws IOException {
        if (!sourceFile.isFile()) {
            throw new IllegalArgumentException("GZIP compression only supports single files, not directories");
        }
        
        // Create GzipParameters and set the compression level
        GzipParameters parameters = new GzipParameters();
        parameters.setCompressionLevel(level);
        
        try (FileInputStream fis = new FileInputStream(sourceFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile);
             GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(fos, parameters)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            long bytesProcessed = 0;
            
            while ((read = bis.read(buffer)) != -1) {
                gzipOut.write(buffer, 0, read);
                bytesProcessed += read;
                
                if (progressCallback != null && totalSize > 0) {
                    progressCallback.accept((double) bytesProcessed / totalSize);
                }
            }
        }
        
        double compressionRatio = (double) outputFile.length() / totalSize;
        logger.info("GZIP compression complete. Compression ratio: {}", compressionRatio);
        
        return compressionRatio;
    }
    
    /**
     * Compresses a file using BZIP2 compression.
     */
    private double compressWithBzip2(File sourceFile, File outputFile, int level, long totalSize,
                                    Consumer<Double> progressCallback) throws IOException {
        if (!sourceFile.isFile()) {
            throw new IllegalArgumentException("BZIP2 compression only supports single files, not directories");
        }
        
        try (FileInputStream fis = new FileInputStream(sourceFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile);
             BZip2CompressorOutputStream bzip2Out = new BZip2CompressorOutputStream(fos, level)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            long bytesProcessed = 0;
            
            while ((read = bis.read(buffer)) != -1) {
                bzip2Out.write(buffer, 0, read);
                bytesProcessed += read;
                
                if (progressCallback != null && totalSize > 0) {
                    progressCallback.accept((double) bytesProcessed / totalSize);
                }
            }
        }
        
        double compressionRatio = (double) outputFile.length() / totalSize;
        logger.info("BZIP2 compression complete. Compression ratio: {}", compressionRatio);
        
        return compressionRatio;
    }
    
    /**
     * Decompresses a ZIP file.
     */
    private double decompressZip(File sourceFile, File outputPath, long totalSize,
                                Consumer<Double> progressCallback) throws IOException {
        long bytesProcessed = 0;
        long totalUncompressedSize = 0;
        
        // First, create output directory if it doesn't exist
        if (!outputPath.exists()) {
            outputPath.mkdirs();
        }
        
        // Get total uncompressed size to calculate progress
        try (ZipFile zipFile = new ZipFile(sourceFile)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                totalUncompressedSize += entry.getSize();
            }
            
            // Now extract all entries
            entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                File entryFile = new File(outputPath, entry.getName());
                
                if (entry.isDirectory()) {
                    // Create directory
                    entryFile.mkdirs();
                } else {
                    // Create parent directories if needed
                    if (entryFile.getParentFile() != null) {
                        entryFile.getParentFile().mkdirs();
                    }
                    
                    // Extract file
                    try (InputStream is = zipFile.getInputStream(entry);
                         BufferedInputStream bis = new BufferedInputStream(is);
                         FileOutputStream fos = new FileOutputStream(entryFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        
                        while ((read = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, read);
                            bytesProcessed += read;
                            
                            if (progressCallback != null && totalUncompressedSize > 0) {
                                progressCallback.accept((double) bytesProcessed / totalUncompressedSize);
                            }
                        }
                    }
                }
            }
        }
        
        double decompressionRatio = totalUncompressedSize > 0 ? (double) totalUncompressedSize / totalSize : 1.0;
        logger.info("ZIP decompression complete. Decompression ratio: {}", decompressionRatio);
        
        return decompressionRatio;
    }
    
    /**
     * Decompresses a GZIP file.
     */
    private double decompressGzip(File sourceFile, File outputFile, long totalSize,
                                 Consumer<Double> progressCallback) throws IOException {
        // For GZIP, output path should be a file, not a directory
        if (outputFile.exists() && outputFile.isDirectory()) {
            // If output is a directory, create a file in that directory with source name minus .gz
            String fileName = sourceFile.getName();
            if (fileName.toLowerCase().endsWith(".gz")) {
                fileName = fileName.substring(0, fileName.length() - 3);
            }
            outputFile = new File(outputFile, fileName);
        }
        
        // Create parent directories if needed
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        
        long bytesProcessed = 0;
        
        try (FileInputStream fis = new FileInputStream(sourceFile);
             GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fis);
             BufferedInputStream bis = new BufferedInputStream(gzipIn);
             FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
                bytesProcessed += read;
                
                // Since we don't know the uncompressed size beforehand,
                // we'll estimate progress based on input file size
                if (progressCallback != null && totalSize > 0) {
                    double progress = Math.min(1.0, (double) fis.getChannel().position() / totalSize);
                    progressCallback.accept(progress);
                }
            }
        }
        
        double decompressionRatio = (double) outputFile.length() / totalSize;
        logger.info("GZIP decompression complete. Decompression ratio: {}", decompressionRatio);
        
        return decompressionRatio;
    }
    
    /**
     * Decompresses a BZIP2 file.
     */
    private double decompressBzip2(File sourceFile, File outputFile, long totalSize,
                                  Consumer<Double> progressCallback) throws IOException {
        // For BZIP2, output path should be a file, not a directory
        if (outputFile.exists() && outputFile.isDirectory()) {
            // If output is a directory, create a file in that directory with source name minus .bz2
            String fileName = sourceFile.getName();
            if (fileName.toLowerCase().endsWith(".bz2")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            outputFile = new File(outputFile, fileName);
        }
        
        // Create parent directories if needed
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        
        long bytesProcessed = 0;
        
        try (FileInputStream fis = new FileInputStream(sourceFile);
             BZip2CompressorInputStream bzip2In = new BZip2CompressorInputStream(fis);
             BufferedInputStream bis = new BufferedInputStream(bzip2In);
             FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
                bytesProcessed += read;
                
                // Since we don't know the uncompressed size beforehand,
                // we'll estimate progress based on input file size
                if (progressCallback != null && totalSize > 0) {
                    double progress = Math.min(1.0, (double) fis.getChannel().position() / totalSize);
                    progressCallback.accept(progress);
                }
            }
        }
        
        double decompressionRatio = (double) outputFile.length() / totalSize;
        logger.info("BZIP2 decompression complete. Decompression ratio: {}", decompressionRatio);
        
        return decompressionRatio;
    }
} 