# Java File Compressor - Class Diagram

Below is the class diagram for the Java File Compressor application, showing the main components and their relationships.

```mermaid
classDiagram
    %% Main application class
    class JavaFileCompressorApp {
        +start(Stage stage) void
        +main(String[] args) void
    }

    %% Model classes
    class FileInfo {
        -File file
        -String name
        -long size
        -String type
        +FileInfo(File file, String name, long size, String type)
        +getFile() File
        +getName() String
        +getSize() long
        +getType() String
        +getFormattedSize() String
    }

    class CompressionSettings {
        -CompressionAlgorithm algorithm
        -int compressionLevel
        -File sourceFile
        -File targetFile
        +CompressionSettings(CompressionAlgorithm algorithm, int compressionLevel, File sourceFile, File targetFile)
        +getAlgorithm() CompressionAlgorithm
        +getCompressionLevel() int
        +getSourceFile() File
        +getTargetFile() File
    }

    %% Enum for compression algorithms
    class CompressionAlgorithm {
        <<enumeration>>
        ZIP
        GZIP
        BZIP2
        -String extension
        -String displayName
        -String identifier
        +getExtension() String
        +getDisplayName() String
        +getIdentifier() String
        +toString() String
        +fromExtension(String extension) CompressionAlgorithm
    }

    %% Controller classes
    class MainController {
        -CompressionService compressionService
        -DecompressionService decompressionService
        -ProgressBar progressBar
        -Label statusLabel
        -ComboBox algorithmComboBox
        -Slider compressionLevelSlider
        +initialize() void
        +handleFileSelect() void
        +handleDragDropped() void
        +handleCompress() void
        +handleDecompress() void
        +updateProgress(double progress) void
        +updateStatus(String status) void
    }

    %% Service classes
    class CompressionService {
        -CompressionTask activeTask
        +compress(CompressionSettings settings) void
        +cancelCompression() void
        +setOnProgressUpdated(Consumer<Double> callback) void
        +setOnCompleted(Consumer<File> callback) void
        +setOnFailed(Consumer<Throwable> callback) void
    }

    class DecompressionService {
        -DecompressionTask activeTask
        +decompress(File compressedFile, File outputDirectory) void
        +cancelDecompression() void
        +setOnProgressUpdated(Consumer<Double> callback) void
        +setOnCompleted(Consumer<File> callback) void
        +setOnFailed(Consumer<Throwable> callback) void
    }

    %% Background task classes
    class CompressionTask {
        -CompressionSettings settings
        +call() File
        -compressFile() File
        -compressDirectory() File
        -updateProgress(long bytesProcessed, long totalBytes) void
    }

    class DecompressionTask {
        -File compressedFile
        -File outputDirectory
        +call() File
        -extractZipFile() File
        -extractGzipFile() File
        -extractBzip2File() File
        -updateProgress(long bytesProcessed, long totalBytes) void
    }

    %% Utility classes
    class FileUtils {
        +getFileInfo(File file) FileInfo
        +formatFileSize(long size) String
        +isCompressedFile(File file) boolean
        +isCompressibleFile(File file) boolean
        +getExtension(File file) String
        +isSupportedForCompression(File file, CompressionAlgorithm algorithm) boolean
        +determineAlgorithm(File file) CompressionAlgorithm
        +getDecompressionOutputPath(File compressedFile) String
        -calculateDirectorySize(File directory) long
        +getFileType(File file) String
    }

    %% Relationships
    JavaFileCompressorApp --> MainController : creates
    MainController --> CompressionService : uses
    MainController --> DecompressionService : uses
    CompressionService --> CompressionTask : creates
    DecompressionService --> DecompressionTask : creates
    CompressionTask --> FileUtils : uses
    DecompressionTask --> FileUtils : uses
    CompressionTask ..> FileInfo : creates
    CompressionTask --> CompressionSettings : uses
    CompressionSettings --> CompressionAlgorithm : uses
    MainController --> FileInfo : uses
    FileUtils ..> FileInfo : creates
    FileUtils --> CompressionAlgorithm : uses
```

## Diagram Explanation

The diagram represents the Java File Compressor application architecture with the following components:

1. **JavaFileCompressorApp**: The main application class that initializes the JavaFX UI.

2. **Model Classes**:

   - `FileInfo`: Stores information about files being processed
   - `CompressionSettings`: Configuration for compression operations
   - `CompressionAlgorithm`: Enum representing supported compression algorithms (ZIP, GZIP, BZIP2)

3. **Controller Classes**:

   - `MainController`: Handles UI events and coordinates between the view and services

4. **Service Classes**:

   - `CompressionService`: Manages compression operations
   - `DecompressionService`: Manages decompression operations

5. **Background Task Classes**:

   - `CompressionTask`: Performs actual compression work in the background
   - `DecompressionTask`: Performs actual decompression work in the background

6. **Utility Classes**:
   - `FileUtils`: Common file handling utilities used throughout the application

The arrows in the diagram show the relationships between classes, such as which classes create or use other classes.
