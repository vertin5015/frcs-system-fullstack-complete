# Weekly RAG Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run the weekly core flow for a legal RAG knowledge-base assistant: crawler/search input, local KB retrieval, large-model answer generation, and database design documentation.

**Architecture:** Add a thin backend Agent facade that orchestrates existing services instead of introducing a new framework. The Agent chooses a simple route, optionally triggers case search, queries the local KB, and returns an answer plus trace steps for demo visibility.

**Tech Stack:** Spring Boot 3.4, Java 21, Spring AI ChatClient through the existing service layer, MyBatis/MySQL, Redis, local JSON KB index, JUnit 5/Mockito.

---

### Task 1: Agent Orchestration API

**Files:**
- Create: `legal_cases-master/src/main/java/com/hnu/legal_cases/dto/agent/AgentAskReqVO.java`
- Create: `legal_cases-master/src/main/java/com/hnu/legal_cases/dto/agent/AgentAskResVO.java`
- Create: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/LegalAgentService.java`
- Create: `legal_cases-master/src/main/java/com/hnu/legal_cases/service/impl/LegalAgentServiceImpl.java`
- Create: `legal_cases-master/src/main/java/com/hnu/legal_cases/controller/AgentController.java`
- Test: `legal_cases-master/src/test/java/com/hnu/legal_cases/service/impl/LegalAgentServiceImplTest.java`

- [x] Step 1: Write a failing unit test showing that an Agent question searches fresh cases, queries KB, and returns trace steps.
- [x] Step 2: Run `.\mvnw.cmd -Dtest=LegalAgentServiceImplTest test` from `legal_cases-master` and confirm it fails because Agent classes do not exist.
- [x] Step 3: Implement the DTOs, service interface, service implementation, and controller.
- [x] Step 4: Re-run the focused test and confirm it passes.

### Task 2: Weekly Delivery Documentation

**Files:**
- Create: `docs/数据库设计文档.md`
- Create: `docs/MacOS开发与爬虫脚本说明.md`
- Create: `docs/本周核心流程交付说明.md`

- [x] Step 1: Document the database tables from the SQL scripts and their role in the RAG assistant.
- [x] Step 2: Document backend, crawler, KB, Agent, and model access flow for the weekly demo.
- [x] Step 3: Document MacOS setup differences and crawler/script caveats.

### Task 3: Verification

**Files:**
- No code files expected beyond Task 1.

- [x] Step 1: Run the focused backend test.
- [x] Step 2: Run backend compilation or the Maven test goal.
- [x] Step 3: Report exact verification output and any remaining environment gaps.
