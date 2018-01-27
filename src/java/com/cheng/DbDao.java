/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
