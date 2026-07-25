# 응답 DTO 컨벤션

새 API를 만들 때 응답 DTO는 아래 규칙을 따른다.

---

## 1. 형태 — Lombok 클래스

Lombok 클래스로 만든다.

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XxxResponse {

    private final Long xxxId;
    private final String title;

    public static XxxResponse of(Long xxxId, String title) {
        return new XxxResponse(xxxId, title);
    }
}
```

- 필드는 전부 `private final`
- 생성자는 `@AllArgsConstructor(access = AccessLevel.PRIVATE)` 로 감추고 정적 팩토리로만 만든다
- `@JsonInclude(NON_NULL)` 을 붙여 null 필드는 응답에서 제외한다

접근자는 `getXxx()` 형태가 된다. `record` 의 `xxx()` 접근자와 헷갈리지 않도록 주의한다.

## 2. 팩토리 — `from(entity)` 를 기본으로

엔티티에서 바로 만들 수 있는 DTO는 **`from(엔티티)` 를 반드시 제공한다.**

```java
public static XxxResponse from(Xxx entity) {
    return of(entity.getXxxId(), entity.getTitle());
}
```

- `from()` 은 내부에서 `of()` 를 호출한다
- `of()` 는 파라미터를 그대로 받는 저수준 팩토리로 남긴다. 쿼리 결과나 프로젝션처럼
  엔티티가 없는 자리에서 쓴다
- 매핑 로직을 서비스에 두지 않는다. 서비스는 `XxxResponse.from(entity)` 한 줄로 끝나야 한다

중첩 DTO도 같은 규칙을 따른다. 상위 DTO의 `from()` 안에서 하위 DTO의 `from()` 을 부른다.

```java
public static LearningResourceResponse from(LearningResource resource) {
    return of(..., FileAssetResponse.from(resource.getFileAsset()), ...);
}
```

## 3. 시간 타입

### 3-1. 타임스탬프는 `LocalDateTime`

`createdAt` / `updatedAt` 처럼 "특정 시점"을 나타내는 필드는 `LocalDateTime` 을 쓴다.

`BaseTimeEntity` 의 `createdAt` / `updatedAt` 이 `LocalDateTime` 이고 DB 컬럼에도 오프셋
정보가 없다. 이걸 `OffsetDateTime` 으로 올리려면 매핑 시점에 타임존을 가정해야 하는데,
없는 정보를 지어내는 것이라 서버 타임존이 바뀌면 값이 틀어진다.

**예외**: 엔티티에서 유래하지 않은 "절대 시각"은 `OffsetDateTime` 을 쓸 수 있다.
현재 `FileView.expiresAt`(presigned URL 만료 시각)이 여기 해당한다. 벽시계 시각이 아니라
특정 순간을 가리키므로 오프셋을 갖는 것이 맞다.

### 3-2. 날짜·기간은 의미에 맞는 타입으로

시점이 아닌 날짜·기간 표현은 정밀도를 억지로 올리지 말고 의미에 맞는 java.time 타입을 쓴다.

| 의미    | 타입        | 예                                            |
| ------- | ----------- | --------------------------------------------- |
| 월 단위 | `YearMonth` | `ProjectResponse.startedMonth` / `endedMonth` |
| 일 단위 | `LocalDate` | 활동 일자 등                                  |

문자열로 내리면 `yyyy-MM` 같은 포맷 규칙을 따로 합의해야 하고 값 검증도 되지 않는다.
타입으로 표현하면 의미가 코드에 드러나고 직렬화 포맷도 일관된다.

엔티티 컬럼 타입과 다를 수 있다. `Project.startedMonth` 는 `LocalDate` 이므로 DTO 매핑
시 `YearMonth.from(...)` 으로 변환한다. 이 변환은 `from(entity)` 안에서 처리한다.

> 참고: 저장되는 시각은 서버(컨테이너)의 JVM 기본 타임존을 따른다. 이 값이 UTC 이면
> KST 와 9시간 어긋나므로, `Dockerfile` 에서 `TZ=Asia/Seoul` 과
> `-Duser.timezone=Asia/Seoul` 로 KST 를 고정한다. 새 실행 환경을 추가할 때도 같은
> 타임존을 맞춰야 값이 일관된다.

## 4. 숫자 타입 — 엔티티 유래 필드는 래퍼 타입

엔티티에서 가져오는 숫자 필드는 `int` 가 아니라 `Integer` 를 쓴다.

엔티티 필드가 전부 `Integer` 라서 원시타입으로 받으면 오토언박싱이 일어나고, 값이 null 이면
"어느 필드가 비었다"가 아니라 매핑 도중 NPE 로 터져서 원인 추적이 어렵다.

**예외**: 엔티티와 무관하게 서버가 계산해서 채우는 값은 원시타입을 쓴다. null 이 될 수
없기 때문이다. `PageMeta` 의 `page` / `size` / `totalElements` / `totalPages` / `hasNext`
가 여기 해당한다.

## 5. 응답 봉투 — 쓰지 않는다

성공 응답은 DTO를 그대로 반환한다. 공통 봉투로 감싸지 않는다.

```java
// O
public ResponseEntity<NoticeResponse> create(...) { ... }
public NoticeResponse update(...) { ... }

// X
public ApiResponse<NoticeResponse> create(...) { ... }
```

- 상태코드는 `ResponseEntity` 로 표현한다. 생성은 `ResponseEntity.created(location)`,
  단순 조회·수정은 DTO를 그대로 반환한다
- 에러 응답만 `GlobalExceptionHandler` 가 `ErrorResponse` 로 감싼다. 이건 예외 경로라
  형태가 달라도 된다
- 목록 응답은 `PageResponse<T>` 를 쓴다. 이건 봉투가 아니라 페이지네이션 메타를 담는
  자료구조다

사용처가 없던 `ApiResponse` 는 삭제했다.

---

## 협업 규칙

여러 기능이 함께 쓰는 공용 DTO·enum(`FileAssetResponse`, `PageResponse`, `common.type.*` 등)은
작업 시작 전에 누가 만들지 정한다.
