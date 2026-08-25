### Run the application:
```
~>$ mvn clean spring-boot:run
```

### MCP Inspector let you inspect an MCP Server for things like:
```

    - Available tools, resources, and prompts
    - Tool input schemas and outputs
    - Server initialization and capabilities
    - Requests/responses while testing tools
    - Errors and protocol behavior

```

### How to run an MCP Inspector with local-mcp-server:
> For the official MCP Inspector, you don't actually need to install it globally. The easiest method is npx. The current package is @modelcontextprotocol/inspector, and the current v2 release requires Node.js 22.19.0 or newer.

#### Install/Update node v-22.19.0 or newer (if nvm installed):
```
~>$ nvm install 22.19.0
~>$ nvm use 22.19.0
~>$ nvm list
```

> Start MCP Inspector:
```
~>$ npx @modelcontextprotocol/inspector
```