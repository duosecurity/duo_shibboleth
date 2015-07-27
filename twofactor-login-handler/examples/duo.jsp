<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.LoginContext" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.LoginHandler" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.session.*" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper" %>
<%@ page import="org.opensaml.saml2.metadata.*" %>

<html>
  <head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Shibboleth Identity Provider - Duo Authentication</title>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/login.css"/>
  </head>

  <body id="homepage">
    <img src="<%= request.getContextPath()%>/images/logo.jpg" alt="Shibboleth Logo"/>
    <h1>Duo Authentication</h1>

    <p>This second-factor authentication page is an example and should be customized.  Refer to the 
       <a href="https://wiki.shibboleth.net/confluence/display/SHIB2/IdPAuthUserPassLoginPage" target="_blank"> documentation</a>.
    </p>
     <div class="content">
       <script src="/Duo-Web-v2.min.js"></script>
       <iframe id="duo_iframe"
                data-host="<%=request.getAttribute("host")%>"
                data-sig-request="<%=request.getAttribute("sigRequest")%>"
                data-post-action="<%=request.getAttribute("actionUrl")%>"
                frameborder="0"
       >
       </iframe>
       <style>
         #duo_iframe {
           width: 100%;
           min-width: 304px;
           max-width: 620px;
           height: 330px;
         }
       </style>
     </div>
  </body>
</html>
