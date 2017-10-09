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

    private Transport mTransport;
    private TrackBank mTrackBank;
    private int mInternalA = 0;
    private int mInternalB = 0;
    private BiMap<MidiId, MatrixCoordinate> mBiMap;
    ControllerFunction[] cf;

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
        //getHost().getMidiOutPort().sendMidi(144, data1, msg.getData2());
        mTrackBank.scrollChannelsDown();
    }


    void scrollChannelsUp() {
        // getHost().getMidiOutPort().sendMidi(msg.getStatusByte(), msg.getData1(), msg.getData2());
        mTrackBank.scrollChannelsUp();
    }

    @Override
    public void init() {

        final ControllerHost host = getHost();


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

        int[] midiKeys = {16, 20, 24, 28, 46, 50, 54, 58,   // cc
                17, 21, 25, 29, 47, 51, 55, 59,   // .
                18, 22, 26, 30, 48, 52, 56, 60,   // .
                1, 4, 7, 10, 13, 16, 19, 22,     // note
                2, 5, 8, 11, 14, 17, 20, 23,     // .       this is mute + solo
                3, 6, 9, 12, 15, 18, 21, 24,     // .
                19, 23, 27, 31, 49, 53, 57, 61,   // cc
                25, 26, 27};  // BL, BR, Solo  // note
        cf = new ControllerFunction[64];


        cf[7 * NUM_TRACKS] = (val) -> scrollChannelsUp();
        cf[7 * NUM_TRACKS + 1] = (val) -> scrollChannelsDown();
        mBiMap.put(new MidiId(144, 25), new MatrixCoordinate(7, 0));
        mBiMap.put(new MidiId(144, 26), new MatrixCoordinate(7, 1));

        for (int i = 0; i < NUM_TRACKS; i++) {
            final int channel = i;

            String stat = "";

            cf[i] = (msg) -> setSend(channel, 0, msg);
            int index = sendAOffset + i;
            stat += "midi[" + index + "]  = " + midiKeys[index];
            mBiMap.put(new MidiId(176, midiKeys[index]), new MatrixCoordinate(0, i));

            index = sendBOffset + i;
            cf[NUM_TRACKS + i] = (msg) -> setSend(channel, 1, msg);
            mBiMap.put(new MidiId(176, midiKeys[index]), new MatrixCoordinate(1, i));

            index = panOffset + i;
            cf[NUM_TRACKS * 2 + i] = (msg) -> setPan(channel, msg);
            mBiMap.put(new MidiId(176, midiKeys[index]), new MatrixCoordinate(2, i));

            index = muteOffset + i;
            cf[NUM_TRACKS * 3 + i] = (msg) -> toggleMute(channel, msg);
            mBiMap.put(new MidiId(144, midiKeys[index]), new MatrixCoordinate(3, i));

            index = soloOffset + i;
            cf[NUM_TRACKS * 4 + i] = (msg) -> toggleSolo(channel, msg);
            mBiMap.put(new MidiId(144, midiKeys[index]), new MatrixCoordinate(4, i));

            index = recOffset + i;
            cf[NUM_TRACKS * 5 + i] = (msg) -> toggleRec(channel, msg);
            mBiMap.put(new MidiId(144, midiKeys[index]), new MatrixCoordinate(5, i));

            index = faderOffset + i;
            cf[NUM_TRACKS * 6 + i] = (msg) -> setVolume(channel, msg);
            mBiMap.put(new MidiId(176, midiKeys[index]), new MatrixCoordinate(6, i));

            host.println(stat);
        }


        Set<MidiId> ks = mBiMap.keySet();
        for (MidiId mi : ks) {
            MatrixCoordinate x = mBiMap.get(mi);
            String msg = String.format("%5d %4d  %d %d", mi.status, mi.data1, x.i, x.j);
            host.println(msg);
        }


        // TODO: Perform your driver initialization here.
        // For now just show a popup notification for verification that it is running.
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

        MidiId mi = new MidiId(msg.getStatusByte(), msg.getData1());
        MatrixCoordinate mc = mBiMap.get(mi);
        if (mc == null) {
            host.println(
                    String.format("%5s %4d  no mc", mi.status, mi.data1));
        } else {
            host.println(
                    String.format("%5s %4d  %d %d", mi.status, mi.data1, mc.i, mc.j));
            ControllerFunction c = cf[NUM_TRACKS * mc.i + mc.j];

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


    private enum STATUS {
        NOTE, CC
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

    private class MatrixCoordinate {
        public final int i;
        public final int j;

        public MatrixCoordinate(int i, int j) {
            this.i = i;
            this.j = j;
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
