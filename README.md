# 22BEC0691
# WebhookApp — Spring Boot startup webhook flow
This Spring Boot app (no controllers) runs on startup and:

1. POSTs to the generateWebhook endpoint with the applicant details.
2. Receives a JSON response with `webhook` and `accessToken`.
3. Posts the final SQL query to the returned `webhook` URL with `Authorization: <accessToken>` header.

## SQL solution
Final query used:
