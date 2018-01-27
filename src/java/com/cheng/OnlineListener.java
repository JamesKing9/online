/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
