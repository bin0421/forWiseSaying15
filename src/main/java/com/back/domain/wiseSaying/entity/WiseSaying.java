//명언 객체 정보
package com.back.domain.wiseSaying.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class WiseSaying {
    public int id;
    public LocalDateTime createDate;
    public LocalDateTime modifyDate;
    public String content;
    public String author;

    // SimpleDb(jackson) 가 DB row 를 객체로 변환할 때 사용
    public WiseSaying() {
    }

    public WiseSaying(int id, String content, String author) {
        this.id = id;
        this.content = content;
        this.author = author;
    }

    public boolean isNew() {
        return id == 0;
    }

    // data.json 에 들어갈 항목만 순서대로
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("content", content);
        map.put("author", author);
        return map;
    }
}
