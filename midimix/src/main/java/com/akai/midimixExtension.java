package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;

public class midimixExtension extends ControllerExtension {

    private MidiMixController mMidiMixController;
    private MultiDeviceController mMultiDeviceController;
    private boolean isSoloHeld = false;
    private MidiMessageHandler mActiveController;

    public interface MidiMessageHandler {
        void handleMidiMessage(ShortMidiMessage msg);

        void flushLights();

        void doPrint();
    }

    protected midimixExtension(final midimixExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }


    @Override
    public void init() {
        final ControllerHost host = getHost();

        mMidiMixController = new MidiMixController(host);
        mMultiDeviceController = new MultiDeviceController(host);
        mActiveController = mMidiMixController;
        MidiMixMapping.init();
        MidiMixMapping.host = host;

        host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        host.getMidiInPort(0).setSysexCallback(this::onSysex0);

        host.showPopupNotification("midimix Initialized");
        host.println("\n   ----   init() done   ----");

    }

    /**
     * Called when we receive short MIDI message on port 0.
     */
    private void onMidi0(ShortMidiMessage msg) {
        if (msg.getData1() == MidiMixMapping.SOLO) {
            if (msg.isNoteOn()) {
                isSoloHeld = true;
            } else if (msg.isNoteOff()) {
                isSoloHeld = false;
            }
        }

        if (isSoloHeld) {
            switch (msg.getData1()) {
                case MidiMixMapping.BANK_LEFT:
                    mActiveController = mMidiMixController;
                    getHost().showPopupNotification("Mixer mode");
                    break;
                case MidiMixMapping.BANK_RIGHT:
                    mActiveController = mMultiDeviceController;
                    getHost().showPopupNotification("Multi-device mode");
                    break;
            }
        }
        mActiveController.handleMidiMessage(msg);
    }

    void printMessage(ShortMidiMessage msg) {
        getHost().println("midiMessage status: " + msg.getStatusByte()
                + " data1: " + msg.getData1() + " data2: " + msg.getData2() + " channel: " + msg.getChannel());
    }

    /**
     * Called when we receive sysex MIDI message on port 0.
     */
    private void onSysex0(final String data) {
        getHost().println("Somehow triggered a sysex: " + data);
    }

    @Override
    public void exit() {
        mMidiMixController.turnAllLightsOff();
        getHost().showPopupNotification("midimix Exited");
    }

    @Override
    public void flush() {

        mActiveController.flushLights();
        mActiveController.doPrint();
    }

}
