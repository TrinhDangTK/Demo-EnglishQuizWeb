package EnglishQuiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Service to create MoMo payment requests using the MoMo v2 API.
 * Uses the test environment by default.
 */
@Service
public class MomoService {

    private static final Logger log = LoggerFactory.getLogger(MomoService.class);

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.api-url}")
    private String apiUrl;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    @Value("${momo.request-type}")
    private String requestType;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MomoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Creates a MoMo payment request and returns the payment URL.
     *
     * @param amount    Amount in VND (minimum 10000)
     * @param orderInfo Description of the payment
     * @param extraData Extra data to pass through (e.g., username)
     * @return The MoMo payment URL to redirect the user to, or null on error
     */
    public String createPaymentUrl(long amount, String orderInfo, String extraData) throws Exception {
        String orderId = partnerCode + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // Build raw signature string (parameters MUST be in alphabetical order)
        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + (extraData != null ? extraData : "")
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        String signature = hmacSHA256(rawSignature, secretKey);
        log.debug("MoMo raw signature: {}", rawSignature);
        log.debug("MoMo signature: {}", signature);

        // Build request JSON
        ObjectNode body = objectMapper.createObjectNode();
        body.put("partnerCode", partnerCode);
        body.put("partnerName", "EnglishQuiz");
        body.put("storeId", "EnglishQuizStore");
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("lang", "vi");
        body.put("requestType", requestType);
        body.put("autoCapture", true);
        body.put("extraData", extraData != null ? extraData : "");
        body.put("signature", signature);

        String requestJson = objectMapper.writeValueAsString(body);
        log.info("MoMo request: {}", requestJson);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> httpResponse =
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        log.info("MoMo response status: {}", httpResponse.statusCode());
        log.info("MoMo response body: {}", httpResponse.body());

        JsonNode root = objectMapper.readTree(httpResponse.body());
        int resultCode = root.path("resultCode").asInt(-1);

        if (resultCode == 0) {
            String payUrl = root.path("payUrl").asText(null);
            log.info("MoMo payUrl: {}", payUrl);
            return payUrl;
        } else {
            String message = root.path("message").asText("Unknown error");
            log.error("MoMo payment creation failed: resultCode={}, message={}", resultCode, message);
            return null;
        }
    }

    /**
     * Verifies the signature from MoMo callback/IPN.
     *
     * @return true if the signature is valid
     */
    public boolean verifySignature(String rawData, String receivedSignature) {
        try {
            String computed = hmacSHA256(rawData, secretKey);
            return computed.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    // ── HMAC-SHA256 ──────────────────────────────────────────────────────────

    private String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
