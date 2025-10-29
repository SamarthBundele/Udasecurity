module com.udacity.catpoint.image {
    // AWS SDK dependencies for image recognition
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    
    // Java base modules
    requires java.desktop;
    
    // Logging
    requires org.slf4j;
    
    // Export the image service package
    exports com.udacity.catpoint.image.service;
}