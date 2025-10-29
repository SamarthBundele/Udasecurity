package com.udacity.catpoint.image.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Cloud-based computer vision service leveraging AWS Rekognition for feline detection.
 * This implementation provides robust image analysis capabilities for security applications.
 * 
 * Configuration Requirements:
 * - AWS credentials must be configured in config.properties
 * - Required IAM permissions: AmazonRekognitionFullAccess
 * - Configuration file should contain: aws.id, aws.secret, aws.region
 * 
 * Setup Process:
 * 1. Access AWS Console and navigate to Identity and Access Management (IAM)
 * 2. Create new user with programmatic access credentials
 * 3. Attach AmazonRekognitionFullAccess policy to the user
 * 4. Configure config.properties with the generated access credentials
 */
public class AwsImageService implements ImageService {

    private Logger logger = LoggerFactory.getLogger(AwsImageService.class);

    // Singleton pattern for AWS client - recommended for performance optimization
    private static RekognitionClient visionAnalysisClient;

    public AwsImageService() {
        Properties configurationProperties = new Properties();
        try (InputStream configStream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            configurationProperties.load(configStream);
        } catch (IOException configurationError) {
            logger.error("Failed to initialize cloud vision service - configuration file not accessible", configurationError);
            return;
        }

        String accessKeyId = configurationProperties.getProperty("aws.id");
        String secretAccessKey = configurationProperties.getProperty("aws.secret");
        String serviceRegion = configurationProperties.getProperty("aws.region");

        AwsCredentials cloudCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        visionAnalysisClient = RekognitionClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(cloudCredentials))
                .region(Region.of(serviceRegion))
                .build();
    }

    /**
     * Analyzes the provided image to determine feline presence using cloud-based computer vision.
     * This method leverages advanced machine learning models to provide accurate detection results.
     * @param image The image data to analyze for feline presence
     * @param confidenceThreshold Minimum confidence level required for positive detection (0-100)
     * @return true if feline presence is detected above the confidence threshold, false otherwise
     */
    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {
        Image cloudVisionImage = null;
        try (ByteArrayOutputStream imageDataStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", imageDataStream);
            cloudVisionImage = Image.builder().bytes(SdkBytes.fromByteArray(imageDataStream.toByteArray())).build();
        } catch (IOException imageProcessingError) {
            logger.error("Failed to process image data for analysis", imageProcessingError);
            return false;
        }
        DetectLabelsRequest analysisRequest = DetectLabelsRequest.builder()
                .image(cloudVisionImage)
                .minConfidence(confidenceThreshold)
                .build();
        DetectLabelsResponse analysisResults = visionAnalysisClient.detectLabels(analysisRequest);
        logDetectionResults(analysisResults);
        return analysisResults.labels().stream()
                .anyMatch(label -> label.name().toLowerCase().contains("cat"));
    }

    private void logDetectionResults(DetectLabelsResponse response) {
        logger.info("Vision analysis detected: " + response.labels().stream()
                .map(detectedLabel -> String.format("%s(%.1f%%)", detectedLabel.name(), detectedLabel.confidence()))
                .collect(Collectors.joining(", ")));
    }
}