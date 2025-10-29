package com.udacity.catpoint.security.application;

import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.service.SecurityService;
import com.udacity.catpoint.security.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** 
 * Visual surveillance interface component that displays camera feed and enables image analysis.
 * This panel provides users with the ability to simulate camera input through image uploads
 * and trigger computer vision analysis for threat detection.
 */
public class ImagePanel extends JPanel implements StatusListener {
    private SecurityService monitoringService;

    private JLabel surveillanceHeader;
    private JLabel imageDisplayArea;
    private BufferedImage activeCameraFrame;

    private int DISPLAY_WIDTH = 300;
    private int DISPLAY_HEIGHT = 225;

    public ImagePanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());
        this.monitoringService = securityService;
        securityService.addStatusListener(this);

        surveillanceHeader = new JLabel("Visual Surveillance Monitor");
        surveillanceHeader.setFont(StyleService.HEADING_FONT);

        imageDisplayArea = new JLabel();
        imageDisplayArea.setBackground(Color.WHITE);
        imageDisplayArea.setPreferredSize(new Dimension(DISPLAY_WIDTH, DISPLAY_HEIGHT));
        imageDisplayArea.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        // Interactive control for simulating camera feed updates
        JButton updateImageButton = new JButton("Update Surveillance Feed");
        updateImageButton.addActionListener(e -> {
            JFileChooser imageSelector = new JFileChooser();
            imageSelector.setCurrentDirectory(new File("."));
            imageSelector.setDialogTitle("Select Surveillance Image");
            imageSelector.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if(imageSelector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            try {
                activeCameraFrame = ImageIO.read(imageSelector.getSelectedFile());
                Image scaledImage = new ImageIcon(activeCameraFrame).getImage();
                imageDisplayArea.setIcon(new ImageIcon(scaledImage.getScaledInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, Image.SCALE_SMOOTH)));
            } catch (IOException |NullPointerException imageError) {
                JOptionPane.showMessageDialog(null, "Unable to process selected image file.");
            }
            repaint();
        });

        // Control for triggering computer vision analysis
        JButton analyzeImageButton = new JButton("Analyze for Threats");
        analyzeImageButton.addActionListener(e -> {
            monitoringService.processImage(activeCameraFrame);
        });

        add(surveillanceHeader, "span 3, wrap");
        add(imageDisplayArea, "span 3, wrap");
        add(updateImageButton);
        add(analyzeImageButton);
    }

    @Override
    public void notify(AlarmStatus status) {
        // Status updates handled through other notification channels
    }

    @Override
    public void catDetected(boolean catDetected) {
        if(catDetected) {
            surveillanceHeader.setText("⚠️ THREAT DETECTED - FELINE PRESENCE CONFIRMED");
        } else {
            surveillanceHeader.setText("Visual Surveillance Monitor - Area Secure");
        }
    }

    @Override
    public void sensorStatusChanged() {
        // Sensor state changes don't require visual updates in this component
    }
}