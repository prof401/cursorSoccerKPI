package com.soccerkpi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Single entry class hosting all HTTP API Lambda handlers.
 *
 * Handlers are referenced using method references in Terraform, e.g.:
 *   com.soccerkpi.Handlers::createGame
 */
public class Handlers {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final DynamoDbClient DDB = DynamoDbClient.builder()
            .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-west-2")))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

    private static final String GAMES_TABLE = System.getenv("GAMES_TABLE");
    private static final String KPI_DEFINITIONS_TABLE = System.getenv("KPI_DEFINITIONS_TABLE");
    private static final String KPI_EVENTS_TABLE = System.getenv("KPI_EVENTS_TABLE");

    // ----- Public handler entry points -----

    public static APIGatewayV2HTTPResponse createGame(APIGatewayV2HTTPEvent event, Context context) {
        return new CreateGameHandler().handleRequest(event, context);
    }

    public static APIGatewayV2HTTPResponse getKpiDefinitions(APIGatewayV2HTTPEvent event, Context context) {
        return new GetKpiDefinitionsHandler().handleRequest(event, context);
    }

    public static APIGatewayV2HTTPResponse recordKpiEvent(APIGatewayV2HTTPEvent event, Context context) {
        return new RecordKpiEventHandler().handleRequest(event, context);
    }

    public static APIGatewayV2HTTPResponse getGameSummary(APIGatewayV2HTTPEvent event, Context context) {
        return new GetGameSummaryHandler().handleRequest(event, context);
    }

    public static APIGatewayV2HTTPResponse health(APIGatewayV2HTTPEvent event, Context context) {
        return new HealthHandler().handleRequest(event, context);
    }

    // ----- Handlers -----

    public static class HealthHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
        @Override
        public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
            long start = System.currentTimeMillis();
            String requestId = context != null ? context.getAwsRequestId() : null;
            try {
                APIGatewayV2HTTPResponse res = okJson(Collections.singletonMap("status", "ok"));
                logStructured(requestId, "health", null, "ok", 200, System.currentTimeMillis() - start, null, null);
                return res;
            } catch (Exception e) {
                logStructured(requestId, "health", null, "error", 500, System.currentTimeMillis() - start, e.getClass().getSimpleName(), e.getMessage());
                return errorJson(500, "Health check failed");
            }
        }
    }

    public static class CreateGameHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
        @Override
        public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
            long start = System.currentTimeMillis();
            String requestId = context != null ? context.getAwsRequestId() : null;
            try {
                CreateGameRequest request;
                if (event.getBody() != null && !event.getBody().isEmpty()) {
                    request = OBJECT_MAPPER.readValue(event.getBody(), CreateGameRequest.class);
                } else {
                    request = new CreateGameRequest();
                }

                String gameId = UUID.randomUUID().toString();
                Game game = new Game(
                        gameId,
                        request.getHomeTeam(),
                        request.getAwayTeam(),
                        request.getKickoffIso(),
                        "CREATED"
                );

                // persist game
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("gameId", AttributeValue.builder().s(game.getGameId()).build());
                item.put("homeTeam", AttributeValue.builder().s(nullToEmpty(game.getHomeTeam())).build());
                item.put("awayTeam", AttributeValue.builder().s(nullToEmpty(game.getAwayTeam())).build());
                if (game.getKickoffIso() != null) {
                    item.put("kickoffIso", AttributeValue.builder().s(game.getKickoffIso()).build());
                }
                item.put("status", AttributeValue.builder().s(game.getStatus()).build());

                DDB.putItem(PutItemRequest.builder()
                        .tableName(GAMES_TABLE)
                        .item(item)
                        .build());

                // seed default KPI definitions for this game
                List<KpiDefinition> defaults = DefaultKpis.defaultKpisForGame(gameId);
                for (KpiDefinition def : defaults) {
                    Map<String, AttributeValue> defItem = new HashMap<>();
                    defItem.put("gameId", AttributeValue.builder().s(def.getGameId()).build());
                    defItem.put("kpiId", AttributeValue.builder().s(def.getKpiId()).build());
                    defItem.put("label", AttributeValue.builder().s(def.getLabel()).build());
                    defItem.put("type", AttributeValue.builder().s(def.getType().name()).build());
                    DDB.putItem(PutItemRequest.builder()
                            .tableName(KPI_DEFINITIONS_TABLE)
                            .item(defItem)
                            .build());
                }

                CreateGameResponse response = new CreateGameResponse(gameId, defaults);
                logStructured(requestId, "createGame", gameId, "ok", 200, System.currentTimeMillis() - start, null, null);
                return okJson(response);
            } catch (Exception e) {
                logStructured(requestId, "createGame", null, "error", 500, System.currentTimeMillis() - start, e.getClass().getSimpleName(), e.getMessage());
                return errorJson(500, "Failed to create game: " + e.getMessage());
            }
        }
    }

    public static class GetKpiDefinitionsHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
        @Override
        public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
            long start = System.currentTimeMillis();
            String requestId = context != null ? context.getAwsRequestId() : null;
            try {
                String gameId = pathParam(event, "gameId");
                if (gameId == null || gameId.isEmpty()) {
                    logStructured(requestId, "getKpiDefinitions", null, "error", 400, System.currentTimeMillis() - start, "Validation", "Missing gameId in path");
                    return errorJson(400, "Missing gameId in path");
                }

                List<KpiDefinition> defs = loadKpisForGame(gameId);
                logStructured(requestId, "getKpiDefinitions", gameId, "ok", 200, System.currentTimeMillis() - start, null, null);
                return okJson(Collections.singletonMap("kpis", defs));
            } catch (Exception e) {
                String gameId = pathParam(event, "gameId");
                logStructured(requestId, "getKpiDefinitions", gameId, "error", 500, System.currentTimeMillis() - start, e.getClass().getSimpleName(), e.getMessage());
                return errorJson(500, "Failed to load KPI definitions: " + e.getMessage());
            }
        }
    }

    public static class RecordKpiEventHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
        @Override
        public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
            long start = System.currentTimeMillis();
            String requestId = context != null ? context.getAwsRequestId() : null;
            String gameId = pathParam(event, "gameId");
            try {
                if (gameId == null || gameId.isEmpty()) {
                    logStructured(requestId, "recordKpiEvent", null, "error", 400, System.currentTimeMillis() - start, "Validation", "Missing gameId in path");
                    return errorJson(400, "Missing gameId in path");
                }

                String body = event.getBody();
                if (body == null || body.trim().isEmpty()) {
                    logStructured(requestId, "recordKpiEvent", gameId, "error", 400, System.currentTimeMillis() - start, "Validation", "Request body is required");
                    return errorJson(400, "Request body is required");
                }

                RecordKpiEventRequest request = OBJECT_MAPPER.readValue(body, RecordKpiEventRequest.class);
                if (request.getKpiId() == null || request.getKpiId().isEmpty()) {
                    logStructured(requestId, "recordKpiEvent", gameId, "error", 400, System.currentTimeMillis() - start, "Validation", "kpiId is required");
                    return errorJson(400, "kpiId is required");
                }
                if (request.getDelta() != null && request.getDelta() != 1 && request.getDelta() != -1) {
                    logStructured(requestId, "recordKpiEvent", gameId, "error", 400, System.currentTimeMillis() - start, "Validation", "delta must be 1 or -1");
                    return errorJson(400, "delta must be 1 or -1 for counter events");
                }
                if (request.getDelta() != null && request.getToggleValue() != null) {
                    logStructured(requestId, "recordKpiEvent", gameId, "error", 400, System.currentTimeMillis() - start, "Validation", "Provide delta or toggleValue not both");
                    return errorJson(400, "Provide either delta (counter) or toggleValue (toggle), not both");
                }
                if (request.getDelta() == null && request.getToggleValue() == null) {
                    logStructured(requestId, "recordKpiEvent", gameId, "error", 400, System.currentTimeMillis() - start, "Validation", "Provide delta or toggleValue");
                    return errorJson(400, "Provide either delta (counter) or toggleValue (toggle)");
                }

                String timestamp = Instant.now().toString();

                Map<String, AttributeValue> item = new HashMap<>();
                item.put("gameId", AttributeValue.builder().s(gameId).build());
                item.put("eventTimestamp", AttributeValue.builder().s(timestamp).build());
                item.put("kpiId", AttributeValue.builder().s(request.getKpiId()).build());
                if (request.getDelta() != null) {
                    item.put("delta", AttributeValue.builder().n(Integer.toString(request.getDelta())).build());
                }
                if (request.getToggleValue() != null) {
                    item.put("toggleValue", AttributeValue.builder().bool(request.getToggleValue()).build());
                }

                DDB.putItem(PutItemRequest.builder()
                        .tableName(KPI_EVENTS_TABLE)
                        .item(item)
                        .build());

                logStructured(requestId, "recordKpiEvent", gameId, "ok", 200, System.currentTimeMillis() - start, null, null);
                return okJson(Collections.singletonMap("status", "OK"));
            } catch (Exception e) {
                logStructured(requestId, "recordKpiEvent", gameId, "error", 500, System.currentTimeMillis() - start, e.getClass().getSimpleName(), e.getMessage());
                return errorJson(500, "Failed to record KPI event: " + e.getMessage());
            }
        }
    }

    public static class GetGameSummaryHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
        @Override
        public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
            long start = System.currentTimeMillis();
            String requestId = context != null ? context.getAwsRequestId() : null;
            try {
                String gameId = pathParam(event, "gameId");
                if (gameId == null || gameId.isEmpty()) {
                    logStructured(requestId, "getGameSummary", null, "error", 400, System.currentTimeMillis() - start, "Validation", "Missing gameId in path");
                    return errorJson(400, "Missing gameId in path");
                }

                // Load definitions
                List<KpiDefinition> defs = loadKpisForGame(gameId);
                Map<String, KpiDefinition> defsById = defs.stream()
                        .collect(Collectors.toMap(KpiDefinition::getKpiId, d -> d));

                // Load all events for game
                QueryRequest query = QueryRequest.builder()
                        .tableName(KPI_EVENTS_TABLE)
                        .keyConditionExpression("gameId = :g")
                        .expressionAttributeValues(Collections.singletonMap(
                                ":g", AttributeValue.builder().s(gameId).build()))
                        .build();

                List<Map<String, AttributeValue>> items = DDB.queryPaginator(query)
                        .items()
                        .stream()
                        .collect(Collectors.toList());

                Map<String, Integer> counterTotals = new HashMap<>();
                Map<String, Boolean> toggleStates = new HashMap<>();

                for (Map<String, AttributeValue> it : items) {
                    String kpiId = it.getOrDefault("kpiId", AttributeValue.builder().s("").build()).s();
                    if (kpiId.isEmpty()) continue;

                    KpiDefinition def = defsById.get(kpiId);
                    if (def == null) continue;

                    if (def.getType() == KpiType.COUNTER && it.containsKey("delta")) {
                        int delta = Integer.parseInt(it.get("delta").n());
                        counterTotals.merge(kpiId, delta, Integer::sum);
                    } else if (def.getType() == KpiType.TOGGLE && it.containsKey("toggleValue")) {
                        toggleStates.put(kpiId, it.get("toggleValue").bool());
                    }
                }

                List<KpiSummary> summaries = new ArrayList<>();
                for (KpiDefinition def : defs) {
                    if (def.getType() == KpiType.COUNTER) {
                        int total = counterTotals.getOrDefault(def.getKpiId(), 0);
                        summaries.add(KpiSummary.counter(def.getKpiId(), def.getLabel(), total));
                    } else {
                        boolean value = toggleStates.getOrDefault(def.getKpiId(), false);
                        summaries.add(KpiSummary.toggle(def.getKpiId(), def.getLabel(), value));
                    }
                }

                GameSummaryResponse response = new GameSummaryResponse(gameId, summaries);
                logStructured(requestId, "getGameSummary", gameId, "ok", 200, System.currentTimeMillis() - start, null, null);
                return okJson(response);
            } catch (Exception e) {
                String gameId = pathParam(event, "gameId");
                logStructured(requestId, "getGameSummary", gameId, "error", 500, System.currentTimeMillis() - start, e.getClass().getSimpleName(), e.getMessage());
                return errorJson(500, "Failed to calculate game summary: " + e.getMessage());
            }
        }
    }

    // ----- Models -----

    public static class CreateGameRequest {
        private String homeTeam;
        private String awayTeam;
        private String kickoffIso; // ISO-8601 string, optional

        public String getHomeTeam() {
            return homeTeam;
        }

        public void setHomeTeam(String homeTeam) {
            this.homeTeam = homeTeam;
        }

        public String getAwayTeam() {
            return awayTeam;
        }

        public void setAwayTeam(String awayTeam) {
            this.awayTeam = awayTeam;
        }

        public String getKickoffIso() {
            return kickoffIso;
        }

        public void setKickoffIso(String kickoffIso) {
            this.kickoffIso = kickoffIso;
        }
    }

    public static class CreateGameResponse {
        private String gameId;
        private List<KpiDefinition> kpis;

        public CreateGameResponse() {
        }

        public CreateGameResponse(String gameId, List<KpiDefinition> kpis) {
            this.gameId = gameId;
            this.kpis = kpis;
        }

        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        public List<KpiDefinition> getKpis() {
            return kpis;
        }

        public void setKpis(List<KpiDefinition> kpis) {
            this.kpis = kpis;
        }
    }

    public static class RecordKpiEventRequest {
        private String kpiId;
        private Integer delta;           // for counters: +1 / -1
        private Boolean toggleValue;     // for toggles: true / false

        public String getKpiId() {
            return kpiId;
        }

        public void setKpiId(String kpiId) {
            this.kpiId = kpiId;
        }

        public Integer getDelta() {
            return delta;
        }

        public void setDelta(Integer delta) {
            this.delta = delta;
        }

        public Boolean getToggleValue() {
            return toggleValue;
        }

        public void setToggleValue(Boolean toggleValue) {
            this.toggleValue = toggleValue;
        }
    }

    public static class Game {
        private String gameId;
        private String homeTeam;
        private String awayTeam;
        private String kickoffIso;
        private String status;

        public Game() {
        }

        public Game(String gameId, String homeTeam, String awayTeam, String kickoffIso, String status) {
            this.gameId = gameId;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.kickoffIso = kickoffIso;
            this.status = status;
        }

        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        public String getHomeTeam() {
            return homeTeam;
        }

        public void setHomeTeam(String homeTeam) {
            this.homeTeam = homeTeam;
        }

        public String getAwayTeam() {
            return awayTeam;
        }

        public void setAwayTeam(String awayTeam) {
            this.awayTeam = awayTeam;
        }

        public String getKickoffIso() {
            return kickoffIso;
        }

        public void setKickoffIso(String kickoffIso) {
            this.kickoffIso = kickoffIso;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public enum KpiType {
        COUNTER,
        TOGGLE
    }

    public static class KpiDefinition {
        private String gameId;
        private String kpiId;
        private String label;
        private KpiType type;

        public KpiDefinition() {
        }

        public KpiDefinition(String gameId, String kpiId, String label, KpiType type) {
            this.gameId = gameId;
            this.kpiId = kpiId;
            this.label = label;
            this.type = type;
        }

        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        public String getKpiId() {
            return kpiId;
        }

        public void setKpiId(String kpiId) {
            this.kpiId = kpiId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public KpiType getType() {
            return type;
        }

        public void setType(KpiType type) {
            this.type = type;
        }
    }

    public static class KpiSummary {
        private String kpiId;
        private String label;
        private Integer total;   // for counters
        private Boolean value;   // for toggles

        public KpiSummary() {
        }

        public static KpiSummary counter(String kpiId, String label, int total) {
            KpiSummary s = new KpiSummary();
            s.kpiId = kpiId;
            s.label = label;
            s.total = total;
            return s;
        }

        public static KpiSummary toggle(String kpiId, String label, boolean value) {
            KpiSummary s = new KpiSummary();
            s.kpiId = kpiId;
            s.label = label;
            s.value = value;
            return s;
        }

        public String getKpiId() {
            return kpiId;
        }

        public void setKpiId(String kpiId) {
            this.kpiId = kpiId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }

        public Boolean getValue() {
            return value;
        }

        public void setValue(Boolean value) {
            this.value = value;
        }
    }

    public static class GameSummaryResponse {
        private String gameId;
        private List<KpiSummary> kpis;

        public GameSummaryResponse() {
        }

        public GameSummaryResponse(String gameId, List<KpiSummary> kpis) {
            this.gameId = gameId;
            this.kpis = kpis;
        }

        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        public List<KpiSummary> getKpis() {
            return kpis;
        }

        public void setKpis(List<KpiSummary> kpis) {
            this.kpis = kpis;
        }
    }

    // ----- KPI defaults -----

    public static class DefaultKpis {
        public static List<KpiDefinition> defaultKpisForGame(String gameId) {
            List<KpiDefinition> list = new ArrayList<>();
            list.add(new KpiDefinition(gameId, "shots_on_target", "Shots on Target", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "shots_off_target", "Shots off Target", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "goals", "Goals", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "tackles_won", "Tackles Won", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "passes_completed", "Passes Completed", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "key_passes", "Key Passes", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "interceptions", "Interceptions", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "fouls_committed", "Fouls Committed", KpiType.COUNTER));
            list.add(new KpiDefinition(gameId, "yellow_card", "Yellow Card", KpiType.TOGGLE));
            list.add(new KpiDefinition(gameId, "red_card", "Red Card", KpiType.TOGGLE));
            list.add(new KpiDefinition(gameId, "clean_sheet", "Clean Sheet (So Far)", KpiType.TOGGLE));
            list.add(new KpiDefinition(gameId, "momentum", "Momentum (Winning)", KpiType.TOGGLE));
            return list;
        }
    }

    // ----- Helpers -----

    private static void logStructured(String requestId, String handler, String gameId, String status, int statusCode, long durationMs, String errorType, String errorMessage) {
        try {
            Map<String, Object> log = new HashMap<>();
            log.put("requestId", requestId);
            log.put("handler", handler);
            if (gameId != null) log.put("gameId", gameId);
            log.put("status", status);
            log.put("statusCode", statusCode);
            log.put("durationMs", durationMs);
            if (errorType != null) log.put("errorType", errorType);
            if (errorMessage != null) log.put("errorMessage", errorMessage);
            System.out.println(OBJECT_MAPPER.writeValueAsString(log));
        } catch (JsonProcessingException ignored) {
            System.err.println("{\"message\":\"Failed to serialize log\"}");
        }
    }

    private static APIGatewayV2HTTPResponse okJson(Object bodyObj) {
        return jsonResponse(200, bodyObj);
    }

    private static APIGatewayV2HTTPResponse errorJson(int status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return jsonResponse(status, body);
    }

    private static APIGatewayV2HTTPResponse jsonResponse(int statusCode, Object bodyObj) {
        try {
            String body = OBJECT_MAPPER.writeValueAsString(bodyObj);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Headers", "*");
            headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody(body)
                    .build();
        } catch (JsonProcessingException e) {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withBody("{\"message\":\"Failed to serialize response\"}")
                    .build();
        }
    }

    private static String pathParam(APIGatewayV2HTTPEvent event, String name) {
        Map<String, String> pathParams = event.getPathParameters();
        if (pathParams == null) return null;
        return pathParams.get(name);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static List<KpiDefinition> loadKpisForGame(String gameId) {
        QueryRequest query = QueryRequest.builder()
                .tableName(KPI_DEFINITIONS_TABLE)
                .keyConditionExpression("gameId = :g")
                .expressionAttributeValues(Collections.singletonMap(
                        ":g", AttributeValue.builder().s(gameId).build()))
                .build();

        List<KpiDefinition> defs = new ArrayList<>();
        DDB.queryPaginator(query).items().forEach(it -> {
            String kpiId = it.get("kpiId").s();
            String label = it.get("label").s();
            KpiType type = KpiType.valueOf(it.get("type").s());
            defs.add(new KpiDefinition(gameId, kpiId, label, type));
        });
        return defs;
    }
}

