#!/bin/bash

cd "$(dirname "$0")"

SHIBBOLETH=/opt/shibboleth-idp
AKEY=`python -c "import base64, os; print base64.b64encode(os.urandom(30))"`

usage () {
    printf >&2 "Usage: $0 [-d shibboleth directory] -i ikey -s skey -h host\n"
    printf >&2 "ikey, skey, and host can be found in Duo account's administration panel at admin.duosecurity.com\n"
}

while getopts d:i:s:h: o
do  
    case "$o" in
        d)  SHIBBOLETH="$OPTARG";;
        i)  IKEY="$OPTARG";;
        s)  SKEY="$OPTARG";;
        h)  HOST="$OPTARG";;
        [?]) usage
            exit 1;;
    esac
done

if [ -z $IKEY ]; then echo "Missing -i (Duo integration key)"; usage; exit 1; fi
if [ -z $SKEY ]; then echo "Missing -s (Duo secret key)"; usage; exit 1; fi
if [ -z $HOST ]; then echo "Missing -h (Duo API hostname)"; usage; exit 1; fi

echo "Installing Duo application to $SHIBBOLETH..."

SHIBBOLETH_ERROR="The directory ($SHIBBOLETH) does not look like a Shibboleth IDP v3 installation. Use the -d option to specify where Shibboleth IDP v3 is installed."

# perform basic checks to verify shibboleth install directory
if [ ! -d "$SHIBBOLETH" ]; then
    echo "$SHIBBOLETH_ERROR"
    exit 1
fi
if [ ! -e "$SHIBBOLETH"/conf/idp.properties ]; then
    echo "$SHIBBOLETH_ERROR"
    exit 1
fi
if [ ! -e "$SHIBBOLETH"/edit-webapp/WEB-INF/lib ]; then
    echo "$SHIBBOLETH_ERROR"
    exit 1
fi
if [ ! -e "$SHIBBOLETH"/flows/authn/conditions/conditions-flow.xml ]; then
    echo "$SHIBBOLETH_ERROR"
    exit 1
fi
if [ ! -e "$SHIBBOLETH"/views ]; then
    echo "$SHIBBOLETH_ERROR"
    exit 1
fi

# make sure we haven't already installed
if find "$SHIBBOLETH"/edit-webapp/WEB-INF/lib/DuoWeb-*.jar &> /dev/null; then
    echo "DuoWeb-*.jar already exists in $SHIBBOLETH/edit-webapp/WEB-INF/lib. Remove this jar to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/edit-webapp/WEB-INF/lib/duo-client-*.jar &> /dev/null; then
    echo "duo-client-*.jar already exists in $SHIBBOLETH/edit-webapp/WEB-INF/lib. Remove this jar to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/edit-webapp/WEB-INF/lib/DuoShibboleth-*.jar &> /dev/null; then
    echo "DuoShibboleth-*.jar already exists in $SHIBBOLETH/edit-webapp/WEB-INF/lib. Remove this jar to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/edit-webapp/js/Duo-Web-*.js &> /dev/null; then
    echo "Duo-Web-*.js already exists in $SHIBBOLETH/edit-webapp/js. Remove this file to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/webapp/WEB-INF/lib/DuoWeb-*.jar &> /dev/null; then
    echo "DuoWeb-*.jar already exists in $SHIBBOLETH/webapp/WEB-INF/lib. Remove this jar to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/webapp/WEB-INF/lib/duo-client-*.jar &> /dev/null; then
    echo "duo-client-*.jar already exists in $SHIBBOLETH/webapp/WEB-INF/lib. Remove this jar to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/webapp/WEB-INF/lib/DuoShibboleth-*.jar &> /dev/null; then
    echo "DuoShibboleth-*.jar already exists in $SHIBBOLETH/webapp/WEB-INF/lib. Remove this jar to continue."
    echo 'exiting'
    exit 1
fi

if find "$SHIBBOLETH"/webapp/js/Duo-Web-*.js &> /dev/null; then
    echo "Duo-Web-*.js already exists in $SHIBBOLETH/webapp/js. Remove this file to continue."
    echo 'exiting'
    exit 1
fi

if [ -e "$SHIBBOLETH"/views/duo.vm ]; then
    echo "duo.vm already exists in $SHIBBOLETH/views. Remove this file to continue."
    echo 'exiting'
    exit 1
fi

if [ -e "$SHIBBOLETH"/flows/authn/Duo/duo-authn-flow.xml ]; then
    echo "duo-authn-flow.xml already exists in $SHIBBOLETH/flows/authn/Duo. Remove this file to continue."
    echo 'exiting'
    exit 1
fi

# we don't actually write to conditions-flow.xml, so just warn if it's already there
grep '<transition on="duo" to="DuoAuth" />' "$SHIBBOLETH"/flows/authn/conditions/conditions-flow.xml >/dev/null
if [ $? = 0 ]; then
    echo "Warning: It looks like the Duo configuration has already been added to Shibboleth's conditions-flow.xml."
fi

grep -E '^duo\.(host|ikey|akey|skey|failmode)' "$SHIBBOLETH"/conf/idp.properties >/dev/null
if [ $? = 0 ]; then
    echo "Warning: It looks like the Duo configuration has already been added to Shibboleth's idp.properties."
fi

echo "Copying in Duo application files..."

# install the duo_java jar
cp IDP_HOME/edit-webapp/WEB-INF/lib/DuoWeb-1.1-SNAPSHOT.jar "$SHIBBOLETH"/edit-webapp/WEB-INF/lib/
if [ $? != 0 ]; then
    echo 'Could not copy DuoWeb-1.1-SNAPSHOT.jar, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the duo_client_java jar
cp IDP_HOME/edit-webapp/WEB-INF/lib/duo-client-0.2.1-jar-with-dependencies.jar "$SHIBBOLETH"/edit-webapp/WEB-INF/lib/
if [ $? != 0 ]; then
    echo 'Could not copy duo-client-0.2.1-jar-with-dependencies.jar, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the shibboleth jar
cp IDP_HOME/edit-webapp/WEB-INF/lib/DuoShibboleth-1.0.jar "$SHIBBOLETH"/edit-webapp/WEB-INF/lib/
if [ $? != 0 ]; then
    echo 'Could not copy DuoShibboleth-1.0.jar, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the duo js
mkdir -p "$SHIBBOLETH"/edit-webapp/js/ && cp IDP_HOME/edit-webapp/js/Duo-Web-v2.min.js "$SHIBBOLETH"/edit-webapp/js/
if [ $? != 0 ]; then
    echo 'Could not copy Duo-Web-v2.min.js, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the duo velocity macro
cp IDP_HOME/views/duo.vm "$SHIBBOLETH"/views/
if [ $? != 0 ]; then
    echo 'Could not copy duo.vm, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the duo authentication flow
mkdir -p "$SHIBBOLETH"/flows/authn/Duo/ && cp IDP_HOME/flows/authn/Duo/duo-authn-flow.xml "$SHIBBOLETH"/flows/authn/Duo/
if [ $? != 0 ]; then
    echo 'Could not copy duo.vm, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

echo "duo_shibboleth_idp jars and configuration files have been installed. Next steps, in order:"
echo "- Edit idp.properties located at $SHIBBOLETH/conf/idp.properties,"
echo "  adding the following to the bottom of the file: "
echo "    duo.ikey = $IKEY"
echo "    duo.skey = $SKEY"
echo "    duo.akey = $AKEY"
echo "    duo.host = $HOST"
echo "- Optionally add a key of duo.failmode with value of either \"safe\" or \"secure\". If absent,"
echo "  the Duo application defaults to \"safe\"."
echo "- Edit conditions-flow.xml located at $SHIBBOLETH/flows/authn/conditions/conditions-flow.xml,"
echo "  adding the following to the top of the <action-state id=\"ValidateUsernamePassword\"> section:"
echo "    <!-- Enable Duo Two-Factor Authentication -->"
echo "    <evaluate expression=\"ValidateUsernamePassword\" />"
echo "    <evaluate expression=\"'duo'\" />"
echo "    <transition on=\"duo\" to=\"DuoAuth\" />"
echo "    <!-- End Duo Two-Factor Authentication -->"
echo "    ..."
echo "- Also, add the following just before the closing </flow> tag:"
echo "    <subflow-state id=\"DuoAuth\" subflow=\"authn/Duo\">"
echo "        <input name=\"calledAsSubflow\" value=\"true\" />"
echo "        <transition on=\"proceed\" to=\"proceed\" />"
echo "    </subflow-state>"
echo "- Run $SHIBBOLETH/bin/build.sh."
echo "- Restart the web server."
