/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/* Copyright 2012 Duo Security Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.duosecurity.shibboleth.idp.twofactor;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;
import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;

/**
 * Login handler to authenticate a username and password against a JAAS source,
 * then authenticate with Duo Security.  Based on UsernamePasswordLoginHandler.
 * 
 */
public class TwoFactorLoginHandler extends AbstractLoginHandler {

     /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TwoFactorLoginHandler.class);

    /** The context-relative path of the servlet used to perform authentication. */
    private String authenticationServletPath;

    // Duo attributes.
    private String skey;
    private String ikey;
    private String akey;
    private String host;

    /**
     * Constructor.
     * 
     * @param servletPath context-relative path to the authentication servlet, may start with "/"
     * @param skeyIn Duo skey
     * @param ikeyIn Duo ikey
     * @param akeyIn Duo akey
     * @param hostIn Duo host
     */
    public TwoFactorLoginHandler(String servletPath, String skeyIn, String ikeyIn, String akeyIn, String hostIn) {
        super();
        setSupportsPassive(false);
        setSupportsForceAuthentication(true);
        authenticationServletPath = servletPath;
        skey = skeyIn;
        ikey = ikeyIn;
        akey = akeyIn;
        host = hostIn;
    }

    /** {@inheritDoc} */
    public void login(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        // forward control to the servlet.
        try {
            httpRequest.getSession().setAttribute(TwoFactorLoginServlet.SKEY_KEY, skey);
            httpRequest.getSession().setAttribute(TwoFactorLoginServlet.IKEY_KEY, ikey);
            httpRequest.getSession().setAttribute(TwoFactorLoginServlet.AKEY_KEY, akey);
            httpRequest.getSession().setAttribute(TwoFactorLoginServlet.HOST_KEY, host);

            String authnServletUrl = HttpServletHelper.getContextRelativeUrl(httpRequest, authenticationServletPath)
                    .buildURL();
            log.debug("Redirecting to {}", authnServletUrl);
            httpResponse.sendRedirect(authnServletUrl);
            return;
        } catch (IOException ex) {
            log.error("Unable to redirect to authentication servlet.", ex);
        }

    }
}
