# online

## 添加数据库的访问驱动

```shell
mysql-connector-java-xxx-bin.jar
```



## 与数据库的交互的封装

```java
package com.cheng;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author james
 */
public class DbDao {

    private Connection conn;
    private String driver;
    private String url;
    private String username;
    private String pass;

    public DbDao() {

    }

    public DbDao(String driver, String url, String username, String pass) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.pass = pass;
    }

    /**
     * 获取数据库连接
     *
     * @return
     */
    public Connection getConn() {
        if (conn == null) {
            try {
                Class.forName(this.driver);
                conn = DriverManager.getConnection(url, username, this.pass);
            } catch (ClassNotFoundException | SQLException ex) {
                Logger.getLogger(DbDao.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return conn;
    }

    /**
     * 插入记录
     *
     * @param sql
     * @param args
     * @return
     */
    public boolean insert(String sql, Object... args) {
        try {
            PreparedStatement pstmt
                    = getConn().prepareStatement(sql);

            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }
            if (pstmt.executeUpdate() != 1) {
                return false;
            }
            pstmt.close();

        } catch (SQLException ex) {
            Logger.getLogger(DbDao.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    /**
     * Execute query.
     *
     * @param sql - Put the SQL statement
     * @param updatable - Whether is can update or not
     * @param args
     * @return
     * @throws java.sql.SQLException
     */
    public ResultSet query(String sql, boolean updatable, Object... args)
            throws SQLException {
        PreparedStatement pstmt = null;
        if (updatable) {

            // Create the updatable 'PrepareStatement'.
            pstmt = getConn().prepareStatement(sql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
        } else {
            pstmt = getConn().prepareStatement(sql);
        }

        // Do for loop to the Oject args.
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }

        return pstmt.executeQuery();
    }

    /**
     * Execute modify.
     *
     * @param sql - an SQL statement that may contain one
     * @param args - the object containing the input parameter value
     * @throws java.sql.SQLException
     */
    public void modify(String sql, Object... args) throws SQLException {
        PreparedStatement pstmt = getConn().prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }
        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
     * 关闭数据库连接的方法
     *
     * @throws SQLException
     */
    public void closeConn() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}
```



## 请求的监听

- 先实现一个`ServletRequestListener` , 它负责监听每次用户请求：
  - 每次用户请求到达时，如果是新的用户会话，将相关信息插入数据表;
  - 如果是老的用户会话，则更新数据表中已有的在线记录。
```java
package com.cheng;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 * @author james
 */
@WebListener
public class RequestListener implements ServletRequestListener {

    // 当用户请求结束、被销毁时触发该方法
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
    }

    // 当用户请求到达、被初始化时触发该方法
    @Override
    public void requestInitialized(ServletRequestEvent sre) {

        HttpServletRequest request
                = (HttpServletRequest) sre.getServletRequest();
        HttpSession session = request.getSession();
        String sessionId = session.getId();

        String ip = request.getRemoteAddr();
        String page = request.getRequestURI();
        String user = (String) session.getAttribute("user");

        user = (user == null) ? "游客" : user;
        try {
            DbDao dd = new DbDao("com.mysql.jdbc.Driver",
                    "jdbc:mysql://localhost:3306/online_inf",
                    "root",
                    "nbuser");
            ResultSet rs = dd.query("select * from online_inf where session_id=?", true, sessionId);
            if (rs.next()) {
                rs.updateString(4, page);
                rs.updateLong(5, System.currentTimeMillis());
                rs.updateRow();
                rs.close();
            } else {
                dd.insert("insert into online_inf values(?,?,?,?,?)",
                        sessionId, user, ip, page, System.currentTimeMillis());

            }
        } catch (SQLException ex) {
            Logger.getLogger(RequestListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
```

## 后台线程的监听
- 接下来，实现一个`ServletContextListener`， 它负责启动一条后台线程，这条后台线程将会定期检查在线记录，并删除那些长时间没有重新请求过的记录。

```java
package com.cheng;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author james
 */
public class OnlineListener implements ServletContextListener {

    /**
     * 超过该时间（10分钟）没有访问本站即认为用户已经离线
     */
    public final int MAX_MILLIS = 10 * 60 * 1000;

    // 应用启动时触发该方法
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 每5秒检查一次
        new javax.swing.Timer(1000 * 5, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    DbDao dd = new DbDao("com.mysql.jdbc.Driver",
                            "jdbc:mysql://localhost:3306/online_inf",
                            "root",
                            "nbuser");
                    ResultSet rs = dd.query("select * from online_inf", false);
                    StringBuffer beRemove = new StringBuffer("(");
                    while (rs.next()) {
                        // 如果距离上次访问时间超过了指定时间
                        if (System.currentTimeMillis()-rs.getLong(5)
                                > MAX_MILLIS) {
                            	// 将需要被删除的session ID添加进来
                                beRemove.append("'");
                                beRemove.append("rs.getString(1)");
                                beRemove.append("'");
                        }
                    }
                    // 有需要删除的记录
                    if (beRemove.length() > 3) {
                        beRemove.setLength(beRemove.length()-3);
                        beRemove.append(")");
                        //删除所有“超过指定时间未重新请求的记录”
                        dd.modify("delete from online_inf where session_id in " +
                                beRemove.toString());
                    }
                    dd.closeConn();
                } catch (SQLException ex) {
                    Logger.getLogger(OnlineListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }).start();

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
```



## 交互的页面

```html
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
```

