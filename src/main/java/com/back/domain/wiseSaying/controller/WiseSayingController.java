//사용자의 입력에 따른 프로그램의 동작 표현
package com.back.domain.wiseSaying.controller;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.domain.wiseSaying.service.WiseSayingService;

import java.util.ArrayList;
import java.util.Scanner;

public class WiseSayingController {
    private final Scanner sc;
    private final WiseSayingService service;

    public WiseSayingController(Scanner sc) {
        this.sc = sc;
        this.service = new WiseSayingService();
    }

    public void write() {
        String content = "";
        while (true) {
            System.out.print("명언) ");
            content = sc.nextLine().trim();

            if (service.isValidString(content)) {
                break;
            }
            System.out.println("명언에는 특수문자를 입력할 수 없습니다. 다시 입력해주세요.");
        }

        String author = "";
        while (true) {
            System.out.print("작가) ");
            author = sc.nextLine().trim();

            if (service.isValidString(author)) {
                break;
            }
            System.out.println("작가에는 특수문자를 입력할 수 없습니다. 다시 입력해주세요.");
        }

        WiseSaying wiseSaying = service.write(content, author);
        System.out.println(wiseSaying.id + "번 명언이 등록되었습니다.");
    }

    public void list(String cmd) {
        int page = 1;
        String keywordType = "";
        String keyword = "";

        if (cmd.contains("?")) {
            String queryString = cmd.split("\\?")[1];
            String[] params = queryString.split("&");

            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    if (keyValue[0].equals("keywordType")) {
                        keywordType = keyValue[1];
                    } else if (keyValue[0].equals("keyword")) {
                        keyword = keyValue[1];
                    } else if (keyValue[0].equals("page")) {
                        try {
                            page = Integer.parseInt(keyValue[1]);
                        } catch (NumberFormatException e) {
                            page = 1;
                        }
                    }
                }
            }
        }

        if (!keywordType.isEmpty() && !keyword.isEmpty()) {
            System.out.println("----------------------");
            System.out.println("검색타입 : " + keywordType);
            System.out.println("검색어 : " + keyword);
            System.out.println("----------------------");
        }

        System.out.println("번호 / 작가 / 명언");
        System.out.println("----------------------");

        ArrayList<WiseSaying> list = service.getPagedList(keywordType, keyword, page);
        for (WiseSaying w : list) {
            System.out.println(w.id + " / " + w.author + " / " + w.content);
        }

        System.out.println("----------------------");

        int totalPages = service.getTotalPages(keywordType, keyword);
        StringBuilder pageStr = new StringBuilder("페이지 : ");
        for (int i = 1; i <= totalPages; i++) {
            if (i == page) {
                pageStr.append("[").append(i).append("]");
            } else {
                pageStr.append(i);
            }
            if (i < totalPages) {
                pageStr.append(" / ");
            }
        }
        System.out.println(pageStr.toString());
    }

    public void remove(String cmd) {
        try {
            int id = Integer.parseInt(cmd.substring(6));
            WiseSaying wiseSaying = service.findById(id);

            if (wiseSaying == null) {
                System.out.println(id + "번 명언은 존재하지 않습니다.");
                return;
            }

            service.remove(wiseSaying);
            System.out.println(id + "번 명언이 삭제되었습니다.");
        } catch (NumberFormatException e) {
            System.out.println("올바른 번호를 입력해주세요.");
        }
    }

    public void modify(String cmd) {
        try {
            int id = Integer.parseInt(cmd.substring(6));
            WiseSaying wiseSaying = service.findById(id);

            if (wiseSaying == null) {
                System.out.println(id + "번 명언은 존재하지 않습니다.");
                return;
            }

            System.out.println("명언(기존) : " + wiseSaying.content);
            String content = "";
            while (true) {
                System.out.print("명언) ");
                content = sc.nextLine().trim();

                if (service.isValidString(content)) {
                    break;
                }
                System.out.println("특수문자는 입력할 수 없습니다. 다시 입력해주세요.");
            }

            System.out.println("작가(기존) : " + wiseSaying.author);
            String author = "";
            while (true) {
                System.out.print("작가) ");
                author = sc.nextLine().trim();

                if (service.isValidString(author)) {
                    break;
                }
                System.out.println("특수문자는 입력할 수 없습니다. 다시 입력해주세요.");
            }

            service.modify(wiseSaying, content, author);
            System.out.println(id + "번 명언이 수정되었습니다.");
        } catch (NumberFormatException e) {
            System.out.println("올바른 번호를 입력해주세요.");
        }
    }

    public void build() {
        service.buildJson();
        System.out.println("data.json 파일의 내용이 갱신되었습니다.");
    }
}