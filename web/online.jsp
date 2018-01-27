<%-- 
    Document   : online
    Created on : Jan 27, 2018, 11:32:53 AM
    Author     : james
--%>

<%@page import="java.sql.ResultSet"%>
<%@page import="com.cheng.DbDao"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page language="java" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title> 用户在线信息 </title>
    </head>
    <body>
        <h1>Online User:</h1>
        <table>
            <%
                DbDao dd = new DbDao("com.mysql.jdbc.Driver",
                        "jdbc:mysql://localhost:3306/online_inf",
                        "root",
                        "nbuser");
                // 查询online_inf表（在线用户表）的全部记录          
                ResultSet rs = dd.query("select * from online_inf", false);
                while (rs.next()) {
            %>
            <tr>
                <td><%=rs.getString(1)%></td>
                <td><%=rs.getString(2)%></td>
                <td><%=rs.getString(3)%></td>
                <td><%=rs.getString(4)%></td>
            </tr>
            <%
                }
            %>
        </table>

    </body>
</html>
