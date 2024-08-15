package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.data.*;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTest {

    private SecurityService securityService;
    private Set<Sensor> sensors;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private FakeImageService imageService = new FakeImageService();


    @BeforeEach
    void init() {
        securityRepository = Mockito.mock(SecurityRepository.class);
        imageService = Mockito.mock(FakeImageService.class);
        securityService = new SecurityService(securityRepository, imageService);
        sensors = new HashSet<>();
        sensors.add(new Sensor("Door_Sensor", SensorType.DOOR));
        sensors.add(new Sensor("Window_Sensor", SensorType.WINDOW));
        sensors.add(new Sensor("Motion_Sensor", SensorType.MOTION));
    }

    @Test
    void ifAlarmIsArmedAndSensorBecomesActivated_systemShouldGoToPendingAlarmState() {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        Sensor sensor = sensors.iterator().next();
        sensor.setActive(false);
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifAlarmIsArmedAndSensorBecomesActivatedAndSystemIsAlreadyPendingAlarm_setAlarmStatusToAlarm() {
        Sensor sensor = sensors.iterator().next();
        sensor.setActive(true);
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);

        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Set<Sensor> sensorsList = securityService.getSensors();
        Sensor sensor1 = sensorsList.iterator().next();
        securityService.changeSensorActivationStatus(sensor1, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifPendingAlarmAndAllSensorsAreInactive_returnToNoAlarmState() {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setAllSensorsInactive();

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest(name = "state")
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_changeInSensorStateShouldNotAffectAlarmState(boolean state) {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = sensors.iterator().next();
        if (state){
            sensor.setActive(false);
        } else {
            sensor.setActive(true);
        }
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensor, state);

        verify(securityRepository, Mockito.never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifSensorIsActivatedWhileAlreadyActiveAndSystemIsInPendingState_changeItToAlarmState() {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = sensors.iterator().next();
        sensor.setActive(true);
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifSensorIsDeactivatedWhileAlreadyInactive_makeNoChangesToAlarmState() {
        Sensor sensor = sensors.iterator().next();
        sensor.setActive(false);
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);

        Set<Sensor> sensorsList = securityService.getSensors();
        Sensor sensor1 = sensorsList.iterator().next();
        securityService.changeSensorActivationStatus(sensor1, false);

        verify(securityRepository, Mockito.never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifImageServiceIdentifiesCatWhileSystemIsArmedHome_setAlarmStatusToAlarm() {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Mockito.when(imageService.imageContainsCat(Mockito.eq(catImage), Mockito.anyFloat())).thenReturn(true);

        securityService.processImage(catImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifImageServiceIdentifiesNoCat_changeStatusToNoAlarmIfSensorsAreNotActive() {
        BufferedImage noCatImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Mockito.when(imageService.imageContainsCat(Mockito.eq(noCatImage), Mockito.anyFloat())).thenReturn(false);

        for (Sensor sensor : sensors) {
            sensor.setActive(false);
        }
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(noCatImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest(name = "alarmStatus")
    @EnumSource(value = AlarmStatus.class, names = {"PENDING_ALARM", "ALARM"})
    void ifSystemIsDisarmed_setStatusToNoAlarm(AlarmStatus alarmStatus) {
        securityService.setAlarmStatus(alarmStatus);
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest(name = "armingStatus")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemIsArmed_resetAllSensorsToInactive(ArmingStatus armingStatus) {
        for (Sensor sensor : sensors) {
            sensor.setActive(true);
            securityService.addSensor(sensor);
        }
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.setArmingStatus(armingStatus);

        Set <Sensor> sensorsList = securityService.getSensors();
        for (Sensor sensor : sensorsList) {
            assertFalse(sensor.getActive());
        }
    }

    @Test
    void ifSystemIsArmedHomeAndCameraShowsCat_setAlarmStatusToAlarm() {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Mockito.when(imageService.imageContainsCat(Mockito.eq(catImage), Mockito.anyFloat())).thenReturn(true);

        securityService.processImage(catImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

}