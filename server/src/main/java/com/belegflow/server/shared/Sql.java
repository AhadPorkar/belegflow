package com.belegflow.server.shared;
import com.belegflow.domain.BusinessException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
/** JDBC values are normalized before they cross the API boundary. No SQL string comes from user input. */
public final class Sql {
    private Sql() {
    }
    public static UUID uuid(Map<String,Object> row,String key) {
        Object v=row.get(key);
        return v==null?null:(UUID)v;
    }
    public static String str(Map<String,Object> row,String key) {
        Object v=row.get(key);
        return v==null?"":v.toString();
    }
    public static boolean bool(Map<String,Object> row,String key) {
        return Boolean.TRUE.equals(row.get(key));
    }
    public static int integer(Map<String,Object> row,String key) {
        return ((Number)row.get(key)).intValue();
    }
    public static Instant instant(Map<String,Object> row,String key) {
        Object v=row.get(key);
        return v==null?null:((Timestamp)v).toInstant();
    }
    public static Map<String,Object> api(Map<String,Object> row) {
        Map<String,Object> result=new LinkedHashMap<>();
        row.forEach((key,value)-> {
            Object v=value; if (v instanceof java.math.BigDecimal d) v=d.toPlainString(); else if (v instanceof Timestamp t) v=t.toInstant().toString(); else if (v instanceof java.sql.Date d) v=d.toLocalDate().toString(); result.put(key,v);
        });
        return result;
    }
    public static List<Map<String,Object>> api(List<Map<String,Object>> rows) {
        return rows.stream().map(Sql::api).toList();
    }
    public static Map<String,Object> one(List<Map<String,Object>> rows) {
        if(rows.isEmpty()) throw new BusinessException("NOT_FOUND");
        return rows.getFirst();
    }
    public static String text(String value,int max,boolean required) {
        String s=value==null?"":value.strip();
        BusinessException.require(s.length()<=max && (!required || !s.isBlank()),"INVALID_INPUT");
        return s;
    }
}
