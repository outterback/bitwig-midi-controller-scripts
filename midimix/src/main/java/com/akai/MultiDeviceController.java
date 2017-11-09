package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.*;

import static com.akai.MidiMixMapping.getCoordFromMidi;

class MultiDeviceController {


    private SettableBooleanValue shouldIPrint;

    void doPrint() {
        if (!shouldIPrint.get()) {
            return;
        }
        shouldIPrint.set(false);
        for (int i = 0; i < NUM_DEVICE_CONTROLLERS; i++) {
            SingleDeviceController sdc = controllers[i];
            host.println(String.format("%-10s: %10s", sdc.getTrackName(), sdc.getDeviceName()));
            String[] params = sdc.getParameterNames();
            Double[] values = sdc.getParameterValues();
            StringBuilder paramNameString = new StringBuilder();
            StringBuilder paramValueString = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                paramNameString.append(String.format("%-7.7s ", params[j]));
                paramValueString.append(String.format("%-7.2f ", values[j]));

            }
            host.println(paramNameString.toString());
            host.println(paramValueString.toString());
            host.println("");
        }
    }

    private interface ControllerFunction {
        void op(ShortMidiMessage msg);
    }

    boolean showPopup = false;

    private static final int NUM_DEVICE_CONTROLLERS = 4;
    private static final int NUM_PARAMETERS_PER_CONTROLLER = 8;

    /* These objects represent remote controls in bitwig
    * Transport controls playhead position, play, pause etc
    * Trackbank holds references to control mixer channels in bitwig
    */
    private final ControllerHost host;
    private final ControllerFunction[][] functionMatrix;

    private final SingleDeviceController[] controllers;

    MultiDeviceController(ControllerHost host) {



        shouldIPrint = new SettableBooleanValue() {

            private boolean state = false;

            @Override
            public void set(boolean b) {
                state = b;
            }

            @Override
            public void toggle() {
                state = !state;
            }

            @Override
            public boolean get() {
                return state;
            }

            @Override
            public void markInterested() {

            }

            @Override
            public void addValueObserver(BooleanValueChangedCallback booleanValueChangedCallback) {
                host.println("shouldIPrint changed: " + state);
            }

            @Override
            public boolean isSubscribed() {
                return false;
            }

            @Override
            public void setIsSubscribed(boolean b) {

            }

            @Override
            public void subscribe() {

            }

            @Override
            public void unsubscribe() {

            }
        };
        this.host = host;
        controllers = new SingleDeviceController[NUM_DEVICE_CONTROLLERS];


        for (int i = 0; i < NUM_DEVICE_CONTROLLERS; i++) {
            String name = String.format("ctrl %d", i + 1);
            controllers[i] = new SingleDeviceController(host, name);
        }
        functionMatrix = new ControllerFunction[8][8];
        initBitwigControls();
        // TODO: deviceBanks = new DeviceBank[NUM_TRACKS];
    }

    private void initBitwigControls() {
        int[][] deviceCCMap =
                {       // one row per two columns on the midi mix
                        {16, 20, 17, 21, 18, 22, 19, 23},
                        {24, 28, 25, 29, 26, 30, 27, 31},
                        {46, 50, 47, 51, 48, 52, 49, 53},
                        {54, 58, 55, 59, 56, 60, 57, 61}
                };

        for (int i = 0; i < NUM_DEVICE_CONTROLLERS; i++) {
            final int controllerId = i;
            for (int j = 0; j < NUM_PARAMETERS_PER_CONTROLLER; j++) {
                final int parameterId = j;
                ComparableMidiMessage compMidiMsg = new ComparableMidiMessage(ShortMidiMessage.CONTROL_CHANGE, deviceCCMap[i][j], 0);
                Coord c = MidiMixMapping.getCoordFromMidi(compMidiMsg);
                functionMatrix[c.x][c.y] = (msg) -> controllers[controllerId].moveParameter(parameterId, msg);
            }

            final int leftColumn = 2 * i;
            final int rightColumn = 2 * i + 1;
            functionMatrix[Row.MUTE][leftColumn] = (msg) -> controllers[controllerId].prevDevice();
            functionMatrix[Row.MUTE][rightColumn] = (msg) -> controllers[controllerId].nextDevice();

            functionMatrix[Row.SOLO][leftColumn] = (msg) -> controllers[controllerId].prevTrack();
            functionMatrix[Row.SOLO][rightColumn] = (msg) -> controllers[controllerId].nextTrack();

            functionMatrix[Row.ARM][leftColumn] = (msg) -> controllers[controllerId].prevParamPage();
            functionMatrix[Row.ARM][rightColumn] = (msg) -> controllers[controllerId].nextParamPage();

        }
    }

    void printMessage(ShortMidiMessage msg) {
        host.println("midiMessage status: " + msg.getStatusByte()
                + " data1: " + msg.getData1() + " data2: " + msg.getData2() + " channel: " + msg.getChannel());
    }

    void handleMidiMessage(ShortMidiMessage msg) {
        if (msg.isNoteOn()) {
            switch (msg.getData1()) {
                case 25:
                    shouldIPrint.set(true);
                    break;
            }
        }
        callFunction(msg);
    }

    private void callFunction(ShortMidiMessage msg) {
        Coord c = getCoordFromMidi(msg);
        if (c == null) {
            return;
        }
        ControllerFunction cf = functionMatrix[c.x][c.y];
        if (cf != null) {
            cf.op(msg);
        }
    }


    private ControllerFunction getFcnFromMidi(ShortMidiMessage msg) {
        Coord c = getCoordFromMidi(msg);
        if (null == c) {
            return null;
        }
        return functionMatrix[c.x][c.y];

    }

    private double intValTo01(int value) {
        return value / 127.0;
    }


    void turnAllLightsOff() {
        int velocity = 0;
        for (int channel = 0; channel < 8; ++channel) {
            host.getMidiOutPort(0)
                    .sendMidi(144, MidiMixMapping.getData1(Row.MUTE, channel), velocity);
            host.getMidiOutPort(0)
                    .sendMidi(144, MidiMixMapping.getData1(Row.SOLO, channel), velocity);
            host.getMidiOutPort(0)
                    .sendMidi(144, MidiMixMapping.getData1(Row.ARM, channel), velocity);
        }
    }

}
