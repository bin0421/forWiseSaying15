package com.back.domain.wiseSaying.controller;

import com.back.App;
import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import com.back.global.app.AppConfig;
import com.back.standard.util.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

public class WiseSayingControllerTest {
    @BeforeAll
    static void beforeAll() {
        AppConfig.setTestMode();
    }

    @BeforeEach
    void setUp() {
        WiseSayingDbRepository.clear();
    }

    @Test
    @DisplayName("종료")
    public void t1() {
        String input = """
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("프로그램을 종료합니다.");
    }

    @Test
    @DisplayName("등록 - 정상 케이스")
    public void t2_1() {
        String input = """
                등록
                현재에 충실하라
                작자미상
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("1번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("등록 - 명언에 특수문자 포함 예외")
    public void t2_2() {
        String input = """
                등록
                특수문자@#$ 포함
                정상명언
                작자미상
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("명언에는 특수문자를 입력할 수 없습니다. 다시 입력해주세요.");
    }

    @Test
    @DisplayName("등록 - 작가에 특수문자 포함 예외")
    public void t2_3() {
        String input = """
            등록
            정상명언
            작가이름!@#
            정상작가
            종료
            """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("작가에는 특수문자를 입력할 수 없습니다. 다시 입력해주세요.");
    }

    @Test
    @DisplayName("목록 - 페이징 기능 (기본, page생략)")
    public void t3_1() {
        String input = """
                목록
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs)
                .contains("번호 / 작가 / 명언")
                .contains("10 / 작자미상 10 / 명언 10")
                .contains("6 / 작자미상 6 / 명언 6")
                .contains("페이지 : [1] / 2");
    }

    @Test
    @DisplayName("목록 - 페이징 기능 (page=2)")
    public void t3_2() {
        String input = """
                목록?page=2
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs)
                .contains("5 / 작자미상 5 / 명언 5")
                .contains("1 / 작자미상 1 / 명언 1")
                .contains("페이지 : 1 / [2]");
    }

    @Test
    @DisplayName("목록 - 검색 기능 (내용에 '1' 검색)")
    public void t3_3() {
        String input = """
                목록?keywordType=content&keyword=1
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs)
                .contains("검색타입 : content")
                .contains("검색어 : 1")
                .contains("10 / 작자미상 10 / 명언 10")
                .contains("1 / 작자미상 1 / 명언 1");
    }

    @Test
    @DisplayName("목록 - 2개 등록 후 조회 (샘플 없는 상태 테스트)")
    public void t3_4() {
        String input = """
                목록
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("번호 / 작가 / 명언");
    }

    @Test
    @DisplayName("삭제 - 존재하지 않는 번호 예외")
    public void t4_1() {
        String input = """
                삭제?id=999
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("999번 명언은 존재하지 않습니다.");
    }

    @Test
    @DisplayName("삭제 - 정상 케이스")
    public void t4_2() {
        String input = """
                삭제?id=1
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("1번 명언이 삭제되었습니다.");
    }

    @Test
    @DisplayName("삭제 - 이미 삭제된 번호 중복 삭제 예외")
    public void t4_3() {
        String input = """
                삭제?id=1
                삭제?id=1
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs)
                .contains("1번 명언이 삭제되었습니다.")
                .contains("1번 명언은 존재하지 않습니다.");
    }

    @Test
    @DisplayName("삭제 - 숫자가 아닌 ID 예외")
    public void t4_4() {
        String input = """
                삭제?id=abc
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("올바른 번호를 입력해주세요.");
    }

    @Test
    @DisplayName("수정 - 존재하지 않는 번호 예외")
    public void t5_1() {
        String input = """
                수정?id=999
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("999번 명언은 존재하지 않습니다.");
    }

    @Test
    @DisplayName("수정 - 숫자가 아닌 ID 예외")
    public void t5_2() {
        String input = """
                수정?id=xyz
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs).contains("올바른 번호를 입력해주세요.");
    }

    @Test
    @DisplayName("수정 - 정상 케이스")
    public void t5_3() {
        String input = """
                수정?id=1
                바뀐명언
                바뀐작가
                목록?page=2
                종료
                """;
        Scanner scanner = TestUtil.genScanner(input);
        ByteArrayOutputStream output = TestUtil.setOutToByteArray();

        new App().run(scanner);

        String rs = output.toString();
        TestUtil.clearSetOutToByteArray();

        assertThat(rs)
                .contains("바뀐명언")
                .contains("바뀐작가");
    }
}