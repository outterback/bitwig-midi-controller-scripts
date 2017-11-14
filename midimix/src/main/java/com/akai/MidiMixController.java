package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

class MidiMixController implements midimixExtension.MidiMessageHandler {

    private interface ControllerFunction {
        void op(ShortMidiMessage msg);
    }

    private static final int NUM_TRACKS = 8;
    private static final int NUM_SENDS = 2;
    private static final int NUM_SCENES = 0;

    /* These objects represent remote controls in bitwig
    * Transport controls playhead position, play, pause etc
    * Trackbank holds references to control mixer channels in bitwig
    */
    private final ControllerHost host;
    private final TrackBank trackBank;
    private MasterTrack masterTrack;
    private final ControllerFunction[][] functionMatrix;

    MidiMixController(ControllerHost host) {
        this.host = host;
        this.trackBank = host.createMainTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES);
        host.println("numTracks = " + trackBank.getSizeOfBank());
        //host.addDeviceNameBasedDiscoveryPair(new String[]{"MIDI Mix"}, new String[]{"MIDI Mix"});

        for (int i = 0; i < 8; i++) {
            this.trackBank.getChannel(i).name().markInterested();
        }
        this.functionMatrix = new ControllerFunction[8][8];
        initBitwigControls();
    }


    @Override
    public void handleMidiMessage(ShortMidiMessage msg) {
        host.println("trackbank size = " + trackBank.getSizeOfBank());
        callFunction(msg);
    }

    private void callFunction(ShortMidiMessage msg) {
        Coord c = MidiMixMapping.getCoordFromMidi(msg);
        if (c == null) {
            return;
        }
        ControllerFunction cf = functionMatrix[c.x][c.y];
        cf.op(msg);
    }

    private void initBitwigControls() {

        // Second parameter, 0 and 1, are the second (column) index in MidiMixMapping.midiMixData1Map
        setFcnAtCoords(Row.EXTRA, 0, this::scrollChannelsUp);
        setFcnAtCoords(Row.EXTRA, 1, this::scrollChannelsDown);

        trackBank.channelCount().markInterested();

        masterTrack = host.createMasterTrack(0);
        masterTrack.name().addValueObserver(s -> {
        });
        masterTrack.getVolume().markInterested();
        trackBank.getChannel(0).name().addValueObserver(s -> host.showPopupNotification("Midimix track 1: " + s));

        // As 0 and 1 above
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
             */
            track.getMute().markInterested();
            track.getSolo().markInterested();
            track.getArm().markInterested();
            track.name().markInterested();
            track.isGroup().markInterested();
        }

    }


    private void setFcnAtCoords(int x, int y, ControllerFunction f) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            host.println("Error setFcnAtCoords x: " + x + " y: " + y);
        }
        functionMatrix[x][y] = f;
    }

    private ControllerFunction getFcnFromMidi(ShortMidiMessage msg) {
        final Coord c = MidiMixMapping.getCoordFromMidi(msg);
        if (null == c) {
            return null;
        }
        return functionMatrix[c.x][c.y];

    }

    private double intValTo01(int value) {
        return value / 127.0;
    }

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
        host.println("toggelMute: " + channel);
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
     */
    @Override
    public void flushLights() {
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

    @Override
    public void doPrint() {

    }
}
