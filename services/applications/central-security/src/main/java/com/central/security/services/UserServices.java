package com.central.security.services;

import com.central.security.model.entites.UserAuth;
import com.central.security.model.entites.Users;
import com.central.security.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServices implements UserDetailsService {
    private final UsersRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new UserAuth(repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found")));

    }

    public List<Users> getUsers() {
        return repository.findAll();
    }
}
