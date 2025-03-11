package com.bank.bootcoinservice.service.user;

import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountRequest;
import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountResponse;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiRequest;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiResponse;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationRequest;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationResponse;
import com.bank.bootcoinservice.dto.event.KafkaValidationRequest;
import com.bank.bootcoinservice.event.BootCoinUserEventListener;
import com.bank.bootcoinservice.model.user.BootCoinUser;
import com.bank.bootcoinservice.repository.BootCoinUserRepository;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BootCoinUserServiceImpl implements BootCoinUserService {
    private final BootCoinUserRepository repository;
    private final KafkaTemplate<String, KafkaValidationRequest> kafkaTemplate;
    private final BootCoinUserEventListener eventListener;
    @Override
    public Single<BootCoinUserRegistrationResponse> registerUser(BootCoinUserRegistrationRequest request) {
        BootCoinUser user = new BootCoinUser();
        user.setDocumentNumber(request.getDocumentNumber());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setBalance(BigDecimal.ZERO);
        user.setHasYanki(request.isAssociateYanki());
        user.setBankAccountId(request.getBankAccountId());
        return repository.save(user)
                .flatMap(savedUser -> {
                    List<Single<Boolean>> validationEvents = new ArrayList<>();
                    if (request.isAssociateYanki()) {
                        validationEvents.add(sendValidationEvent(
                            "bootcoin.yanki.association",
                            savedUser.getDocumentNumber(),
                            savedUser.getPhoneNumber(),
                            null));
                    }
                    if (request.getBankAccountId() != null) {
                        validationEvents.add(sendValidationEvent(
                            "bootcoin.bank.account.association",
                            savedUser.getDocumentNumber(),
                            null,
                            savedUser.getBankAccountId()));
                    }
                    if (validationEvents.isEmpty()) {
                        return Single.just(new BootCoinUserRegistrationResponse(savedUser.getId(),
                            savedUser.isHasYanki(),
                            savedUser.getBankAccountId()));
                    }
                    return Single.zip(validationEvents, results ->
                        new BootCoinUserRegistrationResponse(savedUser.getId(),
                            savedUser.isHasYanki(), savedUser.getBankAccountId()));
                });
    }
    @Override
    public Single<AssociateYankiResponse> associateYanki(AssociateYankiRequest request) {
        return repository.findByDocumentNumber(request.getDocumentNumber())
                .switchIfEmpty(Single.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (user.isHasYanki()) {
                        return Single.error(new RuntimeException("User already " +
                            "has a Yanki account associated"));
                    }
                    return sendValidationEvent(
                        "bootcoin.yanki.association",
                        user.getDocumentNumber(),
                        user.getPhoneNumber(),
                        null)
                            .flatMap(success -> {
                                user.setHasYanki(true);
                                return repository.save(user);
                            })
                            .map(updatedUser ->
                                new AssociateYankiResponse(updatedUser.getId(),
                                    updatedUser.isHasYanki()));
                });
    }
    @Override
    public Single<AssociateBankAccountResponse> associateBankAccount(AssociateBankAccountRequest request) {
        return repository.findByDocumentNumber(request.getDocumentNumber())
                .switchIfEmpty(Single.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (user.getBankAccountId() != null) {
                        return Single.error(new RuntimeException("User already" +
                            " has a bank account associated"));
                    }
                    return sendValidationEvent(
                        "bootcoin.bank.account.association",
                        user.getDocumentNumber(),
                        null,
                        request.getBankAccountId())
                            .flatMap(success -> {
                                user.setBankAccountId(request.getBankAccountId());
                                return repository.save(user);
                            })
                            .map(updatedUser ->
                                new AssociateBankAccountResponse(updatedUser.getId(),
                                    updatedUser.getBankAccountId()));
                });
    }
    @Override
    public Single<Boolean> validateUser(String phoneNumber, String documentNumber) {
        return repository.findByPhoneNumberAndDocumentNumber(phoneNumber, documentNumber)
                .map(wallet -> true)
                .switchIfEmpty(Single.just(false));
    }
    private Single<Boolean> sendValidationEvent(String topic,
                                                String documentNumber,
                                                String phoneNumber,
                                                String bankAccountId) {
        String eventId = UUID.randomUUID().toString();
        KafkaValidationRequest event = new KafkaValidationRequest(eventId,
            documentNumber, phoneNumber, bankAccountId);
        return eventListener.sendEventAndWait(topic, event);
    }
}