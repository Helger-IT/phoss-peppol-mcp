# phoss-peppol-mcp-server

A phoss Peppol MCP (Model Context Protocol) server that exposes Peppol Network 
capabilities as tools to AI models such as Claude.

## Tools exposed

### SMP lookup tools (require network)

| Tool | Description |
|------|-------------|
| `lookup_peppol_participant` | Check if a company is registered on the Peppol network |
| `check_peppol_document_type_support` | Check if a participant supports a specific document type |
| `get_peppol_endpoint_url` | Get the AS4 endpoint URL for sending to a participant |

### Peppol Directory tools (require network, rate-limited to 2 queries/sec)

| Tool | Description |
|------|-------------|
| `search_peppol_directory` | Search for participants by name, identifier, or any other field |

### Identifier validation tools (local, no network)

| Tool | Description |
|------|-------------|
| `validate_participant_id_syntax` | Validate participant identifier format |
| `validate_document_type_id_syntax` | Validate document type identifier format |
| `validate_process_id_syntax` | Validate process identifier format |

### Codelist check tools (local, no network)

| Tool | Description |
|------|-------------|
| `check_participant_id_scheme_in_codelist` | Check if a participant ID scheme (ISO 6523) is in the official codelist |
| `check_document_type_id_in_codelist` | Check if a document type ID is in the official codelist |
| `check_process_id_in_codelist` | Check if a process ID is in the official codelist |
| `check_spis_use_case_id_in_codelist` | Check if a SPIS Use Case ID (e.g. MLS) is in the official codelist |

### Codelist listing tools (local, no network)

All listing tools support text search via `query`, pagination via `offset`/`limit` (default limit: 50), and filtering by `state`.

| Tool | Description | Extra filters |
|------|-------------|---------------|
| `list_participant_id_schemes` | List participant ID schemes | `countryCode` |
| `list_document_type_ids` | List document type IDs | `domainCommunity` (POAC, PRAC, Logistics) |
| `list_process_ids` | List process IDs | — |
| `list_spis_use_case_ids` | List SPIS Use Case IDs | — |
| `get_peppol_codelist_version` | Get the version of all Peppol codelists in use | — |

## Build

```shell
mvn clean package
```

This produces a runnable fat JAR at `target/peppol-mcp-server-0.1.0-SNAPSHOT.jar`.

## Testing

### Level 1 — Unit tests

```shell
mvn test
```

Note: some tests hit the live Peppol test network (SMK).

### Level 2 — MCP Inspector (validates MCP protocol)

```shell
npx @modelcontextprotocol/inspector java -jar target/peppol-mcp-server-0.1.0-SNAPSHOT.jar
```

This opens a browser UI at `http://localhost:5173` where you can invoke
each tool manually and inspect the exact JSON exchanged over the protocol.

### Level 3 — Claude Desktop integration

Add to your Claude Desktop config
(`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS,
 `%APPDATA%\Claude\claude_desktop_config.json` on Windows):

    {
      "mcpServers": {
        "peppol": {
          "command": "java",
          "args": ["-jar", "/absolute/path/to/target/peppol-mcp-server-0.1.0-SNAPSHOT.jar"]
        }
      }
    }

Restart Claude Desktop and ask questions like:
  - "Is company 0192:991825827 registered on Peppol?"
  - "Can I send a Peppol BIS Billing 3.0 invoice to participant 0088:4012345678901?"
  - "Find Peppol participants named Helger in Austria"
  - "Search the Peppol Directory for ATU80962638"
  - "What active document types exist for billing?"
  - "Which participant ID schemes are available for Norway?"
  - "Is 0088 a valid Peppol participant identifier scheme?"
  - "Look up the process ID urn:fdc:peppol.eu:2017:poacc:billing:01:1.0 in the codelist"

## Important: stdout vs stderr

The MCP stdio transport uses stdout exclusively for protocol communication.
Any logging written to stdout will corrupt the protocol framing and break
the connection. The `logback.xml` in this project enforces stderr-only logging.
Never use `System.out.println()` in tool implementations.

## Switching between Peppol production and test network

By default the server uses the Peppol production network. Pass `--network=test`
to use the test network (SMK/SML):

```shell
java -jar target/peppol-mcp-server-full.jar --network=test
```

Claude Desktop config for the test network:

    {
      "mcpServers": {
        "peppol-test": {
          "command": "java",
          "args": ["-jar", "/absolute/path/to/target/peppol-mcp-server-full.jar", "--network=test"]
        }
      }
    }

Other CLI options: `--help`, `--version`.

# News and noteworthy

v0.5.0 - 2026-04-13
* Initial release
