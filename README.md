# Indy Generic Proxy Service

A high-performance HTTP proxy service built on Quarkus that tracks and records external resource accesses during builds for non-Maven, non-NPM files. The service automatically creates Indy remote repositories based on external URLs and ensures all external dependencies are properly tracked and cached, preventing loss of build dependency information.

## Architecture

The service is built with a modular architecture:

- **Core Proxy Engine**: XNIO-based async HTTP proxy server (`HttpProxy.java`)
- **Request Handlers**: `ProxyAcceptHandler` manages incoming connections and routing
- **MITM SSL Support**: `ProxyMITMSSLServer` enables SSL interception and certificate generation
- **Repository Integration**: REST client services for Indy repository management
- **Authentication**: Keycloak integration with bearer token support
- **Observability**: OpenTelemetry tracing and metrics

## Key Features

- **Build Dependency Tracking**: Records all external resource accesses during builds with build ID association
- **Automatic Repository Creation**: Dynamically creates Indy remote repositories based on external host URLs
- **MITM SSL Proxy**: Intercept and proxy HTTPS traffic with custom CA certificates for complete tracking
- **Content Caching**: Intelligent caching with configurable storage strategies to avoid duplicate downloads
- **Repository Management**: Dynamic repository creation and content retrieval via Indy API
- **Authentication**: Keycloak OIDC integration with configurable security
- **High Performance**: Async I/O with configurable worker threads and connection pooling
- **Observability**: Built-in OpenTelemetry tracing and Prometheus metrics

## Technology Stack

- **Runtime**: Quarkus 2.3.0 with Java 11
- **I/O**: XNIO for high-performance async networking
- **HTTP Client**: OkHttp 4.9.2 for repository communication
- **Authentication**: Keycloak 22.0.3 for OIDC/OAuth2
- **Observability**: OpenTelemetry SDK with OTLP export
- **Caching**: Caffeine cache with configurable eviction

## Prerequisites

- JDK 11+
- Maven 3.6.2+
- Running Indy instance (for repository operations)

## Configuration

Key configuration options in `application.yaml`:

```yaml
proxy:
  port: 8082                    # Proxy server port
  secured: true                 # Enable authentication
  worker:
    io.threads: 10              # I/O worker threads
    task.threads: 10            # Task worker threads

MITM:
  enabled: true                 # Enable MITM SSL
  ca.key: /tmp/ssl/ca.der      # CA private key
  ca.cert: /tmp/ssl/ca.crt     # CA certificate

service_proxy:
  services:
    - host: localhost           # Target Indy instance
      port: 8080
      path-pattern: /api/.+     # URL pattern matching
```

## How It Works

The proxy service solves the problem of lost dependency tracking in non-Maven/non-NPM builds by intercepting all external resource requests:

### Build Dependency Tracking Workflow

1. **Client Request**: Build process sends HTTP/HTTPS request to external resource
2. **Proxy Interception**: Request is intercepted by the proxy service
3. **Build ID Association**: External URL is recorded and associated with the current build ID
4. **Repository Check**: Proxy checks if an Indy remote repository exists for the external host
5. **Repository Creation**: If not found, automatically creates a new remote repository for the host
6. **Content Retrieval**: Fetches the resource from the external URL
7. **Content Storage**: Stores the content in the appropriate Indy repository
8. **Response**: Returns the content to the client

### Benefits

- **Complete Dependency Tracking**: No external resource access goes unrecorded
- **Automatic Repository Management**: No manual repository configuration needed
- **Build Reproducibility**: All dependencies are cached and versioned
- **Security**: MITM SSL support ensures even HTTPS resources are tracked
- **Performance**: Intelligent caching reduces duplicate downloads

## Quick Start

1. **Build the project**:
```bash
git clone https://github.com/Commonjava/indy-generic-proxy-service.git
cd indy-generic-proxy-service
mvn clean compile
```

2. **Configure Indy connection** in `application.yaml`:
```yaml
repo-service-api/mp-rest/uri: http://localhost:8080
```

3. **Start in development mode**:
```bash
mvn quarkus:dev
```

4. **Configure your build environment** to use the proxy:
```bash
export http_proxy=http://localhost:8082
export https_proxy=http://localhost:8082
# For builds that support proxy configuration
```

5. **Run your build** - all external resource accesses will be automatically tracked and cached

## Development

### Key Components

- **`HttpProxy`**: Main application class that starts the XNIO-based proxy server
- **`ProxyAcceptHandler`**: Handles incoming connections and manages the tracking workflow
- **`ProxyMITMSSLServer`**: Manages SSL interception and certificate generation for HTTPS tracking
- **`RepositoryService`**: REST client for Indy repository operations and automatic repository creation
- **`ContentRetrievalService`**: Handles content retrieval, caching, and build ID association
- **`ProxyResponseHelper`**: Manages the complete tracking workflow from request to response
- **`TrackingKey`**: Associates external URLs with build IDs for dependency tracking

### Testing

Run the test suite:
```bash
mvn test
```

The project includes comprehensive tests for proxy functionality, SSL handling, and repository integration.

## Production Deployment

### Docker Support

Multiple Dockerfile variants are available:
- `Dockerfile.jvm`: Standard JVM deployment
- `Dockerfile.native`: Native compilation with GraalVM
- `Dockerfile.native-distroless`: Minimal distroless image

### Monitoring

The service exposes:
- Health checks at `/q/health`
- Metrics at `/q/metrics` (Prometheus format)
- OpenTelemetry traces (configurable endpoints)

### Security Considerations

- Configure proper CA certificates for MITM functionality
- Set up Keycloak authentication for production
- Monitor proxy logs for security events
