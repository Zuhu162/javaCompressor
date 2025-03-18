package com.javacompressor;

import com.javacompressor.compression.CompressionAlgorithm;
import com.javacompressor.compression.CompressionService;
import com.javacompressor.compression.CompressionTask;
import com.javacompressor.model.FileInfo;
import com.javacompressor.util.FileUtils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller class for the main view of the application.
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @FXML private Button selectFileButton;
    @FXML private Button compressButton;
    @FXML private Button decompressButton;
    @FXML private Button cancelButton;
    @FXML private ComboBox<CompressionAlgorithm> algorithmComboBox;
    @FXML private Slider compressionLevelSlider;
    @FXML private Label compressionLevelLabel;
    @FXML private CheckBox preserveStructureCheckbox;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Label fileNameLabel;
    @FXML private Label fileSizeLabel;
    @FXML private Label fileTypeLabel;
    @FXML private VBox dropZone;
    @FXML private TextArea compressionStatsArea;
    
    private final ObjectProperty<FileInfo> selectedFile = new SimpleObjectProperty<>();
    private final BooleanProperty processingActive = new SimpleBooleanProperty(false);
    private final BooleanProperty isCompressedFile = new SimpleBooleanProperty(false);
    private final CompressionService compressionService = new CompressionService();
    private Stage stage;
    private CompressionTask currentTask;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    /**
     * Initializes the controller after FXML is loaded.
     */
    @FXML
    public void initialize() {
        // Set up algorithm combo box
        algorithmComboBox.getItems().addAll(CompressionAlgorithm.values());
        algorithmComboBox.getSelectionModel().select(CompressionAlgorithm.ZIP);
        
        // Set up compression level slider
        compressionLevelSlider.setMin(1);
        compressionLevelSlider.setMax(9);
        compressionLevelSlider.setValue(5);
        compressionLevelSlider.setShowTickLabels(true);
        compressionLevelSlider.setShowTickMarks(true);
        compressionLevelSlider.setMajorTickUnit(1);
        compressionLevelSlider.setMinorTickCount(0);
        compressionLevelSlider.setSnapToTicks(true);
        
        // Bind compression level label to slider value
        compressionLevelLabel.textProperty().bind(
                Bindings.format("Compression Level: %.0f", compressionLevelSlider.valueProperty())
        );
        
        // Set up drag and drop for files
        setupDragAndDrop();
        
        // Set up button bindings and event handlers
        setupButtonBindings();
        
        // Clear initial file info
        updateFileInfo(null);
        
        // Initialize status
        statusLabel.setText("Ready");
        progressBar.setProgress(0);
    }
    
    /**
     * Sets the primary stage.
     * 
     * @param stage The primary stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Sets up drag and drop functionality for the drop zone.
     */
    private void setupDragAndDrop() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
    }
    
    /**
     * Handles drag over events for the drop zone.
     * 
     * @param event The drag event
     */
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }
    
    /**
     * Handles drag dropped events for the drop zone.
     * 
     * @param event The drag event
     */
    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;
        
        if (dragboard.hasFiles() && !dragboard.getFiles().isEmpty()) {
            File file = dragboard.getFiles().get(0);
            handleSelectedFile(file);
            success = true;
        }
        
        event.setDropCompleted(success);
        event.consume();
    }
    
    /**
     * Sets up bindings for buttons based on selected file and task status.
     */
    private void setupButtonBindings() {
        // Bind compress and decompress buttons based on file selection, compression status, and processing state
        compressButton.disableProperty().bind(
            Bindings.or(selectedFile.isNull(), 
                        Bindings.or(isCompressedFile, processingActive))
        );
        
        decompressButton.disableProperty().bind(
            Bindings.or(selectedFile.isNull(),
                        Bindings.or(Bindings.not(isCompressedFile), processingActive))
        );
        
        // Bind compression options based on compressed file status and processing state
        BooleanProperty optionsDisableBinding = new SimpleBooleanProperty();
        optionsDisableBinding.bind(Bindings.or(isCompressedFile, processingActive));
        
        algorithmComboBox.disableProperty().bind(optionsDisableBinding);
        compressionLevelSlider.disableProperty().bind(optionsDisableBinding);
        preserveStructureCheckbox.disableProperty().bind(optionsDisableBinding);
        
        // Bind cancel button to active processing state
        cancelButton.disableProperty().bind(Bindings.not(processingActive));
        
        // Set up button actions
        selectFileButton.setOnAction(event -> selectFile());
        compressButton.setOnAction(event -> compressFile());
        decompressButton.setOnAction(event -> decompressFile());
        cancelButton.setOnAction(event -> cancelTask());
        
        // Initially disable processing-dependent controls 
        processingActive.set(false);
    }
    
    /**
     * Opens a file chooser dialog to select a file.
     */
    @FXML
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            handleSelectedFile(file);
        }
    }
    
    /**
     * Processes the selected file.
     * 
     * @param file The selected file
     */
    private void handleSelectedFile(File file) {
        try {
            FileInfo fileInfo = FileUtils.getFileInfo(file);
            selectedFile.set(fileInfo);
            updateFileInfo(fileInfo);
            
            // Update compressed file status
            boolean isCompressed = FileUtils.isCompressedFile(file);
            isCompressedFile.set(isCompressed);
            
            logger.info("Selected file: {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error processing selected file", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to process selected file", e.getMessage());
        }
    }
    
    /**
     * Updates the file information displayed in the UI.
     * 
     * @param fileInfo The file information to display, or null to clear
     */
    private void updateFileInfo(FileInfo fileInfo) {
        if (fileInfo == null) {
            fileNameLabel.setText("No file selected");
            fileSizeLabel.setText("");
            fileTypeLabel.setText("");
            compressionStatsArea.clear();
        } else {
            fileNameLabel.setText("Name: " + fileInfo.getName());
            fileSizeLabel.setText("Size: " + FileUtils.formatFileSize(fileInfo.getSize()));
            fileTypeLabel.setText("Type: " + fileInfo.getType());
        }
    }
    
    /**
     * Compresses the selected file.
     */
    @FXML
    private void compressFile() {
        FileInfo fileInfo = selectedFile.get();
        if (fileInfo == null) {
            return;
        }
        
        try {
            File sourceFile = fileInfo.getFile();
            CompressionAlgorithm algorithm = algorithmComboBox.getValue();
            int compressionLevel = (int) compressionLevelSlider.getValue();
            boolean preserveStructure = preserveStructureCheckbox.isSelected();
            
            // Create output file
            String outputPath = sourceFile.getAbsolutePath() + "." + algorithm.getExtension();
            File outputFile = new File(outputPath);
            
            startCompressionTask(true, sourceFile, outputFile, algorithm, compressionLevel, preserveStructure);
        } catch (Exception e) {
            logger.error("Error starting compression", e);
            showAlert(Alert.AlertType.ERROR, "Compression Error", "Failed to compress file", e.getMessage());
        }
    }
    
    /**
     * Decompresses the selected file.
     */
    @FXML
    private void decompressFile() {
        FileInfo fileInfo = selectedFile.get();
        if (fileInfo == null) {
            return;
        }
        
        try {
            File sourceFile = fileInfo.getFile();
            
            // Determine output path (remove extension from compressed file)
            String outputPath = FileUtils.getDecompressionOutputPath(sourceFile);
            File outputFile = new File(outputPath);
            
            // Determine algorithm based on file extension
            CompressionAlgorithm algorithm = FileUtils.determineAlgorithm(sourceFile);
            
            startCompressionTask(false, sourceFile, outputFile, algorithm, 0, false);
        } catch (Exception e) {
            logger.error("Error starting decompression", e);
            showAlert(Alert.AlertType.ERROR, "Decompression Error", "Failed to decompress file", e.getMessage());
        }
    }
    
    /**
     * Starts a compression or decompression task.
     * 
     * @param compress Whether this is a compression (true) or decompression (false) task
     * @param sourceFile The source file
     * @param outputFile The output file
     * @param algorithm The compression algorithm
     * @param compressionLevel The compression level (for compression only)
     * @param preserveStructure Whether to preserve directory structure (for compression only)
     */
    private void startCompressionTask(boolean compress, File sourceFile, File outputFile,
                                      CompressionAlgorithm algorithm, int compressionLevel,
                                      boolean preserveStructure) {
        // Create and configure the task
        currentTask = new CompressionTask(compress, sourceFile, outputFile, 
                                         algorithm, compressionLevel, preserveStructure);
        
        // Bind task properties to UI
        progressBar.progressProperty().bind(currentTask.progressProperty());
        statusLabel.textProperty().bind(currentTask.messageProperty());
        
        // Set processing active state
        processingActive.set(true);
        
        // Set up task completion handlers
        currentTask.setOnSucceeded(event -> {
            handleTaskCompletion(true, currentTask.getValue());
        });
        
        currentTask.setOnFailed(event -> {
            handleTaskCompletion(false, null);
            Throwable exception = currentTask.getException();
            logger.error("Task failed", exception);
            
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Operation Failed", 
                          "The operation could not be completed", 
                          exception != null ? exception.getMessage() : "Unknown error");
            });
        });
        
        currentTask.setOnCancelled(event -> {
            handleTaskCompletion(false, null);
            logger.info("Task cancelled");
            
            Platform.runLater(() -> {
                statusLabel.setText("Operation cancelled");
            });
        });
        
        // Start the task
        executorService.submit(currentTask);
    }
    
    /**
     * Handles task completion, updating UI elements and displaying results.
     * 
     * @param success Whether the task completed successfully
     * @param result The task result
     */
    private void handleTaskCompletion(boolean success, Double compressionRatio) {
        // Unbind properties
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        
        // Update UI
        Platform.runLater(() -> {
            processingActive.set(false);
            
            if (success && compressionRatio != null) {
                progressBar.setProgress(1.0);
                statusLabel.setText("Operation completed successfully");
                
                // Update compression stats
                updateCompressionStats(compressionRatio);
            } else {
                progressBar.setProgress(0);
                if (!statusLabel.getText().contains("cancelled")) {
                    statusLabel.setText("Operation failed");
                }
            }
        });
        
        currentTask = null;
    }
    
    /**
     * Updates the compression statistics display.
     * 
     * @param compressionRatio The compression ratio
     */
    private void updateCompressionStats(double compressionRatio) {
        FileInfo fileInfo = selectedFile.get();
        if (fileInfo != null) {
            String stats = String.format(
                "Original size: %s\n" +
                "Compression ratio: %.2f%%\n" +
                "Space saved: %.2f%%",
                FileUtils.formatFileSize(fileInfo.getSize()),
                compressionRatio * 100,
                (1 - compressionRatio) * 100
            );
            compressionStatsArea.setText(stats);
        }
    }
    
    /**
     * Cancels the current compression/decompression task.
     */
    @FXML
    private void cancelTask() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
            logger.info("Task cancellation requested");
        }
    }
    
    /**
     * Shows an alert dialog with the specified information.
     * 
     * @param alertType The type of alert
     * @param title The alert title
     * @param header The alert header text
     * @param content The alert content text
     */
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Cleans up resources when the controller is no longer needed.
     */
    public void shutdown() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
        
        executorService.shutdownNow();
    }
} 