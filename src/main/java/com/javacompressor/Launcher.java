package com.javacompressor;

/**
 * Launcher class that serves as the main entry point for the application.
 * This class is needed to properly set up the JavaFX application with the Maven Shade plugin.
 */
public class Launcher {
    
    /**
     * Main method that delegates to the JavaFX Application's main method.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        Main.main(args);
    }
} 