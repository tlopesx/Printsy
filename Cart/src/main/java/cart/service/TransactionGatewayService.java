package cart.service;

import cart.dto.CountResult;
import cart.dto.TransactionResult;
import cart.dto.TransactionInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.logging.Logger;

@Service
public class TransactionGatewayService {

    private static final Logger LOGGER = Logger.getLogger(TransactionGatewayService.class.getName());

    private final WebClient webClient;

    @Autowired
    public TransactionGatewayService(@Qualifier("transactionWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    // checked with: {"query": "query FindImageAvailability($imageId: ID!) { findImageAvailability(imageId: $imageId) }","variables": { "imageId": "1" } }
    public int getTransactionImageAvailability(String imageId) {
        String query = "{ \"query\": \"query checkImageTransactionCount($imageId: String!) { checkImageTransactionCount(imageId: $imageId) { count } }\", \"variables\": { \"imageId\": \"" + imageId + "\" } }";
        LOGGER.info("Query: "+ query);

        // Make a post request and retrieve the result
        CountResult response = webClient.post()
                .uri("/graphql")
                .bodyValue(query)
                .retrieve()
                .bodyToMono(CountResult.class)
                .block();

        if (response == null) {
            LOGGER.severe("Failed to send image count query to transaction-gateway");
            return -1;
        } else {
            LOGGER.info("Successfully sent image count query to transaction-gateway: " + response);
            return response.getCount();
        }
    }


    // checked with: {"query": "mutation completeTransaction($transactions: [TransactionInput!]!) { completeTransaction(transactions: $transactions) { success } }","variables": {"transactions": [{"userId": 123, "productId": 456, "imageId": "image123"}]}}    
    public boolean completeTransaction(List<TransactionInput> transactions) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        
        String transactionJson = objectMapper.writeValueAsString(transactions);
        String mutation = "mutation ($transactions: [TransactionInput!]!) { completeTransaction(transactions: $transactions) { success } }";
        String payload = "{\"query\": \"" + mutation.replace("\"", "\\\"") + "\", \"variables\": {\"transactions\": " + transactionJson + "}}";
        
        try {
            TransactionResult response = webClient.post()
                .uri("/graphql")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(TransactionResult.class)
                .block();

            if (response == null || !response.getSuccess()) {
                LOGGER.severe("Failed to send transaction inputs to Transaction-Gateway");
                return false;
            } else {
                LOGGER.info("Successfully sent transaction inputs to Transaction-Gateway");
                return response.getSuccess();
            }
        } catch (Exception e){
            LOGGER.severe("Error while sending request to Transaction-Gateway: " + e.getMessage());
            return false;
        }


    }
    
}