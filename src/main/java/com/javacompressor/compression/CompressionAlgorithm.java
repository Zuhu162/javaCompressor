package com.javacompressor.compression;

/**
 * Enum representing the available compression algorithms.
 * Each algorithm has its own characteristics in terms of compression ratio,
 * speed, and compatibility with different systems.
 */
public enum CompressionAlgorithm {
    ZIP("zip", "ZIP (Standard)", "zip"),
    GZIP("gz", "GZIP (Fast)", "gz"),
    BZIP2("bz2", "BZIP2 (High Compression)", "bz2");
    
    private final String extension;
    private final String displayName;
    private final String identifier;
    
    /**
     * Creates a new CompressionAlgorithm.
     * 
     * @param extension The file extension for this algorithm
     * @param displayName The human-readable display name
     * @param identifier A unique identifier for the algorithm
     */
    CompressionAlgorithm(String extension, String displayName, String identifier) {
        this.extension = extension;
        this.displayName = displayName;
        this.identifier = identifier;
    }
    
    /**
     * Gets the file extension for this algorithm.
     * 
     * @return The file extension
     */
    public String getExtension() {
        return extension;
    }
    
    /**
     * Gets the display name for this algorithm.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the identifier for this algorithm.
     * 
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Returns the display name when this enum is used in UI components.
     * 
     * @return The display name
     */
    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Gets a CompressionAlgorithm by file extension.
     * 
     * @param extension The file extension to look up
     * @return The matching CompressionAlgorithm, or ZIP if none found
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