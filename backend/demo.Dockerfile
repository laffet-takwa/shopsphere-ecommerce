# Build context is backend/ because demo-service compiles the other services' sources directly.
FROM maven:3.9.9-eclipse-temurin-17 AS build

# The directory layout below is load-bearing, not cosmetic. demo-service's pom references its
# siblings as ../<service>/src/main/java, and Maven resolves those against the project basedir.
# Building with the pom at the workspace root instead made every source root miss, and the image
# shipped a boot jar with an empty BOOT-INF/classes that failed at runtime with a
# ClassNotFoundException. Anchoring WORKDIR to the module and copying siblings to absolute paths
# removes the ambiguity.
WORKDIR /workspace/demo-service

COPY demo-service/pom.xml ./pom.xml
# Only the service source trees are needed. Their resources are intentionally not copied so the
# per-service application.yml files never enter the image.
COPY auth-service/src /workspace/auth-service/src
COPY product-service/src /workspace/product-service/src
COPY order-service/src /workspace/order-service/src
COPY inventory-service/src /workspace/inventory-service/src
COPY payment-service/src /workspace/payment-service/src
COPY notification-service/src /workspace/notification-service/src

COPY demo-service/src ./src
# dependency:go-offline is deliberately not used. It resolves plugin internals such as maven-core
# that the build never needs, and has been observed failing on a transient network timeout, which
# would fail the whole image build for no benefit.
RUN mvn -B package -DskipTests
# Fail loudly rather than shipping a jar that cannot start.
RUN test -f target/classes/com/shopsphere/demo/DemoApplication.class \
 && test -f target/classes/com/shopsphere/product/Product.class \
 && test -f target/classes/com/shopsphere/order/OrderService.class

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/demo-service/target/demo-service.jar app.jar
# Free instances are memory constrained, so cap the heap rather than letting the JVM guess.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]