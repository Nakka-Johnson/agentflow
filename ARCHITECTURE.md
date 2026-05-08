# AgentFlow — Architecture

This document captures the design decisions behind AgentFlow. The goal is to be honest about what was built, why, and what's deliberately incomplete.

## Problem
Engineers spend significant time searching documentation, summarizing PRs, and reviewing code for style consistency. AgentFlow is a small experiment in whether an agentic workflow — an LLM that can call tools and reason about which to use — can compress that work meaningfully without becoming a black box.

## High-level design

```text
POST /agent/run { query, conversationId }
│
▼
AgentOrchestrator
(enum-based state machine: INIT → PLAN → ACT → RESPOND → DONE)
│
┌────────┼────────────┐
▼        ▼            ▼
Bedrock   ToolRegistry   Redis
(Claude)  ┌──────────┐   (memory)
          │ DocSearch│
          │CodeReview│
          └─────┬────┘
                │ HTTP
                ▼
          Python FAISS sidecar
          (retrieval over docs)
```

## Key decisions

### Bedrock over direct Anthropic / OpenAI APIs

Chose AWS Bedrock for the LLM layer because it keeps data in the AWS account boundary (no third-party data sharing), integrates with IAM for credential management, and matches the deployment pattern most enterprises actually use for generative AI workloads. Direct API access would have been faster to wire up, but the Bedrock story is closer to how this would ship in production.

### Enum-based state machine, not Spring State Machine

The orchestrator has four states. Spring State Machine is built for state machines with dozens of states, transitions guarded by complex conditions, and persistence requirements. None of that applies here. A `switch` over an enum is ~30 lines and easier to reason about. Bringing in a framework would be cargo-culting.

### FAISS in a Python sidecar, not in-process

FAISS is a C++ library with first-class Python bindings and no maintained Java wrapper. Options were:

1. JNI wrapper (fragile, hard to deploy, marginal benefit)
2. Lucene HNSW in pure Java (works, but loses the FAISS ecosystem and tooling)
3. Python sidecar exposing an HTTP `/search` endpoint (clean process boundary, easy to swap implementations later)

Picked option 3. The trade-off is one extra container to run and a network hop on every retrieval call. In return, the retrieval layer is fully isolated and could be replaced with a managed vector DB (Pinecone, Weaviate, OpenSearch with k-NN) without touching the Java service.

### Claude Haiku, not Sonnet

Haiku is roughly 10x cheaper per token than Sonnet and fast enough for agent loops. For a portfolio project running under a thousand queries per day, Haiku is the right default. If quality were lacking on harder code review prompts, the swap to Sonnet is one config change.

### Redis for conversation memory

Conversations are short-lived (TTL of a few hours), structure is simple (an append-only list of turns per conversation ID), and access patterns are read-heavy. Redis fits exactly. A relational DB would be overkill; an in-memory map would lose state on restart.

## Tools

### DocSearch
Takes a natural-language query, embeds it via Titan v2, calls the FAISS sidecar's `/search` endpoint, returns the top-K chunks with metadata (source URL, section heading).

### CodeReviewAssist
Takes a diff and a style-guide hint, internally calls DocSearch to retrieve relevant guidelines, then asks Claude to produce structured review comments (file, line, severity, comment) grounded in the retrieved context.

## What's intentionally out of scope

- **Multi-user auth.** No login system; the API is open. Production would need OAuth/JWT and per-user rate limiting.
- **Eval framework.** Currently no automated quality scoring. A roadmap item below.
- **Streaming responses.** All responses are batched. SSE/WebSocket streaming is planned.
- **Prompt injection defenses.** Basic input length checks only. A real production deployment needs structured input validation, output filtering, and detection of jailbreak patterns.
- **Cost tracking.** Bedrock invocations are not metered per request in the app today; CloudWatch metrics are the workaround.

## Roadmap

Order roughly reflects priority.

1. Eval harness: 50 golden queries with human-rated answers, automated regression on each commit.
2. Streaming responses via SSE.
3. Migration of FAISS sidecar to a real managed vector store (OpenSearch with k-NN is the most likely target since it's also AWS-native).
4. Per-tool latency dashboards and Bedrock cost metrics in CloudWatch.
5. Prompt injection test suite.