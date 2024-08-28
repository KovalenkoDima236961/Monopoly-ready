package com.dimon.catanbackend.service;

import com.dimon.catanbackend.dtos.RegistrationUserDto;
import com.dimon.catanbackend.entities.User;
import com.dimon.catanbackend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private UserRepository userRepository;
    private RoleService roleService;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    @Autowired
    @Lazy
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    @Transactional
    public void addFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username).get();
        User friend = userRepository.findByUsername(friendUsername).get();

        user.addFriend(friend);
        userRepository.save(user);
    }

    @Transactional
    public void removeFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username).get();
        User friend = userRepository.findByUsername(friendUsername).get();
        user.removeFriend(friend);
        userRepository.save(user);
    }

    public void deleteUserById(Long id) throws Exception {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.deleteById(id);
        } else {
            throw new Exception("User not found");
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(String.format("User with email '%s' not found", email)));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList())
        );
    }

    public User createNewUser(RegistrationUserDto registrationUserDto) {
        User user = new User();
        user.setEmail(registrationUserDto.getEmail());
        user.setUsername(registrationUserDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationUserDto.getPassword()));
        user.setRoles(new ArrayList<>(List.of(roleService.getUserRole())));
        user.setActive(false);
        return userRepository.save(user);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public Optional<User> findByActivationToken(String token) {
        return userRepository.findByActivationToken(token);
    }
}