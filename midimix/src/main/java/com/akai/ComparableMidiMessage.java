package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

public class ComparableMidiMessage extends ShortMidiMessage {

    ComparableMidiMessage(ShortMidiMessage message) {
        super(message.getStatusByte(), message.getData1(), message.getData2());
    }

    public ComparableMidiMessage(int status, int data1, int data2) {
        super(status, data1, data2);
    }

    /* I implement equals() and hashCode() because I want to use this class as a key in a HashMap.
 */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortMidiMessage message = (ShortMidiMessage) o ;
        return getStatusByte() == message.getStatusByte()
            && getData1() == message.getData1();
    }

    @Override
    public int hashCode() {
        return 31 * getStatusByte() + getData1();
    }

}
