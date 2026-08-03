# Backend Agent and Crawler Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing legal RAG demo degrade cleanly across crawler, model, and SSE failures without changing its core workflow.

**Architecture:** Keep the current backend bridge and local KB. Add small, testable helpers at the existing boundaries: response classification in the official-source bridge, a timeout runner for selected model calls, idempotent stream cleanup, tolerant date parsing, and masked auth logging.

**Tech Stack:** Java 21, Spring Boot, Spring AI, Jsoup, JUnit 5, AssertJ, Mockito, Maven.

---

### Task 1: Add regression tests for observed failures

**Files:**
- Modify: `legal_cases-master/src/test/java/com/hnu/legal_cases/service/impl/CaseCacheServiceImplTest.java`
- Modify: `legal_cases-master/src/test/java/com/hnu/legal_cases/service/AiCallRunnerTest.java`
- Modify: `legal_cases-master/src/test/java/com/hnu/legal_cases/service/EuJpLegalSearchBridgeServiceTest.java`

- [ ] Add tests that require ISO and slash date formats to produce a non-zero sortable score.
- [ ] Add a timeout test proving a slow callable raises a timeout instead of waiting indefinitely.
- [ ] Add a CourtListener response test proving status 202 is classified as deferred rather than a normal HTML page.
- [ ] Run the focused tests and confirm they fail because the helpers do not yet exist.

### Task 2: Implement bounded model calls and crawler fallbacks

**Files:**
- Create: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/AiCallRunner.java`
- Modify: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/impl/SpringAIServiceImpl.java`
- Modify: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/impl/LocalKbServiceImpl.java`
- Modify: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/EuJpLegalSearchBridgeService.java`
- Modify: `legal_cases-master/src/main/resources/application.yml`

- [ ] Add a configurable bounded call with a 15-second default.
- [ ] Use it for translation and RAG answer generation, preserving existing local fallbacks.
- [ ] Represent CourtListener HTTP 202 responses as deferred and return an empty detail result without treating the source as a backend crash.
- [ ] Keep normal Japan and ordinary HTML detail parsing unchanged.
- [ ] Run focused tests and then the full backend tests.

### Task 3: Make SSE completion and lock cleanup idempotent

**Files:**
- Modify: `legal_cases-master/src/main/java/com/hnu/legal_cases/controller/CasesController.java`
- Modify: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/impl/CaseCacheServiceImpl.java`

- [ ] Guard stream sends after completion or client disconnect.
- [ ] Release the distributed lock at most once per stream.
- [ ] Accept both `yyyy-MM-dd` and `yyyy/MM/dd` when scoring cached cases.
- [ ] Add or update focused tests, then run the full test suite.

### Task 4: Remove sensitive verification-code logging

**Files:**
- Modify: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/impl/AuthCodeServiceImpl.java`

- [ ] Log only a masked email and purpose; never log the generated code.
- [ ] Run compile and tests.

### Task 5: Verify and push

- [ ] Run Maven tests with the repository settings workaround if needed.
- [ ] Run backend compile and frontend build.
- [ ] Run `git diff --check` and inspect the diff, excluding existing user files.
- [ ] Commit only the implementation, tests, and this design/plan documentation.
- [ ] Push `main` to `origin` with a descriptive commit message.
