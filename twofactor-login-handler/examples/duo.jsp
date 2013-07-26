<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.LoginContext" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.authn.LoginHandler" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.session.*" %>
<%@ page import="edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper" %>
<%@ page import="org.opensaml.saml2.metadata.*" %>

<html>
  <head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
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
       <script src="/Duo-Web-v1.bundled.js"></script>
       <script>
         Duo.init({
           'host': "<%=request.getAttribute("host")%>",
           'sig_request': "<%=request.getAttribute("sigRequest")%>",
           'post_action': "<%=request.getAttribute("actionUrl")%>"
         });
       </script>
       <iframe height="500" width="620" id="duo_iframe" frameborder="0"></iframe>
     </div>
  </body>
</html>
