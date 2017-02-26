package com.solderbyte.tessract;

import java.util.HashMap;

public class Config {
    public static final String PACKAGE = "com.solderbyte.tessract";

    public static final String DEVICE_NAME = PACKAGE + ".device.name";
    public static final String DEVICE_ADDRESS = PACKAGE + ".device.address";
    public static final String DEVICE_STATE = PACKAGE + ".device.state";

    public static final String INTENT = PACKAGE + ".intent";
    public static final String INTENT_BLUETOOTH = INTENT + ".bluetooth";
    public static final String INTENT_BLUETOOTH_CONNECT = INTENT_BLUETOOTH + ".connect";
    public static final String INTENT_BLUETOOTH_DISCONNECT = INTENT_BLUETOOTH + ".disconnect";
    public static final String INTENT_EXTRA_MSG = INTENT + ".extra.message";
    public static final String INTENT_SERVICE = INTENT + ".service";
    public static final String INTENT_SERVICE_START = INTENT_SERVICE + ".start";
    public static final String INTENT_SERVICE_STOP = INTENT_SERVICE + ".stop";
    public static final String INTENT_SHUTDOWN = INTENT_SERVICE + ".shutdown";


    public static HashMap<Integer, String> SERVICE_FLAGS = new HashMap<Integer, String>() {{
        put(0, "START_STICKY_COMPATIBILITY");
        put(1, "START_FLAG_REDELIVERY, START_STICKY");
        put(2, "START_FLAG_RETRY, START_NOT_STICKY");
        put(3, "START_REDELIVER_INTENT");
        put(15, "START_CONTINUATION_MASK");
    }};

}
