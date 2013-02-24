
package org.rlf.client.ws;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2-hudson-752-
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "AuthException", targetNamespace = "http://ws.provider.rlf.org/")
public class AuthException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private AuthException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public AuthException_Exception(String message, AuthException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public AuthException_Exception(String message, AuthException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.rlf.client.ws.AuthException
     */
    public AuthException getFaultInfo() {
        return faultInfo;
    }

}
