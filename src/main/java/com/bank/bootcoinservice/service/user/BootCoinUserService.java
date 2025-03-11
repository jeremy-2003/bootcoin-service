package com.bank.bootcoinservice.service.user;

import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountRequest;
import com.bank.bootcoinservice.dto.associateaccount.AssociateBankAccountResponse;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiRequest;
import com.bank.bootcoinservice.dto.associateyanki.AssociateYankiResponse;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationRequest;
import com.bank.bootcoinservice.dto.bootcoin.BootCoinUserRegistrationResponse;
import io.reactivex.Single;

public interface BootCoinUserService {
    Single<BootCoinUserRegistrationResponse> registerUser(BootCoinUserRegistrationRequest request);
    Single<AssociateYankiResponse> associateYanki(AssociateYankiRequest request);
    Single<AssociateBankAccountResponse> associateBankAccount(AssociateBankAccountRequest request);
    Single<Boolean> validateUser(String phoneNumber, String documentNumber);
}