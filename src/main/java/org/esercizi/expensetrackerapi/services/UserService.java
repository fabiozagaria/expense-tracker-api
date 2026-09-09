package org.esercizi.expensetrackerapi.services;

import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findById(long id) {
        return userRepository.findById(id)
                .orElseThrow();
    }

}
