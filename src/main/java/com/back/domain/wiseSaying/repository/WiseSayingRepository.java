//명언 저장소 규약 (구현체: 파일 / DB)
package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;

import java.util.List;

public interface WiseSayingRepository {
    // id 가 0 이면 새로 등록(번호 발급), 아니면 수정
    WiseSaying save(WiseSaying wiseSaying);

    WiseSaying findById(int id);

    // id 오름차순 전체 목록
    List<WiseSaying> findAll();

    // 검색 + 페이징 (id 내림차순). keywordType 이 비어 있으면 전체 대상
    List<WiseSaying> findPage(String keywordType, String keyword, int page, int pageSize);

    int count(String keywordType, String keyword);

    boolean delete(int id);
}
