package com.example.peppol.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.peppol.mcp.tools.PeppolCodelistTools;
import com.example.peppol.mcp.tools.PeppolDirectoryTools;
import com.example.peppol.mcp.tools.PeppolIdentifierValidationTools;
import com.example.peppol.mcp.tools.PeppolSmpTools;
import com.helger.peppol.servicedomain.EPeppolNetwork;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Entry point for the phoss Peppol MCP server. The server communicates via stdio (stdin/stdout),
 * which is the standard MCP transport when launched by Claude Desktop or the MCP Inspector. Launch
 * via:<br>
 * java -jar peppol-mcp-server.jar<br>
 * Or configure in Claude Desktop's config:<br>
 *
 * <pre>
 * {
 *   "mcpServers": {
 *     "peppol": {
 *       "command": "java",
 *       "args": ["-jar", "/path/to/peppol-mcp-server.jar"]
 *     }
 *   }
 * }
 * </pre>
 */
public class PhossPeppolMcpServer
{
  private static final Logger LOG = LoggerFactory.getLogger (PhossPeppolMcpServer.class);

  public static void main (final String [] args) throws Exception
  {
    LOG.info ("Starting " + CPhossPeppolMcp.APP_TITLE + " " + CPhossPeppolMcp.BUILD_VERSION + " ...");

    // Instantiate tool providers
    final EPeppolNetwork eNetwork = EPeppolNetwork.PRODUCTION;
    final PeppolSmpTools aSmpTools = new PeppolSmpTools (eNetwork);
    final PeppolDirectoryTools aDirectoryTools = new PeppolDirectoryTools (eNetwork);
    final PeppolIdentifierValidationTools aValidationTools = new PeppolIdentifierValidationTools ();
    final PeppolCodelistTools aCodelistTools = new PeppolCodelistTools ();

    // Build and start the MCP server
    final McpSyncServer server = McpServer.sync (new StdioServerTransportProvider (McpJsonDefaults.getMapper ()))
                                          .serverInfo (new McpSchema.Implementation (CPhossPeppolMcp.APP_NAME,
                                                                                     CPhossPeppolMcp.APP_TITLE,
                                                                                     CPhossPeppolMcp.BUILD_VERSION))
                                          .capabilities (McpSchema.ServerCapabilities.builder ()
                                                                                     // expose tools
                                                                                     // to the AI
                                                                                     .tools (Boolean.TRUE)
                                                                                     .build ())
                                          .tools (// Register all Peppol SMP tools
                                                  aSmpTools.lookupParticipantTool (),
                                                  aSmpTools.checkDocumentTypeSupportTool (),
                                                  aSmpTools.getEndpointUrlTool (),
                                                  // Register all Peppol Directory tools
                                                  aDirectoryTools.searchParticipantsByNameTool (),
                                                  // Syntactic validation tools
                                                  aValidationTools.validateParticipantIdSyntaxTool (),
                                                  aValidationTools.validateDocumentTypeIdSyntaxTool (),
                                                  aValidationTools.validateProcessIdSyntaxTool (),
                                                  // Codelist check tools
                                                  aCodelistTools.checkParticipantIdSchemeInCodelistTool (),
                                                  aCodelistTools.checkDocumentTypeIdInCodelistTool (),
                                                  aCodelistTools.checkProcessIdInCodelistTool (),
                                                  aCodelistTools.checkSPISUseCaseIdInCodelistTool (),
                                                  // Codelist listing tools
                                                  aCodelistTools.listParticipantIdSchemesTool (),
                                                  aCodelistTools.listDocumentTypeIdsTool (),
                                                  aCodelistTools.listProcessIdsTool (),
                                                  aCodelistTools.listSPISUseCaseIdsTool (),
                                                  aCodelistTools.getCodelistVersionTool ())
                                          .build ();

    // The StdioServerTransportProvider reads from stdin in a background thread.
    // Block the main thread to keep the JVM alive until stdin is closed.
    Thread.currentThread ().join ();

    server.close ();

    LOG.info (CPhossPeppolMcp.APP_TITLE + " stopped.");
  }
}
