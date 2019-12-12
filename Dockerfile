FROM java:8

ENV JAR_NAME docker-demo

VOLUME /tmp

COPY target/${JAR_NAME}.jar app.jar

# 设置时区
ENV TZ Asia/Shanghai
RUN lb -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置语言
RUN echo "export LC_ALL=zh_CN.UTF-8" >> /etc/profile
ENV LANG "zh_CN.UTF-8"

# 暴露的端口
EXPOSE 9000

# 容器启动执行的命令
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]