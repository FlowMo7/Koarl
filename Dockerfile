FROM anapsix/alpine-java

COPY ./koarl-backend/build/libs/KoarlBackend.jar /opt/koarl/KoarlBackend.jar

HEALTHCHECK --interval=5m --timeout=3s CMD java -jar /opt/koarl/KoarlBackend.jar healthcheck

CMD ["java", "-jar", "/opt/koarl/KoarlBackend.jar"]
