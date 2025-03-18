package com.javacompressor.model;

import java.io.File;

/**
 * Class that holds information about a file to be compressed or decompressed.
 */
public class FileInfo {
    private final File file;
    private final String name;
    private final long size;
    private final String type;
    
    /**
     * Creates a new FileInfo instance.
     * 
     * @param file The file
     * @param name The file name
     * @param size The file size in bytes
     * @param type The file type or extension
     */
    public FileInfo(File file, String name, long size, String type) {
        this.file = file;
        this.name = name;
        this.size = size;
        this.type = type;
    }
    
    /**
     * Gets the file.
     * 
     * @return The file
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Gets the file name.
     * 
     * @return The file name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the file size in bytes.
     * 
     * @return The file size
     */
    public long getSize() {
        return size;
    }
    
    /**
     * Gets the file type or extension.
     * 
     * @return The file type
     */
    public String getType() {
        return type;
    }
} 