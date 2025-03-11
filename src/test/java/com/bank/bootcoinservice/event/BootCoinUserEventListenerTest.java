package com.bank.bootcoinservice.event;

import com.bank.bootcoinservice.dto.event.KafkaValidationRequest;
import com.bank.bootcoinservice.dto.event.KafkaValidationResponse;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BootCoinUserEventListenerTest {
    @Mock
    private KafkaTemplate<String, KafkaValidationRequest> kafkaTemplate;
    @InjectMocks
    private BootCoinUserEventListener eventListener;
    @Captor
    private ArgumentCaptor<KafkaValidationRequest> requestCaptor;
    @Captor
    private ArgumentCaptor<String> topicCaptor;
    @Captor
    private ArgumentCaptor<String> keyCaptor;
    private KafkaValidationRequest validationRequest;
    private KafkaValidationResponse successResponse;
    private KafkaValidationResponse failureResponse;
    private String testTopic;
    @BeforeEach
    void setUp() {
        // Generate unique event ID for each test
        String eventId = UUID.randomUUID().toString();
        // Initialize test topic
        testTopic = "test.validation.topic";
        // Initialize validation request
        validationRequest = new KafkaValidationRequest();
        validationRequest.setEventId(eventId);
        validationRequest.setDocumentNumber("12345678");
        validationRequest.setPhoneNumber("999888777");
        validationRequest.setBankAccountId("bank123");
        // Initialize success response
        successResponse = new KafkaValidationResponse();
        successResponse.setEventId(eventId);
        successResponse.setSuccess(true);
        successResponse.setErrorMessage(null);
        // Initialize failure response
        failureResponse = new KafkaValidationResponse();
        failureResponse.setEventId(eventId);
        failureResponse.setSuccess(false);
        failureResponse.setErrorMessage("Invalid document number");
        // Configure mock Kafka template
        @SuppressWarnings("unchecked")
        ListenableFuture<SendResult<String, KafkaValidationRequest>> mockFuture = mock(ListenableFuture.class);
        doReturn(mockFuture).when(kafkaTemplate).send(anyString(), anyString(), any(KafkaValidationRequest.class));
    }
    @Test
    public void sendEventAndWait_SendsKafkaMessage() {
        // When
        eventListener.sendEventAndWait(testTopic, validationRequest);
        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), requestCaptor.capture());
        assertEquals(testTopic, topicCaptor.getValue());
        assertEquals(validationRequest.getEventId(), keyCaptor.getValue());
        assertEquals(validationRequest, requestCaptor.getValue());
    }
    @Test
    public void sendEventAndWait_SuccessResponse() {
        // Given
        TestObserver<Boolean> testObserver = new TestObserver<>();
        // When
        eventListener.sendEventAndWait(testTopic, validationRequest)
                .subscribe(testObserver);
        // Then trigger success response
        eventListener.handleValidationResponse(successResponse);
        // Then
        testObserver.assertValue(true);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }
    @Test
    public void handleValidationResponse_IgnoresUnknownEventIds() {
        // Given
        String unknownEventId = "unknown-event-id";
        KafkaValidationResponse unknownResponse = new KafkaValidationResponse();
        unknownResponse.setEventId(unknownEventId);
        unknownResponse.setSuccess(true);
        // When subscribing to a known request
        TestObserver<Boolean> testObserver = new TestObserver<>();
        eventListener.sendEventAndWait(testTopic, validationRequest)
                .subscribe(testObserver);
        // Then handle response for unknown event ID
        eventListener.handleValidationResponse(unknownResponse);
        // Then the test observer should not receive any response
        testObserver.assertNoValues();
        testObserver.assertNotComplete();
        testObserver.assertNoErrors();
    }
    @Test
    public void handleValidationResponse_CleansUpCompletedEvents() {
        // Given
        TestObserver<Boolean> testObserver = new TestObserver<>();
        eventListener.sendEventAndWait(testTopic, validationRequest)
                .subscribe(testObserver);
        // When
        eventListener.handleValidationResponse(successResponse);
        // Then
        // Create a second response with the same event ID
        eventListener.handleValidationResponse(successResponse);
        // The test observer should only receive one value (the first response)
        testObserver.assertValueCount(1);
    }
    @Test
    public void handleValidationResponse_MultipleRequests() {
        // Given
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();
        KafkaValidationRequest request1 = new KafkaValidationRequest();
        request1.setEventId(eventId1);
        KafkaValidationRequest request2 = new KafkaValidationRequest();
        request2.setEventId(eventId2);
        KafkaValidationResponse response1 = new KafkaValidationResponse();
        response1.setEventId(eventId1);
        response1.setSuccess(true);
        KafkaValidationResponse response2 = new KafkaValidationResponse();
        response2.setEventId(eventId2);
        response2.setSuccess(true);
        // When
        TestObserver<Boolean> observer1 = new TestObserver<>();
        TestObserver<Boolean> observer2 = new TestObserver<>();
        eventListener.sendEventAndWait(testTopic, request1)
                .subscribe(observer1);
        eventListener.sendEventAndWait(testTopic, request2)
                .subscribe(observer2);
        // Then
        eventListener.handleValidationResponse(response1);
        eventListener.handleValidationResponse(response2);
        observer1.assertValue(true);
        observer1.assertComplete();
        observer2.assertValue(true);
        observer2.assertComplete();
    }
}