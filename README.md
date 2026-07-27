# likelionUSWCMS-BE
[Web, App] 멋쟁이사자처럼 수원대학교 | Club Management System | Back-End 레포지토리입니다.

## 요구 사항

- JDK 21
- Docker (로컬 MySQL 실행용)

## 환경 변수

| 변수 | 설명 |
| --- | --- |
| `DB_PASSWORD` | MySQL root 비밀번호 |
| `JWT_SECRET` | JWT 서명 키. HS256 을 쓰므로 32자 이상 문자열 |
| `S3_BUCKET_NAME` | Private S3 파일 버킷명 |
| `AWS_REGION` | S3 버킷 리전. 기본값 `ap-northeast-2` |

## 로컬 실행

### 1. MySQL 기동

```bash
DB_PASSWORD=<원하는_비밀번호> docker compose up -d mysql-db
```

3306 포트가 이미 사용 중이면 로컬에 다른 MySQL 이 떠 있는 것이다. 기존 인스턴스를
내리거나, `docker-compose.yml` 의 포트 매핑을 바꿔 쓴다.

### 2. 스키마 생성

`ddl-auto` 가 `validate` 라서 테이블이 없으면 애플리케이션이 기동되지 않는다.
최초 1회 스키마를 적용한다.

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p cms < db/schema.sql
```

### 3. 애플리케이션 실행

```bash
DB_PASSWORD=<위와_동일한_비밀번호> \
JWT_SECRET=<32자_이상_문자열> \
S3_BUCKET_NAME=likelion-usw-cms-files \
AWS_REGION=ap-northeast-2 \
./gradlew bootRun
```

`JWT_SECRET` 은 기본값이 없어서 지정하지 않으면 기동이 실패한다.
로컬에서 실제 S3 API를 호출하려면 AWS CLI 프로필 등 AWS SDK 기본 자격증명 체인에
사용 가능한 자격증명이 있어야 한다. 배포 환경에서는 EC2 IAM Role을 사용한다.
`http://localhost:8080/health` 가 `OK` 를 반환하면 정상이다.

## 테스트

```bash
./gradlew test          # 단위 테스트만 (DB 불필요)
./gradlew clean build   # 컴파일 + 전체 테스트
```

테스트는 `@WebMvcTest` 슬라이스와 Mockito 단위 테스트로만 구성되어 있어 DB 없이 돌아간다.

## 스키마 변경

`db/schema.sql` 은 엔티티로부터 Hibernate 가 생성한 DDL 을 추출한 것이다.
**직접 수정하지 않는다.** 엔티티를 바꿨다면 파일 상단 주석의 절차대로 다시 추출한다.

이 파일은 애플리케이션이 자동으로 적용하지 않는다. 위 2번처럼 수동으로 실행해야 한다.
`src/main/resources` 가 아니라 `db/` 에 둔 것도 그 때문이다. Spring Boot 는
`resources/schema.sql` 을 `spring.sql.init.mode` 설정에 따라 자동 실행하는데,
MySQL 은 내장 DB 가 아니라 기본값(`embedded`)에서는 실행되지 않는다.
같은 이름을 클래스패스에 두면 "자동으로 적용되겠거니" 하는 오해를 부르고, 나중에
누군가 `spring.sql.init.mode=always` 를 켜면 기동할 때마다 `CREATE TABLE` 이 재실행되어
깨진다.

## 문서

- [응답 DTO 컨벤션](docs/CONVENTIONS.md)
