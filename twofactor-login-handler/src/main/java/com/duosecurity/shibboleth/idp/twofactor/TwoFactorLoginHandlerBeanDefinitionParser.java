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

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.idp.config.profile.authn.AbstractLoginHandlerBeanDefinitionParser;

public class TwoFactorLoginHandlerBeanDefinitionParser extends AbstractLoginHandlerBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(TwoFactorLoginHandlerNamespaceHandler.NAMESPACE, "TwoFactorLogin");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TwoFactorLoginHandlerBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return TwoFactorLoginHandlerFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, BeanDefinitionBuilder builder) {
        super.doParse(config, builder);

        if (config.hasAttributeNS(null, "authenticationServletURL")) {
            builder.addPropertyValue("authenticationServletURL", DatatypeHelper.safeTrim(config.getAttributeNS(null,
                    "authenticationServletURL")));
        } else {
            builder.addPropertyValue("authenticationServletURL", "/Authn/DuoUserPassword");
        }

        String jaasConfigurationURL = DatatypeHelper.safeTrim(config.getAttributeNS(null, "jaasConfigurationLocation"));
        log.debug("Setting JAAS configuration file to: {}", jaasConfigurationURL);
        System.setProperty("java.security.auth.login.config", jaasConfigurationURL);

        String skey = DatatypeHelper.safeTrim(config.getAttributeNS(null, "skey"));
        log.debug("Setting Duo skey to: {}", skey);
        builder.addPropertyValue("skey", skey);

        String ikey = DatatypeHelper.safeTrim(config.getAttributeNS(null, "ikey"));
        log.debug("Setting Duo ikey to: {}", ikey);
        builder.addPropertyValue("ikey", ikey);

        String akey = DatatypeHelper.safeTrim(config.getAttributeNS(null, "akey"));
        log.debug("Setting Duo akey to: {}", akey);
        builder.addPropertyValue("akey", akey);

        String host = DatatypeHelper.safeTrim(config.getAttributeNS(null, "host"));
        log.debug("Setting Duo host to: {}", host);
        builder.addPropertyValue("host", host);
    }
}
