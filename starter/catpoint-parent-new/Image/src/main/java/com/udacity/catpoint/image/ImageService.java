package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;

/**
 * Interface for image recognition services that can identify cats in images.
 */
public interface ImageService {
    /**
     * Returns true if the provided image contains a cat.
     * @param image Image to scan
     * @param confidenceThreshold Minimum threshold to consider for cat detection
     * @return true if a cat is detected, false otherwise
     */
    boolean imageContainsCat(BufferedImage image, float confidenceThreshold);
}