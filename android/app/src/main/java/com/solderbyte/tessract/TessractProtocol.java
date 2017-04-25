package com.solderbyte.tessract;


import android.util.Log;

public class TessractProtocol {

    // Log tag
    private static final String LOG_TAG = "Tessract:Protocol";

    // Defaults
    private static String[] defaultActions = {"Off", "On", "Blink", "Pulse"};
    private static String[] defaultAmounts = {"1", "2", "3", "5"};
    private static String[] defaultDurations = {"Fastest", "Fast", "Normal", "Slow"};
    private static String[] defaultRepeats = {"none", "5 secs", "15 secs", "30 secs"};

    /*              Protocol
    ---------------------------------------------
    | 00000000 | 00000000 | 00000000 | 00000000 |
    ---------------------------------------------
        |           |           |           |
        |           |           |           |
        v           v           v           v
      Command      Red        Green       Blue
        |
        |
        |--> 000000XX Action {00: Off, 01: On, 10: Blink, 11: Pulse}
        |
        |--> 0000XX00 Blink/Pulse amount {00: 1, 01: 2, 10: 3, 11: 5}
        |
        |--> 00XX0000 Blink/Pulse duration {00: Fastest, 01: Fast, 10: Normal, 11: Slow}
        |
        |--> XX000000 Blink/Pulse Repeat {00: 0, 01: 5 secs, 10: 15 secs, 11: 30 secs}
    */

    public static byte[] toProtocol(int action, int amount, int duration, int repeat, String rgb) {
        Log.d(LOG_TAG, "toProtocol: " + defaultActions[action] + " " + defaultAmounts[amount] + " " + defaultDurations[duration] + " " + defaultRepeats[repeat]);

        byte command = 0;
        byte red = 0;
        byte green = 0;
        byte blue = 0;

        // Shift in values
        command |= action;
        command <<= 2;
        command |= amount;
        command <<= 2;
        command |= duration;
        command <<= 2;
        command |= repeat;

        // Shift in colors
        red |= Integer.parseInt(rgb.substring(0, 2), 16);
        green |= Integer.parseInt(rgb.substring(2, 4), 16);
        blue |= Integer.parseInt(rgb.substring(4, 6), 16);

        Log.d(LOG_TAG, "Command: " + Integer.toBinaryString(command));
        Log.d(LOG_TAG, "Red: " + Integer.toBinaryString(red));
        Log.d(LOG_TAG, "Green: " + Integer.toBinaryString(green));
        Log.d(LOG_TAG, "Blue: " + Integer.toBinaryString(blue));

        byte[] data = new byte[]{command, red, green, blue};
        return data;
    }

    public static String colorToHex(int color) {
        String rgb = String.format("%06X", (0xFFFFFF & color));
        Log.d(LOG_TAG, "colorToHex: " + color + " -> " +rgb);
        return rgb;
    }
}
