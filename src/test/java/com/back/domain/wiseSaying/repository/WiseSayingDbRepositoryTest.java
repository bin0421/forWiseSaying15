package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.global.app.AppConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WiseSayingDbRepositoryTest {
    private WiseSayingDbRepository repository;

    @BeforeAll
    static void beforeAll() {
        AppConfig.setTestMode();
    }

    @BeforeEach
    void setUp() {
        WiseSayingDbRepository.clear();
        repository = new WiseSayingDbRepository(); // 빈 테이블이면 샘플 10개(1~10번) 생성
    }

    @Test
    @DisplayName("테스트 모드에서는 테스트 DB 를 사용한다")
    public void t0() {
        assertThat(AppConfig.getDbName()).isEqualTo("wise_saying_test");
    }

    @Test
    @DisplayName("빈 테이블이면 샘플 10개가 1~10번으로 생성된다")
    public void t1() {
        List<WiseSaying> all = repository.findAll();

        assertThat(all).hasSize(10);
        assertThat(all.getFirst().id).isEqualTo(1);
        assertThat(all.getLast().id).isEqualTo(10);
        assertThat(all.getLast().content).isEqualTo("명언 10");
        assertThat(all.getLast().createDate).isNotNull();
    }

    @Test
    @DisplayName("save - 새 명언은 AUTO_INCREMENT 로 번호가 발급된다")
    public void t2() {
        WiseSaying saved = repository.save(new WiseSaying(0, "현재에 충실하라", "작자미상"));

        assertThat(saved.id).isEqualTo(11);

        WiseSaying found = repository.findById(11);
        assertThat(found.content).isEqualTo("현재에 충실하라");
        assertThat(found.author).isEqualTo("작자미상");
    }

    @Test
    @DisplayName("save - 기존 명언은 수정된다")
    public void t3() {
        WiseSaying wiseSaying = repository.findById(1);
        wiseSaying.content = "바뀐명언";
        wiseSaying.author = "바뀐작가";
        repository.save(wiseSaying);

        WiseSaying found = repository.findById(1);
        assertThat(found.content).isEqualTo("바뀐명언");
        assertThat(found.author).isEqualTo("바뀐작가");
        assertThat(repository.count("", "")).isEqualTo(10);
    }

    @Test
    @DisplayName("findById - 없는 번호면 null")
    public void t4() {
        assertThat(repository.findById(999)).isNull();
    }

    @Test
    @DisplayName("delete - 삭제 후 조회되지 않고, 없는 번호 삭제는 false")
    public void t5() {
        assertThat(repository.delete(1)).isTrue();
        assertThat(repository.findById(1)).isNull();
        assertThat(repository.delete(1)).isFalse();
        assertThat(repository.count("", "")).isEqualTo(9);
    }

    @Test
    @DisplayName("findPage - id 내림차순, LIMIT/OFFSET 페이징")
    public void t6() {
        List<WiseSaying> page1 = repository.findPage("", "", 1, 5);
        List<WiseSaying> page2 = repository.findPage("", "", 2, 5);
        List<WiseSaying> page3 = repository.findPage("", "", 3, 5);

        assertThat(page1).extracting(w -> w.id).containsExactly(10, 9, 8, 7, 6);
        assertThat(page2).extracting(w -> w.id).containsExactly(5, 4, 3, 2, 1);
        assertThat(page3).isEmpty();
    }

    @Test
    @DisplayName("findPage/count - content LIKE 검색")
    public void t7() {
        List<WiseSaying> result = repository.findPage("content", "1", 1, 5);

        assertThat(result).extracting(w -> w.id).containsExactly(10, 1);
        assertThat(repository.count("content", "1")).isEqualTo(2);
    }

    @Test
    @DisplayName("findPage/count - author LIKE 검색 + 페이징")
    public void t8() {
        assertThat(repository.count("author", "작자미상")).isEqualTo(10);
        assertThat(repository.findPage("author", "작자미상", 2, 3))
                .extracting(w -> w.id)
                .containsExactly(7, 6, 5);
    }

    @Test
    @DisplayName("검색 - 알 수 없는 검색 타입이면 결과 없음, LIKE 특수문자는 그대로 검색")
    public void t9() {
        assertThat(repository.count("title", "1")).isZero();
        assertThat(repository.count("content", "%")).isZero();
        assertThat(repository.count("content", "_")).isZero();
    }

    @Test
    @DisplayName("재시작해도 데이터와 번호가 유지된다 (삭제된 마지막 번호를 재사용하지 않음)")
    public void t10() {
        repository.save(new WiseSaying(0, "명언 11", "작가 11"));
        repository.delete(11);

        // 재시작 = 저장소를 새로 생성
        WiseSayingDbRepository restarted = new WiseSayingDbRepository();
        WiseSaying saved = restarted.save(new WiseSaying(0, "명언 12", "작가 12"));

        assertThat(restarted.count("", "")).isEqualTo(11);
        assertThat(saved.id).isEqualTo(12);
    }

    @Test
    @DisplayName("clear - 테스트 모드가 아니면 테이블을 비울 수 없다")
    public void t11() {
        AppConfig.setDevMode();
        try {
            assertThatThrownBy(WiseSayingDbRepository::clear)
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            AppConfig.setTestMode();
        }
    }
}
