package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.image.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Core security orchestration service that manages system state transitions and threat detection.
 * 
 * This service acts as the central coordinator for all security-related operations, processing
 * sensor inputs, camera feeds, and user commands to maintain optimal security posture.
 * It implements a state machine pattern to handle complex security scenarios and notifications.
 */
public class SecurityService {

    private ImageService visionAnalysisService;
    private SecurityRepository persistenceLayer;
    private Set<StatusListener> eventSubscribers = new HashSet<>();
    private boolean felinePresenceDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.persistenceLayer = securityRepository;
        this.visionAnalysisService = imageService;
    }

    /**
     * Updates the system's operational mode and triggers appropriate security protocols.
     * This method orchestrates state transitions and ensures proper sensor management
     * based on the selected security configuration.
     * @param armingStatus The desired security operational mode
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // When system becomes active, initialize all sensors to baseline state
            // Create a copy to avoid ConcurrentModificationException
            Set<Sensor> sensorsCopy = new HashSet<>(persistenceLayer.getSensors());
            for (Sensor sensor : sensorsCopy) {
                if (sensor.getActive()) {
                    sensor.setActive(false);
                    persistenceLayer.updateSensor(sensor);
                }
            }
        }
        persistenceLayer.setArmingStatus(armingStatus);
        
        // Special handling for home mode with detected feline presence
        if(armingStatus == ArmingStatus.ARMED_HOME && felinePresenceDetected) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Processes feline detection events and adjusts security posture accordingly.
     * This method implements intelligent threat assessment based on visual analysis
     * and current system configuration.
     * @param felineDetected True when feline presence is confirmed, false otherwise
     */
    private void processFelineDetection(Boolean felineDetected) {
        felinePresenceDetected = felineDetected;
        
        if(felineDetected && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            // Activate alarm when pet detected in home protection mode
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!felineDetected && getArmingStatus() != ArmingStatus.DISARMED) {
            // Clear alarm if no pet detected and no other sensors triggered
            if (!anySensorActive()) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }

        eventSubscribers.forEach(subscriber -> subscriber.catDetected(felineDetected));
    }

    /**
     * Subscribes a component to receive security system event notifications.
     * This enables decoupled communication between the service and UI components.
     * @param statusListener The component to receive event notifications
     */
    public void addStatusListener(StatusListener statusListener) {
        eventSubscribers.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        eventSubscribers.remove(statusListener);
    }

    /**
     * Updates the system alarm state and broadcasts the change to all subscribers.
     * This method ensures consistent state management across the application.
     * @param status The new alarm status to be applied
     */
    public void setAlarmStatus(AlarmStatus status) {
        persistenceLayer.setAlarmStatus(status);
        eventSubscribers.forEach(subscriber -> subscriber.notify(status));
    }

    /**
     * Evaluates whether any monitoring device is currently detecting activity.
     * This utility method supports threat assessment logic throughout the service.
     */
    private boolean anySensorActive() {
        return getSensors().stream().anyMatch(Sensor::getActive);
    }

    /**
     * Processes sensor activation events and escalates security alerts appropriately.
     * Implements graduated response based on current system state.
     */
    private void processSensorActivation() {
        if(persistenceLayer.getArmingStatus() == ArmingStatus.DISARMED) {
            return; // System inactive - no response needed
        }
        switch(persistenceLayer.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Handles sensor deactivation events and adjusts security posture accordingly.
     * Implements intelligent de-escalation while maintaining security integrity.
     */
    private void processSensorDeactivation() {
        switch(persistenceLayer.getAlarmStatus()) {
            case PENDING_ALARM -> {
                // Return to normal state if all monitoring devices are quiet
                if (!anySensorActive()) {
                    setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            }
            // Active alarms remain unchanged by individual sensor state changes
            case ALARM -> {
                // Maintain heightened security posture regardless of sensor changes
            }
        }
    }

    /**
     * Updates the operational state of a monitoring device and triggers appropriate security responses.
     * This method orchestrates the complex state machine logic for threat detection and response.
     * @param sensor The monitoring device to update
     * @param active The new operational state for the device
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus currentThreatLevel = getAlarmStatus();
        ArmingStatus currentOperationalMode = getArmingStatus();
        
        // During active alarm conditions, sensor changes don't affect overall state
        if (currentThreatLevel == AlarmStatus.ALARM) {
            sensor.setActive(active);
            persistenceLayer.updateSensor(sensor);
            return;
        }
        
        // Process sensor becoming active
        if(!sensor.getActive() && active) {
            // Escalate from pending to full alarm if additional sensors trigger
            if (currentThreatLevel == AlarmStatus.PENDING_ALARM) {
                setAlarmStatus(AlarmStatus.ALARM);
            } else {
                processSensorActivation();
            }
        } 
        // Process sensor becoming inactive
        else if (sensor.getActive() && !active) {
            sensor.setActive(active);
            persistenceLayer.updateSensor(sensor);
            processSensorDeactivation();
            return;
        }
        // No state change for sensors that remain in their current state
        
        sensor.setActive(active);
        persistenceLayer.updateSensor(sensor);
    }

    /**
     * Analyzes camera feed for potential security threats and updates system state accordingly.
     * This method leverages computer vision services to enhance traditional sensor-based security.
     * @param currentCameraImage The image frame to analyze for threats
     */
    public void processImage(BufferedImage currentCameraImage) {
        processFelineDetection(visionAnalysisService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return persistenceLayer.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return persistenceLayer.getSensors();
    }

    public void addSensor(Sensor sensor) {
        persistenceLayer.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        persistenceLayer.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return persistenceLayer.getArmingStatus();
    }
}