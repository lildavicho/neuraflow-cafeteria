package com.ucacue.bar.security;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmailIgnoreCaseWithTenant(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        return new CustomUserDetails(user);
    }
}
