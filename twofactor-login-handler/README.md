# Overview

**twofactor-login-handler** - Duo two-factor authentication login handler for
Shibboleth 2

Adds Duo two-factor authentication to existing JAAS user authentication for
Shibboleth identity providers.  It is based on the Shibboleth UsernamePassword
login handler.

This login handler has been tested with the Shibboleth 2.3.5 identity provider
and the Duo Web SDK v1.

# Installation and configuration

## Acquire and generate keys

See <http://www.duosecurity.com/docs/duoweb>.  Sign up to get an skey, ikey, and
host, and generate your own secret akey.

## Build and install duo_java

See <https://github.com/duosecurity/duo_java> for details.

In a temporary directory, build a duo_java jar.

    git clone git://github.com/duosecurity/duo_java.git
    mkdir class
    javac duo_java/DuoWeb/src/com/duosecurity/Base64.java duo_java/DuoWeb/src/com/duosecurity/DuoWeb.java duo_java/DuoWeb/src/com/duosecurity/Util.java -d class
    jar cf duo.jar -C class .
        
Copy the jar into the lib directories of the login handler source and 
identity provider installation trees.

    cp duo.jar .../duo_shibboleth/twofactor-login-handler/lib/
    cp duo.jar .../shibboleth-identityprovider-2.X.X/lib/
        
## Build and install the login handler

    mvn package
    cp target/twofactor-login-handler-0.2.jar .../shibboleth-identityprovider-2.X.X/lib/
        
## Configure Tomcat

Enable the servlet in `.../shibboleth-identityprovider-2.X.X/src/main/webapp/WEB-INF/web.xml`.
The default location is `/Authn/DuoUserPassword`.

    <webapp>
        <!-- ... -->    
        <!-- Servlet for two-factor login handler -->
        <servlet>
            <servlet-name>TwoFactorLoginHandler</servlet-name>
            <servlet-class>com.duosecurity.shibboleth.idp.twofactor.TwoFactorLoginServlet</servlet-class>
            <load-on-startup>4</load-on-startup>
        </servlet>

        <servlet-mapping>
            <servlet-name>TwoFactorLoginHandler</servlet-name>
            <url-pattern>/Authn/DuoUserPassword</url-pattern>
        </servlet-mapping>
        <!-- ... -->    
    </webapp>
        
## Install the duo.jsp authentication page

An example is in `examples/duo.jsp`.  Copy this into
`.../shibboleth-identityprovider-2.X.X/src/main/webapp` alongside the existing login.jsp page.

## Deploy Tomcat

Back up any existing IdP config, then `.../shibboleth-identityprovider-2.X.X/install.sh`.

## Serve the Duo Web Javascript page

There are several scripts distributed with duo_java.  The script to use and
its path are determined by the contents of duo.jsp.  The example duo.jsp page
uses:

    <script src="/Duo-Web-v1.bundled.js">

so copy `duo_java/js/Duo-Web-v1.bundled.js` somewhere where it will be served
from the root of your site.

## Configure IdP

### login.config

Set up JAAS; a login.config which works for a UsernamePassword login handler
can be used.

### handler.xml

Add the login handler namespaces and schema, and a LoginHandler stanza.
Replace `skey`, `ikey`, `akey`, and `host` with approprate values.

    <ph:ProfileHandlerGroup
       xmlns:ph="urn:mace:shibboleth:2.0:idp:profile-handler"
       xmlns:twofactor="http://duosecurity.com/2012/shibboleth/idp/twofactor"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xsi:schemaLocation="urn:mace:shibboleth:2.0:idp:profile-handler classpath:/schema/shibboleth-2.0-idp-profile-handler.xsd http://duosecurity.com/2012/shibboleth/idp/twofactor classpath:/schema/shibboleth-2.0-idp-twofactor-login-handler.xsd">
    <!-- ... -->
        <ph:LoginHandler xsi:type="twofactor:TwoFactorLogin" 
            jaasConfigurationLocation="file:///root/shib/idp/conf/login.config"
            skey="skey" ikey="ikey" akey="akey" host="api-eval.duosecurity.com">
            <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</ph:AuthenticationMethod>
        </ph:LoginHandler>
    <!-- ... -->
    </ph:ProfileHandlerGroup>
        
### logging.xml

    <!-- ... -->
        <logger name="org.duosecurity.shibboleth" level="INFO" />
    <!-- ... -->

## Test it out

Start the identity provider and authenticate against it with a
Shibboleth service provider.  You should be prompted to enroll with
Duo after successfully authenticating locally.  Then, on subsequent
logins, you'll authenticate with Duo after successfully authenticating
locally.

# Support

Report any bugs, feature requests, etc. to us directly:
<https://github.com/duosecurity/duo_shibboleth/issues>
