package com.udacity.catpoint.security.application;

import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.service.SecurityService;
import com.udacity.catpoint.security.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Primary status visualization component that presents real-time security system information.
 * This panel implements the observer pattern to receive and display security state changes
 * as they occur throughout the system lifecycle.
 */
public class DisplayPanel extends JPanel implements StatusListener {

    private JLabel activeStatusIndicator;

    public DisplayPanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());

        securityService.addStatusListener(this);

        JLabel applicationTitle = new JLabel("SecureHome Pro - Advanced Security Monitoring");
        JLabel statusDescriptor = new JLabel("Current Security Status:");
        activeStatusIndicator = new JLabel();

        applicationTitle.setFont(StyleService.HEADING_FONT);

        notify(securityService.getAlarmStatus());

        add(applicationTitle, "span 2, wrap");
        add(statusDescriptor);
        add(activeStatusIndicator, "wrap");

    }

    @Override
    public void notify(AlarmStatus status) {
        activeStatusIndicator.setText(status.getDescription());
        activeStatusIndicator.setBackground(status.getColor());
        activeStatusIndicator.setOpaque(true);
    }

    @Override
    public void catDetected(boolean catDetected) {
        // Visual updates handled by status changes - no additional action required
    }

    @Override
    public void sensorStatusChanged() {
        // Status display updates automatically via alarm status changes
    }
}