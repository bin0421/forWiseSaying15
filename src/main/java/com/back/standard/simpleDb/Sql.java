package com.back.standard.simpleDb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    SimpleDb simpleDb;
    StringBuilder sb = new StringBuilder();
    List<Object> params = new ArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... args) {
        if (!sb.isEmpty()) {
            sb.append(" ");
        }
        sb.append(sqlPart);
        if (args != null) {
            Collections.addAll(params, args);
        }
        return this;
    }

    public Sql appendIn(String sqlPart, List<?> list) {
        if (list == null || list.isEmpty()) {
            return append(sqlPart, Collections.emptyList());
        }
        String placeholders = String.join(", ", Collections.nCopies(list.size(), "?"));
        String replacedSql = sqlPart.replace("?", placeholders);
        return append(replacedSql, list.toArray());
    }

    public Sql appendIn(String sqlPart, Object... args) {
        if (args == null || args.length == 0) {
            return append(sqlPart);
        }
        if (args.length == 1 && args[0] instanceof Object[]) {
            return appendIn(sqlPart, Arrays.asList((Object[]) args[0]));
        }
        return appendIn(sqlPart, Arrays.asList(args));
    }

    private void printQuery() {
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(getCompleteSql());
        }
    }

    public String getCompleteSql() {
        String query = sb.toString();
        for (Object param : params) {
            String replacement;
            if (param == null) {
                replacement = "NULL";
            } else if (param instanceof Number || param instanceof Boolean) {
                replacement = param.toString();
            } else {
                replacement = "'" + param.toString().replace("'", "''") + "'";
            }
            query = query.replaceFirst("\\?", replacement);
        }
        return query;
    }

    private PreparedStatement prepareStatement(Connection conn, boolean returnGeneratedKeys) throws SQLException {
        printQuery();
        PreparedStatement ps;
        if (returnGeneratedKeys) {
            ps = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
        } else {
            ps = conn.prepareStatement(sb.toString());
        }

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }

    public long insert() {
        Connection conn = simpleDb.getConnection();
        boolean isTrans = isTransactionActive(conn);
        try (PreparedStatement ps = prepareStatement(conn, true)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (!isTrans) {
                closeConnection(conn);
            }
        }
        return 0;
    }

    public int update() {
        Connection conn = simpleDb.getConnection();
        boolean isTrans = isTransactionActive(conn);
        try (PreparedStatement ps = prepareStatement(conn, false)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (!isTrans) {
                closeConnection(conn);
            }
        }
    }

    public int delete() {
        return update();
    }

    public List<Map<String, Object>> selectRows() {
        Connection conn = simpleDb.getConnection();
        boolean isTrans = isTransactionActive(conn);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = prepareStatement(conn, false);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    if (value instanceof Timestamp) {
                        value = ((Timestamp) value).toLocalDateTime();
                    } else if (value instanceof Number && metaData.getColumnType(i) == Types.BIT) {
                        value = ((Number) value).intValue() != 0;
                    }
                    row.put(columnName, value);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (!isTrans) {
                closeConnection(conn);
            }
        }
        return rows;
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public <T> List<T> selectRows(Class<T> cls) {
        List<Map<String, Object>> rows = selectRows();
        List<T> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            list.add(objectMapper.convertValue(row, cls));
        }
        return list;
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();
        if (row == null) return null;
        return objectMapper.convertValue(row, cls);
    }

    public LocalDateTime selectDatetime() {
        Object val = selectScalar();
        if (val instanceof Timestamp) {
            return ((Timestamp) val).toLocalDateTime();
        }
        return (LocalDateTime) val;
    }

    public Long selectLong() {
        Object val = selectScalar();
        if (val == null) return null;
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return Long.parseLong(val.toString());
    }

    public List<Long> selectLongs() {
        List<Map<String, Object>> rows = selectRows();
        List<Long> longs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            for (Object val : row.values()) {
                if (val instanceof Number) {
                    longs.add(((Number) val).longValue());
                } else if (val != null) {
                    longs.add(Long.parseLong(val.toString()));
                }
                break;
            }
        }
        return longs;
    }

    public String selectString() {
        Object val = selectScalar();
        return val != null ? val.toString() : null;
    }

    public Boolean selectBoolean() {
        Object val = selectScalar();
        if (val == null) return null;
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return Boolean.parseBoolean(val.toString());
    }

    private Object selectScalar() {
        Map<String, Object> row = selectRow();
        if (row == null || row.isEmpty()) return null;
        return row.values().iterator().next();
    }

    private boolean isTransactionActive(Connection conn) {
        try {
            return conn != null && !conn.getAutoCommit();
        } catch (SQLException e) {
            return false;
        }
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }
}