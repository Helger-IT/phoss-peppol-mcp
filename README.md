# phoss-peppol-mcp-server

A phoss Peppol MCP (Model Context Protocol) server that exposes Peppol network lookup
capabilities as tools to AI models such as Claude.

## Tools exposed

- lookup_peppol_participant       — Check if a company is registered on Peppol
- check_peppol_document_type_support — Check if a participant supports a document type
- get_peppol_endpoint_url         — Get the AS4 endpoint URL for sending
- validate_peppol_participant_id  — Validate participant ID format (no network needed)
- search_peppol_directory         — Search for participants by company name/country

## Build

```shell
mvn clean package
```

## Testing

### Level 1 — Unit tests (no network, fast)

```shell
mvn test
```

### Level 2 — MCP Inspector (validates MCP protocol)

```shell
npx @modelcontextprotocol/inspector java -jar target/peppol-mcp-server.jar
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
          "args": ["-jar", "/absolute/path/to/target/peppol-mcp-server.jar"]
        }
      }
    }

Restart Claude Desktop and ask questions like:
  - "Is company 0192:991825827 registered on Peppol?"
  - "Can I send a Peppol BIS Billing 3.0 invoice to participant 0088:4012345678901?"
  - "Find Peppol participants named Helger in Austria"

## Important: stdout vs stderr

The MCP stdio transport uses stdout exclusively for protocol communication.
Any logging written to stdout will corrupt the protocol framing and break
the connection. The `logback.xml` in this project enforces stderr-only logging.
Never use `System.out.println()` in tool implementations.

## Switching to the Peppol test network (SMK)

Change the SML_DNS_ZONE constant in PeppolSmpTools.java to:

    acc.edelivery.tech.ec.europa.eu

This points to the Peppol Acceptance (test) network instead of production.
