package com.back.standard.simpleDb;

import java.sql.*;

public class SimpleDb {
    String host;
    int port;
    String username;
    String password;
    String dbName;
    boolean devMode = false;

    private final ThreadLocal<Connection> transConn = new ThreadLocal<>();
    private final ThreadLocal<Connection> threadConn = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String dbName) {
        this(host, 3306, username, password, dbName);
    }

    public SimpleDb(String host, int port, String username, String password, String dbName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public Connection getConnection() {
        // 1. 트랜잭션 커넥션이 활성화되어 있으면 우선 반환
        Connection conn = transConn.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    return conn;
                }
            } catch (SQLException ignored) {
            }
        }

        // 2. 현재 스레드에 할당된 커넥션이 있으면 반환
        conn = threadConn.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    return conn;
                }
            } catch (SQLException ignored) {
            }
        }

        // 3. 새 커넥션 생성 후 스레드에 바인딩
        try {
            String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true", host, port, dbName);
            conn = DriverManager.getConnection(url, username, password);
            threadConn.set(conn);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void startTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            transConn.set(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        Connection conn = transConn.get();
        if (conn != null) {
            try {
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                transConn.remove();
                closeConnection(conn);
            }
        }
    }

    public void rollback() {
        Connection conn = transConn.get();
        if (conn != null) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                transConn.remove();
                closeConnection(conn);
            }
        }
    }

    public void close() {
        Connection tConn = threadConn.get();
        if (tConn != null) {
            try {
                if (!tConn.isClosed()) {
                    tConn.close();
                }
            } catch (SQLException ignored) {
            } finally {
                threadConn.remove();
            }
        }

        Connection trans = transConn.get();
        if (trans != null) {
            try {
                if (!trans.isClosed()) {
                    trans.close();
                }
            } catch (SQLException ignored) {
            } finally {
                transConn.remove();
            }
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public long run(String sqlStr, Object... params) {
        Sql sql = genSql();
        sql.append(sqlStr, params);
        return sql.update();
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed() && conn.getAutoCommit()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }
}