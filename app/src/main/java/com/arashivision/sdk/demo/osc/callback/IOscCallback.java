package com.arashivision.sdk.demo.osc.callback;

public interface IOscCallback {

    default void onStartRequest() {
    }

    void onSuccessful(Object object);

    void onError(String message);
}
