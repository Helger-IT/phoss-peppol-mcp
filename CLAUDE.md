# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**phoss Peppol MCP** is an MCP (Model Context Protocol) server that exposes Peppol network lookup capabilities as tools to AI models. It uses the phax peppol-commons stack for SMP/SML lookups and the MCP Java SDK for protocol handling. Communication is via stdio (stdin/stdout).

## Build Commands

```shell
mvn clean package              # Build fat JAR (includes tests)
mvn clean package -DskipTests  # Build without tests
mvn test                       # Run all tests
mvn test -Dtest=PeppolSmpToolsTest#testValidParticipantIdFormat  # Single test
```

The shaded JAR is produced at `target/peppol-mcp-server-*.jar`.

## Architecture

**Entry point:** `PeppolMcpServer.main()` — creates tool provider instances, registers all tools with `McpServer.sync()`, and blocks on `Thread.join()`.

**Tool providers** (each class exposes one or more `SyncToolSpecification`):

| Class | Tools | Network? |
|-------|-------|----------|
| `PeppolSmpTools` | SMP lookups (participant, doc type support, endpoint URL, validate ID) | Yes |
| `PeppolDirectoryTools` | Peppol Directory search by company name/country | Yes |
| `PeppolIdentifierValidationTools` | Syntactic validation of participant/doctype/process IDs | No |
| `PeppolCodelistTools` | Codelist presence checks for schemes, doc types, process IDs | No |

**Pattern for adding a new tool:**
1. Write a private `_doSomething()` method that does the work and returns a result object
2. Write a public `doSomethingTool()` method that builds a `McpSchema.Tool` via builder, wraps the handler in a `SyncToolSpecification`
3. Register the tool in `PeppolMcpServer.main()`

**Key design decisions:**
- `PeppolSmpTools` takes `EPeppolNetwork` (PRODUCTION/TEST) as constructor parameter — tests use TEST network
- Static parse helpers (`parseParticipantId`, `parseDocTypeID`, `parseProcessID`) try `parse*()` then `createWithDefaultScheme()` as fallback
- `_executeWithErrorHandling` wraps tool logic: serializes result to JSON on success, returns `isError=true` with message on failure
- All logging goes to stderr via `logback.xml` — stdout is reserved for MCP protocol framing

## Key Dependencies

- `io.modelcontextprotocol.sdk:mcp:1.1.1` — MCP Java SDK (Jackson 3, `tools.jackson.databind`)
- `com.helger.peppol:peppol-commons` + `peppol-smp-client` — Peppol identifier handling, SMP client, codelists
- Parent POM: `com.helger:parent-pom` — provides compiler settings, plugin versions

## MCP SDK API Notes

- All protocol types are nested under `McpSchema` (e.g. `McpSchema.Tool`, `McpSchema.CallToolResult`, `McpSchema.JsonSchema`)
- `McpSchema.JsonSchema` record has 6 fields: `(type, properties, required, additionalProperties, defs, definitions)` — pass `null` for unused last two
- Tool handler signature: `BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>` — get args via `request.arguments().get("key")`
- Transport: `new StdioServerTransportProvider(McpJsonDefaults.getMapper())`

## stdout Constraint

Never use `System.out.println()` in tool implementations. The MCP stdio transport uses stdout exclusively for protocol communication. All logging must go to stderr (enforced in `logback.xml`).
