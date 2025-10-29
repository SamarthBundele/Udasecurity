module com.udacity.catpoint.security {
    // Dependency on the image module
    requires com.udacity.catpoint.image;
    
    // Google libraries
    requires com.google.gson;
    requires com.google.common;
    
    // Swing/AWT for GUI
    requires java.desktop;
    requires java.prefs;
    
    // Logging
    requires org.slf4j;
    
    // MigLayout for GUI layouts (proper module name)
    requires com.miglayout.swing;
    
    // Security module should NOT export any packages - it's the consumer, not provider
    // No exports needed as this is the final application module
    
    // Open the data package to Gson for serialization via reflection
    opens com.udacity.catpoint.security.data to com.google.gson;
}