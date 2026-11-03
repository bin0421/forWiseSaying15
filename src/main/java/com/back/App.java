package com.back;

import com.back.domain.system.controller.SystemController;
import com.back.domain.wiseSaying.controller.WiseSayingController;

import java.util.Scanner;

public class App {
    public void run(Scanner scanner) {
        System.out.println("== 명언 앱 ==");

        SystemController systemController = new SystemController();
        WiseSayingController wiseSayingController = new WiseSayingController(scanner);


        while (true) {
            System.out.print("명령) ");
            String cmd = scanner.nextLine().trim();

            if (cmd.equals("종료")) {
                systemController.exit();
                break;
            } else if (cmd.equals("등록")) {
                wiseSayingController.write();
            } else if (cmd.equals("목록") || cmd.startsWith("목록?")) {
                wiseSayingController.list(cmd);
            } else if (cmd.startsWith("삭제?id=")) {
                wiseSayingController.remove(cmd);
            } else if (cmd.startsWith("수정?id=")) {
                wiseSayingController.modify(cmd);
            } else if (cmd.equals("빌드")) {
                wiseSayingController.build();
            }
        }
    }
}