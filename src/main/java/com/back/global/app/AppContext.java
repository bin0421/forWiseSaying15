//공용 객체(SimpleDb, ObjectMapper) 보관
package com.back.global.app;

import com.back.standard.simpleDb.SimpleDb;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class AppContext {
    public static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static SimpleDb simpleDb;
    private static String simpleDbMode;

    // 모드(dev/test)가 바뀌면 해당 DB 로 다시 연결
    public static synchronized SimpleDb getSimpleDb() {
        if (simpleDb == null || !AppConfig.getMode().equals(simpleDbMode)) {
            if (simpleDb != null) {
                simpleDb.close();
            }

            simpleDb = new SimpleDb(
                    AppConfig.getDbHost(),
                    AppConfig.getDbPort(),
                    AppConfig.getDbUsername(),
                    AppConfig.getDbPassword(),
                    AppConfig.getDbName()
            );
            simpleDbMode = AppConfig.getMode();
        }
        return simpleDb;
    }
}
