package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.HashMap;

public final class MidiMixMapping {
    private MidiMixMapping() {}

    static ControllerHost host;
    private static boolean initialized = false;
     /*  This is the first data byte sent from each knob/fader/button on the akai
     *  Bank left, bank right, solo and master are in the last, shorter row.
     *
     *  Start from upper left corner of the midimix
     *  Solo is a hold button, mute and solo are folded out into two rows (
     *  (0,0) is send A track 2, (0,7) is send A track 8, (6, 3) is fader track 4 e.t.c.
     *
     */
    private static final int[][] midimixData1Map =
           {{16, 20, 24, 28, 46, 50, 54, 58},   // 0  cc        send a
            {17, 21, 25, 29, 47, 51, 55, 59},   // 1  cc        send b
            {18, 22, 26, 30, 48, 52, 56, 60},   // 2  cc        pan
            {1, 4, 7, 10, 13, 16, 19, 22},      // 3  note      mute
            {2, 5, 8, 11, 14, 17, 20, 23},      // 4  note      solo + mute
            {3, 6, 9, 12, 15, 18, 21, 24},      // 5  note      rec
            {19, 23, 27, 31, 49, 53, 57, 61},   // 6  cc        fader
            {25, 26, 27, 62}};                  // 7  note      bank left, bank right, solo, master


    static final int BANK_LEFT = 25;
    static final int BANK_RIGHT = 26;
    static final int SOLO = 27;

    private static final HashMap<ComparableMidiMessage, Coord> mMidiValToMatrixIndex = new HashMap<>();

    static int getData1(int x, int y) {
        if (!initialized) {
            init();
        }
        Coord c = new Coord(x, y);
        if (c.x*8 + c.y > 59 || c.x + c.y < 0) {
            return -1;
        }
        return midimixData1Map[c.x][c.y];
    }

    static Coord getCoordFromMidi(ShortMidiMessage msg) {
        if (!initialized) {
            init();
        }
        Coord c = mMidiValToMatrixIndex.get(new ComparableMidiMessage(msg));
        if (c == null) {
            //host.println("c == null");
        } else {
            //host.println("c.x: " + c.x + " c.y: " + c.y);
        }
        return c;
    }


    static void init() {
        /*
         * This registers where in the map to look for a function
         */
        for (int i = 0; i < 7; ++i) { // Skip extras
            for (int j = 0; j < 8; ++j) {
                int status = ShortMidiMessage.CONTROL_CHANGE;
                if (i == Row.MUTE || i == Row.SOLO || i == Row.ARM) {
                    status = ShortMidiMessage.NOTE_ON;
                }
                int data1 = midimixData1Map[i][j];

                mMidiValToMatrixIndex.put(new ComparableMidiMessage(status, data1, 0), new Coord(i, j));
            }
            mMidiValToMatrixIndex.put(new ComparableMidiMessage(ShortMidiMessage.NOTE_ON, 25, 0), new Coord(Row.EXTRA, 0));

            mMidiValToMatrixIndex.put(new ComparableMidiMessage(ShortMidiMessage.NOTE_ON, 26, 0), new Coord(Row.EXTRA, 1));
            mMidiValToMatrixIndex.put(new ComparableMidiMessage(ShortMidiMessage.CONTROL_CHANGE, 62, 0), new Coord(Row.EXTRA, 3));
        }
        initialized = true;
    }


}
