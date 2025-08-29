package main.java.com.example.webhookapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WebhookRunner implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting webhook flow...");

        // === 1. Generate Webhook ===
        String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        String requestBody = "{"
                + "\"name\":\"John Doe\","
                + "\"regNo\":\"22BEC0691\","
                + "\"email\":\"john@example.com\""
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                generateWebhookUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            System.err.println("Failed to generate webhook: " + response.getBody());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.getBody());

        String webhookUrl = jsonNode.get("webhook").asText();
        String accessToken = jsonNode.get("accessToken").asText();

        System.out.println("Webhook URL: " + webhookUrl);
        System.out.println("Access Token: " + accessToken);

        // === 2. The Final SQL Query ===
        String finalSqlQuery =
                "SELECT p.AMOUNT AS SALARY, " +
                "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
                "d.DEPARTMENT_NAME " +
                "FROM PAYMENTS p " +
                "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
                "ORDER BY p.AMOUNT DESC " +
                "LIMIT 1;";

        // === 3. Submit SQL Query to Webhook ===
        HttpHeaders postHeaders = new HttpHeaders();
        postHeaders.setContentType(MediaType.APPLICATION_JSON);
        postHeaders.set("Authorization", accessToken); // if required use "Bearer " + accessToken

        String answerBody = "{ \"finalQuery\": \"" + finalSqlQuery.replace("\"", "\\\"") + "\" }";

        HttpEntity<String> answerEntity = new HttpEntity<>(answerBody, postHeaders);

        ResponseEntity<String> answerResponse = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, answerEntity, String.class);

        if (answerResponse.getStatusCode() == HttpStatus.OK) {
            System.out.println("Successfully submitted SQL query!");
            System.out.println("Response: " + answerResponse.getBody());
        } else {
            System.err.println("Failed to submit answer: " + answerResponse.getBody());
        }
    }
}
