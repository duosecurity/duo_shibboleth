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

import com.duosecurity.DuoWeb;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine;
import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationException;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.UsernamePrincipal;

import edu.internet2.middleware.shibboleth.idp.authn.provider.UsernamePasswordCredential;

/**
 * This Servlet authenticates a user via JAAS, and then authenticates with
 * Duo Security.  Based on UsernamePasswordLoginServlet.
 * The user's credential is always added to the returned {@link Subject} as
 * a {@link UsernamePasswordCredential} within the subject's private credentials.
 * 
 * By default, this Servlet assumes that the authentication method {@value AuthnContext#PPT_AUTHN_CTX} to be returned to
 * the authentication engine. This can be override by setting the servlet configuration parameter
 * {@value LoginHandler#AUTHENTICATION_METHOD_KEY}.
 */
public class TwoFactorLoginServlet extends HttpServlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 20120119L;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TwoFactorLoginServlet.class);

    /** The authentication method returned to the authentication engine. */
    private String authenticationMethod;

    /** Name of JAAS configuration used to authenticate users. */
    private String jaasConfigName = "ShibUserPassAuth";

    /** init-param which can be passed to the servlet to override the default JAAS config. */
    private final String jaasInitParam = "jaasConfigName";

    /** Login page name. */
    private String loginPage = "login.jsp";

    /** init-param which can be passed to the servlet to override the default login page. */
    private final String loginPageInitParam = "loginPage";

    /** Duo authentication page name. */
    private String duoPage = "duo.jsp";

    /** init-param which can be passed to the servlet to override the default Duo authentication page. */
    private final String duoPageInitParam = "duoPage";

    /** Parameter name to indicate login failure. */
    private final String failureParam = "loginFailed";

    /** HTTP request parameter containing the user name. */
    private final String usernameAttribute = "j_username";

    /** HTTP request parameter containing the user's password. */
    private final String passwordAttribute = "j_password";

    /** HTTP request parameter containing the response returned by Duo. */
    private final String duoResponseAttribute = "sig_response";

    /** the key in a HttpSession where user subjects are stored. */
    public static final String USER_SUBJECT_KEY = "duo.usersubject";

    /** keys in a HttpSevletRequest where Duo attributes are stored. */
    public static final String SKEY_KEY = "duo.skey";
    public static final String IKEY_KEY = "duo.ikey";
    public static final String AKEY_KEY = "duo.akey";
    public static final String HOST_KEY = "duo.host";

    /** {@inheritDoc} */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (getInitParameter(jaasInitParam) != null) {
            jaasConfigName = getInitParameter(jaasInitParam);
        }

        if (getInitParameter(loginPageInitParam) != null) {
            loginPage = getInitParameter(loginPageInitParam);
        }
        if (!loginPage.startsWith("/")) {
            loginPage = "/" + loginPage;
        }
        if (getInitParameter(duoPageInitParam) != null) {
            duoPage = getInitParameter(duoPageInitParam);
        }
        if (!duoPage.startsWith("/")) {
            duoPage = "/" + duoPage;
        }
        
        String method =
                DatatypeHelper.safeTrimOrNullString(config.getInitParameter(LoginHandler.AUTHENTICATION_METHOD_KEY));
        if (method != null) {
            authenticationMethod = method;
        } else {
            authenticationMethod = AuthnContext.PPT_AUTHN_CTX;
        }
    }

    /** {@inheritDoc} */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String username = request.getParameter(usernameAttribute);
        String password = request.getParameter(passwordAttribute);
        String duoResponse = request.getParameter(duoResponseAttribute);

        if (duoResponse != null) {
            // We have a Duo response, verify it.
            String ikey = (String)request.getSession().getAttribute(IKEY_KEY);
            String skey = (String)request.getSession().getAttribute(SKEY_KEY);
            String akey = (String)request.getSession().getAttribute(AKEY_KEY);
            // Remove Duo attributes, just in case the session will persist.
            request.getSession().removeAttribute(SKEY_KEY);
            request.getSession().removeAttribute(IKEY_KEY);
            request.getSession().removeAttribute(AKEY_KEY);

            String duoUsername = DuoWeb.verifyResponse(ikey, skey, akey, duoResponse);
            // Get the subject we stored in the session after authentication.
            Subject userSubject = (Subject)request.getSession().getAttribute(USER_SUBJECT_KEY);
            // Set authentication attributes if we find a principal
            // matching the Duo username; assume we were the only ones to
            // add a UsernamePrincpal.
            Set<UsernamePrincipal> principals = userSubject.getPrincipals(UsernamePrincipal.class);
            Iterator iter = principals.iterator();
            while (iter.hasNext()) {
                UsernamePrincipal principal = (UsernamePrincipal)iter.next();
                if (duoUsername.equals(principal.getName())) {
                    // Duo username matches the one we locally authed with,
                    // user is legit.
                    request.setAttribute(LoginHandler.SUBJECT_KEY, userSubject);
                    request.setAttribute(LoginHandler.AUTHENTICATION_METHOD_KEY, authenticationMethod);
                    request.getSession().removeAttribute(USER_SUBJECT_KEY);
                    AuthenticationEngine.returnToAuthenticationEngine(request, response);
                    return;
                }
            }
            // Something was fake, expired, or not matching.
            redirectToLoginPage(request, response);
            return;
        } else if (username == null || password == null) {
            // We don't have Duo response or user/pass, first interaction.
            redirectToLoginPage(request, response);
            return;
        } else {
            // We don't have a Duo response, we do have user/pass.
            // Send to Duo page only after verifying user/pass.
            try {
                authenticateUser(request, username, password);
                String ikey = (String)request.getSession().getAttribute(IKEY_KEY);
                String skey = (String)request.getSession().getAttribute(SKEY_KEY);
                String akey = (String)request.getSession().getAttribute(AKEY_KEY);
                String host = (String)request.getSession().getAttribute(HOST_KEY);
                // Remove Duo attributes, just in case the session will persist.
                request.getSession().removeAttribute(HOST_KEY);

                request.setAttribute("host", host);
                String sigRequest = DuoWeb.signRequest(ikey, skey, akey, username);
                request.setAttribute("sigRequest", sigRequest);
                redirectToDuoPage(request, response);
            } catch (LoginException e) {
                request.setAttribute(failureParam, "true");
                request.setAttribute(LoginHandler.AUTHENTICATION_EXCEPTION_KEY, new AuthenticationException(e));
                redirectToLoginPage(request, response);
            }
        }
    }

    /**
     * Sends the user to a page with an actionUrl attribute pointing back.
     * 
     * @param path path to page
     * @param request current request
     * @param response current response
     */
    protected void redirectToPage(String path, HttpServletRequest request, HttpServletResponse response) {

        StringBuilder actionUrlBuilder = new StringBuilder();
        if(!"".equals(request.getContextPath())){
            actionUrlBuilder.append(request.getContextPath());
        }
        actionUrlBuilder.append(request.getServletPath());
        
        request.setAttribute("actionUrl", actionUrlBuilder.toString());

        try {
            request.getRequestDispatcher(path).forward(request, response);
            log.debug("Redirecting to page {}", path);
        } catch (IOException ex) {
            log.error("Unable to redirect to page.", ex);
        } catch (ServletException ex) {
            log.error("Unable to redirect to page.", ex);
        }
    }

    /**
     * Sends the user to the login page.
     * 
     * @param request current request
     * @param response current response
     */
    protected void redirectToLoginPage(HttpServletRequest request, HttpServletResponse response) {
        redirectToPage(loginPage, request, response);
    }

    /**
     * Sends the user to the Duo authentication page.
     * 
     * @param request current request
     * @param response current response
     */
    protected void redirectToDuoPage(HttpServletRequest request, HttpServletResponse response) {
        redirectToPage(duoPage, request, response);
    }

    /**
     * Authenticate a username and password against JAAS. If authentication succeeds the subject is placed in the session.
     * 
     * @param request current authentication request
     * @param username the principal name of the user to be authenticated
     * @param password the password of the user to be authenticated
     * 
     * @throws LoginException thrown if there is a problem authenticating the user
     */
    protected void authenticateUser(HttpServletRequest request, String username, String password) throws LoginException {
        try {
            log.debug("Attempting to authenticate user {}", username);

            SimpleCallbackHandler cbh = new SimpleCallbackHandler(username, password);

            javax.security.auth.login.LoginContext jaasLoginCtx = new javax.security.auth.login.LoginContext(
                    jaasConfigName, cbh);

            jaasLoginCtx.login();
            log.debug("Successfully authenticated user {}", username);

            Subject loginSubject = jaasLoginCtx.getSubject();

            Set<Principal> principals = loginSubject.getPrincipals();
            principals.add(new UsernamePrincipal(username));

            Set<Object> publicCredentials = loginSubject.getPublicCredentials();

            Set<Object> privateCredentials = loginSubject.getPrivateCredentials();
            privateCredentials.add(new UsernamePasswordCredential(username, password));

            Subject userSubject = new Subject(false, principals, publicCredentials, privateCredentials);
            request.getSession().setAttribute(USER_SUBJECT_KEY, userSubject);
        } catch (LoginException e) {
            log.debug("User authentication for " + username + " failed", e);
            throw e;
        } catch (Throwable e) {
            log.debug("User authentication for " + username + " failed", e);
            throw new LoginException("unknown authentication error");
        }
    }

    /**
     * A callback handler that provides static name and password data to a JAAS loging process.
     * 
     * This handler only supports {@link NameCallback} and {@link PasswordCallback}.
     */
    protected class SimpleCallbackHandler implements CallbackHandler {

        /** Name of the user. */
        private String uname;

        /** User's password. */
        private String pass;

        /**
         * Constructor.
         * 
         * @param username The username
         * @param password The password
         */
        public SimpleCallbackHandler(String username, String password) {
            uname = username;
            pass = password;
        }

        /**
         * Handle a callback.
         * 
         * @param callbacks The list of callbacks to process.
         * 
         * @throws UnsupportedCallbackException If callbacks has a callback other than {@link NameCallback} or
         *             {@link PasswordCallback}.
         */
        public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {

            if (callbacks == null || callbacks.length == 0) {
                return;
            }

            for (Callback cb : callbacks) {
                if (cb instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) cb;
                    ncb.setName(uname);
                } else if (cb instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) cb;
                    pcb.setPassword(pass.toCharArray());
                }
            }
        }
    }
}