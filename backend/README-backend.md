Backend setup notes

1) Database properties (application.yml or env variables):

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flowgate
    username: flowgate
    password: flowgatepass
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

2) Flyway:

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

3) Run locally:

- mvn -f backend/pom.xml clean package
- mvn -f backend spring-boot:run

4) Notes:
- A starter Spring Boot project was generated into this `backend/` folder with Web, Data JPA, Validation, Security, PostgreSQL Driver, Flyway, Lombok, and Actuator dependencies.
- Ensure Java 21 is installed and MAVEN_HOME / PATH configured before building.
