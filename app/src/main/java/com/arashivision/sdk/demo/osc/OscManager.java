package com.arashivision.sdk.demo.osc;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arashivision.sdk.demo.model.CaptureExposureData;
import com.arashivision.sdk.demo.osc.callback.IOscCallback;
import com.arashivision.sdk.demo.osc.delegate.IOscRequestDelegate;
import com.arashivision.sdkcamera.camera.InstaCameraManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OscManager {

    private static class OscManagerHolder {
        private static final OscManager INSTANCE = new OscManager();
    }

    private OscManager() {
    }

    public static OscManager getInstance() {
        return OscManagerHolder.INSTANCE;
    }

    private static final String CMD_STATE_DONE = "done";
    private static final String CMD_STATE_IN_PROGRESS = "inProgress";
    private static final String CMD_STATE_ERROR = "error";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mRequestExecutor = Executors.newSingleThreadExecutor();
    private IOscRequestDelegate mOscRequestDelegate;

    public void setOscRequestDelegate(@NonNull IOscRequestDelegate oscRequestDelegate) {
        mOscRequestDelegate = oscRequestDelegate;
    }

    public void setOptions(@NonNull String options, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                String cmd = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{" + options + "}}}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onSuccessful(null));
                        }
                    } else if (callback != null) {
                        mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                    }
                } else if (callback != null) {
                    mHandler.post(() -> callback.onError(oscResult.getResult()));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void takePicture(@Nullable String options, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                if (options != null) {
                    String cmd = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{" + options + "}}}";
                    OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                    if (oscResult.isSuccessful()) {
                        JSONObject jsonObject = new JSONObject(oscResult.getResult());
                        if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                            if (callback != null) {
                                mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                            }
                            return;
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(oscResult.getResult()));
                        }
                        return;
                    }
                }
                String cmdId;
                String cmd = "{\"name\":\"camera.takePicture\"}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_IN_PROGRESS.equals(jsonObject.getString("state"))) {
                        cmdId = jsonObject.getString("id");
                    } else {
                        if (callback != null) {
                            String errorMsg = getErrorMessage(oscResult.getResult());
                            mHandler.post(() -> callback.onError(errorMsg));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        String errorMsg = oscResult.getResult();
                        mHandler.post(() -> callback.onError(errorMsg));
                    }
                    return;
                }
                if (!TextUtils.isEmpty(cmdId)) {
                    cmd = "{\"id\":\"" + cmdId + "\"}";
                    for (int i = 0; i < 60; i++) {
                        oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdStatusUrl(), cmd, getHttpHeaders());
                        if (oscResult.isSuccessful()) {
                            JSONObject jsonObject = new JSONObject(oscResult.getResult());
                            if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                                if (callback != null) {
                                    JSONObject results = jsonObject.getJSONObject("results");
                                    String path = null;
                                    if (results.has("_fileGroup")) {
                                        path = results.getString("_fileGroup");
                                    }
                                    if (TextUtils.isEmpty(path) || "[]".equals(path)) {
                                        path = results.getString("fileUrl");
                                    }
                                    path = path.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\\\/", "/");
                                    String[] paths = path.split(",");
                                    for (int j = 0; j < paths.length; j++) {
                                        paths[j] = paths[j].replaceAll("\"", "");
                                    }
                                    mHandler.post(() -> callback.onSuccessful(paths));
                                }
                                return;
                            } else if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                                if(callback != null){
                                    String errorMsg = getErrorMessage(oscResult.getResult());
                                    mHandler.post(() -> callback.onError(errorMsg));
                                }
                                return;
                            }
                        } else {
                            String message = oscResult.getResult();
                            if (callback != null) {
                                mHandler.post(() -> callback.onError(message));
                            }
                            return;
                        }
                        Thread.sleep(1000);
                    }
                    if (callback != null) {
                        mHandler.post(() -> callback.onError("Timeout. Please use command \"listFiles\" to get."));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void startRecord(@Nullable String options, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                if (options != null) {
                    String cmd = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{" + options + "}}}";
                    OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                    if (oscResult.isSuccessful()) {
                        JSONObject jsonObject = new JSONObject(oscResult.getResult());
                        if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                            if (callback != null) {
                                mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                            }
                            return;
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(oscResult.getResult()));
                        }
                        return;
                    }
                }
                String cmd = "{\"name\":\"camera.startCapture\"}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onSuccessful(null));
                        }
                    } else if (callback != null) {
                        mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                    }
                } else if (callback != null) {
                    mHandler.post(() -> callback.onError(oscResult.getResult()));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void stopRecord(@Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                String cmd = "{\"name\":\"camera.stopCapture\"}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            JSONObject results = jsonObject.getJSONObject("results");
                            String path = results.getString("fileUrls");
                            path = path.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\\\/", "/");
                            String[] paths = path.split(",");
                            for (int i = 0; i < paths.length; i++) {
                                paths[i] = paths[i].replaceAll("\"", "");
                            }
                            if (paths.length == 2 && paths[1].contains("_00_") && paths[0].contains("_10_")) {
                                String tmp = paths[0];
                                paths[0] = paths[1];
                                paths[1] = tmp;
                            }
                            mHandler.post(() -> callback.onSuccessful(paths));
                        }
                    } else if (callback != null) {
                        mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                    }
                } else if (callback != null) {
                    mHandler.post(() -> callback.onError(oscResult.getResult()));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void customRequest(@NonNull String oscApi, @Nullable String content, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                OSCResult oscResult;
                if (content == null) {
                    oscResult = mOscRequestDelegate.sendRequestByGet(getOscUrl(oscApi), getHttpHeaders());
                } else {
                    oscResult = mOscRequestDelegate.sendRequestByPost(getOscUrl(oscApi), content, getHttpHeaders());
                }
                if (oscResult.isSuccessful()) {
                    if (callback != null) {
                        mHandler.post(() -> callback.onSuccessful(oscResult.getResult()));
                    }
                } else if (callback != null) {
                    mHandler.post(() -> callback.onError(oscResult.getResult()));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void getCaptureExposureParamsForX2(@Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                String cmd1 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"hdr\":\"off\",\"captureMode\":\"image\",\"_FocusSensor\":3,\"_MultiVideoMode\":\"all\"}}}";
                OSCResult oscResult1 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd1, getHttpHeaders());
                if (oscResult1.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult1.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult1.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult1.getResult()));
                    }
                    return;
                }

                String cmd2 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"_StillExpoCalc\":1}}}";
                OSCResult oscResult2 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd2, getHttpHeaders());
                if (oscResult2.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult2.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult2.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult2.getResult()));
                    }
                    return;
                }

                CaptureExposureData exposureData = new CaptureExposureData();
                String cmd3 = "{\"name\":\"camera.getOptions\",\"parameters\":{\"optionNames\":[\"exposureProgram\",\"iso\",\"shutterSpeed\",\"whiteBalance\",\"_WbRGain\",\"_WbBGain\"]}}";
                OSCResult oscResult3 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd3, getHttpHeaders());
                if (oscResult3.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult3.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult3.getResult())));
                        }
                        return;
                    } else {
                        JSONObject results = jsonObject.getJSONObject("results");
                        JSONObject options = results.getJSONObject("options");
                        exposureData.setExposureProgram(options.getInt("exposureProgram"));
                        exposureData.setIso(options.getInt("iso"));
                        exposureData.setShutterSpeed(options.getDouble("shutterSpeed"));
                        exposureData.setWhiteBalance(options.getString("whiteBalance"));
                        exposureData.set_WbBGain(options.getInt("_WbBGain"));
                        exposureData.set_WbRGain(options.getInt("_WbRGain"));
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult3.getResult()));
                    }
                    return;
                }

                String cmd4 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"_StillExpoCalc\":0}}}";
                OSCResult oscResult4 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd4, getHttpHeaders());
                if (oscResult4.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult4.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult4.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult4.getResult()));
                    }
                    return;
                }
                if (callback != null) {
                    mHandler.post(() -> callback.onSuccessful(exposureData));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void takeSingleSensorPictureForX2(int sensor, @NonNull CaptureExposureData exposureData, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                String multiVideoMode = sensor == 1 ? "front" : "rear";
                String cmd1 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"hdr\":\"off\",\"captureMode\":\"image\",\"_FocusSensor\":" + sensor + ",\"_MultiVideoMode\":\"" + multiVideoMode + "\"}}}";
                OSCResult oscResult1 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd1, getHttpHeaders());
                if (oscResult1.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult1.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult1.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult1.getResult()));
                    }
                    return;
                }

                String cmd2 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"exposureProgram\":"
                        + exposureData.getExposureProgram() + ",\"iso\":" + exposureData.getIso() + ",\"shutterSpeed\":"
                        + exposureData.getShutterSpeed() + ",\"whiteBalance\":\"" + exposureData.getWhiteBalance() + "\",\"_WbRGain\":"
                        + exposureData.get_WbRGain() + ",\"_WbBGain\":" + exposureData.get_WbBGain() + "}}}";
                OSCResult oscResult2 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd2, getHttpHeaders());
                if (oscResult2.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult2.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult2.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult2.getResult()));
                    }
                    return;
                }

                String cmdId;
                String cmd3 = "{\"name\":\"camera.takePicture\"}";
                OSCResult oscResult3 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd3, getHttpHeaders());
                if (oscResult3.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult3.getResult());
                    if (CMD_STATE_IN_PROGRESS.equals(jsonObject.getString("state"))) {
                        cmdId = jsonObject.getString("id");
                    } else {
                        if (callback != null) {
                            String errorMsg = getErrorMessage(oscResult3.getResult());
                            mHandler.post(() -> callback.onError(errorMsg));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        String errorMsg = oscResult3.getResult();
                        mHandler.post(() -> callback.onError(errorMsg));
                    }
                    return;
                }

                if (!TextUtils.isEmpty(cmdId)) {
                    cmd3 = "{\"id\":\"" + cmdId + "\"}";
                    for (int i = 0; i < 60; i++) {
                        oscResult3 = mOscRequestDelegate.sendRequestByPost(getOscCmdStatusUrl(), cmd3, getHttpHeaders());
                        if (oscResult3.isSuccessful()) {
                            JSONObject jsonObject = new JSONObject(oscResult3.getResult());
                            if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                                if (callback != null) {
                                    JSONObject results = jsonObject.getJSONObject("results");
                                    String path = null;
                                    if (results.has("_fileGroup")) {
                                        path = results.getString("_fileGroup");
                                    }
                                    if (TextUtils.isEmpty(path) || "[]".equals(path)) {
                                        path = results.getString("fileUrl");
                                    }
                                    path = path.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\\\/", "/");
                                    String[] paths = path.split(",");
                                    for (int j = 0; j < paths.length; j++) {
                                        paths[j] = paths[j].replaceAll("\"", "");
                                    }
                                    mHandler.post(() -> callback.onSuccessful(paths));
                                }
                                return;
                            } else if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                                if(callback != null){
                                    String errorMsg = getErrorMessage(oscResult3.getResult());
                                    mHandler.post(() -> callback.onError(errorMsg));
                                }
                                return;
                            }
                        } else {
                            String message = oscResult3.getResult();
                            if (callback != null) {
                                mHandler.post(() -> callback.onError(message));
                            }
                            return;
                        }
                        Thread.sleep(1000);
                    }
                    if (callback != null) {
                        mHandler.post(() -> callback.onError("Timeout. Please use command \"listFiles\" to get."));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    private String getOscUrl(String oscApi) {
        return InstaCameraManager.getInstance().getCameraHttpPrefix() + oscApi;
    }

    private String getOscCmdExecuteUrl() {
        return getOscUrl("/osc/commands/execute");
    }

    private String getOscCmdStatusUrl() {
        return getOscUrl("/osc/commands/status");
    }

    private Map<String, String> getHttpHeaders() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json; charset= utf-8");
        headerMap.put("Accept", "application/json");
        headerMap.put("X-XSRF-Protected", "1");
        return headerMap;
    }

    private String getErrorMessage(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.has("error")) {
                JSONObject error = jsonObject.getJSONObject("error");
                String code = error.has("code") ? error.getString("code") + ". " : "";
                String message = error.has("message") ? error.getString("message") + "." : "";
                return code + message;
            }
        } catch (Exception ignore) {
            // ignore
        }
        return result;
    }
}
