package com.mcwendyqueen.ratelimit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.mcwendyqueen.service.eligibilityClient.OrderPolicyEligibilityClient.EligibilityStatus;
import com.mcwendyqueen.service.eligibilityClient.ResiliantOrderPolicyEligibilityClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * T04 acceptance: the full app on real Tomcat, with each client a distinct loopback source address
 * (127.0.0.x). This is where MockMvc and Tomcat differ: path parameters such as ";x=1" still route to
 * createOrder on Tomcat, so classification must use the routed path. Only eligibility is mocked, because
 * it approves at random (known issue 1).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitAcceptanceTest {

    private static final String ORDER_BODY = "{\"name\":\"acceptance\"}";

    @LocalServerPort
    private int port;

    @MockBean
    private ResiliantOrderPolicyEligibilityClient eligibilityClient;

    @BeforeEach
    void approveAllOrders() {
        when(eligibilityClient.checkEligibility(any())).thenReturn(EligibilityStatus.APPROVED);
    }

    @Test
    void createOrder_overOrdersLimit_realServerReturns429WithRetryAfter() throws IOException {
        String client = "127.0.0.21";

        assertEquals(201, send(client, "POST", "/api/v1/orders", ORDER_BODY).status());
        assertEquals(201, send(client, "POST", "/api/v1/orders", ORDER_BODY).status());

        Response throttled = send(client, "POST", "/api/v1/orders", ORDER_BODY);
        assertEquals(429, throttled.status());
        assertEquals("10", throttled.header("Retry-After"));
        assertTrue(throttled.body().contains("\"code\":429"), throttled.body());
        assertTrue(throttled.body().contains("\"requestId\""), throttled.body());
    }

    @Test
    void createOrder_secondClient_unaffectedWhileFirstThrottled() throws IOException {
        for (int i = 0; i < 2; i++) {
            send("127.0.0.22", "POST", "/api/v1/orders", ORDER_BODY);
        }
        assertEquals(429, send("127.0.0.22", "POST", "/api/v1/orders", ORDER_BODY).status());

        assertEquals(201, send("127.0.0.23", "POST", "/api/v1/orders", ORDER_BODY).status());
    }

    @Test
    void createOrder_pathParameterOnOrders_stillCountsAsOrders() throws IOException {
        String client = "127.0.0.24";

        assertEquals(201, send(client, "POST", "/api/v1/orders;x=1", ORDER_BODY).status());
        assertEquals(201, send(client, "POST", "/api/v1/orders;x=2", ORDER_BODY).status());

        assertEquals(429, send(client, "POST", "/api/v1/orders;x=3", ORDER_BODY).status());
    }

    @Test
    void createOrder_pathParameterOnVersionSegment_stillCountsAsOrders() throws IOException {
        String client = "127.0.0.25";

        assertEquals(201, send(client, "POST", "/api/v1;v=1/orders", ORDER_BODY).status());
        assertEquals(201, send(client, "POST", "/api/v1;v=2/orders", ORDER_BODY).status());

        assertEquals(429, send(client, "POST", "/api/v1;v=3/orders", ORDER_BODY).status());
    }

    @Test
    void docsAndAdmin_whileClientThrottled_stillAnswer() throws IOException {
        String client = "127.0.0.26";
        for (int i = 0; i < 2; i++) {
            send(client, "POST", "/api/v1/orders", ORDER_BODY);
        }
        assertEquals(429, send(client, "POST", "/api/v1/orders", ORDER_BODY).status());

        for (int i = 0; i < 5; i++) {
            assertEquals(200, send(client, "GET", "/openapi", null).status());
            assertNotEquals(429, send(client, "GET", "/swagger-ui.html", null).status());
            assertEquals(200, send(client, "GET", "/api/v1/admin/kafka/listeners", null).status());
        }
    }

    // One HTTP/1.0 exchange from a chosen local address; java.net.http cannot bind a source address.
    private Response send(String clientAddress, String method, String path, String jsonBody) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port, InetAddress.getByName(clientAddress), 0)) {
            byte[] body = jsonBody == null ? new byte[0] : jsonBody.getBytes(StandardCharsets.UTF_8);
            String head = method + " " + path + " HTTP/1.0\r\n"
                    + "Host: localhost\r\n"
                    + (jsonBody == null ? "" : "Content-Type: application/json\r\n")
                    + "Content-Length: " + body.length + "\r\n\r\n";

            OutputStream out = socket.getOutputStream();
            out.write(head.getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();

            InputStream in = socket.getInputStream();
            return Response.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private record Response(int status, Map<String, String> headers, String body) {

        static Response parse(String raw) {
            int split = raw.indexOf("\r\n\r\n");
            String[] lines = raw.substring(0, split).split("\r\n");
            Map<String, String> headers = new LinkedHashMap<>();

            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT), lines[i].substring(colon + 1).trim());
            }

            return new Response(Integer.parseInt(lines[0].split(" ")[1]), headers, raw.substring(split + 4));
        }

        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }
}
