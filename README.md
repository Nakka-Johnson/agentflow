# AgentFlow

AgentFlow is a Java 21 and Spring Boot 3.5 agentic developer-assistance service. It exposes a single HTTP API that accepts a developer query, chooses an execution path, optionally retrieves grounded context, and returns a structured response with trace information.

The project is intentionally small at the orchestration layer, but it still models a realistic multi-service setup: a Spring Boot backend, a Python retrieval sidecar, AWS Bedrock generation, and a pluggable tool registry. The goal is to explore how far a compact, explainable agent loop can go without introducing a heavyweight workflow engine or a large framework stack.

## Technical Overview

The main API is `POST /agent/run`, implemented in the Spring controller layer. Requests carry a `query` and optional `conversationId`, and responses include the generated answer, the conversation ID, tool call summaries, and end-to-end latency. That keeps the integration simple while still exposing enough trace data to understand how a response was produced.

The runtime is organized around a small agent flow:

1. The request reaches the API layer.
2. The agent service chooses an execution mode.
3. The orchestrator or Bedrock-backed path handles planning and generation.
4. Tools are invoked when retrieval or code-review grounding is needed.
5. The final response is returned with latency and tool metadata.

Two agent modes are already present in the codebase:

- **Stub / local mode** uses a small enum-based orchestrator with keyword-based planning.
- **Bedrock mode** uses AWS Bedrock for direct generation through the AWS SDK.

The tool layer is pluggable through a registry. The current tool contract is defined in [Tool.java](app/src/main/java/com/nakka/agentflow/tools/Tool.java), and tools are registered automatically by [ToolRegistry.java](app/src/main/java/com/nakka/agentflow/tools/ToolRegistry.java). The current tools are:

- **DocSearch** - semantic retrieval over engineering documentation using a Python FAISS sidecar.
- **CodeReviewAssist** - retrieves review guidance via DocSearch and produces structured review feedback.

The current local planner is split into a `Planner` interface and a `NaivePlanner` implementation. The heuristic planner routes review-like prompts to `code_review_assist` and everything else to `doc_search`. In the current codebase, that gives the project a clear path for replacing the planner later without changing the orchestration or tool interfaces.

The retrieval sidecar is a separate FastAPI service in [retrieval-service/main.py](retrieval-service/main.py). It loads a FAISS index and metadata at startup, computes embeddings with SentenceTransformers, and exposes `POST /search` for top-K similarity search. The Java service calls it over HTTP, which keeps retrieval isolated and easy to swap later.

At the application boundary, the Spring controller delegates to [AgentService.java](app/src/main/java/com/nakka/agentflow/agent/AgentService.java), which currently has two concrete implementations:

- [StubAgentService.java](app/src/main/java/com/nakka/agentflow/agent/StubAgentService.java) for the local/orchestrated path.
- [BedrockAgentService.java](app/src/main/java/com/nakka/agentflow/agent/BedrockAgentService.java) for the direct Bedrock generation path.

The orchestrator itself lives in [Orchestrator.java](app/src/main/java/com/nakka/agentflow/agent/Orchestrator.java). It uses an explicit enum-based state machine with the states `INIT`, `PLAN`, `ACT`, `RESPOND`, and `DONE`. The implementation is deliberately compact: the code is easy to reason about, easy to test, and easy to replace later if the agent loop becomes more sophisticated.

## Architecture

The repository is organized around a clear separation of concerns:

- API layer: request validation and response shaping.
- Agent layer: orchestration, planning, and execution mode selection.
- Tool layer: documentation retrieval and code-review assistance.
- LLM layer: AWS Bedrock client integration.
- Retrieval layer: Python FAISS sidecar.
- Configuration layer: typed configuration properties and HTTP client setup.

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the design decisions, trade-offs, and what is intentionally out of scope.

The current Java package structure is:

- [com.nakka.agentflow.api](app/src/main/java/com/nakka/agentflow/api) for the HTTP controller.
- [com.nakka.agentflow.api.dto](app/src/main/java/com/nakka/agentflow/api/dto) for request and response records.
- [com.nakka.agentflow.agent](app/src/main/java/com/nakka/agentflow/agent) for orchestrator, planner, and service implementations.
- [com.nakka.agentflow.tools](app/src/main/java/com/nakka/agentflow/tools) for the tool contract, registry, and concrete tools.
- [com.nakka.agentflow.llm](app/src/main/java/com/nakka/agentflow/llm) for Bedrock integration.
- [com.nakka.agentflow.config](app/src/main/java/com/nakka/agentflow/config) for typed configuration and HTTP client setup.

## Stack

Java 21, Spring Boot 3.5, AWS Bedrock, FastAPI, FAISS, SentenceTransformers, Maven, JUnit 5, Spring Boot test support, and Lombok.

## Status

The core wiring is in place: API, orchestrator, tool registry, retrieval sidecar, typed configuration, and Bedrock client. The current planner is still heuristic, the code-review tool still contains a stubbed response shape, and the longer-term roadmap is to replace the naive planner with model-driven tool selection, add evaluation harnesses, and expand production hardening.

The service defaults are defined in [app/src/main/resources/application.yaml](app/src/main/resources/application.yaml):

- Server port: `8080`
- Retrieval base URL: `http://localhost:8001`
- Default retrieval top-K: `4`
- Default agent mode: `stub`
- Bedrock region: `us-east-1`
- Bedrock model: `anthropic.claude-3-haiku-20240307-v1:0`
- Bedrock max tokens: `1024`
- Bedrock temperature: `0.3`

The management endpoint exposure is also intentionally minimal: health, info, and metrics.

## Running locally

The repo currently runs as two cooperating services:

1. Start the Spring Boot app from [app](app) on port `8080`.
2. Start the retrieval sidecar from [retrieval-service](retrieval-service) on port `8001`.

The Java service expects the retrieval service to be available at the configured base URL. The retrieval service expects a prebuilt FAISS index and metadata file at startup.

Recommended local commands:

- From [app](app): `./mvnw test` or `mvnw.cmd test`
- From [retrieval-service](retrieval-service): create the Python environment and run the FastAPI app separately

For the broader build order and local setup notes, see [ARCHITECTURE.md](./ARCHITECTURE.md).

## Testing

The repository already includes a Spring Boot context test plus agent-focused tests under [app/src/test/java/com/nakka/agentflow](app/src/test/java/com/nakka/agentflow). The current testing strategy is centered on fast unit and slice-level checks around the orchestrator, service implementations, and startup wiring.

## Roadmap

The roadmap in [ARCHITECTURE.md](./ARCHITECTURE.md) is still the source of truth, but the main next steps are:

- replace the keyword planner with model-driven tool selection
- finish the grounded code-review path
- add an evaluation harness with golden queries
- add streaming responses
- add stronger prompt-injection defenses
- improve observability and cost tracking
- consider a managed vector store later if the FAISS sidecar is retired

## Repository Notes

The workspace currently contains the README update and two planner-related Java files in the agent package. They belong to the same project state and are included in the final commit so the repository history stays consistent.

## Author

Nakka Johnson · [LinkedIn](https://linkedin.com/in/nakka-johnson) · [GitHub](https://github.com/Nakka-Johnson)
