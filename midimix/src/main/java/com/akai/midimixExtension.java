package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.ControllerExtension;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Set;

public class midimixExtension extends ControllerExtension {
    protected midimixExtension(final midimixExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    private static final int[] midiKeys =
            { 16, 20, 24, 28, 46, 50, 54, 58,   // cc
             17, 21, 25, 29, 47, 51, 55, 59,   // .
             18, 22, 26, 30, 48, 52, 56, 60,   // .
             1, 4, 7, 10, 13, 16, 19, 22,     // note
             2, 5, 8, 11, 14, 17, 20, 23,     // .       this is mute + solo
             3, 6, 9, 12, 15, 18, 21, 24,     // .
             19, 23, 27, 31, 49, 53, 57, 61,   // cc
            25, 26, 27};  // BL, BR, Solo  // note

    int[][] a = {{1,2}, {3,4}, {5}, {9, 10, 11}};

    private static final int[][] midiKeys2d =
                   {{16, 20, 24, 28, 46, 50, 54, 58},   // cc
                    {17, 21, 25, 29, 47, 51, 55, 59},   // .
                    {18, 22, 26, 30, 48, 52, 56, 60},   // .
                    {1, 4, 7, 10, 13, 16, 19, 22},     // note
                    {2, 5, 8, 11, 14, 17, 20, 23},     // .       this is mute + solo
                    {3, 6, 9, 12, 15, 18, 21, 24},     // .
                    {19, 23, 27, 31, 49, 53, 57, 61},   // cc
                    {25, 26, 27}};  // BL, BR, Solo  // note

    private Transport mTransport;
    private TrackBank mTrackBank;
    private int mInternalA = 0;
    private int mInternalB = 0;
    private BiMap<MidiId, xy> mBiMap;
    private static final int NOTE_ON = 144;
    private static final int NOTE_OFF = 128;
    private static final int CC = 176;
    private static final int MUTE_ROW = 3;
    private static final int SOLO_ROW = 4;
    private static final int REC_ROW  = 5;


    ControllerFunction[] cf;
    ControllerFunction[][] cf2d;

    double intValTo01(int value) {
        return value / 127.0;
    }

    void setSend(int channel, int send, ShortMidiMessage msg) {
        getHost().println("in setSend  chan:" + channel + "  send: " + send + "  new val = " + intValTo01(msg.getData2()));
        mTrackBank.getChannel(channel).sendBank().getItemAt(send).value().set(intValTo01(msg.getData2()));
    }

    void setVolume(int channel, ShortMidiMessage msg) {
        mTrackBank.getChannel(channel).getVolume().value().set(intValTo01(msg.getData2()));
    }

    void setPan(int channel, ShortMidiMessage msg) {
        mTrackBank.getChannel(channel).getPan().value().set(intValTo01(msg.getData2()));
    }

    void toggleMute(int channel, ShortMidiMessage msg) {
        Channel c = mTrackBank.getChannel(channel);
        int midiVal = msg.getData2();
        if (c.getMute().get()) {
            midiVal = 0;
        }
        getHost().getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), midiVal);
        mTrackBank.getChannel(channel).getMute().toggle();
    }

    private void toggleRec(int channel, ShortMidiMessage msg) {

        int midiVal = msg.getData2();
        if (mTrackBank.getChannel(channel).getArm().get()) {
            midiVal = 0;
        }
        getHost().getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), midiVal);
        mTrackBank.getChannel(channel).getArm().toggle();
    }

    private void toggleSolo(int channel, ShortMidiMessage msg) {
        int midiVal = msg.getData2();
        if (mTrackBank.getChannel(channel).getSolo().get()) {
            midiVal = 0;
        }
        getHost().getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), midiVal);
        mTrackBank.getChannel(channel).getSolo().toggle();
    }

    int NUM_TRACKS = 8;
    int NUM_SENDS = 2;
    int NUM_SCENES = 0;


    void scrollChannelsDown() {
        fixAllLights();
        //getHost().getMidiOutPort().sendMidi(144, data1, msg.getData2());
        mTrackBank.scrollChannelsDown();
    }


    void scrollChannelsUp() {
        fixAllLights();
        // getHost().getMidiOutPort().sendMidi(msg.getStatusByte(), msg.getData1(), msg.getData2());
        mTrackBank.scrollChannelsUp();
    }


    void turnOffAllLights() {
        getHost().println("Fixing Lights");
        for (int channel = 0; channel < NUM_TRACKS; ++channel) {
            int velocity = 0;
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[MUTE_ROW][channel], velocity);
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[SOLO_ROW][channel], velocity);
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[REC_ROW][channel], velocity);


        }
    }


    void fixAllLights() {
        getHost().println("Fixing Lights");
        for (int channel = 0; channel < NUM_TRACKS; ++channel) {
            int velocity = 0;
            if (mTrackBank.getChannel(channel).getMute().get()) {
                velocity = 127;
            }
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[MUTE_ROW][channel], velocity);

            velocity = 0;
            if (mTrackBank.getChannel(channel).getSolo().get()) {
                velocity = 127;
            }
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[SOLO_ROW][channel], velocity);

            velocity = 0;
            if (mTrackBank.getChannel(channel).getArm().get()) {
                velocity = 127;
            }
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[REC_ROW][channel], velocity);


        }
    }

    @Override
    public void init() {

        final ControllerHost host = getHost();
        host.println("hello ");
        host.println(a[0][0] + "   " + a[0][1] + "   " + a[1][0] + "   " +  a[0][1] + "   " + a[2][0] + "  " + a[3][2]);

        int sendAOffset = 0;
        int sendBOffset = 8;
        int panOffset = 16;
        int muteOffset = 24;
        int soloOffset = 32;
        int recOffset = 40;
        int faderOffset = 48;


        mTransport = host.createTransport();
        mTrackBank = host.createTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES);

        for (int i = 0; i < NUM_TRACKS; i++) {
            mTrackBank.getChannel(i).getMute().markInterested();
            mTrackBank.getChannel(i).getSolo().markInterested();
            mTrackBank.getChannel(i).getArm().markInterested();
        }

        mBiMap = HashBiMap.create();


        host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
        host.getMidiInPort(0).setSysexCallback((String data) -> onSysex0(data));

        cf = new ControllerFunction[64];
        cf2d = new ControllerFunction[8][8];

        cf2d[7][0] = (val) -> scrollChannelsUp();
        cf2d[7][1] = (val) -> scrollChannelsDown();
        mBiMap.put(new MidiId(NOTE_ON, 25), new xy(7, 0));
        mBiMap.put(new MidiId(NOTE_ON, 26), new xy(7, 1));

        for (int y = 0; y < NUM_TRACKS; y++) {
            final int channel = y;

            String stat = "";

            cf2d[0][y] = (msg) -> setSend(channel, 0, msg);
            int index = sendAOffset + y;
            stat += "midi[" + index + "]  = " + midiKeys[index];
            mBiMap.put(new MidiId(CC, midiKeys[index]), new xy(0, y));

            index = sendBOffset + y;
            cf2d[1][y] = (msg) -> setSend(channel, 1, msg);
            mBiMap.put(new MidiId(CC, midiKeys[index]), new xy(1, y));

            index = panOffset + y;
            cf2d[2][y] = (ShortMidiMessage msg) -> setPan(channel, msg);
            mBiMap.put(new MidiId(CC, midiKeys[index]), new xy(2, y));

            index = muteOffset + y;
            cf2d[3][y] = (ShortMidiMessage msg) -> toggleMute(channel, msg);
            mBiMap.put(new MidiId(NOTE_ON, midiKeys[index]), new xy(3, y));

            index = soloOffset + y;
            cf2d[4][y] = (ShortMidiMessage msg) -> toggleSolo(channel, msg);
            mBiMap.put(new MidiId(NOTE_ON, midiKeys[index]), new xy(4, y));

            index = recOffset + y;
            cf2d[5][y] = (ShortMidiMessage msg) -> toggleRec(channel, msg);
            mBiMap.put(new MidiId(NOTE_ON, midiKeys[index]), new xy(5, y));

            index = faderOffset + y;
            cf2d[6][ y] = (ShortMidiMessage msg) -> setVolume(channel, msg);
            mBiMap.put(new MidiId(CC, midiKeys[index]), new xy(6, y));

            host.println(stat);
        }


        Set<MidiId> ks = mBiMap.keySet();
        for (MidiId mi : ks) {
            xy x = mBiMap.get(mi);
            String msg = String.format("%5d %4d  %d %d", mi.status, mi.data1, x.x, x.y);
            host.println(msg);
        }

        host.showPopupNotification("midimix Initialized");
        host.println("init() done");
    }

    /**
     * Called when we receive short MIDI message on port 0.
     */
    private void onMidi0(ShortMidiMessage msg) {
        final ControllerHost host = getHost();

        String msgType = "other (pb, press, pc)";
        if (msg.isNoteOn() || msg.isNoteOff()) {

            host.getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), msg.getData2());
            msgType = "note";
        } else if (msg.isControlChange()) {
            msgType = "cc";
        } else {
        }

        if (msg.getData1() == 62) {
            host.println(" --# { Turning off all lights }");
            fixAllLights();
            return;
        }
        MidiId mi = new MidiId(msg.getStatusByte(), msg.getData1());
        xy mc = mBiMap.get(mi);
        if (mc == null) {
            host.println(
                    String.format("%5s %4d  no mc", mi.status, mi.data1));
        } else {
            host.println(
                    String.format("%5s %4d  %d %d", mi.status, mi.data1, mc.x, mc.y));
            ControllerFunction c = cf2d[mc.x][mc.y];

            c.op(msg);

        }
        host.println(
                String.format("%8s %4d  %4d  %4d  %4d", msgType, msg.getStatusByte(), msg.getData1(), msg.getData2(), mInternalA));


        //host.getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1());

        // int cc = mBiMap.inverse().get(mc); // find out how fast inverse() is


    }

    private interface ControllerFunction {
        void op(ShortMidiMessage msg);
    }

    private class MidiId {
        private int status;
        private int data1;

        public MidiId(int status, int data1) {
            this.status = status;
            this.data1 = data1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MidiId midiId = (MidiId) o;

            if (status != midiId.status) return false;
            return data1 == midiId.data1;
        }

        @Override
        public int hashCode() {
            int result = status;
            result = 31 * result + data1;
            return result;
        }
    }

    private class xy {
        public final int x;
        public final int y;

        public xy(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Called when we receive sysex MIDI message on port 0.
     */
    private void onSysex0(final String data) {
        // MMC Transport Controls:
        if (data.equals("f07f7f0605f7"))
            mTransport.rewind();
        else if (data.equals("f07f7f0604f7"))
            mTransport.fastForward();
        else if (data.equals("f07f7f0601f7"))
            mTransport.stop();
        else if (data.equals("f07f7f0602f7"))
            mTransport.play();
        else if (data.equals("f07f7f0606f7"))
            mTransport.record();
    }

    @Override
    public void exit() {
        // TODO: Perform any cleanup once the driver exits
        // For now just show a popup notification for verification that it is no longer running.
        getHost().showPopupNotification("midimix Exited");
    }

    @Override
    public void flush() {
        // TODO Send any updates you need here.
    }

}
