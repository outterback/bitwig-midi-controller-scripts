package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.*;

import java.util.ArrayList;

public class SingleDeviceController {

    TrackBank trackBank;
    DeviceBank deviceBank;
    CursorRemoteControlsPage remoteControlsPage;
    ControllerHost host;

    public SingleDeviceController(ControllerHost host) {
        this.host = host;
        trackBank = host.createTrackBank(1, 0, 0);
        trackBank.getChannel(0).name().markInterested();
        deviceBank = trackBank.getChannel(0).createDeviceBank(1);
        remoteControlsPage = deviceBank.getDevice(0).createCursorRemoteControlsPage(8);
        for (int j = 0; j < 8; j++) {
            remoteControlsPage.getParameter(j).name().markInterested();
            remoteControlsPage.getParameter(j).value().markInterested();
        }

        Device d = deviceBank.getDevice(0);
        d.name().markInterested();
        d.position().markInterested();
    }

    void moveParameter(int parameterId, ShortMidiMessage msg) {
        double newVal = msg.getData2() / 127.0f;

        String s = String.format("%10s %10s %10s %3.2f %3.2f", getTrackName(), getDeviceName(), getParameterNames()[parameterId], getParameterValues()[parameterId], newVal);
        host.println("new val: " + newVal);
        host.println(s);
        remoteControlsPage.getParameter(parameterId).set(newVal);
    }

    void nextTrack() { // scrolls to the right
        trackBank.scrollChannelsDown();
    }

    void prevTrack() { // scrolls to the left
        trackBank.scrollChannelsUp();
    }

    void nextDevice() {
        deviceBank.scrollDown();
    }

    void prevDevice() {
        deviceBank.scrollUp();
    }

    void nextParamPage() {
        remoteControlsPage.selectNextPage(true);
    }

    void prevParamPage() {
        remoteControlsPage.selectPreviousPage(true);
    }

    String getTrackName() {
        return trackBank.getChannel(0).name().get();
    }
    String getDeviceName() {
        return deviceBank.getDevice(0).name().get();
    }
    String getParamPageName() {
        return remoteControlsPage.pageNames().get(remoteControlsPage.selectedPageIndex().get());
    }

    String[] getParameterNames() {
        ArrayList<String> paramNames = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            paramNames.add(remoteControlsPage.getParameter(i).name().get());
        }
        return paramNames.toArray(new String[0]);
    }
    Double[] getParameterValues() {
        ArrayList<Double> paramValues = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            paramValues.add(remoteControlsPage.getParameter(i).value().get());
        }
        return paramValues.toArray(new Double[0]);
    }
}
