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

import edu.internet2.middleware.shibboleth.idp.config.profile.authn.AbstractLoginHandlerFactoryBean;

/**
 * Factory bean for {@link TwoFactorLoginHandler}s.
 */
public class TwoFactorLoginHandlerFactoryBean extends AbstractLoginHandlerFactoryBean{

    /** URL to authentication servlet. */
    private String authenticationServletURL;

    /**
     * Gets the URL to authentication servlet.
     * 
     * @return URL to authentication servlet
     */
    public String getAuthenticationServletURL() {
        return authenticationServletURL;
    }

    /**
     * Sets URL to authentication servlet.
     * 
     * @param url URL to authentication servlet
     */
    public void setAuthenticationServletURL(String url) {
        authenticationServletURL = url;
    }

    // Duo attributes
    private String skey = null;
    private String ikey = null;
    private String akey = null;
    private String host = null;

    // Duo attribute getter/setters
    public String getSkey(){
        return skey;
    }
    public void setSkey(String s){
        skey = s;
    }
    public String getIkey(){
        return ikey;
    }
    public void setIkey(String s){
        ikey = s;
    }
    public String getAkey(){
        return akey;
    }
    public void setAkey(String s){
        akey = s;
    }
    public String getHost(){
        return host;
    }
    public void setHost(String s){
        host = s;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        TwoFactorLoginHandler handler = new TwoFactorLoginHandler(authenticationServletURL, skey, ikey, akey, host);

        populateHandler(handler);

        return handler;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return TwoFactorLoginHandler.class;
    }
}
