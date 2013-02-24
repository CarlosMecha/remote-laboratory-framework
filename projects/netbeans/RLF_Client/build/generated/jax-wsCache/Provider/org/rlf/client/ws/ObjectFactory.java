
package org.rlf.client.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.rlf.client.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _LoginResponse_QNAME = new QName("http://ws.provider.rlf.org/", "loginResponse");
    private final static QName _DescribeTools_QNAME = new QName("http://ws.provider.rlf.org/", "describeTools");
    private final static QName _TakeToolsResponse_QNAME = new QName("http://ws.provider.rlf.org/", "takeToolsResponse");
    private final static QName _GetStatus_QNAME = new QName("http://ws.provider.rlf.org/", "getStatus");
    private final static QName _FormatException_QNAME = new QName("http://ws.provider.rlf.org/", "FormatException");
    private final static QName _GetStatusResponse_QNAME = new QName("http://ws.provider.rlf.org/", "getStatusResponse");
    private final static QName _DescribeToolsResponse_QNAME = new QName("http://ws.provider.rlf.org/", "describeToolsResponse");
    private final static QName _AuthException_QNAME = new QName("http://ws.provider.rlf.org/", "AuthException");
    private final static QName _Logout_QNAME = new QName("http://ws.provider.rlf.org/", "logout");
    private final static QName _LogoutResponse_QNAME = new QName("http://ws.provider.rlf.org/", "logoutResponse");
    private final static QName _ConnectionException_QNAME = new QName("http://ws.provider.rlf.org/", "ConnectionException");
    private final static QName _ToolException_QNAME = new QName("http://ws.provider.rlf.org/", "ToolException");
    private final static QName _TakeTools_QNAME = new QName("http://ws.provider.rlf.org/", "takeTools");
    private final static QName _Login_QNAME = new QName("http://ws.provider.rlf.org/", "login");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.rlf.client.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TakeToolsResponse }
     * 
     */
    public TakeToolsResponse createTakeToolsResponse() {
        return new TakeToolsResponse();
    }

    /**
     * Create an instance of {@link GetStatus }
     * 
     */
    public GetStatus createGetStatus() {
        return new GetStatus();
    }

    /**
     * Create an instance of {@link TakeTools }
     * 
     */
    public TakeTools createTakeTools() {
        return new TakeTools();
    }

    /**
     * Create an instance of {@link Logout }
     * 
     */
    public Logout createLogout() {
        return new Logout();
    }

    /**
     * Create an instance of {@link LogoutResponse }
     * 
     */
    public LogoutResponse createLogoutResponse() {
        return new LogoutResponse();
    }

    /**
     * Create an instance of {@link LoginResponse }
     * 
     */
    public LoginResponse createLoginResponse() {
        return new LoginResponse();
    }

    /**
     * Create an instance of {@link DescribeTools }
     * 
     */
    public DescribeTools createDescribeTools() {
        return new DescribeTools();
    }

    /**
     * Create an instance of {@link DescribeToolsResponse }
     * 
     */
    public DescribeToolsResponse createDescribeToolsResponse() {
        return new DescribeToolsResponse();
    }

    /**
     * Create an instance of {@link AuthException }
     * 
     */
    public AuthException createAuthException() {
        return new AuthException();
    }

    /**
     * Create an instance of {@link ToolException }
     * 
     */
    public ToolException createToolException() {
        return new ToolException();
    }

    /**
     * Create an instance of {@link Login }
     * 
     */
    public Login createLogin() {
        return new Login();
    }

    /**
     * Create an instance of {@link FormatException }
     * 
     */
    public FormatException createFormatException() {
        return new FormatException();
    }

    /**
     * Create an instance of {@link GetStatusResponse }
     * 
     */
    public GetStatusResponse createGetStatusResponse() {
        return new GetStatusResponse();
    }

    /**
     * Create an instance of {@link ConnectionException }
     * 
     */
    public ConnectionException createConnectionException() {
        return new ConnectionException();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LoginResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "loginResponse")
    public JAXBElement<LoginResponse> createLoginResponse(LoginResponse value) {
        return new JAXBElement<LoginResponse>(_LoginResponse_QNAME, LoginResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DescribeTools }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "describeTools")
    public JAXBElement<DescribeTools> createDescribeTools(DescribeTools value) {
        return new JAXBElement<DescribeTools>(_DescribeTools_QNAME, DescribeTools.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TakeToolsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "takeToolsResponse")
    public JAXBElement<TakeToolsResponse> createTakeToolsResponse(TakeToolsResponse value) {
        return new JAXBElement<TakeToolsResponse>(_TakeToolsResponse_QNAME, TakeToolsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatus }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "getStatus")
    public JAXBElement<GetStatus> createGetStatus(GetStatus value) {
        return new JAXBElement<GetStatus>(_GetStatus_QNAME, GetStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FormatException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "FormatException")
    public JAXBElement<FormatException> createFormatException(FormatException value) {
        return new JAXBElement<FormatException>(_FormatException_QNAME, FormatException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatusResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "getStatusResponse")
    public JAXBElement<GetStatusResponse> createGetStatusResponse(GetStatusResponse value) {
        return new JAXBElement<GetStatusResponse>(_GetStatusResponse_QNAME, GetStatusResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DescribeToolsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "describeToolsResponse")
    public JAXBElement<DescribeToolsResponse> createDescribeToolsResponse(DescribeToolsResponse value) {
        return new JAXBElement<DescribeToolsResponse>(_DescribeToolsResponse_QNAME, DescribeToolsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "AuthException")
    public JAXBElement<AuthException> createAuthException(AuthException value) {
        return new JAXBElement<AuthException>(_AuthException_QNAME, AuthException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Logout }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "logout")
    public JAXBElement<Logout> createLogout(Logout value) {
        return new JAXBElement<Logout>(_Logout_QNAME, Logout.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LogoutResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "logoutResponse")
    public JAXBElement<LogoutResponse> createLogoutResponse(LogoutResponse value) {
        return new JAXBElement<LogoutResponse>(_LogoutResponse_QNAME, LogoutResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ConnectionException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "ConnectionException")
    public JAXBElement<ConnectionException> createConnectionException(ConnectionException value) {
        return new JAXBElement<ConnectionException>(_ConnectionException_QNAME, ConnectionException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ToolException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "ToolException")
    public JAXBElement<ToolException> createToolException(ToolException value) {
        return new JAXBElement<ToolException>(_ToolException_QNAME, ToolException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TakeTools }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "takeTools")
    public JAXBElement<TakeTools> createTakeTools(TakeTools value) {
        return new JAXBElement<TakeTools>(_TakeTools_QNAME, TakeTools.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Login }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.provider.rlf.org/", name = "login")
    public JAXBElement<Login> createLogin(Login value) {
        return new JAXBElement<Login>(_Login_QNAME, Login.class, null, value);
    }

}
