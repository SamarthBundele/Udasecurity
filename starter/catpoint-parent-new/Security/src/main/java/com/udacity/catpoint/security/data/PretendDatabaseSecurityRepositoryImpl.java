package com.udacity.catpoint.security.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

/**
 * In-memory security data repository with persistent storage capabilities.
 * This implementation provides a lightweight data layer for demonstration and development,
 * utilizing Java Preferences API for state persistence across application sessions.
 * Note: This implementation prioritizes simplicity over testability.
 */
public class PretendDatabaseSecurityRepositoryImpl implements SecurityRepository{

    private Set<Sensor> monitoringDevices;
    private AlarmStatus currentThreatLevel;
    private ArmingStatus operationalMode;

    // Persistent storage configuration keys
    private static final String DEVICE_REGISTRY = "SENSORS";
    private static final String THREAT_LEVEL_STATE = "ALARM_STATUS";
    private static final String OPERATIONAL_MODE_STATE = "ARMING_STATUS";

    private static final Preferences persistentStorage = Preferences.userNodeForPackage(PretendDatabaseSecurityRepositoryImpl.class);
    private static final Gson jsonSerializer = new Gson(); // Handles object serialization for storage

    public PretendDatabaseSecurityRepositoryImpl() {
        // Initialize system state from persistent storage with sensible defaults
        currentThreatLevel = AlarmStatus.valueOf(persistentStorage.get(THREAT_LEVEL_STATE, AlarmStatus.NO_ALARM.toString()));
        operationalMode = ArmingStatus.valueOf(persistentStorage.get(OPERATIONAL_MODE_STATE, ArmingStatus.DISARMED.toString()));

        // Restore monitoring device registry from serialized storage
        // Note: JSON serialization of complex objects indicates this is a prototype implementation
        String deviceRegistryData = persistentStorage.get(DEVICE_REGISTRY, null);
        if(deviceRegistryData == null) {
            monitoringDevices = new TreeSet<>();
        } else {
            Type deviceCollectionType = new TypeToken<Set<Sensor>>() {
            }.getType();
            monitoringDevices = jsonSerializer.fromJson(deviceRegistryData, deviceCollectionType);
        }
    }

    @Override
    public void addSensor(Sensor sensor) {
        monitoringDevices.add(sensor);
        persistentStorage.put(DEVICE_REGISTRY, jsonSerializer.toJson(monitoringDevices));
    }

    @Override
    public void removeSensor(Sensor sensor) {
        monitoringDevices.remove(sensor);
        persistentStorage.put(DEVICE_REGISTRY, jsonSerializer.toJson(monitoringDevices));
    }

    @Override
    public void updateSensor(Sensor sensor) {
        monitoringDevices.remove(sensor);
        monitoringDevices.add(sensor);
        persistentStorage.put(DEVICE_REGISTRY, jsonSerializer.toJson(monitoringDevices));
    }

    @Override
    public void setAlarmStatus(AlarmStatus alarmStatus) {
        this.currentThreatLevel = alarmStatus;
        persistentStorage.put(THREAT_LEVEL_STATE, this.currentThreatLevel.toString());
    }

    @Override
    public void setArmingStatus(ArmingStatus armingStatus) {
        this.operationalMode = armingStatus;
        persistentStorage.put(OPERATIONAL_MODE_STATE, this.operationalMode.toString());
    }

    @Override
    public Set<Sensor> getSensors() {
        return monitoringDevices;
    }

    @Override
    public AlarmStatus getAlarmStatus() {
        return currentThreatLevel;
    }

    @Override
    public ArmingStatus getArmingStatus() {
        return operationalMode;
    }
}