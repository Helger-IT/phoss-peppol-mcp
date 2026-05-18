# phoss-peppol-mcp-server

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.peppol.mcp/phoss-peppol-mcp-server/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.peppol.mcp/phoss-peppol-mcp-server/)
[![javadoc](https://javadoc.io/badge2/com.helger.peppol.mcp/phoss-peppol-mcp-server/javadoc.svg)](https://javadoc.io/doc/com.helger.peppol.mcp/phoss-peppol-mcp-server)
<!-- ph-badge-end -->

A phoss Peppol MCP (Model Context Protocol) server that exposes Peppol Network 
capabilities as tools to AI models such as Claude.

## Tools exposed

### SMP lookup tools (require network)

| Tool | Description |
|------|-------------|
| `lookup_peppol_participant` | Check if a company is registered on the Peppol network |
| `check_peppol_document_type_support` | Check if a participant supports a specific document type |
| `get_peppol_endpoint_url` | Get the AS4 endpoint URL for sending to a participant |
| `get_smp_service_groups` | List ALL document types a participant has registered on its SMP |
| `get_smp_signature_info` | Inspect the X.509 certificate that signed the SMP response (subject, issuer, validity) |

### DNS resolution tools (require network)

| Tool | Description |
|------|-------------|
| `resolve_peppol_dns` | Resolve the Peppol U-NAPTR DNS chain for a participant; returns the queried DNS hostname and the resolved SMP base URL |

### Certificate validation tools (require network for CRL/OCSP)

| Tool | Description |
|------|-------------|
| `check_certificate_chain` | Validate a PEM-encoded X.509 certificate against the official Peppol AP or SMP trust stores (production, test, or all); checks issuer trust, validity, and revocation |

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
  - "List all document types registered for participant 0088:4012345678901"
  - "Resolve the Peppol DNS for 0088:4012345678901 and show me the SMP URL"
  - "Validate this AP certificate against the Peppol production trust store"

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

v0.5.1 - 2026-05-08
* Added SMP tool `get_smp_service_groups` to list all document types a participant has registered
* Added SMP tool `get_smp_signature_info` to inspect the X.509 certificate that signed an SMP response
* Added DNS tool `resolve_peppol_dns` to diagnose Peppol U-NAPTR DNS resolution
* Added certificate tool `check_certificate_chain` to validate AP and SMP certificates against the official Peppol trust stores

v0.5.0 - 2026-04-13
* Initial release
