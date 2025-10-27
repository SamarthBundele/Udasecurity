package com.udacity.catpoint.security.application;

import com.udacity.catpoint.security.data.PretendDatabaseSecurityRepositoryImpl;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.service.SecurityService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Main application window that orchestrates the security monitoring interface.
 * 
 * This class serves as the primary container for all security system components,
 * managing the initialization and layout of monitoring panels. It follows a
 * composition-based architecture where each panel handles specific functionality.
 */
public class CatpointGui extends JFrame {
    private SecurityRepository dataRepository = new PretendDatabaseSecurityRepositoryImpl();
    private ImageService visionService = new FakeImageService();
    private SecurityService monitoringService = new SecurityService(dataRepository, visionService);
    private DisplayPanel statusDisplayPanel = new DisplayPanel(monitoringService);
    private ControlPanel systemControlPanel = new ControlPanel(monitoringService);
    private SensorPanel deviceManagementPanel = new SensorPanel(monitoringService);
    private ImagePanel cameraViewPanel = new ImagePanel(monitoringService);

    public CatpointGui() {
        setLocation(150, 120);
        setSize(650, 900);
        setTitle("SecureHome Pro - Advanced Security Monitoring");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel primaryContainer = new JPanel();
        primaryContainer.setLayout(new MigLayout());
        primaryContainer.add(statusDisplayPanel, "wrap");
        primaryContainer.add(cameraViewPanel, "wrap");
        primaryContainer.add(systemControlPanel, "wrap");
        primaryContainer.add(deviceManagementPanel);

        getContentPane().add(primaryContainer);

    }
}