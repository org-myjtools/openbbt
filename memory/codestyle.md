# OpenBBT Java Code Style Guide

## Formatting
- **Indentation**: 4 spaces (no tabs)
- **Braces**: K&R style — opening brace on same line
- **Imports**: Explicit only, no wildcards. Order: java.*, external libs, project-internal

## Java Features (Java 21)
- **Sealed classes**: Used for closed hierarchies (e.g., `AbstractCommand`)
- **Pattern matching**: Switch with record deconstruction + `instanceof` patterns — used heavily
- **Streams**: Extensive functional pipelines, `.toList()` terminal op
- **`var`**: Used conservatively for obvious types
- **Optional**: Preferred over returning null — use for all optional return values
- **Text blocks**: Not used in codebase

## Naming
| Element | Convention | Examples |
|---------|-----------|---------|
| Classes | PascalCase + role suffix | `InstallCommand`, `JooqPlanRepository`, `StringAssertionFactory` |
| Methods | camelCase, verb-first | `getNodeChildren()`, `existsNode()`, `buildTestPlan()` |
| Static factories | `of()` | `Log.of()`, `Pair.of()`, `Lazy.of(supplier)` |
| Constants | UPPER_SNAKE_CASE | `TABLE_PLAN_NODE`, `FIELD_NODE_ID` |
| Generics | Single letter or descriptive | `<T>`, `<T,U>` |

## Logging
Use the custom `Log` wrapper, not `LoggerFactory` directly:
```java
private static final Log log = Log.of();            // default category
private static final Log log = Log.of("rest");      // subcategory → org.myjtools.openbbt.rest
log.warn("No factory found for {}", type.getSimpleName());
```

## Exceptions
Custom base: `OpenBBTException extends RuntimeException` with `{}` placeholder formatting:
```java
throw new OpenBBTException("Node {} not found in plan {}", nodeId, planId);
```
- Prefer unchecked exceptions
- `@Serial private static final long serialVersionUID = 1L;` in serializable exceptions
- Swallow-and-ignore: `try { x.close(); } catch (Exception ignored) {}`

## Null Handling
- Return `Optional<T>` instead of nullable values
- Use `Map.of()` / `List.of()` for empty collections instead of null
- Direct null checks only when mapping from external data (e.g., DB records)

## Lombok
Conservative and selective:
- `@Getter`, `@Setter` — field accessors
- `@NoArgsConstructor`, `@EqualsAndHashCode`, `@ToString` — boilerplate
- `requires static lombok;` in module-info.java
- **Never** use `@Data` (too broad) — prefer explicit annotations

## Patterns
**Fluent builder on domain objects** (method chaining returns `this`):
```java
new TestPlanNode()
    .nodeType(NodeType.TEST_PLAN)
    .name("test name")
    .addTags(Set.of("smoke"));
```

**Static factory `of()`**:
```java
public static <T> Lazy<T> of(Supplier<T> supplier) { return new Lazy<>(supplier); }
```

**Template method for factories** (`fillSuppliers()` pattern):
```java
public class StringAssertionFactory extends AssertionFactoryAdapter<String> {
    @Override
    protected void fillSuppliers() {
        suppliers.put(ASSERTION_EQUALS, it -> new AssertionAdapter(Matchers.comparesEqualTo(it)));
    }
}
```

**SPI via JPMS** (plugins use `provides ... with ...` in module-info.java):
```java
provides StepProvider with RestStepProvider;
provides ConfigProvider with RestConfigProvider;
```

## JPMS Module Structure
```java
module org.myjtools.openbbt.mymodule {
    requires org.myjtools.openbbt.core;
    requires org.myjtools.jexten;
    requires static lombok;               // optional deps = static

    exports org.myjtools.openbbt.mymodule;
    opens org.myjtools.openbbt.mymodule to org.myjtools.jexten;  // for reflection

    uses SomeExtensionPoint;
    provides SomeExtensionPoint with MyImpl;
}
```

## Package Structure
```
org.myjtools.openbbt.<module>
├── (root)         — public API classes
├── .contributors  — extension points / SPI interfaces
├── .backend       — execution/engine internals
├── .util          — reusable utilities (Log, Lazy, Pair, Either)
└── .test          — test utilities (in test source tree)
```
Organize by **concept/concern**, not by layer.

## Javadoc
- **Public API**: Full Javadoc with `@param`, `@return`, `@throws`, example blocks where complex
- **Internal/private**: Minimal or none — code structure explains intent
- **Inline comments**: Rare, explain *why* not *what*. Use step comments for multi-phase algorithms:
  ```java
  // Step 1: initialize HAS_ISSUES from VALIDATION_STATUS
  // Step 2: propagate upward — mark ancestors
  ```

## Tests
- Class suffix: `Test` (not `Tests`)
- Method names: descriptive, no `test` prefix needed with JUnit 5
  - `insertPlanNodeWithAllFields()`, `deleteNode_cascadesDeletion()`
- **Assertions**: AssertJ (`assertThat(x).isEqualTo(y)`) — never raw JUnit asserts
- Setup/teardown: `@BeforeEach`, `@AfterEach`
- Build test fixtures with fluent builder, not constructor overloads

## Interfaces vs Abstract Classes
- Interfaces: minimal contract, annotated `@ExtensionPoint` if plugin SPI
- Abstract classes: Template Method pattern, `abstract protected void fillX()` hooks
- Adapters: implement interface, wrap external library (e.g., Hamcrest, jOOQ)