package com.javacompressor.compression;

/**
 * Available compression algorithms with their properties.
 * Each has different tradeoffs between speed and compression quality.
 */
public enum CompressionAlgorithm {
    ZIP("zip", "ZIP (Standard)", "zip"),
    GZIP("gz", "GZIP (Fast)", "gz"),
    BZIP2("bz2", "BZIP2 (High Compression)", "bz2");
    
    private final String extension;
    private final String displayName;
    private final String identifier;
    
    /**
     * Creates a new algorithm entry
     */
    CompressionAlgorithm(String extension, String displayName, String identifier) {
        this.extension = extension;
        this.displayName = displayName;
        this.identifier = identifier;
    }
    
    // Gets the file extension (e.g., "zip", "gz")
    public String getExtension() {
        return extension;
    }
    
    // Gets the user-friendly name
    public String getDisplayName() {
        return displayName;
    }
    
    // Gets the unique ID for the algorithm
    public String getIdentifier() {
        return identifier;
    }
    
    // Shows the friendly name in UI dropdowns
    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Finds the matching algorithm for a file extension
     * Falls back to ZIP if we can't figure it out
     */
    public static CompressionAlgorithm fromExtension(String extension) {
        if (extension == null) {
            return ZIP;
        }
        
        String lowerExt = extension.toLowerCase();
        for (CompressionAlgorithm algorithm : values()) {
            if (algorithm.getExtension().equals(lowerExt)) {
                return algorithm;
            }
        }
        
        return ZIP;
    }
} 