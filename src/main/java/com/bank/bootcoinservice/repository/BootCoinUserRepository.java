package com.bank.bootcoinservice.repository;

import com.bank.bootcoinservice.model.user.BootCoinUser;
import io.reactivex.Maybe;
import org.springframework.data.repository.reactive.RxJava2CrudRepository;

public interface BootCoinUserRepository extends RxJava2CrudRepository<BootCoinUser, String> {
    Maybe<BootCoinUser> findByDocumentNumber(String documentNumber);
    Maybe<BootCoinUser> findByPhoneNumberAndDocumentNumber(String phoneNumber, String documentNumber);
}
