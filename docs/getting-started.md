# Getting Started with OpenBBT

This guide walks you through installing OpenBBT, configuring your first testProject, installing plugins, and generating a test testPlan.

## Requirements

- **Java 21** or higher (`java -version` to check)
- Internet access to download plugins from Maven Central (or a configured Maven repository)

---

## 1. Installation

### Download

Download the latest distribution ZIP from the [releases page](https://github.com/org-myjtools/openbbt/releases) and extract it to a directory of your choice:

```
openbbt-<version>/
├── bin/
│   ├── openbbt        # Unix/macOS launcher
│   └── openbbt.bat    # Windows launcher
└── lib/               # Runtime JARs (do not modify)
```

### Build from source

```bash
git clone https://github.com/org-myjtools/openbbt.git
cd openbbt
mvn package -pl openbbt-cli -am -DskipTests
```

The distribution ZIP will be at `openbbt-cli/target/openbbt-cli-<version>-dist.zip`. Extract it to the directory of your choice.

---

## 2. Add the executable to PATH

### Linux / macOS

```bash
# Replace with your actual installation path
export OPENBBT_HOME=/opt/openbbt-1.0.0-alpha1
export PATH="$OPENBBT_HOME/bin:$PATH"
```

To make this permanent, add those two lines to your shell profile (`~/.bashrc`, `~/.zshrc`, or `~/.profile`) and reload it:

```bash
source ~/.bashrc   # or source ~/.zshrc
```

Verify:

```bash
openbbt --help
```

### Windows

1. Open **Start → Search → "Edit the system environment variables"**
2. Click **Environment Variables…**
3. Under *System variables*, select `Path` and click **Edit**
4. Click **New** and add the full path to the `bin\` folder, e.g. `C:\tools\openbbt-1.0.0-alpha1\bin`
5. Click **OK** on every dialog

Open a new Command Prompt and verify:

```cmd
openbbt --help
```

---

## 3. Create the testProject configuration file

OpenBBT is driven by an `openbbt.yaml` file that you place in your testProject's working directory. The default name is `openbbt.yaml`; you can override it with the `-f` flag.

### Minimal example

```yaml
testProject:
  organization: Acme Corp
  name: My Project
  test-suites:
    - name: smoke
      tag-expression: "smoke"
    - name: regression
      tag-expression: "regression"

plugins:
  - gherkin

configuration:
  core.resourcePath: src/test/resources/features
```

### Full reference

```yaml
testProject:
  organization: string          # Organisation name
  name: string                  # Project name
  description: string           # Optional description
  test-suites:
    - name: string              # Suite name (used with -s flag)
      description: string       # Optional description
      tag-expression: string    # Tag filter, e.g. "@smoke and not @wip"

plugins:
  - gherkin                     # Short name (resolves to org.myjtools.openbbt.plugins:gherkin-openbbt-plugin)
  - org.example:my-plugin       # Or full group:artifact coordinate

configuration:
  core.resourcePath: path/to/features           # Where test resources live (relative to CWD)
  core.environmentPath: .openbbt                # Plugin/data cache directory (default: .openbbt)
  core.artifacts.local.repository: ~/.m2/repository  # Local Maven cache (default: ~/.m2/repository)
  core.artifacts.repository.url: https://repo.example.com/maven2  # Custom Maven repo (optional)
  core.artifacts.repository.username: user      # Credentials for custom repo (optional)
  core.artifacts.repository.password: secret
  core.persistence.mode: in-memory             # in-memory | file | remote (default: in-memory)
  core.persistence.file: .openbbt/data/db      # Path when mode=file
  core.idTagPattern: "ID-(\\w+)"               # Regex to extract IDs from Gherkin tags (optional)
  core.definitionTag: definition               # Tag marking a node as a definition (optional)
  core.implementationTag: implementation       # Tag marking a node as an implementation (optional)

profiles:                       # Named sets of placeholder values
  staging:
    env: staging
    base-url: https://staging.example.com
  production:
    env: production
    base-url: https://example.com
```

**Placeholder substitution** — use `{{key}}` in any configuration value and it will be replaced by the active profile's value:

```yaml
configuration:
  base-url: "{{base-url}}/api"
```

Activate a profile with the `-p` flag:

```bash
openbbt testPlan -p staging -s smoke
```

---

## 4. Install plugins

Plugins provide components such as the test format support (e.g., Gherkin), executable steps, hooks, and reports,
among other things. Run the `install` command once per testProject, or whenever you add a new plugin to `openbbt.yaml`.
Artifacts are downloaded from Maven Central and cached locally under the `core.environmentPath` directory
(`.openbbt/` by default).

```bash
# Install all plugins declared in openbbt.yaml
openbbt install
```

```bash
# Re-install from scratch (removes cached plugins first)
openbbt install --clean
```

### What gets installed

```
.openbbt/
└── plugins/
    ├── manifests/     # Plugin descriptors (YAML)
    └── artifacts/     # Downloaded JARs, grouped by Maven coordinates
```

---

## 5. Generate a test testPlan

The `testPlan` command loads the plugins, reads your test resources, and assembles a test testPlan. On success it prints a testPlan identifier (a ULID) to stdout.

```bash
# Generate testPlan for one suite
openbbt testPlan -s smoke
```

```bash
# Generate testPlan for multiple suites
openbbt testPlan -s smoke -s regression
```

```bash
# Show the full testPlan tree (scenarios, steps, …)
openbbt testPlan -s smoke --detail
```

```bash
# Pass extra parameters at runtime
openbbt testPlan -s smoke -D base-url=https://staging.example.com
```

```bash
# Use a profile
openbbt testPlan -s smoke -p staging
```

---

## 6. Command reference

All commands share a common set of global options:

| Option | Short | Description |
|--------|-------|-------------|
| `--file` | `-f` | Path to the configuration file (default: `openbbt.yaml`) |
| `--suite` | `-s` | Test suite name; may be repeated for multiple suites |
| `--profile` | `-p` | Activate a named profile defined in `openbbt.yaml` |
| `-D key=value` | | Override any configuration key at runtime |
| `--debug` | `-d` | Enable verbose debug logging |
| `--help` | | Print help for the command |

### `install`

Install the plugins declared in `openbbt.yaml`.

```bash
openbbt install [-f <file>] [--clean]
```

| Option | Description |
|--------|-------------|
| `--clean` / `-c` | Delete existing plugin cache before installing |

### `testPlan`

Assemble and display the test testPlan.

```bash
openbbt testPlan [-f <file>] [-s <suite>]... [--detail] [-p <profile>] [-D key=value]...
```

| Option | Description |
|--------|-------------|
| `--detail` | Print the full testPlan tree (suites, scenarios, steps) |

### `show-config`

Display all resolved configuration values and available options.

```bash
openbbt show-config [-f <file>] [-p <profile>] [-D key=value]...
```

### `purge`

Delete all local OpenBBT data (plugins, cache, persisted plans).

```bash
openbbt purge [-f <file>]
```

> **Warning:** This removes the entire `.openbbt/` directory. You will need to run `install` again afterwards.

---

## 7. Typical workflow

```bash
# 1. Create your openbbt.yaml in the testProject root
vim openbbt.yaml

# 2. Install plugins (once, or when openbbt.yaml changes)
openbbt install

# 3. Inspect resolved configuration
openbbt show-config 

# 4. Generate the test testPlan for a suite
openbbt testPlan-s smoke

# 5. Inspect testPlan details
openbbt testPlan -s smoke --detail

# 6. Use a profile for environment-specific values
openbbt testPlan -s smoke -p staging
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `No SuiteAssembler found` | Plugin not installed or incompatible | Run `openbbt install` and verify the plugin name in `openbbt.yaml` |
| `Test suite 'X' not found` | Suite name mismatch | Check the `test-suites[].name` values in `openbbt.yaml` |
| `Failed to read configuration file` | Wrong path or missing file | Pass the correct path with `-f` |
| `No test testPlan nodes assembled` | Tag expression matches nothing | Verify the `tag-expression` and that resources exist at `core.resourcePath` |
| Artifact download fails | Network or repository config | Check `core.artifacts.repository.url` and proxy settings |

Enable debug logging with `-d` to get detailed output for any issue:

```bash
openbbt testPlan -f openbbt.yaml -s smoke -d
```
