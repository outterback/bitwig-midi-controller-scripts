package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;

import java.util.HashMap;

public class midimixExtension extends ControllerExtension {

    private String masterName;

    public void setMasterName(String masterName) {
        ControllerHost host = getHost();
        host.println("new MasterName: " + masterName);
        this.masterName = masterName;
    }
    
    public String getMasterName() {
        ControllerHost host = getHost();
        host.println("  returning mastername: " + this.masterName);
        return this.masterName;
    }

    private interface ControllerFunction {
        void op(ShortMidiMessage msg);
    }


    /*  This is the first data byte sent from each knob/fader/button on the akai
 *  Bank left, bank right, solo and master are in the last, shorter row.
 *
 *  Start from upper left corner of the midimix
 *  Solo is a hold button, mute and solo are folded out into two rows (
 *  (0,0) is send A track 2, (0,7) is send A track 8, (6, 3) is fader track 4 e.t.c.
 *
 */
    private static final int[][] midiKeys2d =
        {{16, 20, 24, 28, 46, 50, 54, 58},   // 0  cc        send a
            {17, 21, 25, 29, 47, 51, 55, 59},   // 1  cc        send b
            {18, 22, 26, 30, 48, 52, 56, 60},   // 2  cc        pan
            {1, 4, 7, 10, 13, 16, 19, 22},   // 3  note      mute
            {2, 5, 8, 11, 14, 17, 20, 23},   // 4  note      solo + mute
            {3, 6, 9, 12, 15, 18, 21, 24},   // 5  note      rec
            {19, 23, 27, 31, 49, 53, 57, 61},   // 6  cc        fader
            {25, 26, 27, 62}};                  // 7  note      bank left, bank right, solo, master

    /* These objects represent remote controls in bitwig
     * Transport controls playhead position, play, pause etc
     * Trackbank holds references to control mixer channels in bitwig
     */

    private Transport mTransport;
    private TrackBank mTrackBank;
    private MasterTrack mMasterTrack;
    private MidiMixController mMidiMixController;

    private HashMap<MidiId, Coord> mMidiValToCoordMap; // maps the values of midiKeys2d to its keys
    private ControllerFunction[][] cf2d;

    private static final int NUM_TRACKS = 8;
    private static final int NUM_SENDS = 2;
    private static final int NUM_SCENES = 0;
    private final DeviceBank[] deviceBanks = new DeviceBank[NUM_TRACKS];

    protected midimixExtension(final midimixExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    private void initMap(HashMap<MidiId, Coord> map) {
        if (null == map) {
            return;
        }

        /*
         * This registers where in the map to look for a function
         */
        for (int i = 0; i < 7; ++i) { // Skip extras
            for (int j = 0; j < 8; ++j) {
                int status = ShortMidiMessage.CONTROL_CHANGE;
                if (i == 3 || i == 4 || i == 5) {
                    status = ShortMidiMessage.NOTE_ON;
                }
                int data1 = midiKeys2d[i][j];
                map.put(new MidiId(status, data1), new Coord(i, j));
            }
            map.put(new MidiId(ShortMidiMessage.NOTE_ON, 25), new Coord(7, 0));
            map.put(new MidiId(ShortMidiMessage.NOTE_OFF, 25), new Coord(7, 0));

            map.put(new MidiId(ShortMidiMessage.NOTE_ON, 26), new Coord(7, 1));
            map.put(new MidiId(ShortMidiMessage.NOTE_OFF, 26), new Coord(7, 1));

        }
    }

    @Override
    public void init() {

        final ControllerHost host = getHost();
        host.println("hello ");


        Preferences p = host.getPreferences();

        // These need to be instantiated in init(), bitwig will not allow calls to these methods from a callback
        mTransport = host.createTransport();
        mTrackBank = host.createTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES);


        mMidiMixController = new MidiMixController(host, mTrackBank);


        mMidiValToCoordMap = new HashMap<>();
        initMap(mMidiValToCoordMap);

        host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        host.getMidiInPort(0).setSysexCallback(this::onSysex0);

        cf2d = new ControllerFunction[8][8];

        cf2d[7][0] = this::scrollChannelsUp;
        cf2d[7][1] = this::scrollChannelsDown;

        mTrackBank.channelCount().markInterested();
        mMasterTrack = host.createMasterTrack(0);
        mMasterTrack.name().addValueObserver(new StringValueChangedCallback() {
            @Override
            public void valueChanged(String s) {
                setMasterName(s);
            }
        });
        mMasterTrack.getVolume().markInterested();

        for (int y = 0; y < NUM_TRACKS; y++) {  // I ❤ λ's
            final int channel = y;
            final Track track = mTrackBank.getChannel(channel);
            deviceBanks[channel] = track.createDeviceBank(2);
            cf2d[0][y] = (msg) -> setSend(channel, 0, msg);
            cf2d[1][y] = (msg) -> setSend(channel, 1, msg);
            cf2d[2][y] = (msg) -> setPan(channel, msg);
            cf2d[3][y] = (msg) -> toggleMute(channel, msg);
            cf2d[4][y] = (msg) -> toggleSolo(channel, msg);
            cf2d[5][y] = (msg) -> toggleRec(channel, msg);
            cf2d[6][y] = (msg) -> setVolume(channel, msg);

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

        host.showPopupNotification("midimix Initialized");
        host.println("\n   ----   init() done   ----");
        flushLights();

    }

    /**
     * Called when we receive short MIDI message on port 0.
     */
    private void onMidi0(ShortMidiMessage msg) {

        final ControllerHost host = getHost();
        host.println("channelCount = " + mTrackBank.channelCount().get());
        /*
        class xThread extends Thread {
            ShortMidiMessage msg;
            ControllerHost host;
            xThread(ShortMidiMessage msg, ControllerHost host) {
                this.msg = msg;
                this.host = host;
            }
            public void run() {
                try {
                    host.println("msg {"+ msg.getData1()+"} sleeping");
                    Process process = new ProcessBuilder(
                        //"H:\\runme.bat").start();
                        "bash.exe", "-c", "usr/bin/python3 ~/f.py").start();
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;

                    //System.out.printf("Output of running %s is:", Arrays.toString(args));

                    while ((line = br.readLine()) != null) {
                        host.println(line);
                    }
                    sleep(50);
                     host.println("msg {"+ msg.getData1()+"} awake");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        Thread t = new xThread(msg, host);
        t.start();
        host.println("After thread");
        */

        for (int i = 0; i < 8; i++) {
            String name = mTrackBank.getChannel(i).name().get();
            boolean isGroup = mTrackBank.getChannel(i).isGroup().get();
            host.println("track: "+i+" name = " + name + " group: " + isGroup);
            if (name.equals(getMasterName())) {
                host.println("   MASTER   ");
            }
        }

        String msgType;
        if (msg.isNoteOn() || msg.isNoteOff()) {
            //host.getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), msg.getData2());
            msgType = "note";
        } else if (msg.isControlChange()) {
            msgType = "cc";
        } else {
            msgType = "other (pb, press, pc)";
        }

        //host.println("msgType: " + msgType);

        // Special commands, temporary?
        if (msg.getData1() == 62) {
            host.println(" --# { Turning off all lights }");
            turnAllLightsOff();
        } else if (msg.getData1() == 27 && msg.isNoteOn()) {
            host.println(" --# { Fixing all lights }");
            flushLights();
        }

        MidiId mi = new MidiId(msg.getStatusByte(), msg.getData1());

        Coord mc = mMidiValToCoordMap.get(mi);
        if (mc == null) {
            //host.println(
            //    String.format("%5s %4d  no mc", mi.status, mi.data1));
        } else {
            host.println(
                String.format("%5s %4d  %d %d", mi.status, mi.data1, mc.x, mc.y));
            ControllerFunction c = cf2d[mc.x][mc.y];
            c.op(msg);
            //mMidiMixController.getName(mc.y);

        }
        //host.println(
        //    String.format("%8s %4d  %4d  %4d", msgType, msg.getStatusByte(),
//msg.getData1(), msg.getData2()));


    }


    private void scrollChannelsDown(ShortMidiMessage msg) {
        //getHost().getMidiOutPort().sendMidi(144, data1, msg.getData2());
        ControllerHost host = getHost();
        host.println("data1 = " + msg.getData1());
        if (msg.getStatusByte() == ShortMidiMessage.NOTE_ON) {
            host.println("down on");
            mTrackBank.scrollChannelsDown();
        } else if (msg.getStatusByte() == ShortMidiMessage.NOTE_OFF) {
            flushLights();
            host.println("down off");
        }
    }


    private void scrollChannelsUp(ShortMidiMessage msg) {
        ControllerHost host = getHost();
        // getHost().getMidiOutPort().sendMidi(msg.getStatusByte(), msg.getData1(), msg.getData2());
        host.println("data1 = " + msg.getData1());
        if (msg.getStatusByte() == ShortMidiMessage.NOTE_ON) {
            host.println("up on");
            mTrackBank.scrollChannelsUp();
        } else if (msg.getStatusByte() == ShortMidiMessage.NOTE_OFF) {
            host.println("up off");
            flushLights();
        }

    }


    private void turnAllLightsOff() {
        ControllerHost host = getHost();
        host.println("Turning off all lights");
        int velocity = 0;
        for (int channel = 0; channel < 8; ++channel) {
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.MUTE][channel], velocity);
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.SOLO][channel], velocity);
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.ARM][channel], velocity);
        }
    }


    /**
     *
     */
    private void flushLights() {
        ControllerHost host = getHost();
        host.println("Fixing Lights");
        for (int channel = 0; channel < 8; ++channel) {
            int velocity = 0;
            final Track track = mTrackBank.getChannel(channel);
            boolean isTrack = false;
            if (track.name().get().equals("")) {
                host.println("NO TRACK!");
            } else {
                isTrack = true;
                host.println("IS TRACK!k");
            }
            if (!isTrack) {
                continue;
            }

            //host.println("channel: " + channel + " mute: " + track.getMute().get() + " solo: " + track.getSolo().get() + " arm: " + track.getArm().get());
            if (!track.getMute().get()) {
                velocity = 127;
            }
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.MUTE][channel], velocity);

            velocity = 0;
            if (track.getSolo().get()) {
                velocity = 127;
            }
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.SOLO][channel], velocity);

            velocity = 0;
            if (track.getArm().get()) {
                velocity = 127;
            }
            getHost().getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.ARM][channel], velocity);


        }
    }


    private double intValTo01(int value) {
        return value / 127.0;
    }

    private void setSend(int channel, int send, ShortMidiMessage msg) {
        getHost().println("in setSend  chan:" + channel + "  send: " + send + "  new val = " + intValTo01(msg.getData2()));
        mTrackBank.getChannel(channel).sendBank().getItemAt(send).value().set(intValTo01(msg.getData2()));
    }

    private void setVolume(int channel, ShortMidiMessage msg) {
        mTrackBank.getChannel(channel).getVolume().value().set(intValTo01(msg.getData2()));
    }

    private void setPan(int channel, ShortMidiMessage msg) {
        mTrackBank.getChannel(channel).getPan().value().set(intValTo01(msg.getData2()));
    }

    private void toggleMute(int channel, ShortMidiMessage msg) {
        Track track = mTrackBank.getChannel(channel);
        int midiVal = msg.getData2();
        if (!track.getMute().get()) {
            midiVal = 0;
        }
        getHost().getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), midiVal);
        track.getMute().toggle();
    }

    private void toggleRec(int channel, ShortMidiMessage msg) {
        final Track track = mTrackBank.getChannel(channel);
        int midiVal = msg.getData2();
        if (track.getArm().get()) {
            midiVal = 0;
        }
        getHost().getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), midiVal);
        track.getArm().toggle();
    }

    private void toggleSolo(int channel, ShortMidiMessage msg) {
        int midiVal = msg.getData2();
        final Track track = mTrackBank.getChannel(channel);
        if (track.getSolo().get()) {
            midiVal = 0;
        }
        getHost().getMidiOutPort(0).sendMidi(msg.getStatusByte(), msg.getData1(), midiVal);
        track.getSolo().toggle();
    }

    /**
     * Called when we receive sysex MIDI message on port 0.
     */
    private void onSysex0(final String data) {
        // MMC Transport Controls:
        switch (data) {
            case "f07f7f0605f7":
                mTransport.rewind();
                break;
            case "f07f7f0604f7":
                mTransport.fastForward();
                break;
            case "f07f7f0601f7":
                mTransport.stop();
                break;
            case "f07f7f0602f7":
                mTransport.play();
                break;
            case "f07f7f0606f7":
                mTransport.record();
                break;
        }
    }

    @Override
    public void exit() {
        // TODO: Perform any cleanup once the driver exits
        // For now just show a popup notification for verification that it is no longer running.
        turnAllLightsOff();
        getHost().showPopupNotification("midimix Exited");
    }

    @Override
    public void flush() {
        // TODO Send any updates you need here.
    }

}
