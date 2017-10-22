package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

/**
 * Created by Oscar on 10/18/2017.
 */
public class MidiMixController {

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

    private final ControllerHost host;
    private final TrackBank trackBank;

    MidiMixController(ControllerHost host, TrackBank trackBank) {
        this.host = host;
        this.trackBank = host.createTrackBank(8, 2, 0);
        for (int i = 0; i < 8; i++) {
            this.trackBank.getChannel(i).name().markInterested();
        }
        //this.trackBank = trackBank;
    }

    void getName(int channel) {
        host.println("channel: "+ channel +" name: \"" + this.trackBank.getChannel(channel).name().get() + "\"");
    }

    void scrollChannelsDown(ShortMidiMessage msg) {
        host.println("data1 = " + msg.getData1());
        if (msg.getStatusByte() == ShortMidiMessage.NOTE_ON) {
            host.println("down on");
            trackBank.scrollChannelsDown();
        } else if (msg.getStatusByte() == ShortMidiMessage.NOTE_OFF) {
            flushLights();
            host.println("down off");
        }
    }


    void scrollChannelsUp(ShortMidiMessage msg) {
        host.println("data1 = " + msg.getData1());
        if (msg.getStatusByte() == ShortMidiMessage.NOTE_ON) {
            host.println("up on");
            trackBank.scrollChannelsUp();
        } else if (msg.getStatusByte() == ShortMidiMessage.NOTE_OFF) {
            host.println("up off");
            flushLights();
        }

    }


    void turnAllLightsOff() {
        host.println("Turning off all lights");
        int velocity = 0;
        for (int channel = 0; channel < 8; ++channel) {
            host.getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.MUTE][channel], velocity);
            host.getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.SOLO][channel], velocity);
            host.getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.ARM][channel], velocity);
        }
    }


    /**
     *
     */
    void flushLights() {
        host.println("Fixing Lights");
        for (int channel = 0; channel < 8; ++channel) {
            int velocity = 0;
            final Track track = trackBank.getChannel(channel);
            host.println("channel: " + channel + " mute: " + track.getMute().get() + " solo: " + track.getSolo().get() + " arm: " + track.getArm().get());
            if (!track.getMute().get()) {
                velocity = 127;
            }
            host.getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.MUTE][channel], velocity);

            velocity = 0;
            if (track.getSolo().get()) {
                velocity = 127;
            }
            host.getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.SOLO][channel], velocity);

            velocity = 0;
            if (track.getArm().get()) {
                velocity = 127;
            }
            host.getMidiOutPort(0).sendMidi(144, midiKeys2d[Row.ARM][channel], velocity);


        }
    }
}
