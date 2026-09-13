package org.esercizi.expensetrackerapi.services;

import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final UserService userService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;




}
