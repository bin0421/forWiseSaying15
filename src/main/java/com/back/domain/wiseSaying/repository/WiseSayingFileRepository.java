//데이터(파일)의 CRUD - 이전 단계 구현체. WiseSayingService 에서 new WiseSayingFileRepository() 로 바꾸면 파일 저장으로 동작
package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.global.app.AppContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WiseSayingFileRepository implements WiseSayingRepository {
    private final String dbDir;

    public WiseSayingFileRepository() {
        this("db/wiseSaying");
    }

    public WiseSayingFileRepository(String dbDir) {
        this.dbDir = dbDir;

        File dir = new File(dbDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (findAll().isEmpty()) {
            for (int i = 1; i <= 10; i++) {
                save(new WiseSaying(0, "명언 " + i, "작자미상 " + i));
            }
        }
    }

    @Override
    public WiseSaying save(WiseSaying wiseSaying) {
        if (wiseSaying.isNew()) {
            wiseSaying.id = findLastId() + 1;
            saveLastId(wiseSaying.id);
        }

        try {
            String json = AppContext.objectMapper.writeValueAsString(wiseSaying.toMap());
            Files.writeString(Path.of(dbDir, wiseSaying.id + ".json"), json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return wiseSaying;
    }

    @Override
    public WiseSaying findById(int id) {
        Path path = Path.of(dbDir, id + ".json");
        if (!Files.exists(path)) {
            return null;
        }

        try {
            return AppContext.objectMapper.readValue(path.toFile(), WiseSaying.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<WiseSaying> findAll() {
        List<WiseSaying> wiseSayings = new ArrayList<>();
        File[] files = new File(dbDir).listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (file.isFile() && name.matches("\\d+\\.json")) {
                    wiseSayings.add(findById(Integer.parseInt(name.replace(".json", ""))));
                }
            }
        }
        wiseSayings.sort(Comparator.comparingInt(w -> w.id));
        return wiseSayings;
    }

    @Override
    public List<WiseSaying> findPage(String keywordType, String keyword, int page, int pageSize) {
        List<WiseSaying> list = search(keywordType, keyword).reversed();

        int startIndex = (Math.max(page, 1) - 1) * pageSize;
        if (startIndex >= list.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(startIndex, Math.min(startIndex + pageSize, list.size())));
    }

    @Override
    public int count(String keywordType, String keyword) {
        return search(keywordType, keyword).size();
    }

    @Override
    public boolean delete(int id) {
        return new File(dbDir, id + ".json").delete();
    }

    private List<WiseSaying> search(String keywordType, String keyword) {
        List<WiseSaying> all = findAll();
        if (keywordType == null || keywordType.isEmpty() || keyword == null || keyword.isEmpty()) {
            return all;
        }

        List<WiseSaying> filtered = new ArrayList<>();
        for (WiseSaying w : all) {
            if (keywordType.equals("content") && w.content.contains(keyword)) {
                filtered.add(w);
            } else if (keywordType.equals("author") && w.author.contains(keyword)) {
                filtered.add(w);
            }
        }
        return filtered;
    }

    private int findLastId() {
        try {
            Path path = Path.of(dbDir, "lastId.txt");
            if (Files.exists(path)) {
                return Integer.parseInt(Files.readString(path).trim());
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return 0;
    }

    private void saveLastId(int lastId) {
        try {
            Files.writeString(Path.of(dbDir, "lastId.txt"), String.valueOf(lastId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
