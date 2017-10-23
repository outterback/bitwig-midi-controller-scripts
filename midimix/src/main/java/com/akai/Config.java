package com.akai;

import com.bitwig.extension.controller.api.ControllerHost;

public class Config {
    final ControllerHost host;

    private boolean debugPrintActive = false;

    public Config(ControllerHost host) {
        this.host = host;
    }

    void debugPrint(String str) {
        if (debugPrintActive) {
            host.println(str);
        }
    }
}
