# API Mocking (MSW) for Frontend Tests

Frontend component tests run in isolation using **Mock Service Worker (MSW)**.

## How it works

- `src/test/setup.ts` starts an MSW server before all tests.
- Every test resets handlers after execution.
- The default handlers return deterministic responses for commonly used `/api/v1/**` endpoints.
- The catch-all handler prevents any real network calls for unknown `/api/v1/*` routes.

## Adding a new mocked endpoint

1. Edit `src/test/mocks/handlers.ts`.
2. Add a new handler using `http.get/http.post/...`.
3. If your endpoint needs custom behavior, export a helper (see examples below) and override with `server.use(...)` in a
   specific test.

## Overriding handlers per test

Example:

```ts
import { server } from '@/test/mocks/server';
import { mockErrorHandler } from '@/test/mocks/handlers';

it('shows API error', async () => {
  server.use(mockErrorHandler('/groups', 500));
  // ...render component and assert
});
```
