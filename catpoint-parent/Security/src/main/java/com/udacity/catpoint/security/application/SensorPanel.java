package com.udacity.catpoint.security.application;

import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.security.data.SensorType;
import com.udacity.catpoint.security.service.SecurityService;
import com.udacity.catpoint.security.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Device management interface that enables users to configure and monitor security sensors.
 * This panel provides comprehensive sensor lifecycle management including registration,
 * activation control, and removal capabilities for testing and operational use.
 */
public class SensorPanel extends JPanel implements StatusListener {

    private SecurityService monitoringService;

    private JLabel managementTitle = new JLabel("Device Management Console");
    private JLabel deviceNameLabel = new JLabel("Device Name:");
    private JLabel deviceTypeLabel = new JLabel("Device Type:");
    private JTextField deviceNameInput = new JTextField();
    private JComboBox deviceTypeSelector = new JComboBox(SensorType.values());
    private JButton registerDeviceButton = new JButton("Register New Device");

    private JPanel deviceRegistryPanel;
    private JPanel deviceRegistrationPanel;

    public SensorPanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());
        this.monitoringService = securityService;
        securityService.addStatusListener(this);

        managementTitle.setFont(StyleService.HEADING_FONT);
        registerDeviceButton.addActionListener(e ->
                registerNewDevice(new Sensor(deviceNameInput.getText(),
                        SensorType.valueOf(deviceTypeSelector.getSelectedItem().toString()))));

        deviceRegistrationPanel = buildDeviceRegistrationInterface();
        deviceRegistryPanel = new JPanel();
        deviceRegistryPanel.setLayout(new MigLayout());

        refreshDeviceRegistry(deviceRegistryPanel);

        add(managementTitle, "wrap");
        add(deviceRegistrationPanel, "span");
        add(deviceRegistryPanel, "span");
    }

    /**
     * Constructs the device registration interface with input controls and submission capability.
     */
    private JPanel buildDeviceRegistrationInterface() {
        JPanel registrationInterface = new JPanel();
        registrationInterface.setLayout(new MigLayout());
        registrationInterface.add(deviceNameLabel);
        registrationInterface.add(deviceNameInput, "width 50:100:200");
        registrationInterface.add(deviceTypeLabel);
        registrationInterface.add(deviceTypeSelector, "wrap");
        registrationInterface.add(registerDeviceButton, "span 3");
        return registrationInterface;
    }

    /**
     * Retrieves the current device registry and populates the display panel with interactive controls.
     * Devices are presented in their registration order with status indicators and management options.
     * @param displayPanel The panel to populate with the current device registry
     */
    private void refreshDeviceRegistry(JPanel displayPanel) {
        displayPanel.removeAll();
        monitoringService.getSensors().stream().sorted().forEach(device -> {
            JLabel deviceStatusLabel = new JLabel(String.format("%s (%s): %s", 
                device.getName(), device.getSensorType().toString(), 
                (device.getActive() ? "ACTIVE" : "STANDBY")));
            JButton deviceToggleControl = new JButton((device.getActive() ? "Deactivate" : "Activate"));
            JButton deviceRemovalControl = new JButton("Unregister Device");

            deviceToggleControl.addActionListener(e -> updateDeviceStatus(device, !device.getActive()));
            deviceRemovalControl.addActionListener(e -> unregisterDevice(device));

            // Apply consistent sizing for professional appearance
            displayPanel.add(deviceStatusLabel, "width 300:300:300");
            displayPanel.add(deviceToggleControl, "width 100:100:100");
            displayPanel.add(deviceRemovalControl, "wrap");
        });

        repaint();
        revalidate();
    }

    /**
     * Updates a monitoring device's operational status and refreshes the registry display.
     * @param device The monitoring device to update
     * @param activeState The desired operational state for the device
     */
    private void updateDeviceStatus(Sensor device, Boolean activeState) {
        monitoringService.changeSensorActivationStatus(device, activeState);
        refreshDeviceRegistry(deviceRegistryPanel);
    }

    /**
     * Registers a new monitoring device with the security system and updates the display.
     * @param device The monitoring device to register
     */
    private void registerNewDevice(Sensor device) {
        if(monitoringService.getSensors().size() < 4) {
            monitoringService.addSensor(device);
            refreshDeviceRegistry(deviceRegistryPanel);
        } else {
            JOptionPane.showMessageDialog(null, "Device limit reached. Upgrade to SecureHome Pro Premium for unlimited devices!");
        }
    }

    /**
     * Removes a monitoring device from the security system and updates the registry display.
     * @param device The monitoring device to unregister
     */
    private void unregisterDevice(Sensor device) {
        monitoringService.removeSensor(device);
        refreshDeviceRegistry(deviceRegistryPanel);
    }

    @Override
    public void notify(AlarmStatus status) {
        // No behavior necessary for alarm status changes
    }

    @Override
    public void catDetected(boolean catDetected) {
        // No behavior necessary for cat detection changes
    }

    @Override
    public void sensorStatusChanged() {
        // Refresh the sensor display when sensor status changes
        refreshDeviceRegistry(deviceRegistryPanel);
    }
}