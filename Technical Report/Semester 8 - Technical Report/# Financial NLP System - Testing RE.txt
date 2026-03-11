# Financial NLP System - Testing README

This document explains all tests currently in the project:
- what each test does,
- why it is used,
- and how to run it.

## 1. Testing Overview

The project currently uses three testing layers:

1. Frontend component tests (React Testing Library + Jest).
2. Backend API/unit/integration tests (Jest + Supertest).
3. NLP rule evaluation runs (ablation study script).

## 2. Test Files and Purpose

### Frontend tests (`client/src/components/*.test.js`)

1. `client/src/components/Login.test.js`
- What it tests: login submit success and login error display.
- Why it is used: confirms users can sign in and see clear feedback on failure.

2. `client/src/components/Register.test.js`
- What it tests: client-side password validation and successful registration callback.
- Why it is used: prevents weak-password submissions and verifies registration flow.

3. `client/src/components/UploadDocument.test.js`
- What it tests: upload button disabled when no file exists, and upload trigger when a file is selected.
- Why it is used: ensures safe upload UX and avoids accidental empty submissions.

### Backend tests (`server/tests/*.test.js`)

1. `server/tests/auth.test.js`
- What it tests: register/login success and rejection paths (weak password, duplicate user, invalid credentials).
- Why it is used: protects core authentication and account security behavior.

2. `server/tests/users.test.js`
- What it tests: role-based access control for user listing/deletion.
- Why it is used: ensures admin-only actions stay restricted.

3. `server/tests/documents.test.js`
- What it tests: upload, retrieval, deletion, auth checks, file type checks, and max-size checks.
- Why it is used: protects the full document lifecycle and file safety rules.

4. `server/tests/auditFlags.test.js`
- What it tests: RAG classification and risk rule outputs from financial text.
- Why it is used: validates consistency of automated financial risk flagging.

5. `server/tests/nerAccuracy.test.js`
- What it tests: NER precision/recall/F1 on labeled sample texts.
- Why it is used: measures extraction quality and tracks NLP quality changes.

6. `server/tests/summarizationExplainability.test.js`
- What it tests: summary evaluation contract, decision trace completeness, and audit evidence presence.
- Why it is used: ensures NLP results are explainable and auditable.

7. `server/tests/nlpProcessingFailure.test.js`
- What it tests: fallback behavior when the Python NLP microservice fails.
- Why it is used: confirms backend stays resilient and still writes safe defaults.

8. `server/tests/e2eSmoke.test.js`
- What it tests: end-to-end smoke flow (`register -> login -> upload -> list -> nlp status`).
- Why it is used: verifies major API journey works as one connected flow.

### Evaluation test script (rule impact)

1. `server/scripts/runAblationStudy.js`
- What it tests: baseline versus rule-disabled variants for audit rules.
- Why it is used: measures rule value (precision/recall/F1 and RAG accuracy) before changing production logic.

## 3. How to Run Tests

## Root setup

Run these once from project root:

```bash
npm install
npm --prefix client install
npm --prefix server install
```

### Run frontend tests

From project root:

```bash
npm --prefix client test
```

Run only the current frontend test files once:

```bash
npm --prefix client test -- --watchAll=false --runTestsByPath src/components/Login.test.js src/components/Register.test.js src/components/UploadDocument.test.js
```

### Run all backend tests

From project root:

```bash
npm --prefix server test
```

### Run specific backend tests

```bash
npm --prefix server test -- tests/auth.test.js
npm --prefix server test -- tests/users.test.js
npm --prefix server test -- tests/documents.test.js
npm --prefix server test -- tests/auditFlags.test.js
npm --prefix server test -- tests/nerAccuracy.test.js
npm --prefix server test -- tests/summarizationExplainability.test.js
npm --prefix server test -- tests/nlpProcessingFailure.test.js
npm --prefix server test -- tests/e2eSmoke.test.js
```

### Run ablation study

From project root:

```bash
npm run ablation
```

This writes output files to `server/results/ablation/`.

## 4. Test Dependencies and Notes

1. `server/tests/nerAccuracy.test.js` needs the Python NLP service running on `http://127.0.0.1:8000`.
2. Most backend route tests use local SQLite test data created during test execution.
3. The backend Jest command uses `--detectOpenHandles --forceExit` (defined in `server/package.json`) to reduce hanging test runs.
4. Frontend tests use component-level mocks where needed for fast and stable behavior checks.

## 5. Why This Test Suite Matters

1. It protects critical user journeys: authentication, upload, and document access.
2. It enforces security and permission rules (auth + admin-only restrictions).
3. It validates NLP quality, explainability, and failure resilience.
4. It gives a quick smoke path to catch regressions early.
5. It provides rule-level evidence before changing financial risk logic.

## 6. Suggested Test Order (Fast to Deep)

When validating changes quickly:

1. Frontend component tests (`client`).
2. Backend smoke + targeted backend file tests (`server`).
3. Full backend test run.
4. Ablation study for rule-related NLP changes.
