//실행 모드(dev/test)와 DB 접속 정보 관리
package com.back.global.app;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {
    private static final String ENV_FILE = ".env";
    private static final Properties fileProps = loadEnvFile();

    // 실행 모드는 -Dapp.mode=test 로 지정 (gradle test 는 자동으로 test)
    private static String mode = System.getProperty("app.mode", "dev");

    public static String getMode() {
        return mode;
    }

    public static void setDevMode() {
        mode = "dev";
    }

    public static void setTestMode() {
        mode = "test";
    }

    public static boolean isTestMode() {
        return mode.equals("test");
    }

    public static String getDbHost() {
        return get("DB_HOST", "localhost");
    }

    public static int getDbPort() {
        return Integer.parseInt(get("DB_PORT", "3306"));
    }

    public static String getDbUsername() {
        return get("DB_USERNAME", "root");
    }

    public static String getDbPassword() {
        return get("DB_PASSWORD", "");
    }

    public static String getDbName() {
        if (isTestMode()) {
            return get("DB_TEST_NAME", "wise_saying_test");
        }
        return get("DB_DEV_NAME", "wise_saying_dev");
    }

    // 빌드 결과 파일 위치 (테스트는 build 폴더 안에 생성)
    public static Path getDataJsonPath() {
        if (isTestMode()) {
            return Path.of("build", "test-data", "data.json");
        }
        return Path.of("data.json");
    }

    // 환경변수 > .env 파일 > 기본값 순서로 읽음
    private static String get(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        value = fileProps.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        return defaultValue;
    }

    private static Properties loadEnvFile() {
        Properties props = new Properties();
        Path path = Path.of(ENV_FILE);

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(ENV_FILE + " 파일을 읽을 수 없습니다.", e);
            }
        }
        return props;
    }
}
