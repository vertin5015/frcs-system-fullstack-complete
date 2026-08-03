# Backend Agent and Crawler Reliability Design

## Goal

Keep the course-demo path usable when an upstream crawler, model endpoint, or browser SSE connection is unavailable.

## Design

- CourtListener detail pages returning HTTP 202 are treated as a deferred anti-bot response. The search result remains usable, and detail ingestion is skipped without a stack trace storm. A normal HTML detail page is still parsed as before.
- Model calls used by keyword translation and RAG answer generation run through a bounded timeout and fall back to the existing local behavior. Case summaries retain their existing fallback behavior.
- SSE callbacks use one terminal state and one lock-release state. Once the browser disconnects or the stream completes, later crawler callbacks are ignored.
- Cache date parsing accepts both `yyyy-MM-dd` and `yyyy/MM/dd`.
- Authentication-code logs redact the email and never print the code value.

## Scope

This does not add a complex planning Agent, a new vector database, or a new crawler service. Eur-Lex remains an optional data source and may return no results when its WAF blocks server-side requests.

## Verification

Add focused unit tests for date parsing, bounded model execution, CourtListener deferred responses, and SSE terminal-state behavior where practical. Run the Maven test suite, backend compile, frontend build, and `git diff --check` before pushing.
