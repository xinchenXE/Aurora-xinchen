# Java 25 运行环境（配置走 Nacos，jar 内置 bootstrap.properties）
FROM eclipse-temurin:25-jre
RUN rm -f /etc/localtime \
&& ln -sv /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
&& echo "Asia/Shanghai" > /etc/timezone
COPY target/aurora-xinchen-0.0.1.jar /app/aurora-xinchen-0.0.1.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/aurora-xinchen-0.0.1.jar"]
EXPOSE 8080
