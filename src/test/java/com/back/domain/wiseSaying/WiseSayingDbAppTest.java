package com.back.domain.wiseSaying;

import com.back.App;
import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import com.back.global.app.AppConfig;
import com.back.standard.util.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

// App 전체 흐름 + MySQL 통합 테스트
public class WiseSayingDbAppTest {
    @BeforeAll
    static void beforeAll() {
        AppConfig.setTestMode();
    }

    @BeforeEach
    void setUp() throws Exception {
        WiseSayingDbRepository.clear();
        Files.deleteIfExists(AppConfig.getDataJsonPath());
    }

    private String run(String input) {
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();
        return rs;
    }

    @Test
    @DisplayName("등록 - 샘플 10개 다음 번호인 11번으로 등록된다")
    public void t1() {
        String rs = run("""
                등록
                현재에 충실하라
                작자미상
                종료
                """);

        assertThat(rs).contains("11번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("저장 후 재시작해도 데이터와 번호가 유지된다")
    public void t2() {
        run("""
                등록
                현재에 충실하라
                작자미상
                삭제?id=11
                등록
                과거에 집착하지 마라
                작자미상
                종료
                """);

        // 앱 재시작
        String rs = run("""
                목록
                등록
                재시작 후 명언
                홍길동
                종료
                """);

        assertThat(rs)
                .contains("12 / 작자미상 / 과거에 집착하지 마라")
                .doesNotContain("현재에 충실하라")
                .contains("13번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("수정 - DB 에 반영되어 다시 실행해도 수정된 내용이 보인다")
    public void t3() {
        run("""
                수정?id=3
                바뀐명언
                바뀐작가
                종료
                """);

        String rs = run("""
                목록?page=2
                종료
                """);

        assertThat(rs)
                .contains("3 / 바뀐작가 / 바뀐명언")
                .doesNotContain("3 / 작자미상 3 / 명언 3");
    }

    @Test
    @DisplayName("검색 + 페이징 - 작가 검색 결과의 2페이지")
    public void t4() {
        String rs = run("""
                목록?keywordType=author&keyword=작자미상&page=2
                종료
                """);

        assertThat(rs)
                .contains("검색타입 : author")
                .contains("5 / 작자미상 5 / 명언 5")
                .contains("1 / 작자미상 1 / 명언 1")
                .doesNotContain("6 / 작자미상 6 / 명언 6")
                .contains("페이지 : 1 / [2]");
    }

    @Test
    @DisplayName("빌드 - DB 내용으로 data.json 을 만든다")
    public void t5() throws Exception {
        String rs = run("""
                등록
                현재에 충실하라
                작자미상
                삭제?id=1
                빌드
                종료
                """);

        assertThat(rs).contains("data.json 파일의 내용이 갱신되었습니다.");

        String json = Files.readString(AppConfig.getDataJsonPath());
        assertThat(json)
                .startsWith("[")
                .contains("\"id\" : 11")
                .contains("\"content\" : \"현재에 충실하라\"")
                .contains("\"author\" : \"작자미상\"")
                .doesNotContain("\"id\" : 1,")
                .doesNotContain("createDate");
        assertThat(AppConfig.getDataJsonPath()).isEqualTo(Path.of("build", "test-data", "data.json"));
    }
}
