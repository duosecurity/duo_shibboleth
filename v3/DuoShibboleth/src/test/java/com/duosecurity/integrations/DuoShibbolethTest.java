package com.duosecurity.integrations;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import java.lang.reflect.*;

import org.junit.*;
import static org.junit.Assert.*;

public class DuoShibbolethTest {

    DuoShibboleth duoShibboleth;

    @Before
    public void setUp() {
        duoShibboleth = DuoShibboleth.instance("DIXXXXXXXXXXXXXXXXXX", 
            "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef", 
            "useacustomerprovidedapplicationsecretkey", 
            "fakeurl",
            "testuser");
    }

    @Test
    public void testNoPublicConstructors() throws ClassNotFoundException {
        Class duoShibbolethClass = Class.forName("com.duosecurity.integrations.DuoShibboleth");
        assertEquals(0, duoShibbolethClass.getConstructors().length);
    }

    @Test
    public void testNotSingleton() {
        DuoShibboleth otherDuoShibboleth = DuoShibboleth.instance("DIXXXXXXXXXXXXXXXXXX", 
            "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef", 
            "useacustomerprovidedapplicationsecretkey", 
            "fakeurl",
            "testuser");
        assertFalse(duoShibboleth==otherDuoShibboleth);
    }

    @Test
    public void testPerformPreauth_whenFailmodeSecure() {
        try {
            assertEquals("auth", duoShibboleth.performPreauth("secure"));
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

    @Test
    public void testPerformPreauth_whenFailmodeInvalid() {
        try {
            duoShibboleth.performPreauth("bad");
            fail("Expected IllegalArgumentException.");
        } catch (Exception e) {
        }
    }

    @Test
    public void testPerformPreauth_whenFailmodeSafeAndDuoUnreachable() {
        try {
            assertEquals("allow", duoShibboleth.performPreauth("safe"));
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

    @Test
    public void testSignRequest() {
        assertNotNull(duoShibboleth.signRequest());
    }

    @Test
    public void testverifyResponseEqualsUsername_whenResponseValid() {
        String request_sig = duoShibboleth.signRequest();
        String[] sigs = request_sig.split(":");
        String app_sig = sigs[1];

        String response = "AUTH|dGVzdHVzZXJ8RElYWFhYWFhYWFhYWFhYWFhYWFh8MTYxNTcyNzI0Mw==|d20ad0d1e62d84b00a3e74ec201a5917e77b6aef:" + app_sig;
        assertTrue(duoShibboleth.verifyResponseEqualsUsername(response));
    }

    @Test
    public void testverifyResponseEqualsUsername_whenResponseForAnotherUser() {
        String request_sig = duoShibboleth.signRequest();
        String[] sigs = request_sig.split(":");
        String app_sig = sigs[1];

        String response = "AUTH|dGVzdHVzZXJ8RElYWFhYWFhYWFhYWFhYWFhYWFh8MTYxNTcyNzI0Mw==|d20ad0d1e62d84b00a3e74ec201a5917e77b6aef:" + app_sig;

        DuoShibboleth otherDuoShibboleth = DuoShibboleth.instance("DIXXXXXXXXXXXXXXXXXX", 
            "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef", 
            "useacustomerprovidedapplicationsecretkey", 
            "fakeurl",
            "usertoauthenticate");

        assertFalse(otherDuoShibboleth.verifyResponseEqualsUsername(response));
    }

    @Test
    public void testverifyResponseEqualsUsername_whenResponseBad() {
        String response = "junkresponse";
        assertFalse(duoShibboleth.verifyResponseEqualsUsername(response));
    }

    @Test
    public void testverifyResponseEqualsUsername_whenResponseEmpty() {
        String response = "";
        assertFalse(duoShibboleth.verifyResponseEqualsUsername(response));
    }

}
