package com.Sprint.Sprint.Service;

import com.Sprint.Sprint.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = userRepository.findByUsername(username);

        // 2. A CORREÇÃO: Se o banco devolver null, você É OBRIGADO a lançar esse erro
        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }

        // 3. Se achou, retorna o usuário
        return user;

        //return userRepository.findByUsername(username);
    }
}
