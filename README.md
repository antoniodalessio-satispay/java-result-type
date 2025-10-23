# Java Result Type

A lightweight Java library for functional error handling using the Result and LazyResult pattern. This library provides a type-safe way to handle operations that may succeed or fail, eliminating the need for exceptions in control flow.

## Features

- **Type-safe error handling**: Explicit success and failure types
- **Lazy evaluation**: Defer computation until needed with `LazyResult`
- **Functional composition**: Chain operations with `map` and `mapError`
- **Java 1.8+ compatible**: Works with lambda expressions and method references
- **Zero dependencies**: Lightweight with no external dependencies

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.satispay</groupId>
    <artifactId>java-result-type</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Core Concepts

### Result<T, E>

`Result` represents the outcome of an operation that can either succeed with a value of type `T` or fail with an error of type `E`.

**Methods:**
- `Result.success(T data)` - Creates a successful result
- `Result.failure(E error)` - Creates a failed result
- `boolean isSuccess()` - Checks if the result is successful
- `T getData()` - Gets the success value (null if failure)
- `E getError()` - Gets the error value (null if success)

### LazyResult<T, E>

`LazyResult` represents a deferred computation that will produce a `Result<T, E>` when evaluated. It allows you to compose operations before execution.

**Methods:**
- `LazyResult.create(supplier, errorMapper)` - Creates a lazy result from a supplier
- `evaluate()` - Executes the computation and returns a `Result`
- `LazyResult.map(lazyResult, mapper)` - Transforms the success value
- `LazyResult.map(lazyResult, mapper, errorMapper)` - Transforms both success and error values
- `LazyResult.mapError(lazyResult, errorMapper)` - Transforms only the error value

## Usage Examples

### Basic Result Usage

```java
// Creating a successful result
Result<Integer, String> success = Result.success(42);
System.out.println(success.isSuccess()); // true
System.out.println(success.getData()); // 42

// Creating a failed result
Result<Integer, String> failure = Result.failure("Operation failed");
System.out.println(failure.isSuccess()); // false
System.out.println(failure.getError()); // "Operation failed"
```

### Creating a LazyResult

```java
// Define a lazy computation that might fail
LazyResult<Integer, String> lazyResult = LazyResult.create(
    () -> {
        // Some computation that might throw an exception
        return 10 / 2;
    },
    ex -> "Error: " + ex.getMessage()
);

// Execute the computation
Result<Integer, String> result = lazyResult.evaluate();
if (result.isSuccess()) {
    System.out.println("Result: " + result.getData());
}
```

### Handling Exceptions

```java
LazyResult<String, String> lazyResult = LazyResult.create(
    () -> {
        // This will throw an exception
        return "test".substring(10);
    },
    ex -> "Failed with: " + ex.getClass().getSimpleName()
);

Result<String, String> result = lazyResult.evaluate();
// result.isSuccess() == false
// result.getError() == "Failed with: StringIndexOutOfBoundsException"
```

### Mapping Values

Transform success values while preserving the error type:

```java
LazyResult<Integer, String> lazyResult = LazyResult.create(
    () -> 5,
    ex -> "Error: " + ex.getMessage()
);

// Transform the success value
LazyResult<String, String> mapped = LazyResult.map(
    lazyResult,
    i -> "Number: " + i
);

Result<String, String> result = mapped.evaluate();
// result.getData() == "Number: 5"
```

### Chaining Multiple Transformations

```java
LazyResult<Integer, String> lazyResult = LazyResult.create(
    () -> 10,
    ex -> "Error: " + ex.getMessage()
);

// Chain multiple transformations
LazyResult<String, String> pipeline = LazyResult.map(
    LazyResult.map(
        LazyResult.map(lazyResult, i -> i * 2),  // 10 * 2 = 20
        i -> i + 3                                // 20 + 3 = 23
    ),
    i -> "Result: " + i                           // "Result: 23"
);

Result<String, String> result = pipeline.evaluate();
// result.getData() == "Result: 23"
```

### Transforming Error Types

Transform both success and error types simultaneously:

```java
LazyResult<Integer, String> lazyResult = LazyResult.create(
    () -> {
        throw new RuntimeException("Something went wrong");
    },
    ex -> "Error: " + ex.getMessage()
);

// Transform both value and error types
LazyResult<String, Integer> transformed = LazyResult.map(
    lazyResult,
    i -> "Value: " + i,           // Transform success: Integer -> String
    error -> error.length()       // Transform error: String -> Integer
);

Result<String, Integer> result = transformed.evaluate();
// result.isSuccess() == false
// result.getError() == 27 (length of "Error: Something went wrong")
```

### Mapping Only Errors

Transform error types while keeping the success type unchanged:

```java
LazyResult<Integer, String> lazyResult = LazyResult.create(
    () -> {
        throw new RuntimeException("Failed");
    },
    ex -> ex.getMessage()
);

// Map only the error type
LazyResult<Integer, ErrorCode> mapped = LazyResult.mapError(
    lazyResult,
    errorMsg -> new ErrorCode(500, errorMsg)
);

Result<Integer, ErrorCode> result = mapped.evaluate();
// result.getError().getCode() == 500
```

### Real-World Example: Database Operations

```java
public class UserService {

    public LazyResult<User, String> findUserById(int userId) {
        return LazyResult.create(
            () -> {
                // Simulate database query
                if (userId <= 0) {
                    throw new IllegalArgumentException("Invalid user ID");
                }
                return database.query("SELECT * FROM users WHERE id = ?", userId);
            },
            ex -> "Database error: " + ex.getMessage()
        );
    }

    public LazyResult<String, String> getUserEmail(int userId) {
        LazyResult<User, String> userResult = findUserById(userId);

        // Transform User to email String
        return LazyResult.map(userResult, user -> user.getEmail());
    }

    public Result<String, String> processUser(int userId) {
        LazyResult<String, String> emailResult = getUserEmail(userId);

        // Only evaluate when needed
        Result<String, String> result = emailResult.evaluate();

        if (result.isSuccess()) {
            String email = result.getData();
            // Send email...
            return Result.success("Email sent to " + email);
        } else {
            return Result.failure(result.getError());
        }
    }
}
```

### Real-World Example: API Call Pipeline

```java
public class ApiService {

    public LazyResult<JsonResponse, ApiError> fetchData(String endpoint) {
        return LazyResult.create(
            () -> httpClient.get(endpoint),
            ex -> new ApiError(500, "Network error", ex)
        );
    }

    public LazyResult<List<User>, ApiError> getUsers() {
        return LazyResult.map(
            fetchData("/api/users"),
            response -> parseUsers(response.getBody())
        );
    }

    public LazyResult<List<String>, String> getUserNames() {
        return LazyResult.map(
            getUsers(),
            users -> users.stream()
                         .map(User::getName)
                         .collect(Collectors.toList()),
            apiError -> "Failed to fetch users: " + apiError.getMessage()
        );
    }

    public void displayUsers() {
        Result<List<String>, String> result = getUserNames().evaluate();

        if (result.isSuccess()) {
            result.getData().forEach(System.out::println);
        } else {
            System.err.println(result.getError());
        }
    }
}
```

## Benefits

1. **Explicit error handling**: Errors are part of the type signature, making it clear which operations can fail
2. **No exception-driven control flow**: Avoid try-catch blocks for expected failures
3. **Composable**: Chain operations without nested error handling
4. **Lazy evaluation**: Build complex pipelines that only execute when needed
5. **Type safety**: The compiler ensures you handle both success and failure cases

## Best Practices

1. **Use LazyResult for deferred operations**: When building pipelines or when execution should be deferred
2. **Use Result for immediate values**: When you already have a success or failure value
3. **Keep error types informative**: Use descriptive error types that provide context
4. **Evaluate at boundaries**: Evaluate LazyResult at the edges of your application (controllers, main methods)
5. **Transform errors early**: Use mapError to convert low-level exceptions to domain errors

## License

This project is part of the Satispay engineering toolkit.

## Contributing

Contributions are welcome! Please ensure all tests pass before submitting a pull request.

```bash
mvn clean test
```
