package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityService to verify all application requirements
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;
    
    @Mock
    private ImageService imageService;
    
    @Mock
    private StatusListener statusListener;

    private SecurityService securityService;
    private Sensor sensor1;
    private Sensor sensor2;
    private Set<Sensor> sensors;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        
        sensor1 = new Sensor("Door Sensor", SensorType.DOOR);
        sensor2 = new Sensor("Window Sensor", SensorType.WINDOW);
        
        sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
    }

    // Requirement 1: If alarm is armed and a sensor becomes activated, put the system into pending alarm status
    @Test
    void sensorActivated_systemArmedAndNoAlarm_setsPendingAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor1.setActive(false);
        
        securityService.changeSensorActivationStatus(sensor1, true);
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Requirement 2: If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm
    @Test
    void sensorActivated_systemArmedAndPendingAlarm_setsAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor1.setActive(false);
        
        securityService.changeSensorActivationStatus(sensor1, true);
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Requirement 3: If pending alarm and all sensors are inactive, return to no alarm state
    @Test
    void allSensorsInactive_systemPendingAlarm_setsNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensor1.setActive(true);
        sensor2.setActive(false);
        
        securityService.changeSensorActivationStatus(sensor1, false);
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Requirement 4: If alarm is active, change in sensor state should not affect the alarm state
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sensorStateChanged_alarmActive_noAlarmStatusChange(boolean sensorActive) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        
        securityService.changeSensorActivationStatus(sensor1, sensorActive);
        
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Requirement 5: If a sensor is activated while already active and the system is in pending state, change it to alarm state
    @Test
    void sensorActivated_alreadyActiveAndPending_setsAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor1.setActive(false);
        
        securityService.changeSensorActivationStatus(sensor1, true);
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Requirement 6: If a sensor is deactivated while already inactive, make no changes to the alarm state
    @Test
    void sensorDeactivated_alreadyInactive_noAlarmStatusChange() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor1.setActive(false);
        
        securityService.changeSensorActivationStatus(sensor1, false);
        
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Requirement 7: If the camera image contains a cat while the system is armed-home, put the system into alarm status
    @Test
    void imageProcessed_catDetectedAndArmedHome_setsAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Requirement 8: If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active
    @Test
    void imageProcessed_noCatDetectedAndNoActiveSensors_setsNoAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensor1.setActive(false);
        sensor2.setActive(false);
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Requirement 8 - Edge case: No cat detected but sensors are active
    @Test
    void imageProcessed_noCatDetectedButSensorsActive_noAlarmStatusChange() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensor1.setActive(true); // At least one sensor is active
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Requirement 9: If the system is disarmed, set the status to no alarm
    @Test
    void systemDisarmed_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Requirement 10: If the system is armed, reset all sensors to inactive
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_resetsAllSensorsToInactive(ArmingStatus armingStatus) {
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensor1.setActive(true);
        sensor2.setActive(true);
        
        securityService.setArmingStatus(armingStatus);
        
        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
        verify(securityRepository).updateSensor(sensor1);
        verify(securityRepository).updateSensor(sensor2);
    }

    // Requirement 11: If the system is armed-home while the camera shows a cat, set the alarm status to alarm
    @Test
    void systemArmedHome_catCurrentlyDetected_setsAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        // First detect cat
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        // Then arm home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Additional test for requirement 11: Cat detected while disarmed, then armed away should NOT trigger alarm
    @Test
    void systemArmedAway_catCurrentlyDetected_noAlarmStatusChange() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        // First detect cat while disarmed
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        // Then arm away - should NOT trigger alarm (only ARMED_HOME triggers alarm for cats)
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Additional tests for edge cases and full coverage

    @Test
    void addStatusListener_addsListener() {
        securityService.addStatusListener(statusListener);
        
        // Trigger a status change to verify listener is added
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(statusListener).catDetected(true);
    }

    @Test
    void removeStatusListener_removesListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
        
        // Trigger a status change to verify listener is removed
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(statusListener, never()).catDetected(anyBoolean());
    }

    @Test
    void addSensor_callsRepositoryAddSensor() {
        Sensor newSensor = new Sensor("Motion Sensor", SensorType.MOTION);
        
        securityService.addSensor(newSensor);
        
        verify(securityRepository).addSensor(newSensor);
    }

    @Test
    void removeSensor_callsRepositoryRemoveSensor() {
        securityService.removeSensor(sensor1);
        
        verify(securityRepository).removeSensor(sensor1);
    }

    @Test
    void getAlarmStatus_returnsRepositoryAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        
        AlarmStatus result = securityService.getAlarmStatus();
        
        assertEquals(AlarmStatus.PENDING_ALARM, result);
    }

    @Test
    void getSensors_returnsRepositorySensors() {
        when(securityRepository.getSensors()).thenReturn(sensors);
        
        Set<Sensor> result = securityService.getSensors();
        
        assertEquals(sensors, result);
    }

    @Test
    void getArmingStatus_returnsRepositoryArmingStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        
        ArmingStatus result = securityService.getArmingStatus();
        
        assertEquals(ArmingStatus.ARMED_AWAY, result);
    }

    // Test cat detection while system is disarmed
    @Test
    void imageProcessed_catDetectedButDisarmed_noAlarmStatusChange() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test cat detection while system is armed away
    @Test
    void imageProcessed_catDetectedAndArmedAway_noAlarmStatusChange() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test sensor deactivation during alarm state
    @Test
    void sensorDeactivated_alarmActive_noAlarmStatusChange() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor1.setActive(true);
        
        securityService.changeSensorActivationStatus(sensor1, false);
        
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test sensor deactivation during pending alarm with other sensors still active
    @Test
    void sensorDeactivated_pendingAlarmWithOtherSensorsActive_noAlarmStatusChange() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensor1.setActive(true);
        sensor2.setActive(true); // Other sensor still active
        
        securityService.changeSensorActivationStatus(sensor1, false);
        
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
}