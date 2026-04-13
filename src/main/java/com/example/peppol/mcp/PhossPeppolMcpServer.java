/*
 * Copyright (C) 2026 Philip Helger
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.peppol.mcp;

import java.io.PrintWriter;

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
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Entry point for the phoss Peppol MCP server. The server communicates via stdio (stdin/stdout),
 * which is the standard MCP transport when launched by Claude Desktop or the MCP Inspector. Launch
 * via:<br>
 * java -jar peppol-mcp-server.jar<br>
 * java -jar peppol-mcp-server.jar --network=test<br>
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
@Command (name = "phoss-peppol-mcp-server",
          description = "phoss Peppol MCP Server — exposes Peppol Network lookup tools to AI models",
          mixinStandardHelpOptions = true,
          versionProvider = PhossPeppolMcpServer.VersionProvider.class)
public class PhossPeppolMcpServer implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger (PhossPeppolMcpServer.class);

  @Option (names = "--network",
           paramLabel = "network",
           description = "Peppol Network to use: production (default) or test",
           defaultValue = "production")
  private String m_sNetwork;

  static class VersionProvider implements CommandLine.IVersionProvider
  {
    @Override
    public String [] getVersion ()
    {
      return new String [] { CPhossPeppolMcp.APP_TITLE + " " + CPhossPeppolMcp.BUILD_VERSION };
    }
  }

  @Override
  public void run ()
  {
    final EPeppolNetwork eNetwork;
    if ("test".equalsIgnoreCase (m_sNetwork))
      eNetwork = EPeppolNetwork.TEST;
    else
      if ("production".equalsIgnoreCase (m_sNetwork))
        eNetwork = EPeppolNetwork.PRODUCTION;
      else
        throw new CommandLine.ParameterException (new CommandLine (this),
                                                  "Invalid network '" +
                                                                          m_sNetwork +
                                                                          "'. Must be 'production' or 'test'.");

    LOG.info ("Starting " +
              CPhossPeppolMcp.APP_TITLE +
              " " +
              CPhossPeppolMcp.BUILD_VERSION +
              " on " +
              eNetwork.name () +
              " Network ...");

    // Instantiate tool providers
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

    try
    {
      // The StdioServerTransportProvider reads from stdin in a background thread.
      // Block the main thread to keep the JVM alive until stdin is closed.
      Thread.currentThread ().join ();
    }
    catch (final InterruptedException ex)
    {
      Thread.currentThread ().interrupt ();
    }

    server.close ();

    LOG.info (CPhossPeppolMcp.APP_TITLE + " stopped.");
  }

  public static void main (final String [] args)
  {
    // Redirect picocli output to stderr — stdout is reserved for MCP protocol framing
    final var aCmdLine = new CommandLine (new PhossPeppolMcpServer ());
    aCmdLine.setOut (new PrintWriter (System.err, true));
    aCmdLine.setErr (new PrintWriter (System.err, true));
    System.exit (aCmdLine.execute (args));
  }
}
