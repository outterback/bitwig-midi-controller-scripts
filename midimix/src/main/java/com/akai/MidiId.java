package com.akai;

class MidiId {
    int status;
    int data1;

    MidiId(int status, int data1) {
        this.status = status;
        this.data1 = data1;
    }

    /* I implement equals() and hashCode() because I want to use this class as a key in a HashMap.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MidiId midiId = (MidiId) o;
        return status == midiId.status && data1 == midiId.data1;
    }

    @Override
    public int hashCode() {
        int result = status;
        result = 31 * result + data1;
        return result;
    }
}

