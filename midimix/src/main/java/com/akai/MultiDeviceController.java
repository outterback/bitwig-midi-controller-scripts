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
        StringBuilder consoleUiStr = new StringBuilder();
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

    private static final int NUM_TRACKS = 8;
    private static final int NUM_SENDS = 2;
    private static final int NUM_SCENES = 0;
    private static final int NUM_DEVICE_CONTROLLERS = 4;
    private static final int NUM_PARAMETERS_PER_CONTROLLER = 8;

    /* These objects represent remote controls in bitwig
    * Transport controls playhead position, play, pause etc
    * Trackbank holds references to control mixer channels in bitwig
    */
    private final ControllerHost host;
    private MasterTrack masterTrack;
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
            controllers[i] = new SingleDeviceController(host);
        }
        functionMatrix = new ControllerFunction[8][8];
        initBitwigControls();
        // TODO: deviceBanks = new DeviceBank[NUM_TRACKS];
    }

    void initBitwigControls() {
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
        }
    }

    void printMessage(ShortMidiMessage msg) {
        host.println("midiMessage status: " + msg.getStatusByte()
                + " data1: " + msg.getData1() + " data2: " + msg.getData2() + " channel: " + msg.getChannel());
    }

    void handleMidiMessage(ShortMidiMessage msg) {
        //host.println("handleMidiMessage");
        //printMessage(msg);


        if (msg.isNoteOn()) {
            switch (msg.getData1()) {
                case 25:
                    shouldIPrint.set(true);
                    break;
                case 10:
                    controllers[1].nextTrack();
                    break;
                case 7:
                    controllers[1].prevTrack();
                    break;
            }
        }

        callFunction(msg);
    }

    void callFunction(ShortMidiMessage msg) {
        Coord c = getCoordFromMidi(msg);
        if (c == null) {
            return;
        }
        ControllerFunction cf = functionMatrix[c.x][c.y];
        if (cf == null) {
            // host.println("cf == null");
        } else {
            cf.op(msg);
        }
    }

    /*
    private void initBitwigControls() {
        setFcnAtCoords(Row.EXTRA, 0, this::scrollChannelsUp);
        setFcnAtCoords(Row.EXTRA, 1, this::scrollChannelsDown);

        trackBank.channelCount().markInterested();

        masterTrack = host.createMasterTrack(0);
        masterTrack.name().addValueObserver(new StringValueChangedCallback() {
            @Override
            public void valueChanged(String s) {

            }
        });
        masterTrack.getVolume().markInterested();
        trackBank.getChannel(0).name().addValueObserver(new StringValueChangedCallback() {
            @Override
            public void valueChanged(String s) {
                host.showPopupNotification("Midimix track 1: " + s);
            }
        });
        setFcnAtCoords(Row.EXTRA, 3, (msg) -> masterTrack.getVolume().set(intValTo01(msg.getData2())));

        for (int y = 0; y < NUM_TRACKS; y++) {  // I ❤ λ's
            final int channel = y;
            final Track track = trackBank.getChannel(channel);
            setFcnAtCoords(Row.SEND_A, y, (msg) -> setSend(channel, 0, msg));
            setFcnAtCoords(Row.SEND_B, y, (msg) -> setSend(channel, 1, msg));
            setFcnAtCoords(Row.PAN, y, (msg) -> setPan(channel, msg));
            setFcnAtCoords(Row.MUTE, y, (msg) -> toggleMute(channel, msg));
            setFcnAtCoords(Row.SOLO, y, (msg) -> toggleSolo(channel, msg));
            setFcnAtCoords(Row.ARM, y, (msg) -> toggleRec(channel, msg));
            setFcnAtCoords(Row.FADER, y, (msg) -> setVolume(channel, msg));


            /* to be able to read parameters in bitwig, we need to either
             * a) call getmute().markinterested()  or
             * b) add a value observer via mtrackbank.getchannel(i).getmute().addvalueobserver(...)
             * or else we will be blocked from using getmute().get().
             *
            track.getMute().markInterested();
            track.getSolo().markInterested();
            track.getArm().markInterested();
            track.name().markInterested();
            track.isGroup().markInterested();
        }

    }
    */

    private void setFcnAtCoords(int x, int y, ControllerFunction f) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            host.println("Error setFcnAtCoords x: " + x + " y: " + y);
        }
        functionMatrix[x][y] = f;
    }

    private ControllerFunction getFcnFromMidi(ShortMidiMessage msg) {
        Coord c = getCoordFromMidi(msg);
        if (null == c) {
            //host.println("c is null" + msg.getStatusByte() + " " + msg.getData1());
            return null;
        }
        return functionMatrix[c.x][c.y];

    }

    private double intValTo01(int value) {
        return value / 127.0;
    }

    /*
    private void setSend(int channel, int send, ShortMidiMessage msg) {
        trackBank.getChannel(channel).sendBank().getItemAt(send).value().set(intValTo01(msg.getData2()));
    }

    private void setVolume(int channel, ShortMidiMessage msg) {
        trackBank.getChannel(channel).getVolume().value().set(intValTo01(msg.getData2()));
    }

    private void setPan(int channel, ShortMidiMessage msg) {
        trackBank.getChannel(channel).getPan().value().set(intValTo01(msg.getData2()));
    }

    private void toggleMute(int channel, ShortMidiMessage msg) {
        trackBank.getChannel(channel).getMute().toggle();
    }

    private void toggleRec(int channel, ShortMidiMessage msg) {
        trackBank.getChannel(channel).getArm().toggle();
    }

    private void toggleSolo(int channel, ShortMidiMessage msg) {
        trackBank.getChannel(channel).getSolo().toggle();

    }

    private void scrollChannelsDown(ShortMidiMessage msg) {
        trackBank.scrollChannelsDown();
    }


    private void scrollChannelsUp(ShortMidiMessage msg) {
        trackBank.scrollChannelsUp();
    }
    */

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


    /**
     *
     *
     void flushLights() {
     for (int channel = 0; channel < 8; ++channel) {
     int velocity = 0;
     final Track track = trackBank.getChannel(channel);
     if (!track.getMute().get()) {
     velocity = 127;
     }
     host.getMidiOutPort(0)
     .sendMidi(144, MidiMixMapping.getData1(Row.MUTE, channel), velocity);

     velocity = 0;
     if (track.getSolo().get()) {
     velocity = 127;
     }
     host.getMidiOutPort(0)
     .sendMidi(144, MidiMixMapping.getData1(Row.SOLO, channel), velocity);

     velocity = 0;
     if (track.getArm().get()) {
     velocity = 127;
     }
     host.getMidiOutPort(0)
     .sendMidi(144, MidiMixMapping.getData1(Row.ARM, channel), velocity);


     }
     }
     */
}
