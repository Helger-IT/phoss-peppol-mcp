package com.example.peppol.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.peppol.mcp.tools.PeppolDirectoryTools;
import com.example.peppol.mcp.tools.PeppolSmpTools;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Entry point for the Peppol MCP server. The server communicates via stdio (stdin/stdout), which is
 * the standard MCP transport when launched by Claude Desktop or the MCP Inspector. Launch via: java
 * -jar peppol-mcp-server.jar Or configure in Claude Desktop's config: { "mcpServers": { "peppol": {
 * "command": "java", "args": ["-jar", "/path/to/peppol-mcp-server.jar"] } } }
 */
public class PeppolMcpServer
{
  private static final Logger LOG = LoggerFactory.getLogger (PeppolMcpServer.class);

  public static void main (final String [] args) throws Exception
  {
    LOG.info ("Starting Peppol MCP Server...");

    // Instantiate tool providers
    final PeppolSmpTools smpTools = new PeppolSmpTools ();
    final PeppolDirectoryTools directoryTools = new PeppolDirectoryTools ();

    // Build and start the MCP server
    final McpSyncServer server = McpServer.sync (new StdioServerTransportProvider ())
                                          .serverInfo ("peppol-mcp-server", "1.0.0")
                                          .capabilities (McpSchema.ServerCapabilities.builder ()
                                                                                     // expose tools
                                                                                     // to the AI
                                                                                     .tools (Boolean.TRUE)
                                                                                     .build ())
                                          // Register all Peppol tools
                                          .tools (smpTools.lookupParticipantTool (),
                                                  smpTools.checkDocumentTypeSupportTool (),
                                                  smpTools.getEndpointUrlTool (),
                                                  smpTools.validateParticipantIdTool (),
                                                  directoryTools.searchParticipantsByNameTool ())
                                          .build ();

    // The StdioServerTransportProvider reads from stdin in a background thread.
    // Block the main thread to keep the JVM alive until stdin is closed.
    Thread.currentThread ().join ();

    server.close ();

    LOG.info ("Peppol MCP Server stopped.");
  }
}
