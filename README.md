# Java File Compressor

A desktop app designed to compress and decompress files with various algorithms. Super simple to use!

![Java File Compressor](docs/images/screenshot.png)

## What it does

### Core Features

- **Multiple Algorithms:** Supports ZIP, GZIP, and BZIP2
- **Compression Control:** Adjust levels (1-9) for speed vs. size tradeoffs
- **Versatile:** Handles individual files or entire folders
- **Live Progress:** See compression happening in real-time
- **Drag & Drop:** Just drag files right onto the app
- **Stats:** Shows how much space you saved

### Interface

- **Clean UI:** Simple, intuitive design that just works
- **Visual Progress:** See exactly how far along your compression is
- **File Details:** Check size and type before compressing
- **Smart Errors:** Helpful messages when things don't work out

## Getting Started

### What You'll Need

- Java JDK 17+ (sorry, needs the newer Java features)
- Maven (only if building from source)

### Running It

1. Download the latest JAR from the releases page
2. Double-click or run: `java -jar java-file-compressor-1.0-SNAPSHOT.jar`

### Building It Yourself

```bash
git clone https://github.com/yourusername/java-file-compressor.git
cd java-file-compressor
mvn clean package
java -jar target/java-file-compressor-1.0-SNAPSHOT.jar
```

## How It Works

### Tech Stack

- **Java 17:** Base language with newer features
- **JavaFX:** For the UI (much nicer than Swing!)
- **Apache Commons Compress:** Handles the actual compression magic
- **Maven:** Dependencies and build process
- **SLF4J/Logback:** For logging when things go wrong

### Behind the Scenes

The app uses a pretty standard architecture:

- **Model:** Data structures for files and compression settings
- **View:** JavaFX UI components
- **Controller:** Handles what happens when you click things
- **Service:** Background work for compression/decompression

See the detailed [class diagram](docs/class-diagram.md) for a visual representation of how everything fits together.

### Algorithm Details

- **ZIP:** Classic format everyone knows - good compatibility
- **GZIP:** Faster compression, decent ratios - great for most files
- **BZIP2:** Better compression but slower - best for text files

## Quick Examples

### Compressing

1. Select a file or drag it in
2. Pick your algorithm (ZIP is usually fine)
3. Set compression level (9 for smallest size, 1 for speed)
4. Hit "Compress" and wait for the magic

### Decompressing

1. Select a compressed file
2. Click "Decompress"
3. That's it!

## What I Learned

This project taught me a ton about:

- **Java File Handling:** Working with the File API, recursively processing directories, and handling various file types
- **Stream Processing:** Using Java's input/output streams with buffering for efficient file operations
- **Compression Algorithms:** Understanding the tradeoffs between different compression methods:
  - Dictionary-based vs statistical modeling approaches
  - How compression levels affect memory usage and CPU requirements
  - When to choose specific algorithms based on file content type
- **JavaFX:** Building responsive UIs with background tasks that don't freeze the interface
- **Error Handling:** Gracefully managing file access permissions, disk space issues, and corrupted archives
- **Progress Tracking:** Calculating and displaying accurate progress for lengthy operations
- **Drag & Drop:** Implementing intuitive user interactions using Java's DnD API

The trickiest parts were calculating accurate file sizes for nested directories and maintaining a responsive UI during long compression tasks. Had to learn about Platform.runLater() and Task classes in JavaFX to keep everything smooth!

## License

This project is under the MIT License - see the LICENSE file for details.

## Built with

- Apache Commons Compress for the compression algorithms
- JavaFX team for the UI framework
- Everyone who's tested and provided feedback
