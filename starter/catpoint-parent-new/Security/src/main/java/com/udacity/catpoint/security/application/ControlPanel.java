package com.udacity.catpoint.security.application;

import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.service.SecurityService;
import com.udacity.catpoint.security.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interactive control interface for managing security system operational modes.
 * This panel provides user-friendly access to system arming configurations
 * with visual feedback for the current operational state.
 */
public class ControlPanel extends JPanel {

    private SecurityService monitoringService;
    private Map<ArmingStatus, JButton> operationalControls;


    public ControlPanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());
        this.monitoringService = securityService;

        JLabel controlSectionTitle = new JLabel("Security System Controls");
        controlSectionTitle.setFont(StyleService.HEADING_FONT);

        add(controlSectionTitle, "span 3, wrap");

        // Create interactive controls for each operational mode
        operationalControls = Arrays.stream(ArmingStatus.values())
                .collect(Collectors.toMap(mode -> mode, mode -> new JButton(mode.getDescription())));

        // Configure each control with appropriate behavior and visual feedback
        operationalControls.forEach((mode, control) -> {
            control.addActionListener(e -> {
                monitoringService.setArmingStatus(mode);
                operationalControls.forEach((status, button) -> 
                    button.setBackground(status == mode ? status.getColor() : null));
            });
        });

        // Add controls in consistent order for better user experience
        Arrays.stream(ArmingStatus.values()).forEach(mode -> add(operationalControls.get(mode)));

        ArmingStatus activeMode = monitoringService.getArmingStatus();
        operationalControls.get(activeMode).setBackground(activeMode.getColor());


    }
}