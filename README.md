# Clean REST API Demo

Spring Boot 4 project implementing the endpoint-design and service-layer
exception-handling pattern discussed earlier: resource-based routes, correct
status codes, and a `ResourceNotFoundException` / `DuplicateResourceException`
/ `BusinessRuleException` hierarchy mapped to RFC 7807 Problem Details by a
single `@RestControllerAdvice`.

## Requirements

- Java 17+
- Maven 3.9+

## Run it

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080` with an in-memory H2 database,
seeded with 3 products (one of them locked, to demo the 422 case).

## Swagger UI

Interactive docs, try requests straight from the browser:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI spec:

```
http://localhost:8080/v3/api-docs
```

## H2 console (optional)

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:productdb
User: sa / Password: (empty)
```

## Try the endpoints with curl

**List (200)**
```bash
curl http://localhost:8080/api/products
```

**Get one (200)**
```bash
curl http://localhost:8080/api/products/1
```

**Get a missing one → 404 Problem Details**
```bash
curl -i http://localhost:8080/api/products/999
```

**Create (201 + Location header)**
```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Webcam","price":39.90}'
```

**Create a duplicate → 409**
```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Webcam","price":39.90}'
```

**Invalid payload → 400 with field errors**
```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"","price":-5}'
```

**Delete a locked product → 422**
```bash
curl -i -X DELETE http://localhost:8080/api/products/3
```

**Delete a real one → 204**
```bash
curl -i -X DELETE http://localhost:8080/api/products/1
```

## Project layout

```
src/main/java/com/example/demo/
├── controller/   → ProductController (thin, no error handling)
├── service/      → ProductService (business logic, throws domain exceptions)
├── repository/   → ProductRepository (Spring Data JPA)
├── model/        → Product (JPA entity)
├── dto/          → ProductRequest / ProductResponse
├── exception/    → ApiException hierarchy + GlobalExceptionHandler
└── config/       → OpenApiConfig, DataSeeder
```
