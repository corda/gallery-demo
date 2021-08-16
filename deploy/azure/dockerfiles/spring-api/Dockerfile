FROM openjdk:8-alpine
RUN mkdir -p /opt/gateway
COPY *.jar /opt/gateway/api.jar
COPY /resources/shell/*.sh /opt/gateway
WORKDIR /opt/gateway
RUN dos2unix run-api.sh && chmod +x run-api.sh
ENTRYPOINT ["./run-api.sh"]