FROM openjdk:8-alpine

COPY target/uberjar/kms2-clj.jar /kms2-clj/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/kms2-clj/app.jar"]
