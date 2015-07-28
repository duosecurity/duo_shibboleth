package com.duosecurity.integrations;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.squareup.okhttp.Response;

import com.duosecurity.duoweb.DuoWeb;
import com.duosecurity.client.Http;

public class DuoShibboleth {

    /** Number of tries to attempt a preauth call to Duo. */
    private static final int MAX_TRIES = 3;

    private final Logger log = LoggerFactory.getLogger(DuoShibboleth.class);

    private final String ikey;
    private final String skey;
    private final String akey;
    private final String host;
    private final String username;

    public static DuoShibboleth instance(String ikey, String skey, String akey, String host, String username) {
        if (ikey == null) {
            throw new IllegalArgumentException("Null value not allowed for ikey");
        }
        if (skey == null) {
            throw new IllegalArgumentException("Null value not allowed for skey");
        }
        if (akey == null) {
            throw new IllegalArgumentException("Null value not allowed for akey");
        }
        if (host == null) {
            throw new IllegalArgumentException("Null value not allowed for host");
        }
        if (username == null) {
            throw new IllegalArgumentException("Null value not allowed for username");
        }
        return new DuoShibboleth(ikey, skey, akey, host, username);
    }

    private DuoShibboleth() {
        this.ikey = null;
        this.skey = null;
        this.akey = null;
        this.host = null;
        this.username = null;
    }

    private DuoShibboleth(String ikey, String skey, String akey, String host, String username) {
        this.ikey = ikey;
        this.skey = skey;
        this.akey = akey;
        this.host = host;
        this.username = username;
    }

    private Response sendPreAuthRequest(String username) throws Exception {
        Http request = new Http("POST", host, "/auth/v2/preauth", 10);
        request.addParam("username", username);
        request.signRequest(ikey, skey);
        return request.executeHttpRequest();
    }
    
    public String performPreauth(String failmode) throws Exception {
        if (failmode.equals("secure")) {
            return "auth";
        } else if (!failmode.equals("safe")) {
            throw new IllegalArgumentException("Failmode must be one of either safe or secure.");
        }

        // Check if Duo authentication is even necessary by calling preauth
        for (int i = 0; ; i++) {
            try {
                Response preAuthResponse = sendPreAuthRequest(username);
                int statusCode = preAuthResponse.code();
                if (statusCode/100 == 5) {
                    log.warn("Duo 500 error. Fail open for user:" + username);
                    return "allow";
                }
                // parse response
                JSONObject json = new JSONObject(preAuthResponse.body().string());
                if (!json.getString("stat").equals("OK")) {
                    throw new Exception(
                    "Duo error code (" + json.getInt("code") + "): " + json.getString("message"));
                }
                String result = json.getJSONObject("response").getString("result");
                if (result.equals("allow")) {
                    log.info("Duo 2FA bypass for user:" + username);
                    return "allow";
                }
                break;
            } catch (java.io.IOException e) {
                if (i >= MAX_TRIES-1){
                    log.warn("Duo server unreachable. Fail open for user:" + username);
                    return "allow";
                }
            }
        }
        return "auth";
    }

    public String signRequest() {
        return DuoWeb.signRequest(ikey, skey, akey, username);
    }

    public boolean verifyResponseEqualsUsername(String duoResponse) {
        try {
            String duoVerifiedResponse = DuoWeb.verifyResponse(ikey, skey, akey, duoResponse);
            boolean usernameMatches = duoVerifiedResponse.equals(username);
            if (usernameMatches) {
                log.info(username + " successfully Duo two-factor authenticated.");
                return true;
            } else {
                log.info(username + " attempted Duo two-factor authentication but username " + duoVerifiedResponse + " was sent in the Duo response.");
            }
        } catch (Exception e) {
            log.error("An exception occurred while " + username + " attempted Duo two-factor authentication.", e);
        }
        return false;
    }

}