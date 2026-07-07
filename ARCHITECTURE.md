# AgentFlow — Architecture

This document captures the design decisions behind AgentFlow. The goal is to be honest about what was built, why, and what's deliberately incomplete.

## Problem
Engineers spend significant time searching documentation, summarizing PRs, and reviewing code for style consistency. AgentFlow is a small experiment in whether an agentic workflow — an LLM that can call tools and reason about which to use — can compress that work meaningfully without becoming a black box.

The codebase is intentionally compact so the control flow stays readable. Instead of trying to build a general agent platform, the project focuses on one API, one orchestrator, one retrieval path, and one generation path.

## High-level design

```text
POST /agent/run { query, conversationId }
│
▼
AgentController
│
▼
AgentService
├─ StubAgentService ──► Orchestrator
│                       (INIT → PLAN → ACT → RESPOND → DONE)
│                           │
│                           ├─ Planner / NaivePlanner
│                           └─ ToolRegistry
│                               ├─ DocSearchTool ──► Python FAISS sidecar
│                               └─ CodeReviewAssistTool ──► DocSearchTool
└─ BedrockAgentService ──► BedrockClient ──► AWS Bedrock Runtime
│
RetrievalProperties + HttpClientConfig configure the sidecar client
and application.yaml supplies the runtime defaults.
```

The current implementation is split into two execution modes:

- **Stub / local mode** runs the orchestrator, planner, and tool registry.
- **Bedrock mode** skips the local orchestration loop and directly generates text through AWS Bedrock.

Conversation memory is intentionally not fully implemented yet. Redis is part of the future direction, but the current codebase keeps the runtime simple and stateless.

## Key decisions

### Bedrock over direct Anthropic / OpenAI APIs

Chose AWS Bedrock for the LLM layer because it keeps data in the AWS account boundary (no third-party data sharing), integrates with IAM for credential management, and matches the deployment pattern most enterprises actually use for generative AI workloads. Direct API access would have been faster to wire up, but the Bedrock story is closer to how this would ship in production.

### Enum-based state machine, not Spring State Machine

The orchestrator has five states. Spring State Machine is built for state machines with dozens of states, transitions guarded by complex conditions, and persistence requirements. None of that applies here. A `switch` over an enum is compact, easy to test, and easy to replace later if the agent loop grows. Bringing in a framework would be cargo-culting.

The current planner is deliberately naive. It routes review-like prompts to `code_review_assist` and everything else to `doc_search`. That keeps the orchestration behavior deterministic while the LLM-driven planner is still on the roadmap.

### Planner interface, not hard-coded orchestration logic

The planner is separated into a `Planner` interface and a `NaivePlanner` implementation so the project can swap in a model-driven planner later without changing the orchestrator contract. This gives the codebase a seam for future evolution while keeping the current logic small.

### FAISS in a Python sidecar, not in-process

FAISS is a C++ library with first-class Python bindings and no maintained Java wrapper. Options were:

1. JNI wrapper (fragile, hard to deploy, marginal benefit)
2. Lucene HNSW in pure Java (works, but loses the FAISS ecosystem and tooling)
3. Python sidecar exposing an HTTP `/search` endpoint (clean process boundary, easy to swap implementations later)

Picked option 3. The trade-off is one extra container to run and a network hop on every retrieval call. In return, the retrieval layer is fully isolated and could be replaced with a managed vector DB (Pinecone, Weaviate, OpenSearch with k-NN) without touching the Java service.

### Claude Haiku, not Sonnet

Haiku is roughly 10x cheaper per token than Sonnet and fast enough for agent loops. For a portfolio project running under a thousand queries per day, Haiku is the right default. If quality were lacking on harder code review prompts, the swap to Sonnet is one config change.

### Redis for conversation memory

Conversation memory is not fully wired yet. The intended shape is short-lived conversations with append-only turn history, which would fit Redis well. A relational DB would be overkill; an in-memory map would lose state on restart. Until that work lands, the service remains effectively stateless aside from the request/response trace returned to the caller.

## Core Components

### API layer

The API layer is centered on [AgentController](app/src/main/java/com/nakka/agentflow/api/AgentController.java). It exposes `POST /agent/run`, validates the request body, and forwards execution to the agent service abstraction. The request/response types live in [com.nakka.agentflow.api.dto](app/src/main/java/com/nakka/agentflow/api/dto).

### Agent layer

The agent layer owns the branching behavior:

- [AgentService](app/src/main/java/com/nakka/agentflow/agent/AgentService.java) defines the contract.
- [StubAgentService](app/src/main/java/com/nakka/agentflow/agent/StubAgentService.java) runs the local orchestration path.
- [BedrockAgentService](app/src/main/java/com/nakka/agentflow/agent/BedrockAgentService.java) runs the direct Bedrock path.
- [Orchestrator](app/src/main/java/com/nakka/agentflow/agent/Orchestrator.java) implements the local agent loop.
- [Planner](app/src/main/java/com/nakka/agentflow/agent/Planner.java) and [NaivePlanner](app/src/main/java/com/nakka/agentflow/agent/NaivePlanner.java) provide the planning seam.

The orchestrator records state transitions and tool calls so the service can report what happened during execution instead of hiding the whole process behind a single opaque string.

### Tool layer

The tool layer is deliberately small:

- [Tool](app/src/main/java/com/nakka/agentflow/tools/Tool.java) defines a name, description, and executable contract.
- [ToolRegistry](app/src/main/java/com/nakka/agentflow/tools/ToolRegistry.java) discovers and registers all tool beans.
- [DocSearchTool](app/src/main/java/com/nakka/agentflow/tools/DocSearchTool.java) performs retrieval calls.
- [CodeReviewAssistTool](app/src/main/java/com/nakka/agentflow/tools/CodeReviewAssistTool.java) composes retrieval results into code-review feedback.

The tool registry keeps the system extensible without making the orchestration layer know about concrete tool classes.

### LLM layer

[BedrockClient](app/src/main/java/com/nakka/agentflow/llm/BedrockClient.java) wraps AWS Bedrock Runtime. It builds the client from [BedrockProperties](app/src/main/java/com/nakka/agentflow/config/BedrockProperties.java), sends the prompt payload, and extracts the generated text from the Bedrock response body. The direct Bedrock execution path is isolated so generation can be replaced or upgraded without affecting the rest of the application.

### Retrieval layer

The retrieval sidecar in [retrieval-service/main.py](retrieval-service/main.py) loads a FAISS index, loads chunk metadata, embeds the query with SentenceTransformers, and returns ranked search hits. On the Java side, [DocSearchTool](app/src/main/java/com/nakka/agentflow/tools/DocSearchTool.java) uses [RetrievalProperties](app/src/main/java/com/nakka/agentflow/config/RetrievalProperties.java) and [HttpClientConfig](app/src/main/java/com/nakka/agentflow/config/HttpClientConfig.java) to talk to the sidecar over HTTP with explicit timeouts.

### Configuration layer

The runtime defaults live in [app/src/main/resources/application.yaml](app/src/main/resources/application.yaml):

- application name: `agentflow`
- server port: `8080`
- agent mode: `stub`
- retrieval base URL: `http://localhost:8001`
- retrieval top-K: `4`
- Bedrock region: `us-east-1`
- Bedrock model: `anthropic.claude-3-haiku-20240307-v1:0`
- Bedrock max tokens: `1024`
- Bedrock temperature: `0.3`

The configuration package keeps those external settings typed and isolated from the business logic.

### Observability and request shape

The service returns latency and tool usage metadata in [AgentRunResponse](app/src/main/java/com/nakka/agentflow/api/dto/AgentRunResponse.java). That makes it easier to inspect behavior from the outside without attaching a debugger or reading internal logs.

## Runtime Flow

1. The client sends `POST /agent/run`.
2. The controller validates and forwards the request.
3. The selected agent service executes either the local orchestration path or the Bedrock path.
4. In stub mode, the orchestrator plans a tool, executes it, and shapes a response.
5. In Bedrock mode, the service calls Bedrock directly and returns the generated output.
6. Retrieval-backed tools call the Python sidecar over HTTP when grounding is required.

This is intentionally a narrow runtime model. The point is to make the control flow understandable, not to build a universal agent platform.

## Testing

The repository includes Spring Boot context coverage and agent-oriented tests under [app/src/test/java/com/nakka/agentflow](app/src/test/java/com/nakka/agentflow). The testing focus is on startup, orchestration, and service wiring rather than on broad end-to-end automation.

The current validation shape is:

- Spring context startup checks
- orchestrator behavior checks
- Bedrock and stub service coverage
- retrieval-sidecar behavior through HTTP-oriented tool tests

## What's intentionally out of scope

- **Multi-user auth.** No login system; the API is open. Production would need OAuth/JWT and per-user rate limiting.
- **Eval framework.** Currently no automated quality scoring. A roadmap item below.
- **Streaming responses.** All responses are batched. SSE/WebSocket streaming is planned.
- **Prompt injection defenses.** Basic input length checks only. A real production deployment needs structured input validation, output filtering, and detection of jailbreak patterns.
- **Cost tracking.** Bedrock invocations are not metered per request in the app today; CloudWatch metrics are the workaround.
- **Conversation memory.** Redis-backed memory is still a future enhancement.

## Roadmap

Order roughly reflects priority.

1. Eval harness: 50 golden queries with human-rated answers, automated regression on each commit.
2. Streaming responses via SSE.
3. Migration of FAISS sidecar to a real managed vector store (OpenSearch with k-NN is the most likely target since it's also AWS-native).
4. Per-tool latency dashboards and Bedrock cost metrics in CloudWatch.
5. Prompt injection test suite.

Longer-term, the most likely evolution is:

- replace the keyword-based planner with a model-driven planner
- finish the grounded code-review response path
- add a real conversation-memory store
- replace the FAISS sidecar with a managed vector store if the deployment story needs it

## Build log

- Friday: Project scaffolding, API contract, Spring Boot health endpoint, validation working.
- Current state: README and architecture docs updated to reflect the implemented code paths, the planner seam, and the current roadmap.