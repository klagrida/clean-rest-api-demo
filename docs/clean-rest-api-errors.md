# Designing REST Endpoints and Handling Errors the Clean Way

*A resource-first approach to routes and status codes, and a service-layer exception pattern that keeps controllers dumb and error responses consistent — with examples in Spring Boot 4.*

Most REST APIs don't fall apart because of a missing framework feature. They fall apart because routes drift into verb-shaped URLs, status codes get chosen by habit instead of meaning, and error handling gets scattered across every controller as one-off try/catch blocks. Fix those two things — endpoint shape and error flow — and everything downstream gets easier: testing, documentation, client integration, debugging at 2am.

## Part one — shaping the endpoints

### Resources, not actions

A URL names a thing, not a verb. The HTTP method already carries the action, so `/getProducts` or `/createProduct` is redundant at best and confusing at worst. Stick to plural nouns and let the method do its job:

```
GET    /api/products          // list
GET    /api/products/{id}     // get one
POST   /api/products          // create
PUT    /api/products/{id}     // full update
PATCH  /api/products/{id}     // partial update
DELETE /api/products/{id}     // delete
```

Relationships nest naturally under the parent resource:

```
GET  /api/products/{id}/reviews
POST /api/products/{id}/reviews
```

### Status codes that carry meaning

A consumer of your API should be able to branch on the status code alone, without parsing the body. That only works if the codes are used consistently.

| Action | Success code |
|---|---|
| GET | `200 OK` |
| POST (create) | `201 Created` + `Location` header |
| PUT / PATCH | `200` (or `204` with no body) |
| DELETE | `204 No Content` |

```java
@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
    ProductResponse created = service.create(req);
    URI location = URI.create("/api/products/" + created.id());
    return ResponseEntity.created(location).body(created);
}
```

### Filtering, sorting, and pagination as query params

Keep the resource path stable and push everything optional into the query string. It keeps the route cacheable and predictable.

```
GET /api/products?category=shoes&sort=price,desc&page=0&size=20
```

```java
@GetMapping
public Page<ProductResponse> getAll(
        @RequestParam Optional<String> category,
        Pageable pageable) {
    return service.findAll(category, pageable);
}
```

### Versioning

Spring Boot 4 adds native API versioning support, so you no longer have to hand-roll it through headers or URL prefixes:

```java
@GetMapping(value = "/products", version = "1")
```

If you'd rather encode the version in the path (`/api/v1/products`), that still works fine — just pick one convention and apply it project-wide rather than mixing strategies.

> The route tells you what the resource is. The method tells you what's happening to it. The status code tells you how it went. If any of those three is doing someone else's job, the API gets harder to reason about.

## Part two — errors, thrown from the service layer

The cleanest pattern splits error handling into three layers that never overlap: the **service** throws domain-specific exceptions and knows nothing about HTTP; the **controller** calls the service and does no error handling at all; a single **global handler** maps each exception type to a status code and response body, once, for the whole application.

### A small, HTTP-agnostic exception hierarchy

These live in their own package and describe business outcomes, not transport details:

```java
// Base for all "expected" business errors
public abstract class ApiException extends RuntimeException {
    public ApiException(String message) { super(message); }
}

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " with id " + id + " not found");
    }
}

public class DuplicateResourceException extends ApiException {
    public DuplicateResourceException(String message) { super(message); }
}

public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) { super(message); }
}
```

### The service just throws

```java
@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse findById(Long id) {
        Product p = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return toResponse(p);
    }

    public ProductResponse create(ProductRequest req) {
        if (repository.existsByName(req.name())) {
            throw new DuplicateResourceException(
                "A product named '" + req.name() + "' already exists");
        }
        Product p = new Product();
        p.setName(req.name());
        p.setPrice(req.price());
        return toResponse(repository.save(p));
    }

    public void delete(Long id) {
        Product p = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (p.isLockedForOrders()) {
            throw new BusinessRuleException("Cannot delete a product with active orders");
        }
        repository.delete(p);
    }
}
```

### The controller does not know errors exist

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping("/{id}")
    public ProductResponse getOne(@PathVariable Long id) {
        return service.findById(id); // exception propagates up, nothing caught here
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

### One place maps exceptions to HTTP

Spring Boot 4 defaults to [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) for error bodies, so every error — regardless of which exception triggered it — comes back in the same predictable shape.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // log ex here — never expose ex.getMessage() to the client
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}
```

Laid out flat, the mapping this handler owns looks like this:

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | `404 Not Found` |
| `DuplicateResourceException` | `409 Conflict` |
| `BusinessRuleException` | `422 Unprocessable Entity` |
| `MethodArgumentNotValidException` | `400 Bad Request` |

### Why the split holds up

- **Single responsibility.** The service owns business logic and domain exceptions. The controller owns HTTP wiring. The advice owns exception-to-status mapping. None of them reach into the others' job.
- **Testable in isolation.** A service unit test just asserts that `ResourceNotFoundException` is thrown — no web layer, no mock HTTP request needs to be spun up.
- **Every endpoint, same shape.** Throw `DuplicateResourceException` from any service, anywhere in the app, and it comes back as the same `409` with the same Problem Details body. The mapping is written once.
- **Cheap to extend.** A new business rule means a new exception subclass and one new handler method. Nothing else in the codebase changes.

One refinement worth adopting once the hierarchy grows: give `ApiException` an abstract `getStatus()` method that each subclass implements, then write a single generic handler for the whole hierarchy instead of one method per subclass. It trades a little explicitness for a lot less boilerplate once you're past five or six exception types.

---

*Get the routes to name resources honestly and the errors to flow through one funnel, and the rest of the API — docs, client SDKs, tests, on-call debugging — gets to be boring in the good way.*
