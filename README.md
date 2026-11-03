# forWiseSaying15 — 명언 앱 MySQL 버전

[forWisesaying13to14](https://github.com/bin0421/forWisesaying13to14) 의 파일 저장 명언 앱을
[simpleDB](https://github.com/bin0421/simpleDB) 의 `SimpleDb` 를 재사용해 **MySQL 저장**으로 바꾼 버전입니다.
등록·목록·삭제·수정·검색·페이징·빌드 기능은 그대로입니다.

## 1. 테이블 설계

테이블 생성 SQL: [`db/schema.sql`](db/schema.sql)

```sql
CREATE TABLE IF NOT EXISTS wise_saying (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    createDate DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifyDate DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    content    TEXT         NOT NULL,
    author     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);
```

| DB | 용도 |
|---|---|
| `wise_saying_dev` | 앱 실행용 (개발 DB) |
| `wise_saying_test` | 테스트 전용. 매 테스트 전에 `TRUNCATE wise_saying` |

## 2. MySQL 띄우고 DB·테이블 만들기

### 접속 정보 설정

비밀번호 등은 소스에 없고 **환경변수 또는 `.env` 파일**에서 읽습니다. (`.env` 는 git 에 올라가지 않음)

```bash
cp .env.example .env
# .env 의 DB_PASSWORD 를 원하는 값으로 채우기
```

| 키 | 기본값 | 설명 |
|---|---|---|
| `DB_HOST` | `localhost` | |
| `DB_PORT` | `3306` | 3306 을 이미 다른 MySQL 이 쓰면 `3307` 등으로 |
| `DB_USERNAME` | `root` | |
| `DB_PASSWORD` | (없음) | **필수** |
| `DB_DEV_NAME` | `wise_saying_dev` | 개발 DB 이름 |
| `DB_TEST_NAME` | `wise_saying_test` | 테스트 DB 이름 |

읽는 순서: 환경변수 → `.env` → 기본값 ([`AppConfig`](src/main/java/com/back/global/app/AppConfig.java))

### 방법 A — Docker (권장)

```bash
docker compose up -d
```

`docker-compose.yml` 이 `.env` 의 `DB_PASSWORD`, `DB_PORT` 를 그대로 사용하고,
최초 기동 시 `db/schema.sql` 을 자동 실행해 **개발/테스트 DB 와 테이블을 모두 생성**합니다.

초기화를 다시 하려면 볼륨까지 지우고 다시 띄웁니다: `docker compose down -v && docker compose up -d`

### 방법 B — 로컬에 설치된 MySQL

```bash
mysql -u root -p < db/schema.sql
```

## 3. 실행 / 테스트

```bash
./gradlew run --console=plain -q   # 앱 실행 (개발 DB)
./gradlew test                     # 테스트 (테스트 DB)
```

IntelliJ 에서는 `Main` 실행 / 테스트 클래스 실행으로도 동작합니다. (작업 디렉터리 = 프로젝트 루트여야 `.env` 를 읽음)

- 실행 모드는 시스템 프로퍼티 `app.mode` 로 결정됩니다. 기본 `dev`, `gradle test` 는 `build.gradle.kts` 에서 `test` 로 지정합니다.
- 테스트 클래스는 `@BeforeAll` 에서 `AppConfig.setTestMode()` 도 호출하므로, IDE 에서 직접 실행해도 개발 DB 를 건드리지 않습니다.
- `WiseSayingDbRepository.clear()`(TRUNCATE)는 테스트 모드가 아니면 예외를 던집니다.
- `빌드` 결과: 개발 모드는 프로젝트 루트의 `data.json`, 테스트 모드는 `build/test-data/data.json`.

## 4. 리포지터리를 어떻게 갈아 끼웠나

```
WiseSayingController ──> WiseSayingService ──> WiseSayingRepository (interface)
        (변경 없음)          (최소 변경)            ├─ WiseSayingDbRepository   ← 현재 사용 (MySQL, SimpleDb)
                                                    └─ WiseSayingFileRepository ← 이전 파일 저장 방식
```

1. 기존 파일 저장 클래스 `WiseSayingRepository` 를 **인터페이스**로 바꾸고, 메서드를 저장소 공통 동작으로 정리했습니다.
   `save / findById / findAll / findPage / count / delete`
2. 기존 파일 구현은 `WiseSayingFileRepository` 로 이름을 바꿔 인터페이스를 구현하게 했습니다.
3. MySQL 구현 `WiseSayingDbRepository` 를 새로 만들었습니다. SQL 실행은 앞 과제의 `SimpleDb`/`Sql` 을 그대로 가져와 사용합니다(`com.back.standard.simpleDb`, 포트 지정 생성자만 추가).
   - 등록: `INSERT` 후 `AUTO_INCREMENT` 로 발급된 id 를 객체에 넣음 → 번호는 DB 가 관리 (`lastId.txt` 불필요, 재시작·삭제 후에도 번호 재사용 없음)
   - 수정: `UPDATE ... WHERE id = ?`, 삭제: `DELETE ... WHERE id = ?`
   - 검색: `WHERE content LIKE ?` / `WHERE author LIKE ?` (컬럼명은 화이트리스트, 값은 PreparedStatement 파라미터, `%`/`_` 이스케이프)
   - 페이징: `ORDER BY id DESC LIMIT ? OFFSET ?`, 전체 페이지 수는 `SELECT COUNT(*)`
   - 테이블이 비어 있으면 기존과 같이 샘플 명언 10개를 넣음
4. **서비스 변경점** (컨트롤러는 그대로)
   - 생성자 한 줄: `new WiseSayingDbRepository()` — 저장소 교체 지점. `new WiseSayingFileRepository()` 로 바꾸면 파일 저장으로 동작. 테스트용으로 `WiseSayingService(WiseSayingRepository)` 생성자도 추가.
   - 번호 발급(`lastId++`)을 저장소에 위임, 검색·페이징을 메모리 필터링 대신 저장소(SQL)에 위임.
   - `빌드` 는 jackson 으로 `data.json` 생성.
5. **테스트 변경점**: 기존 `WiseSayingControllerTest` 는 `setUp` 의 `WiseSayingRepository.clear()` → `WiseSayingDbRepository.clear()` 와 `@BeforeAll` 테스트 모드 지정만 바꾸고 나머지 케이스는 그대로 통과합니다.

## 5. 테스트 구성

| 테스트 | 내용 |
|---|---|
| `WiseSayingControllerTest` | 기존 통합 테스트 (그대로) |
| `WiseSayingDbRepositoryTest` | DB 저장소 단위: AUTO_INCREMENT 발급, 수정, 삭제, LIKE 검색, LIMIT/OFFSET 페이징, 재시작 후 번호 유지, 테스트 모드 보호 |
| `WiseSayingDbAppTest` | 앱 전체 흐름: 등록 → 재시작 → 목록/번호 유지, 수정 반영, 검색+페이징, `data.json` 빌드 |
| `TestUtilTest` | 테스트 도구 |

모든 테스트는 `wise_saying_test` DB 를 사용하며 `@BeforeEach` 에서 `TRUNCATE` 합니다.
