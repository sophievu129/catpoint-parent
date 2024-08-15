package com.udacity.catpoint.image.service;

import java.awt.image.BufferedImage;

public interface ImageService {
    /**
     * Analyzes the provided image and returns a result.
     *
     * @param image the image to analyze
     * @return true if the analysis is successful (e.g., detects a cat), false otherwise
     */
    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}