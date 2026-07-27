FROM eclipse-temurin:21-jre
WORKDIR /app

# 컨테이너 기본 타임존을 KST로 고정한다. 미설정 시 UTC로 동작해
# @CreatedDate/@LastModifiedDate가 채우는 시각이 실제 한국 시각보다 9시간 어긋난다.
ENV TZ=Asia/Seoul

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --chown=appuser:appgroup build/libs/*.jar app.jar

USER appuser

EXPOSE 8080
# -Duser.timezone으로 JVM 타임존도 명시해, TZ 환경변수 처리와 무관하게 KST를 보장한다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
