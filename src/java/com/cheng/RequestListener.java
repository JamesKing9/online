/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
