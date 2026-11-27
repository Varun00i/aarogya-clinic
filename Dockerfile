FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM tomcat:9.0-jdk17-temurin

ENV JAVA_OPTS="-Duser.home=/var/lib/aarogya"
WORKDIR /usr/local/tomcat

COPY --from=build /workspace/target/aarogya.war /usr/local/tomcat/webapps/ROOT.war
COPY docker/render-entrypoint.sh /usr/local/bin/render-entrypoint.sh
RUN chmod +x /usr/local/bin/render-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/render-entrypoint.sh"]
