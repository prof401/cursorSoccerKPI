# Soccer KPI MVP

Minimal web app to track soccer KPIs during a college game. Frontend is Next.js + TailwindCSS, backend is AWS Lambda (Java) behind API Gateway, with DynamoDB for storage.

## Architecture overview

- **Frontend**: Next.js (TypeScript, TailwindCSS)
- **Backend**: AWS Lambda (Java 21) + API Gateway HTTP API
- **Database**: DynamoDB
  - `games` — basic game metadata
  - `kpi_definitions` — per-game KPI definitions (label + type)
  - `kpi_events` — per-game KPI events (counter deltas or toggle values)

### API surface

- `POST /games` → `createGame`
  - Request: `{ "homeTeam"?: string, "awayTeam"?: string, "kickoffIso"?: string }`
  - Response: `{ "gameId": string, "kpis": KpiDefinition[] }`
- `GET /games/{gameId}/kpis` → `getKpiDefinitions`
  - Response: `{ "kpis": KpiDefinition[] }`
- `POST /games/{gameId}/events` → `recordKpiEvent`
  - Counter event: `{ "kpiId": string, "delta": 1 | -1 }`
  - Toggle event: `{ "kpiId": string, "toggleValue": boolean }`
- `GET /games/{gameId}/summary` → `getGameSummary`
  - Response: `{ "gameId": string, "kpis": KpiSummary[] }`
- `GET /health` → health check (returns `{ "status": "ok" }` for load balancers or deployment checks)

## Running the frontend locally

1. Install dependencies:

   ```bash
   npm install
   ```

2. Set the API base URL (from Terraform output after deploy):

   ```bash
   echo "NEXT_PUBLIC_API_BASE_URL=https://your-api-id.execute-api.your-region.amazonaws.com" > .env.local
   ```

   For the default `$default` stage, the base URL is just the API endpoint from the `http_api_url` Terraform output.

3. Start the dev server:

   ```bash
   npm run dev
   ```

4. Visit:
   - `http://localhost:3000` → create a game, get links
   - `http://localhost:3000/game/{gameId}/track` → player UI
   - `http://localhost:3000/game/{gameId}/dashboard` → coach dashboard

## Building and deploying Lambdas

1. Build the Lambda JAR:

   ```bash
   cd lambda
   mvn clean package
   cd ..
   ```

   This produces `lambda/target/lambda.jar`, which Terraform is configured to upload.

2. Configure AWS credentials (any standard method, e.g. `aws configure`, SSO, or environment variables) and choose a region:

   ```bash
   export AWS_REGION=us-west-2
   ```

3. Deploy infrastructure (DynamoDB tables, Lambda functions, API Gateway HTTP API):

   ```bash
   cd infra
   terraform init
   terraform apply
   cd ..
   ```

   The `infra/` directory is split into `main.tf`, `variables.tf`, `dynamodb.tf`, `iam.tf`, `lambda.tf`, `api_gateway.tf`, and `outputs.tf`. For production, you can use remote state: see `infra/backend.tf.example` for S3 backend setup.

4. After apply completes, note the `http_api_url` output. Use that value for `NEXT_PUBLIC_API_BASE_URL` in `.env.local` of the Next.js app.

## Hosting the frontend (Vercel)

To run the app in production instead of only locally:

1. Push the repo to GitHub and import the project in [Vercel](https://vercel.com).
2. In the Vercel project **Settings → Environment Variables**, add:
   - `NEXT_PUBLIC_API_BASE_URL` = your API base URL (e.g. `https://xxxx.execute-api.us-east-1.amazonaws.com` from Terraform output `http_api_url`).
3. For production, set **CORS** on the API: in Terraform use a variable, e.g. `terraform apply -var='cors_origins=["https://your-app.vercel.app"]'`, or add your production domain to the list.
4. Redeploy the frontend after any change to `NEXT_PUBLIC_API_BASE_URL` (it is baked in at build time).

The repo includes a `vercel.json` so Vercel detects Next.js and runs `npm run build`. No other config is required.

## Local testing vs cloud

For the MVP and lowest operational overhead, the recommended flow is:

- Run the **frontend locally** (`npm run dev`).
- Point it at the **deployed API Gateway URL** in AWS.

If you prefer to run Lambdas locally, you can introduce AWS SAM or LocalStack later and map the same routes to local functions, but that is intentionally omitted here for simplicity and cost.

## Tests

- **Frontend**: `npm run test` (Jest + React Testing Library). Covers home page form and create-game API call (mocked).
- **Lambda**: `cd lambda && mvn test` (JUnit 5). Covers health handler and RecordKpiEvent validation (null body, missing gameId).
- **CI**: GitHub Actions (`.github/workflows/ci.yml`) runs lint, frontend build and test, Lambda tests, and `terraform validate` on push/PR to main or master.

## Cost considerations

- All DynamoDB tables use **on‑demand (PAY_PER_REQUEST)** billing.
- No provisioned concurrency on Lambdas; cold starts are acceptable for this MVP.
- Single HTTP API with four routes keeps API Gateway cost minimal.

