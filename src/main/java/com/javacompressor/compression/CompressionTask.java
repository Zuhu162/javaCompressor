package com.javacompressor.compression;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * JavaFX Task for performing compression and decompression operations.
 */
public class CompressionTask extends Task<Double> {
    private static final Logger logger = LoggerFactory.getLogger(CompressionTask.class);
    
    private final boolean compress;
    private final File sourceFile;
    private final File outputFile;
    private final CompressionAlgorithm algorithm;
    private final int compressionLevel;
    private final boolean preserveStructure;
    private final CompressionService compressionService;
    
    /**
     * Creates a new CompressionTask.
     * 
     * @param compress Whether this is a compression (true) or decompression (false) task
     * @param sourceFile The source file
     * @param outputFile The output file
     * @param algorithm The compression algorithm
     * @param compressionLevel The compression level (for compression only)
     * @param preserveStructure Whether to preserve directory structure (for compression only)
     */
    public CompressionTask(boolean compress, File sourceFile, File outputFile,
                          CompressionAlgorithm algorithm, int compressionLevel,
                          boolean preserveStructure) {
        this.compress = compress;
        this.sourceFile = sourceFile;
        this.outputFile = outputFile;
        this.algorithm = algorithm;
        this.compressionLevel = compressionLevel;
        this.preserveStructure = preserveStructure;
        this.compressionService = new CompressionService();
    }
    
    /**
     * Executes the compression or decompression task.
     * 
     * @return The compression/decompression ratio
     * @throws Exception If an error occurs during processing
     */
    @Override
    protected Double call() throws Exception {
        String operationType = compress ? "Compression" : "Decompression";
        try {
            logger.info("Starting {} task", operationType);
            updateMessage("Starting " + operationType.toLowerCase() + "...");
            updateProgress(0, 1);
            
            // Run the actual compression/decompression operation
            Double result;
            if (compress) {
                updateMessage("Compressing file...");
                result = compressionService.compressFile(sourceFile, outputFile, algorithm, 
                                                       compressionLevel, preserveStructure, 
                                                       this::updateProgressInternal);
            } else {
                updateMessage("Decompressing file...");
                result = compressionService.decompressFile(sourceFile, outputFile, algorithm,
                                                         this::updateProgressInternal);
            }
            
            updateMessage(operationType + " complete");
            updateProgress(1, 1);
            
            logger.info("{} task completed successfully", operationType);
            return result;
        } catch (Exception e) {
            logger.error("{} task failed", operationType, e);
            updateMessage(operationType + " failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Updates the task progress and checks for cancellation.
     * 
     * @param progress The progress value (0-1)
     */
    private void updateProgressInternal(double progress) {
        // Check if task has been cancelled
        if (isCancelled()) {
            throw new RuntimeException("Task was cancelled");
        }
        
        // Update progress
        updateProgress(progress, 1);
    }
} 