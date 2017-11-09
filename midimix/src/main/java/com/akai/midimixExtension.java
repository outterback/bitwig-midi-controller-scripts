package com.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;

public class midimixExtension extends ControllerExtension {

    private MidiMixController mMidiMixController;
    private MultiDeviceController mMultiDeviceController;
    protected midimixExtension(final midimixExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }


    @Override
    public void init() {
        final ControllerHost host = getHost();
        Preferences p = host.getPreferences();

        mMidiMixController = new MidiMixController(host);
        mMultiDeviceController = new MultiDeviceController(host);
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
        //mMidiMixController.handleMidiMessage(msg);
        mMultiDeviceController.handleMidiMessage(msg);
    }



    /**
     * Called when we receive sysex MIDI message on port 0.
     */
    private void onSysex0(final String data) {
        getHost().println("Somehow triggered a sysex: " + data);
    }

    @Override
    public void exit() {
        // TODO: Perform any cleanup once the driver exits
        // For now just show a popup notification for verification that it is no longer running.
        mMidiMixController.turnAllLightsOff();
        getHost().showPopupNotification("midimix Exited");
    }

    @Override
    public void flush() {


        //mMultiDeviceController.flushLights();
        mMultiDeviceController.doPrint();
        // TODO Send any updates you need here.
    }

}
