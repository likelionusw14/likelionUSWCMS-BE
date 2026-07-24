-- likelion USW CMS 스키마
--
-- 엔티티 정의로부터 Hibernate 가 생성한 DDL 을 추출한 것이다.
-- 손으로 고치지 말고, 엔티티를 바꾼 뒤 아래 절차로 다시 뽑는다.
--
--   1. docker run -d --name cms-schema-gen -e MYSQL_ROOT_PASSWORD=<pw> \
--        -e MYSQL_DATABASE=cms -p 127.0.0.1:3307:3306 mysql:8.0 \
--        --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
--   2. SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/cms?...' \
--        SPRING_JPA_HIBERNATE_DDL_AUTO=create ./gradlew bootRun
--   3. mysqldump -h 127.0.0.1 -P 3307 -u root -p<pw> --no-data --skip-comments \
--        --skip-set-charset --compact cms | sed -e 's/ AUTO_INCREMENT=[0-9]*//g' -e '/^\/\*!/d'
--
-- 적용 방법은 README.md 의 "로컬 실행" 절을 참고한다. 애플리케이션이 자동으로
-- 실행하지 않으므로 최초 1회 수동으로 적용해야 한다.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `Account` (
  `cohortId` int DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `part` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `providerId` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accountStatus` enum('ACTIVE','PENDING') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `ActivityCertificate` (
  `activityEndedAt` date DEFAULT NULL,
  `activityStartedAt` date DEFAULT NULL,
  `certificateId` bigint NOT NULL AUTO_INCREMENT,
  `cohortId` bigint NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `fileAssetId` bigint DEFAULT NULL,
  `issuedAt` datetime(6) DEFAULT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `userId` bigint NOT NULL,
  `studentIdSnapshot` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nameSnapshot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `departmentSnapshot` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `failureReason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issueStatus` enum('FAILED','ISSUED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `partSnapshot` enum('BACKEND','COMMON','DESIGN','FRONTEND','PLANNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`certificateId`),
  KEY `idx_certificate_user_id` (`userId`),
  KEY `idx_certificate_cohort_id` (`cohortId`),
  KEY `idx_certificate_file_asset` (`fileAssetId`),
  KEY `idx_certificate_issue_status` (`issueStatus`),
  KEY `idx_certificate_user_created` (`userId`,`createdAt`),
  CONSTRAINT `FK4836qxa2yoma0sdpu6h8bse5l` FOREIGN KEY (`userId`) REFERENCES `AppUser` (`userId`),
  CONSTRAINT `FK8637vm01hsr7slh0913thsg28` FOREIGN KEY (`fileAssetId`) REFERENCES `FileAsset` (`fileAssetId`),
  CONSTRAINT `FKmkli9o80jcqaij83yai0w571t` FOREIGN KEY (`cohortId`) REFERENCES `Cohort` (`cohortId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `AdminAuditLog` (
  `actorUserId` bigint NOT NULL,
  `auditLogId` bigint NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requestId` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resourceId` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `resourceType` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `afterData` json DEFAULT NULL,
  `beforeData` json DEFAULT NULL,
  PRIMARY KEY (`auditLogId`),
  KEY `idx_audit_actor` (`actorUserId`),
  KEY `idx_audit_resource` (`resourceType`,`resourceId`),
  KEY `idx_audit_created_at` (`createdAt`),
  KEY `idx_audit_request_id` (`requestId`),
  CONSTRAINT `FKacshwyfiikgvgx09vept9vo2v` FOREIGN KEY (`actorUserId`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `AppUser` (
  `version` int NOT NULL,
  `approvedAt` datetime(6) DEFAULT NULL,
  `approvedBy` bigint DEFAULT NULL,
  `cohortId` bigint NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `deletedAt` datetime(6) DEFAULT NULL,
  `rejectedAt` datetime(6) DEFAULT NULL,
  `rejectedBy` bigint DEFAULT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `userId` bigint NOT NULL AUTO_INCREMENT,
  `studentId` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rejectionReason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `kakaoSubject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accountStatus` enum('ACTIVE','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `part` enum('BACKEND','COMMON','DESIGN','FRONTEND','PLANNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `systemRole` enum('ADMIN','MEMBER') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `UK6pa8ktdyhc68wk7hxw5yq5xua` (`studentId`),
  UNIQUE KEY `UKmcgnr9pri3m94r8to4iyk76ka` (`kakaoSubject`),
  KEY `idx_app_user_cohort_id` (`cohortId`),
  KEY `idx_app_user_approved_by` (`approvedBy`),
  KEY `idx_app_user_rejected_by` (`rejectedBy`),
  KEY `idx_app_user_account_status` (`accountStatus`),
  KEY `idx_app_user_system_role` (`systemRole`),
  KEY `idx_app_user_cohort_part` (`cohortId`,`part`),
  KEY `idx_app_user_status_created` (`accountStatus`,`createdAt`),
  CONSTRAINT `FK29luogvamot1jrs1qnqfryw4m` FOREIGN KEY (`approvedBy`) REFERENCES `AppUser` (`userId`),
  CONSTRAINT `FK5xhsntvij4xkm69qw10c1a3yl` FOREIGN KEY (`cohortId`) REFERENCES `Cohort` (`cohortId`),
  CONSTRAINT `FKcue4jansa5816v27gknehext8` FOREIGN KEY (`rejectedBy`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `Attendance` (
  `version` int NOT NULL,
  `attendanceId` bigint NOT NULL AUTO_INCREMENT,
  `checkedAt` datetime(6) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `scheduleId` bigint NOT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `updatedBy` bigint DEFAULT NULL,
  `userId` bigint NOT NULL,
  `memo` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `checkInSource` enum('ADMIN','SELF_CODE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ABSENT','LATE','NOT_CHECKED','PRESENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`attendanceId`),
  UNIQUE KEY `uk_attendance_user_schedule` (`userId`,`scheduleId`),
  KEY `idx_attendance_schedule_id` (`scheduleId`),
  KEY `idx_attendance_updated_by` (`updatedBy`),
  KEY `idx_attendance_schedule_status` (`scheduleId`,`status`),
  KEY `idx_attendance_user_created` (`userId`,`createdAt`),
  CONSTRAINT `FK3b4xhhhp74x81bcdf4jdhw8vx` FOREIGN KEY (`updatedBy`) REFERENCES `AppUser` (`userId`),
  CONSTRAINT `FKmnv8qsmx3gpyp4o126jhbeqck` FOREIGN KEY (`userId`) REFERENCES `AppUser` (`userId`),
  CONSTRAINT `FKo9ykykvtftnv9tq4oyjuvs0x8` FOREIGN KEY (`scheduleId`) REFERENCES `Schedule` (`scheduleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `Cohort` (
  `endedAt` date DEFAULT NULL,
  `number` int NOT NULL,
  `startedAt` date DEFAULT NULL,
  `cohortId` bigint NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','CLOSED','PLANNED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`cohortId`),
  UNIQUE KEY `UKoj0qn9vuvyucrouavrcivaslu` (`number`),
  UNIQUE KEY `UK5b9dqj3c4ieaox8luy6scqepa` (`name`),
  KEY `idx_cohort_status` (`status`),
  KEY `idx_cohort_status_number` (`status`,`number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `FileAsset` (
  `createdAt` datetime(6) NOT NULL,
  `deletedAt` datetime(6) DEFAULT NULL,
  `fileAssetId` bigint NOT NULL AUTO_INCREMENT,
  `sizeBytes` bigint NOT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `uploadedBy` bigint NOT NULL,
  `checksumSha256` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mimeType` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `objectKey` varchar(768) COLLATE utf8mb4_unicode_ci NOT NULL,
  `originalFileName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purpose` enum('CERTIFICATE','LEARNING_RESOURCE','NOTICE_IMAGE','PROJECT_THUMBNAIL') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`fileAssetId`),
  UNIQUE KEY `UKsqo60soxfvhy8n349y5i1xlqf` (`objectKey`),
  KEY `idx_file_asset_uploaded_by` (`uploadedBy`),
  KEY `idx_file_asset_purpose` (`purpose`),
  KEY `idx_file_asset_created_at` (`createdAt`),
  CONSTRAINT `FKq55hebbjbysleuuww8obn9krl` FOREIGN KEY (`uploadedBy`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `LearningResource` (
  `version` int NOT NULL,
  `week` int NOT NULL,
  `archivedAt` datetime(6) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `createdBy` bigint NOT NULL,
  `fileAssetId` bigint NOT NULL,
  `resourceId` bigint NOT NULL AUTO_INCREMENT,
  `updatedAt` datetime(6) NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `targetPart` enum('BACKEND','COMMON','DESIGN','FRONTEND','PLANNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`resourceId`),
  KEY `idx_resource_file_asset` (`fileAssetId`),
  KEY `idx_resource_created_by` (`createdBy`),
  KEY `idx_resource_created_at` (`createdAt`),
  KEY `idx_resource_week` (`week`),
  KEY `idx_resource_target_part` (`targetPart`),
  KEY `idx_resource_week_part_created` (`week`,`targetPart`,`createdAt`),
  CONSTRAINT `FK5ub9twff28qvcbjrylek0wum4` FOREIGN KEY (`fileAssetId`) REFERENCES `FileAsset` (`fileAssetId`),
  CONSTRAINT `FKimpli3aaniqlncvsb733u6neu` FOREIGN KEY (`createdBy`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `Notice` (
  `isFixed` bit(1) NOT NULL,
  `version` int NOT NULL,
  `archivedAt` datetime(6) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `createdBy` bigint NOT NULL,
  `imageAssetId` bigint DEFAULT NULL,
  `noticeId` bigint NOT NULL AUTO_INCREMENT,
  `publishedAt` datetime(6) NOT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `externalUrl` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag` enum('OTHER','PROJECT','PROMOTION_EVENT','SCHEDULE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`noticeId`),
  KEY `idx_notice_image_asset` (`imageAssetId`),
  KEY `idx_notice_created_by` (`createdBy`),
  KEY `idx_notice_tag` (`tag`),
  KEY `idx_notice_published_at` (`publishedAt`),
  KEY `idx_notice_fixed_published` (`isFixed`,`publishedAt`,`noticeId`),
  CONSTRAINT `FKl1rrjc9m0d351aeoymjqduvec` FOREIGN KEY (`createdBy`) REFERENCES `AppUser` (`userId`),
  CONSTRAINT `FKlp15nhhbymp1587r1o1as35tg` FOREIGN KEY (`imageAssetId`) REFERENCES `FileAsset` (`fileAssetId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `Project` (
  `endedMonth` date NOT NULL,
  `startedMonth` date NOT NULL,
  `version` int NOT NULL,
  `cohortId` bigint NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `createdBy` bigint NOT NULL,
  `deletedAt` datetime(6) DEFAULT NULL,
  `projectId` bigint NOT NULL AUTO_INCREMENT,
  `thumbnailAssetId` bigint DEFAULT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `projectType` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `deployUrl` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `githubUrl` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`projectId`),
  KEY `idx_project_thumbnail` (`thumbnailAssetId`),
  KEY `idx_project_cohort_id` (`cohortId`),
  KEY `idx_project_created_by` (`createdBy`),
  KEY `idx_project_type` (`projectType`),
  KEY `idx_project_created_at` (`createdAt`),
  KEY `idx_project_cohort_created` (`cohortId`,`createdAt`),
  CONSTRAINT `FK70hrbyim484i53fgfn69008oo` FOREIGN KEY (`thumbnailAssetId`) REFERENCES `FileAsset` (`fileAssetId`),
  CONSTRAINT `FK85e065eqe9i8exemlpj83geex` FOREIGN KEY (`cohortId`) REFERENCES `Cohort` (`cohortId`),
  CONSTRAINT `FKtgukcu668pjo82bdj8amnno4f` FOREIGN KEY (`createdBy`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `ProjectParticipation` (
  `createdAt` datetime(6) NOT NULL,
  `participationId` bigint NOT NULL AUTO_INCREMENT,
  `projectId` bigint NOT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `userId` bigint NOT NULL,
  `role` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`participationId`),
  UNIQUE KEY `uk_participation_user_project` (`userId`,`projectId`),
  KEY `idx_participation_project_id` (`projectId`),
  CONSTRAINT `FKjuyqta1a0vw0hp6oqhuvrd6d1` FOREIGN KEY (`projectId`) REFERENCES `Project` (`projectId`),
  CONSTRAINT `FKldu7dgql0jc3bix1j7l8lnxm3` FOREIGN KEY (`userId`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `ProjectTag` (
  `createdAt` datetime(6) NOT NULL,
  `projectId` bigint NOT NULL,
  `tagId` bigint NOT NULL,
  PRIMARY KEY (`projectId`,`tagId`),
  KEY `idx_project_tag_tag_id` (`tagId`),
  CONSTRAINT `FK1v72javxqo69xrh08gsyasaf1` FOREIGN KEY (`tagId`) REFERENCES `Tag` (`tagId`),
  CONSTRAINT `FKhk7g8q27ecpjqeq5fbu7xqkg1` FOREIGN KEY (`projectId`) REFERENCES `Project` (`projectId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `Schedule` (
  `isAllDay` bit(1) NOT NULL,
  `scheduleDate` date NOT NULL,
  `startTime` time(6) DEFAULT NULL,
  `version` int NOT NULL,
  `cohortId` bigint NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `createdBy` bigint NOT NULL,
  `deletedAt` datetime(6) DEFAULT NULL,
  `scheduleId` bigint NOT NULL AUTO_INCREMENT,
  `updatedAt` datetime(6) NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`scheduleId`),
  KEY `idx_schedule_cohort_id` (`cohortId`),
  KEY `idx_schedule_created_by` (`createdBy`),
  KEY `idx_schedule_date` (`scheduleDate`),
  KEY `idx_schedule_cohort_date` (`cohortId`,`scheduleDate`),
  KEY `idx_schedule_date_sort` (`scheduleDate`,`isAllDay`,`startTime`,`title`),
  CONSTRAINT `FK237j1v8x3tqfewdwu5tehhxgj` FOREIGN KEY (`cohortId`) REFERENCES `Cohort` (`cohortId`),
  CONSTRAINT `FK4iygtkvlxi7kgyu6e90dhoe6a` FOREIGN KEY (`createdBy`) REFERENCES `AppUser` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `Tag` (
  `createdAt` datetime(6) NOT NULL,
  `tagId` bigint NOT NULL AUTO_INCREMENT,
  `updatedAt` datetime(6) NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scope` enum('PROJECT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`tagId`),
  UNIQUE KEY `uk_tag_scope_name` (`scope`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
