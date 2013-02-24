
package org.rlf.client.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2-hudson-752-
 * Generated source version: 2.2
 * 
 */
@WebService(name = "Provider", targetNamespace = "http://ws.provider.rlf.org/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface Provider {


    /**
     * 
     * @param auth
     * @return
     *     returns java.lang.String
     * @throws ConnectionException_Exception
     * @throws AuthException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getStatus", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.GetStatus")
    @ResponseWrapper(localName = "getStatusResponse", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.GetStatusResponse")
    @Action(input = "http://ws.provider.rlf.org/Provider/getStatusRequest", output = "http://ws.provider.rlf.org/Provider/getStatusResponse", fault = {
        @FaultAction(className = AuthException_Exception.class, value = "http://ws.provider.rlf.org/Provider/getStatus/Fault/AuthException"),
        @FaultAction(className = ConnectionException_Exception.class, value = "http://ws.provider.rlf.org/Provider/getStatus/Fault/ConnectionException")
    })
    public String getStatus(
        @WebParam(name = "auth", targetNamespace = "")
        String auth)
        throws AuthException_Exception, ConnectionException_Exception
    ;

    /**
     * 
     * @param hashPass
     * @param user
     * @return
     *     returns java.lang.String
     * @throws ConnectionException_Exception
     * @throws AuthException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "login", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.Login")
    @ResponseWrapper(localName = "loginResponse", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.LoginResponse")
    @Action(input = "http://ws.provider.rlf.org/Provider/loginRequest", output = "http://ws.provider.rlf.org/Provider/loginResponse", fault = {
        @FaultAction(className = AuthException_Exception.class, value = "http://ws.provider.rlf.org/Provider/login/Fault/AuthException"),
        @FaultAction(className = ConnectionException_Exception.class, value = "http://ws.provider.rlf.org/Provider/login/Fault/ConnectionException")
    })
    public String login(
        @WebParam(name = "user", targetNamespace = "")
        String user,
        @WebParam(name = "hash_pass", targetNamespace = "")
        String hashPass)
        throws AuthException_Exception, ConnectionException_Exception
    ;

    /**
     * 
     * @param auth
     * @return
     *     returns java.lang.Boolean
     * @throws ConnectionException_Exception
     * @throws AuthException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "logout", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.Logout")
    @ResponseWrapper(localName = "logoutResponse", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.LogoutResponse")
    @Action(input = "http://ws.provider.rlf.org/Provider/logoutRequest", output = "http://ws.provider.rlf.org/Provider/logoutResponse", fault = {
        @FaultAction(className = AuthException_Exception.class, value = "http://ws.provider.rlf.org/Provider/logout/Fault/AuthException"),
        @FaultAction(className = ConnectionException_Exception.class, value = "http://ws.provider.rlf.org/Provider/logout/Fault/ConnectionException")
    })
    public Boolean logout(
        @WebParam(name = "auth", targetNamespace = "")
        String auth)
        throws AuthException_Exception, ConnectionException_Exception
    ;

    /**
     * 
     * @param auth
     * @param array
     * @return
     *     returns java.lang.String
     * @throws FormatException_Exception
     * @throws ConnectionException_Exception
     * @throws AuthException_Exception
     * @throws ToolException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "takeTools", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.TakeTools")
    @ResponseWrapper(localName = "takeToolsResponse", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.TakeToolsResponse")
    @Action(input = "http://ws.provider.rlf.org/Provider/takeToolsRequest", output = "http://ws.provider.rlf.org/Provider/takeToolsResponse", fault = {
        @FaultAction(className = AuthException_Exception.class, value = "http://ws.provider.rlf.org/Provider/takeTools/Fault/AuthException"),
        @FaultAction(className = ConnectionException_Exception.class, value = "http://ws.provider.rlf.org/Provider/takeTools/Fault/ConnectionException"),
        @FaultAction(className = ToolException_Exception.class, value = "http://ws.provider.rlf.org/Provider/takeTools/Fault/ToolException"),
        @FaultAction(className = FormatException_Exception.class, value = "http://ws.provider.rlf.org/Provider/takeTools/Fault/FormatException")
    })
    public String takeTools(
        @WebParam(name = "auth", targetNamespace = "")
        String auth,
        @WebParam(name = "array", targetNamespace = "")
        String array)
        throws AuthException_Exception, ConnectionException_Exception, FormatException_Exception, ToolException_Exception
    ;

    /**
     * 
     * @param auth
     * @return
     *     returns java.lang.String
     * @throws ConnectionException_Exception
     * @throws AuthException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "describeTools", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.DescribeTools")
    @ResponseWrapper(localName = "describeToolsResponse", targetNamespace = "http://ws.provider.rlf.org/", className = "org.rlf.client.ws.DescribeToolsResponse")
    @Action(input = "http://ws.provider.rlf.org/Provider/describeToolsRequest", output = "http://ws.provider.rlf.org/Provider/describeToolsResponse", fault = {
        @FaultAction(className = ConnectionException_Exception.class, value = "http://ws.provider.rlf.org/Provider/describeTools/Fault/ConnectionException"),
        @FaultAction(className = AuthException_Exception.class, value = "http://ws.provider.rlf.org/Provider/describeTools/Fault/AuthException")
    })
    public String describeTools(
        @WebParam(name = "auth", targetNamespace = "")
        String auth)
        throws AuthException_Exception, ConnectionException_Exception
    ;

}