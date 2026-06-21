package com.arashivision.sdk.demo.osc;

public class OSCResult {

    private final boolean isSuccessful;
    private final String result;

    public OSCResult(boolean isSuccessful, String result) {
        this.isSuccessful = isSuccessful;
        this.result = result;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getResult() {
        return result;
    }
}
