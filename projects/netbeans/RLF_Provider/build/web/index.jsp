<%-- 
    Document   : index
    Created on : 27-ago-2011, 18:54:09
    Author     : Carlos A. Rodríguez Mecha
--%>

<%@page import="org.rlf.monitor.ws.ToolInfo"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.rlf.provider.ws.exception.ConnectionException"%>
<%@page import="org.rlf.provider.ws.exception.AuthException"%>
<%@page import="org.rlf.cipher.dummy.RLF_DummyCipher"%>
<%@page import="org.rlf.cipher.RLF_Cipher"%>
<%@page import="org.rlf.monitor.ws.Monitor" %>

<%@page contentType="text/html" pageEncoding="UTF-8" session="true"%>
<!DOCTYPE html>
<%

    // 1. Obtención de las herramientas.
    ArrayList<ToolInfo> tools = new ArrayList<ToolInfo>();
    String token = (String) request.getSession(true).getAttribute("token");
    String error = new String();
    if (token != null) {
        Monitor monitor = new Monitor();
        try {
            tools = monitor.getStatus(token);
        } catch (Exception e) {
            error = "Fail to obtain the tools";
        }
    }


%>
<html>
    <head>
        <link rel="shortcut icon" href="css/images/favicon.ico"/>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <link rel="stylesheet" href="css/jquery.mobile-1.0b2.min.css" /> 
        <script src="js/jquery-1.6.2.min.js"></script> 
        <script src="js/jquery.mobile-1.0b2.min.js"></script> 
        <title>RLF Monitor</title>

    </head>
    <body>
        <div data-role="page"> 
            <div data-role="header" data-position="inline">
                <a href="index.jsp?op=logout" data-icon="arrow-l" data-transition="slide" data-direction="reverse">Logout</a>
                <h1>RLF Monitor</h1>
                <a href="javascript:document.location.reload();" data-icon="refresh" data-transition="fade" data-theme="b">Update</a>
            </div>
            <div data-role="content">
                <ul data-role="listview" data-inset="true" class="ui-listview ui-listview-inset ui-corner-all ui-shadow">
                    <%
                        for (ToolInfo info : tools) {
                            String src;
                            switch (info.getStatus()) {
                                case ONLINE:
                                    src = "css/images/free-web.png";
                                    break;
                                case INUSE:
                                     src = "css/images/exec-web.png";
                                     break;
                                default:
                                     src = "css/images/busy-web.png";
                                     break;
                            }
                            out.println("<li class=\"ui-li ui-li-static ui-body-c ui-corner-top ui-li-has-thumb\">");
                            out.println("<img src=\""+ src +"\" class=\"ui-li-thumb\"/>");
                            out.println("<h3 class=\"ui-li-heading\">" + info.getName() + "</h3>");
                            out.println("<p class=\"ui-li-desc\">" + info.getId() + "</p>");
                            out.println("</li>");
                        }

                    %>
                </ul>
                <h2 style="color: red; text-align: center"><%=error%></h2>

            </div> 
            <div data-role="footer">
                <h1>RLF <i>Prototype 1</i></h1>
            </div> 
        </div>
    </body>
</html>
