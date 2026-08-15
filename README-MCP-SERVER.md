Spring AI 2.0.0 MCP becomes another adapter/facade over that service.

So you don't need to rewrite your REST controllers or repositories.

```text
                    ┌─────────────────────┐
                    │     AI Client       │
                    │ Claude / ChatGPT /  │
                    │ your MCP client     │
                    └──────────┬──────────┘
                               │ MCP
                               ▼
                    ┌─────────────────────┐
                    │    MCP Server       │
                    │                     │
                    │ @McpTool methods    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Your Service      │
                    │ UserService         │
                    │ OrderService        │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Repository / DB     │
                    └─────────────────────┘
```

Spring AI 2.0.0 has first-class MCP server starters and annotation-based `@McpTool`, `@McpResource`, etc. ([Home][1])

## 1. What MCP actually gives you

Suppose you already have:

```java
@Service
public class CustomerService {

    public Customer getById(Long id) { ... }

    public List<Customer> findAll() { ... }

    public Customer create(CreateCustomerRequest request) { ... }

    public Customer update(Long id, UpdateCustomerRequest request) { ... }

    public void delete(Long id) { ... }
}
```

And perhaps REST:

```text
GET    /customers/{id}
GET    /customers
POST   /customers
PUT    /customers/{id}
DELETE /customers/{id}
```

MCP lets you expose selected operations as **tools**:

```text
get_customer
search_customers
create_customer
update_customer
delete_customer
```

An LLM doesn't directly call your repository or database.

It asks the MCP server:

```json
{
  "name": "get_customer",
  "arguments": {
    "id": 42
  }
}
```

Your MCP tool invokes:

```java
customerService.getById(42)
```

and returns a structured result.

---

# 2. Spring AI 2.0.0 dependency

For an HTTP MCP server, Spring AI 2.0.0 provides the WebMVC starter:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

Then configure: (application.yaml)

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
```

Or application.properties

```properties
spring.ai.mcp.server.protocol=STREAMABLE
```

Spring AI 2.0 allow to customize the defaults MCP endpoint to something else with the conjunction of server.servlet.context-path=/api

```properties
##Spring-Servlet Config
server.servlet.context-path=/api

# Spring.AI MCP server config
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.streamable-http.mcp-endpoint=/edgemcp
```

the effective URL is typically:
```
http://localhost:8080/api/edgemcp
```

The Spring AI documentation confirms that the Streamable HTTP endpoint is configurable and defaults value is set to
```
/mcp
```

Spring AI 2.0 recommends **Streamable HTTP** rather than the older SSE transport for new HTTP servers; SSE is deprecated in 2.0.0. ([Home][1])

If you're using Spring Boot + Spring AI's auto-configuration, you don't have to manually construct the MCP server. The starter does that for you. ([Home][2])

---

# 3. Your first MCP tool

Create a separate class that acts as the MCP adapter.

For example:

```java
@Component
public class CustomerMcpTools {

    private final CustomerService customerService;

    public CustomerMcpTools(CustomerService customerService) {
        this.customerService = customerService;
    }

    @McpTool(
        name = "get_customer",
        description = "Get a customer by their ID"
    )
    public Customer getCustomer(
            @McpToolParam(
                description = "The customer ID",
                required = true
            )
            Long id) {

        return customerService.getById(id);
    }
}
```

That's essentially it.

Spring AI discovers the annotated Spring bean and registers `get_customer` as an MCP tool. The annotation infrastructure automatically generates the tool's parameter schema. ([Home][3])

---

# 4. Expose your CRUD operations

You could make the adapter look like this:

```java
@Component
public class CustomerMcpTools {

    private final CustomerService customerService;

    public CustomerMcpTools(CustomerService customerService) {
        this.customerService = customerService;
    }

    @McpTool(
        name = "get_customer",
        description = "Get a customer by ID"
    )
    public Customer getCustomer(Long id) {
        return customerService.getById(id);
    }

    @McpTool(
        name = "search_customers",
        description = "Search customers by name or email"
    )
    public List<Customer> searchCustomers(String query) {
        return customerService.search(query);
    }

    @McpTool(
        name = "create_customer",
        description = "Create a new customer"
    )
    public Customer createCustomer(
            String name,
            String email) {

        return customerService.create(
            new CreateCustomerRequest(name, email)
        );
    }

    @McpTool(
        name = "update_customer",
        description = "Update an existing customer"
    )
    public Customer updateCustomer(
            Long id,
            String name,
            String email) {

        return customerService.update(
            id,
            new UpdateCustomerRequest(name, email)
        );
    }

    @McpTool(
        name = "delete_customer",
        description = "Delete a customer by ID"
    )
    public void deleteCustomer(Long id) {
        customerService.delete(id);
    }
}
```

The important architecture is:

```text
McpTool
   ↓
CustomerService
   ↓
CustomerRepository
   ↓
Database
```

**Don't do this:**

```java
@McpTool
public Customer getCustomer(Long id) {
    return customerRepository.findById(id).orElseThrow();
}
```

Prefer:

```java
@McpTool
public Customer getCustomer(Long id) {
    return customerService.getById(id);
}
```

That way your business rules remain in one place.

---

# 5. REST and MCP can coexist

This is one of the nice things about this approach.

You can have:

```text
                    CustomerService
                   /              \
                  /                \
        REST Controller          MCP Tools
              │                      │
              ▼                      ▼
       HTTP clients              AI clients
```

For example:

```java
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    @GetMapping("/{id}")
    public Customer get(@PathVariable Long id) {
        return service.getById(id);
    }
}
```

And separately:

```java
@Component
public class CustomerMcpTools {

    private final CustomerService service;

    @McpTool(name = "get_customer")
    public Customer getCustomer(Long id) {
        return service.getById(id);
    }
}
```

Both call the same service.

That's usually the architecture I'd recommend.

---

# 6. But don't expose every CRUD method blindly

This is an important MCP concept.

MCP tools are **capabilities you're giving an AI agent**.

So don't think:

> "I have CRUD endpoints, therefore I'll expose all CRUD endpoints."

Think:

> "What actions should an AI agent be allowed to perform?"

For example:

### Read operations

Usually straightforward:

```text
get_customer
search_customers
get_customer_orders
get_order
search_orders
```

### Mutations

Be more deliberate:

```text
create_customer
update_customer
cancel_order
```

### Dangerous operations

Potentially don't expose directly:

```text
delete_everything
execute_sql
update_customer_credit_limit
refund_payment
change_user_role
```

Or expose them with strong authorization/business validation.

MCP has server-side security considerations, and Spring AI 2.0 provides MCP security documentation as well. ([Home][4])

---

# 7. A better CRUD MCP design

Imagine your application manages orders.

Your REST API might have:

```text
POST   /orders
PUT    /orders/{id}
DELETE /orders/{id}
```

For an AI agent, I wouldn't necessarily expose those names directly.

Instead:

```java
@McpTool(
    name = "create_order",
    description = """
        Create an order for a customer.
        The customer must already exist.
        Do not use this tool to modify existing orders.
        """
)
public Order createOrder(
        Long customerId,
        List<OrderItem> items) {

    return orderService.create(customerId, items);
}
```

And:

```java
@McpTool(
    name = "cancel_order",
    description = """
        Cancel an existing order.
        Only orders that have not shipped can be cancelled.
        """
)
public Order cancelOrder(Long orderId) {
    return orderService.cancel(orderId);
}
```

Notice something important:

**MCP doesn't have to mirror your REST API.**

Your REST API is designed for application clients.

Your MCP tools should be designed around **useful AI actions**.

---

# 8. Tool descriptions matter a lot

This:

```java
@McpTool(
    name = "update_customer",
    description = "Update customer"
)
```

is weak.

Prefer:

```java
@McpTool(
    name = "update_customer",
    description = """
        Update a customer's name or email address.
        The customer must already exist.
        This operation cannot change the customer's ID.
        """
)
```

The description is part of the interface the AI sees.

Likewise:

```java
@McpToolParam(
    description = "The unique customer ID",
    required = true
)
Long customerId
```

is better than:

```java
Long customerId
```

Spring AI's annotation support uses these annotations to generate the tool metadata/schema automatically. ([Home][5])

---

# 9. DTOs are preferable to JPA entities

I'd also avoid returning JPA entities directly.

Instead of:

```java
@McpTool
public CustomerEntity getCustomer(Long id) {
    ...
}
```

use:

```java
@McpTool
public CustomerDto getCustomer(Long id) {
    return customerService.getCustomerDto(id);
}
```

For example:

```java
public record CustomerDto(
    Long id,
    String name,
    String email,
    String status
) {}
```

Why?

Because your database model isn't necessarily your AI-facing contract.

You don't want:

```text
CustomerEntity
 ├── orders
 │    ├── customer
 │    │    ├── orders
 │    │    ...
```

accidentally becoming your tool output.

Keep the MCP contract explicit.

---

# 10. Complete minimal example

A minimal Spring Boot MCP server could be:

### Dependency

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

### Configuration

```yaml
spring:
  ai:
    mcp:
      server:
        name: customer-server
        version: 1.0.0
        protocol: STREAMABLE
```

Or application.properties

```properties
spring.ai.mcp.server.name=customer-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=STREAMABLE
```

### Existing service

```java
@Service
public class CustomerService {

    public CustomerDto findById(Long id) {
        // repository + business logic
        return ...;
    }

    public List<CustomerDto> search(String query) {
        return ...;
    }

    public CustomerDto create(String name, String email) {
        return ...;
    }

    public CustomerDto update(
            Long id,
            String name,
            String email) {
        return ...;
    }

    public void delete(Long id) {
        // business validation
    }
}
```

### MCP adapter

```java
@Component
public class CustomerMcpTools {

    private final CustomerService customerService;

    public CustomerMcpTools(CustomerService customerService) {
        this.customerService = customerService;
    }

    @McpTool(
        name = "get_customer",
        description = "Get a customer by ID"
    )
    public CustomerDto getCustomer(
            @McpToolParam(
                description = "Unique customer ID",
                required = true
            )
            Long id) {

        return customerService.findById(id);
    }

    @McpTool(
        name = "search_customers",
        description = "Search customers by name or email"
    )
    public List<CustomerDto> searchCustomers(
            @McpToolParam(
                description = "Name or email search text",
                required = true
            )
            String query) {

        return customerService.search(query);
    }

    @McpTool(
        name = "create_customer",
        description = "Create a new customer"
    )
    public CustomerDto createCustomer(
            String name,
            String email) {

        return customerService.create(name, email);
    }

    @McpTool(
        name = "update_customer",
        description = "Update an existing customer's name or email"
    )
    public CustomerDto updateCustomer(
            Long id,
            String name,
            String email) {

        return customerService.update(id, name, email);
    }

    @McpTool(
        name = "delete_customer",
        description = "Delete a customer by ID"
    )
    public void deleteCustomer(Long id) {
        customerService.delete(id);
    }
}
```

That's a perfectly reasonable starting point.

---

# 11. MCP resources vs tools

For CRUD, you'll primarily use **tools**.

Think:

| MCP concept    | Use it for                |
| -------------- | ------------------------- |
| `@McpTool`     | Actions / operations      |
| `@McpResource` | Readable resources/data   |
| `@McpPrompt`   | Reusable prompt templates |

Spring AI 2.0 supports all three server-side annotation types. ([Home][5])

For example:

```text
Tool:
    get_customer(42)

Resource:
    customer://42

Tool:
    create_customer(...)

Prompt:
    customer_summary
```

For a normal CRUD application, start with `@McpTool`. You can add resources later if you have a real use case for them.

---

# 12. Streamable HTTP vs STDIO

You have two particularly useful choices.

### STDIO

Good for:

```text
Claude Desktop
local development
local MCP server process
```

Spring AI uses:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
```

with:

```yaml
spring:
  ai:
    mcp:
      server:
        stdio: true
```

### Streamable HTTP

Better when your CRUD application is already a deployed Spring Boot service:

```text
AI client
    │
    │ HTTP
    ▼
your-app:8080
    │
    ▼
MCP tools
    │
    ▼
services
    │
    ▼
database
```

Use:

```xml
spring-ai-starter-mcp-server-webmvc
```

and:

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
```

Spring AI 2.0 specifically documents Streamable HTTP as the current WebMVC/WebFlux transport, with SSE marked deprecated since 2.0.0. ([Home][1])

---

## 13. One thing that's easy to misunderstand

You **don't need Spring AI's `ChatClient` to build an MCP server**.

These are separate concepts:

```text
Spring AI
│
├── ChatClient
│     └── Your application talks to an LLM
│
└── MCP
      ├── MCP Client
      └── MCP Server
```

Your application can simply be:

```text
Spring Boot
   +
Spring AI MCP Server
   +
Your existing services
   +
Your existing repositories
```

without having an LLM inside the application.

The external AI client connects to your MCP server.

---

## Recommended structure

For an existing CRUD application, I'd organize it roughly like this:

```text
src/main/java/com/example/customer/

├── controller/
│   └── CustomerController.java
│
├── service/
│   └── CustomerService.java
│
├── repository/
│   └── CustomerRepository.java
│
├── domain/
│   └── Customer.java
│
├── dto/
│   └── CustomerDto.java
│
└── mcp/
    └── CustomerMcpTools.java
```

The key principle is:

```text
REST ────────┐
             │
             ▼
       CustomerService
             ▲
             │
MCP ─────────┘
```

**Don't put business logic in `CustomerMcpTools`.** Make it a thin MCP adapter.

For Spring AI 2.0.0, the official docs are especially useful for the [MCP server starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html), [MCP annotations](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html), and [Streamable HTTP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-streamable-http-server-boot-starter-docs.html). ([Home][1])

If your actual project is **Spring Boot + JPA CRUD**, the next useful step is to take one concrete entity (e.g. `Product`, `Customer`, or `Order`) and wire its **Repository → Service → MCP tools → Claude/ChatGPT MCP client** end-to-end.

[1]: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html?utm_source=chatgpt.com "MCP Server Boot Starter :: Spring AI Reference"
[2]: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html?utm_source=chatgpt.com "Model Context Protocol (MCP) :: Spring AI Reference"
[3]: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html?utm_source=chatgpt.com "MCP Annotations :: Spring AI Reference"
[4]: https://docs.spring.io/spring-ai/reference/api/mcp/?utm_source=chatgpt.com "Index of /spring-ai/reference/api/mcp"
[5]: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html?utm_source=chatgpt.com "MCP Server Annotations :: Spring AI Reference"
