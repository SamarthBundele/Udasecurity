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
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean catCurrentlyDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // Requirement 10: If the system is armed, reset all sensors to inactive
            getSensors().forEach(sensor -> {
                sensor.setActive(false);
                securityRepository.updateSensor(sensor);
            });
        }
        securityRepository.setArmingStatus(armingStatus);
        
        // Requirement 11: If the system is armed-home while the camera shows a cat, set the alarm status to alarm
        if(armingStatus == ArmingStatus.ARMED_HOME && catCurrentlyDetected) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catCurrentlyDetected = cat;
        
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            // Requirement 7: If the camera image contains a cat while the system is armed-home, put the system into alarm status
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && getArmingStatus() != ArmingStatus.DISARMED) {
            // Requirement 8: If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active
            if (!anySensorActive()) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Helper method to check if any sensor is currently active
     */
    private boolean anySensorActive() {
        return getSensors().stream().anyMatch(Sensor::getActive);
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> {
                // Requirement 3: If pending alarm and all sensors are inactive, return to no alarm state
                if (!anySensorActive()) {
                    setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            }
            // Requirement 4: If alarm is active, change in sensor state should not affect the alarm state
            case ALARM -> {
                // Do nothing - alarm state should not change when sensors are deactivated
            }
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus currentAlarmStatus = getAlarmStatus();
        ArmingStatus currentArmingStatus = getArmingStatus();
        
        // Requirement 4: If alarm is active, change in sensor state should not affect the alarm state
        if (currentAlarmStatus == AlarmStatus.ALARM) {
            sensor.setActive(active);
            securityRepository.updateSensor(sensor);
            return;
        }
        
        // Handle sensor activation
        if(!sensor.getActive() && active) {
            // Requirement 5: If a sensor is activated while already active and the system is in pending state, change it to alarm state
            if (currentAlarmStatus == AlarmStatus.PENDING_ALARM) {
                setAlarmStatus(AlarmStatus.ALARM);
            } else {
                handleSensorActivated();
            }
        } 
        // Handle sensor deactivation
        else if (sensor.getActive() && !active) {
            sensor.setActive(active);
            securityRepository.updateSensor(sensor);
            handleSensorDeactivated();
            return;
        }
        // Requirement 6: If a sensor is deactivated while already inactive, make no changes to the alarm state
        // This is handled by the else case - no action needed
        
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}