//데이터(MySQL)의 CRUD
package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.global.app.AppConfig;
import com.back.global.app.AppContext;
import com.back.standard.simpleDb.SimpleDb;
import com.back.standard.simpleDb.Sql;

import java.util.List;

public class WiseSayingDbRepository implements WiseSayingRepository {
    private static final String TABLE = "wise_saying";

    private final SimpleDb simpleDb;

    public WiseSayingDbRepository() {
        this.simpleDb = AppContext.getSimpleDb();

        if (count("", "") == 0) {
            for (int i = 1; i <= 10; i++) {
                save(new WiseSaying(0, "명언 " + i, "작자미상 " + i));
            }
        }
    }

    // 테스트 전용: 테이블 비우기 (AUTO_INCREMENT 도 1 로 초기화)
    public static void clear() {
        if (!AppConfig.isTestMode()) {
            throw new IllegalStateException("테이블 비우기는 테스트 모드에서만 가능합니다.");
        }
        AppContext.getSimpleDb().run("TRUNCATE " + TABLE);
    }

    @Override
    public WiseSaying save(WiseSaying wiseSaying) {
        if (wiseSaying.isNew()) {
            long id = simpleDb.genSql()
                    .append("INSERT INTO " + TABLE)
                    .append("SET createDate = NOW()")
                    .append(", modifyDate = NOW()")
                    .append(", content = ?", wiseSaying.content)
                    .append(", author = ?", wiseSaying.author)
                    .insert();
            wiseSaying.id = (int) id;
        } else {
            simpleDb.genSql()
                    .append("UPDATE " + TABLE)
                    .append("SET modifyDate = NOW()")
                    .append(", content = ?", wiseSaying.content)
                    .append(", author = ?", wiseSaying.author)
                    .append("WHERE id = ?", wiseSaying.id)
                    .update();
        }
        return wiseSaying;
    }

    @Override
    public WiseSaying findById(int id) {
        return simpleDb.genSql()
                .append("SELECT * FROM " + TABLE)
                .append("WHERE id = ?", id)
                .selectRow(WiseSaying.class);
    }

    @Override
    public List<WiseSaying> findAll() {
        return simpleDb.genSql()
                .append("SELECT * FROM " + TABLE)
                .append("ORDER BY id ASC")
                .selectRows(WiseSaying.class);
    }

    @Override
    public List<WiseSaying> findPage(String keywordType, String keyword, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * pageSize;

        Sql sql = simpleDb.genSql().append("SELECT * FROM " + TABLE);
        appendWhere(sql, keywordType, keyword);
        return sql
                .append("ORDER BY id DESC")
                .append("LIMIT ? OFFSET ?", pageSize, offset)
                .selectRows(WiseSaying.class);
    }

    @Override
    public int count(String keywordType, String keyword) {
        Sql sql = simpleDb.genSql().append("SELECT COUNT(*) FROM " + TABLE);
        appendWhere(sql, keywordType, keyword);
        return sql.selectLong().intValue();
    }

    @Override
    public boolean delete(int id) {
        return simpleDb.genSql()
                .append("DELETE FROM " + TABLE)
                .append("WHERE id = ?", id)
                .delete() > 0;
    }

    // 검색 조건. 컬럼명은 화이트리스트로만 허용 (SQL 인젝션 방지)
    private void appendWhere(Sql sql, String keywordType, String keyword) {
        if (isBlank(keywordType) || isBlank(keyword)) {
            return;
        }

        String column = switch (keywordType) {
            case "content" -> "content";
            case "author" -> "author";
            default -> null;
        };

        if (column == null) {
            // 알 수 없는 검색 타입이면 결과 없음 (기존 파일 저장소와 동일한 동작)
            sql.append("WHERE 1 = 0");
            return;
        }

        sql.append("WHERE " + column + " LIKE ?", "%" + escapeLike(keyword) + "%");
    }

    private String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private boolean isBlank(String str) {
        return str == null || str.isEmpty();
    }
}
