package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.*;

import java.util.ArrayList;

class SingleDeviceController {

    private TrackBank trackBank;
    private DeviceBank deviceBank;
    private CursorRemoteControlsPage remoteControlsPage;
    private ControllerHost host;
    private String name;

    SingleDeviceController(ControllerHost host, String name) {
        this.host = host;
        this.name = name;
        trackBank = host.createTrackBank(1, 0, 0);
        trackBank.getChannel(0).name().markInterested();
        trackBank.getChannel(0).name().addValueObserver(s -> host.showPopupNotification(String.format(
                "%s", getControllerInfoString()
        )));
        deviceBank = trackBank.getChannel(0).createDeviceBank(1);
        remoteControlsPage = deviceBank.getDevice(0).createCursorRemoteControlsPage(8);
        for (int j = 0; j < 8; j++) {

            remoteControlsPage.getParameter(j).name().markInterested();
            remoteControlsPage.getParameter(j).value().markInterested();
        }
        remoteControlsPage.pageNames().markInterested();
        remoteControlsPage.selectedPageIndex().addValueObserver(i -> host.showPopupNotification(String.format(
                "%s", getControllerInfoString()
        )));
        Device d = deviceBank.getDevice(0);
        d.name().markInterested();
        d.position().markInterested();
    }

    private String getControllerInfoString() {
        return String.format("%8s     %8s    %16s     %16s", name, getTrackName(), getDeviceName(), getParamPageName());
    }

    void moveParameter(int parameterId, ShortMidiMessage msg) {
        double newVal = msg.getData2() / 127.0f;
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
    private String getParamPageName() {
        String pageName = "";
        try {
            pageName = remoteControlsPage.pageNames().get(remoteControlsPage.selectedPageIndex().get());
        } catch (Exception ignored) {
        }

        return pageName;
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
