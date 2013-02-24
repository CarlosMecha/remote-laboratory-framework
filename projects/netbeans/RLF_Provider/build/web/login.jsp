<%-- 
    Document   : login
    Created on : 28-ago-2011, 11:29:23
    Author     : rodriguezmecha
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" session="true" %>
<!DOCTYPE html>
<%
    String error = new String();
    try {
        switch (Integer.parseInt(request.getParameter("error"))) {
            case 401:
                error = "Authentication failure";
                break;
            case 500:
                error = "System failure";
                break;
            default:
                break;
        }
    } catch (Exception e) {
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
            <div data-role="header">
                <h1>RLF Monitor</h1>                   
            </div> 
            <div data-role="content">
                <div style="text-align: center">
                    <img src="css/images/client_icon.png" alt="RLF"/>
                </div>
                <form id="loginForm" method="post" action="index.jsp">
                    <input type="hidden" id="operation" name="op" value="login"/>
                    <div data-role="fieldcontain">
                        <label for="userText">User</label>
                        <input type="text" id="userText" name="id"/>
                    </div>
                    <div data-role="fieldcontain">
                        <label for="passText">Password</label>
                        <input type="password" id="passText" name="t"/>
                    </div>
                    <input type="submit" id="loginButton" value="Login"/>
                </form>
                <h2 style="color: red; text-align: center"><%=error%></h2>
            </div> 
            <div data-role="footer">
                <h1>RLF <i>Prototype 1</i></h1>
            </div> 
        </div>
    </body>
</html>
