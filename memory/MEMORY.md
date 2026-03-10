# OpenBBT Project Memory

## Project Overview
**OpenBBT** — Java-based BDD testing framework with pluggable architecture.
- Org: `org.myjtools.openbbt` | Version: `1.0.0-alpha1` | License: MIT
- Java 21 + Maven 3.x | JPMS modules throughout
- Branch strategy: `main` (stable) ← `develop` (integration) ← `feature/*` / `fix/*`
- PRs target `develop`, not `main`

## Style Guide
See `memory/codestyle.md` — covers: 4-space indent, K&R braces, Optional-first, custom Log wrapper, Lombok (conservative), sealed+pattern-matching, JPMS SPI pattern, AssertJ tests, fluent builders, static `of()` factories.

## Architecture (see architecture.md for details)
Multi-module Maven monorepo. Key modules:
- **openbbt-core** — Core BDD runtime, step execution, expression matching, assertions
- **openbbt-persistence** — jOOQ + Flyway + HSQLDB/PostgreSQL
- **openbbt-cli** — PicoCLI CLI, subcommands: browse, init, install, version, purge, plan, serve, show-config, tui, lsp
- **openbbt-lsp** — Eclipse LSP4J language server
- **openbbt-tui** — Lanterna terminal UI
- **openbbt-vscode** — TypeScript VS Code extension (npm build, separate from Maven)
- **openbbt-plugin-starter** — Template POM for plugin development
- **openbbt-it** — Integration tests (TestContainers)
- **openbbt-docgen-maven-plugin** — Doc generation from code
- **plugins/gherkin-openbbt-plugin** — Gherkin feature file support (built via `plugins` profile)
- **plugins/rest-openbbt-plugin** — REST-assured HTTP testing steps
- **plugins/markdown-plan-openbbt-plugin** — Markdown-based test plan definitions

## Key Files
- `pom.xml` — Root POM, parent: `myjtools-parent:1.4.0`, Java 21
- `openbbt-core/src/main/java/org/myjtools/openbbt/core/OpenBBTRuntime.java` — Central runtime
- `openbbt-core/src/main/java/org/myjtools/openbbt/core/OpenBBTFile.java` — Config file parser
- `openbbt-core/src/main/java/org/myjtools/openbbt/core/OpenBBTConfig.java` — Config keys/provider
- `openbbt-persistence/src/main/java/...JooqRepositoryFactory.java` — 3-mode persistence factory
- `openbbt-cli/src/main/java/.../MainCommand.java` — CLI entry point
- `.github/workflows/verify.yml` — CI: build+test+SonarCloud on push/PR to main
- `.github/workflows/publish.yml` — CD: deploy to GitHub Packages on `v*` tags
- `docs/getting-started.md` / `docs/rest-config.md` — User docs

## Technology Stack
| Area | Library/Version |
|------|----------------|
| Plugin framework | JExten 1.0.0-alpha2 |
| Config | imconfig 1.5.1 |
| Cache | Caffeine 3.2.2 |
| JSON/YAML | Jackson 2.18.2, SnakeYAML 2.5 |
| SQL | jOOQ 3.19.18, Flyway 11.3.1, HikariCP 6.2.1 |
| HTTP testing | REST-assured 5.4.0 |
| Assertions | Hamcrest 2.2 |
| Terminal UI | Lanterna 3.1.2 |
| Gherkin | gherkin-parser 1.0.4 |
| LSP | LSP4J 1.0.0 |
| CLI | PicoCLI 4.7.7 |
| IDs | ULID Creator 5.2.3 |
| Code gen | Lombok 1.18.38 |
| Testing | JUnit 5, AssertJ 3.27.7, TestContainers 1.21.3 |

## Configuration (openbbt.yaml)
```yaml
project:
  name: string
  organization: string
  test-suites:
    - name: string
      tag-expression: string
plugins:
  - gherkin  # short name or full Maven coord
configuration:
  core.resourcePath: path
  core.persistence.mode: transient|file|remote
  rest.baseURL: url
profiles:
  staging:
    key: value
```

## Persistence Modes
- `transient` — temp HSQLDB file (default dev)
- `file` — persistent HSQLDB file
- `remote` — PostgreSQL

## Key Architecture Patterns
- Plugin extensibility via JExten + JPMS ModuleLayerProvider
- SuiteAssembler interface for adding new test plan formats
- TestPlanNode tree model (PLAN > SUITE > CASE > STEP)
- Hash-based plan caching (reuses if resource+config hashes match)
- ConfigProvider extension for configuration injection
- Lazy<T> for deferred initialization

## Build Commands
```bash
mvn verify                          # Full build + tests
mvn verify -P plugins               # Include plugin modules
mvn -B verify                       # CI mode
```

## Data Types
- Renamed from `number` → `integer` in recent commit (b92f695)
- integer assertions use double braces `{{value}}`

## Recent Notable Changes (from git log)
- VSCode Test Plan enhancements + serve backend (ad3c000)
- Double braces for integer-assertion in rest-steps.yaml (b92f695)
- Default ENV_PATH fix in install --clean (159bb6f)
- GitHub community standard files added (36b4509)
- number→integer rename, gherkin-parser updated to 1.0.4 (a958f75)