package com.arashivision.sdk.demo.osc.delegate;

import com.arashivision.sdk.demo.osc.OSCResult;

import java.util.Map;

public interface IOscRequestDelegate {

    OSCResult sendRequestByGet(String url, Map<String, String> headerMap);

    OSCResult sendRequestByPost(String url, String content, Map<String, String> headerMap);
}
