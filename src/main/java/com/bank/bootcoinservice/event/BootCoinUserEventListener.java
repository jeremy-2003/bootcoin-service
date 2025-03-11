package com.bank.bootcoinservice.event;

import com.bank.bootcoinservice.dto.event.KafkaValidationRequest;
import com.bank.bootcoinservice.dto.event.KafkaValidationResponse;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BootCoinUserEventListener {
    private final KafkaTemplate<String, KafkaValidationRequest> kafkaTemplate;
    private final Map<String, PublishSubject<Boolean>> pendingEvents = new ConcurrentHashMap<>();
    private final Map<String, PublishSubject<String>> pendingErrors = new ConcurrentHashMap<>();
    public BootCoinUserEventListener(KafkaTemplate<String, KafkaValidationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public Single<Boolean> sendEventAndWait(String topic, KafkaValidationRequest request) {
        PublishSubject<Boolean> successSubject = PublishSubject.create();
        PublishSubject<String> errorSubject = PublishSubject.create();
        pendingEvents.put(request.getEventId(), successSubject);
        pendingErrors.put(request.getEventId(), errorSubject);
        kafkaTemplate.send(topic, request.getEventId(), request);
        return successSubject.firstOrError()
                .flatMap(success -> success ? Single.just(true) :
                        errorSubject.firstOrError()
                                .flatMap(errorMessage ->
                                    Single.error(new RuntimeException(
                                        "Validation failed: " + errorMessage))));
    }
    @KafkaListener(topics = "bootcoin.validation.response", groupId = "bootcoin-service")
    public void handleValidationResponse(KafkaValidationResponse response) {
        processKafkaResponse(response.getEventId(), response.isSuccess(), response.getErrorMessage());
    }
    private void processKafkaResponse(String eventId, boolean success, String errorMessage) {
        if (pendingEvents.containsKey(eventId)) {
            if (success) {
                pendingEvents.get(eventId).onNext(true);
            } else {
                pendingErrors.get(eventId).onNext(errorMessage);
            }
            pendingEvents.remove(eventId);
            pendingErrors.remove(eventId);
        }
    }
}