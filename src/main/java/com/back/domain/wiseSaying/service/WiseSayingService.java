//사용자의 입력과 관계없는 로직
package com.back.domain.wiseSaying.service;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import com.back.domain.wiseSaying.repository.WiseSayingRepository;
import com.back.global.app.AppConfig;
import com.back.global.app.AppContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WiseSayingService {
    private static final int ITEMS_PER_PAGE = 5;

    private final WiseSayingRepository repository;

    public WiseSayingService() {
        // 저장소 교체 지점: 파일 저장소를 쓰려면 new WiseSayingFileRepository()
        this(new WiseSayingDbRepository());
    }

    public WiseSayingService(WiseSayingRepository repository) {
        this.repository = repository;
    }

    public boolean isValidString(String str) {
        return str.matches("^[a-zA-Z0-9가-힣\\s]*$");
    }

    public ArrayList<WiseSaying> search(String keywordType, String keyword) {
        return new ArrayList<>(repository.findPage(keywordType, keyword, 1, Integer.MAX_VALUE));
    }

    // 번호는 저장소(DB 의 AUTO_INCREMENT)가 발급
    public WiseSaying write(String content, String author) {
        WiseSaying wiseSaying = new WiseSaying(0, content, author);
        return repository.save(wiseSaying);
    }

    public ArrayList<WiseSaying> findAll() {
        return new ArrayList<>(repository.findAll());
    }

    public WiseSaying findById(int id) {
        return repository.findById(id);
    }

    public void remove(WiseSaying wiseSaying) {
        repository.delete(wiseSaying.id);
    }

    public void modify(WiseSaying wiseSaying, String content, String author) {
        wiseSaying.content = content;
        wiseSaying.author = author;
        repository.save(wiseSaying);
    }

    public void buildJson() {
        List<Object> list = new ArrayList<>();
        for (WiseSaying w : repository.findAll()) {
            list.add(w.toMap());
        }

        try {
            Path path = AppConfig.getDataJsonPath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, AppContext.objectMapper.writeValueAsString(list));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<WiseSaying> getPagedList(String keywordType, String keyword, int page) {
        return new ArrayList<>(repository.findPage(keywordType, keyword, page, ITEMS_PER_PAGE));
    }

    public int getTotalPages(String keywordType, String keyword) {
        int totalCount = repository.count(keywordType, keyword);
        int totalPages = (int) Math.ceil((double) totalCount / ITEMS_PER_PAGE);
        return Math.max(1, totalPages);
    }
}
