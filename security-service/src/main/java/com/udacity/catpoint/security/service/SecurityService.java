package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.image.service.FakeImageService;

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

    private FakeImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, FakeImageService imageService) {
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
        } else if (armingStatus == ArmingStatus.ARMED_AWAY || armingStatus == ArmingStatus.ARMED_HOME) {
            securityRepository.setArmingStatus(armingStatus);
            setAllSensorsInactive();
        }
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else {
            setAlarmStatus(AlarmStatus.NO_ALARM);
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
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return;
        }
        if (securityRepository.getAlarmStatus() == AlarmStatus.NO_ALARM){
            setAlarmStatus(AlarmStatus.PENDING_ALARM);
        } else if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
    }


    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */

    private void handleSensorDeactivated() {
        AlarmStatus alarmStatus = securityRepository.getAlarmStatus();
        if(alarmStatus == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

    }



    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        boolean isSensorActive = sensor.getActive();
        if(isSensorActive) {
            if (active){
                if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
                    setAlarmStatus(AlarmStatus.ALARM);
                }
            } else {
                sensor.setActive(active);
                securityRepository.updateSensor(sensor);
                handleSensorDeactivated();
            }
        } else {
            if (active){
                sensor.setActive(active);
                securityRepository.updateSensor(sensor);
                handleSensorActivated();
            }
        }
    }

    public void setAllSensorsInactive() {
        Set<Sensor> sensors = getSensors();
        for (Sensor sensor : sensors) {
            changeSensorActivationStatus(sensor, false);
        }
        if(allSensorsInactive() && securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    private boolean allSensorsInactive() {
        Set<Sensor> sensors = getSensors();
        for(Sensor s: sensors) {
            if(s.getActive()) {
                return false;
            }
        }
        return true;
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
