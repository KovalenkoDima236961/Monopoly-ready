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

/**
 * Service class responsible for handling business logic related to User entities.
 * It implements the {@link UserDetailsService} interface to provide user details
 * for authentication and authorization in Spring Security.
 *
 * The service interacts with the {@link UserRepository} to perform CRUD operations
 * on the User entity and provides additional functionality such as managing friendships,
 * creating new users, and finding users by username, email, or ID.
 *
 * Annotations used:
 * - {@link Service} to mark this as a Spring service component.
 * - {@link Transactional} to ensure the operations are handled in a transactional context where needed.
 * - {@link RequiredArgsConstructor} to generate a constructor with required dependencies.
 * - {@link Lazy} for injecting the {@link PasswordEncoder} to avoid circular dependency issues.
 *
 */

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private UserRepository userRepository;
    private RoleService roleService;
    private PasswordEncoder passwordEncoder;

    /**
     * Sets the {@link UserRepository} for this service.
     *
     * @param userRepository the user repository to be injected
     */
    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Sets the {@link RoleService} for this service.
     *
     * @param roleService the role service to be injected
     */
    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Sets the {@link PasswordEncoder} for this service.
     *
     * @param passwordEncoder the password encoder to be injected
     */
    @Autowired
    @Lazy
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves all users from the database.
     *
     * @return a list of all users
     */
    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    /**
     * Adds a friend to a user's friend list.
     *
     * @param username the username of the user
     * @param friendUsername the username of the friend to be added
     */
    @Transactional
    public void addFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username).get();
        User friend = userRepository.findByUsername(friendUsername).get();

        user.addFriend(friend);
        userRepository.save(user);
    }

    /**
     * Removes a friend from a user's friend list.
     *
     * @param username the username of the user
     * @param friendUsername the username of the friend to be removed
     */
    @Transactional
    public void removeFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username).get();
        User friend = userRepository.findByUsername(friendUsername).get();
        user.removeFriend(friend);
        userRepository.save(user);
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id the ID of the user to be deleted
     * @throws Exception if the user is not found
     */
    public void deleteUserById(Long id) throws Exception {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.deleteById(id);
        } else {
            throw new Exception("User not found");
        }
    }

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Finds a user by their email.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Finds a user by their ID.
     *
     * @param id the ID to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Loads a user by their email for Spring Security authentication.
     *
     * @param email the email of the user to load
     * @return a {@link UserDetails} object containing the user's details
     * @throws UsernameNotFoundException if the user is not found
     */
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

    /**
     * Creates a new user from a {@link RegistrationUserDto}.
     *
     * @param registrationUserDto the DTO containing the new user's information
     * @return the created user
     */
    public User createNewUser(RegistrationUserDto registrationUserDto) {
        User user = new User();
        user.setEmail(registrationUserDto.getEmail());
        user.setUsername(registrationUserDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationUserDto.getPassword()));
        user.setRoles(new ArrayList<>(List.of(roleService.getUserRole())));
        user.setActive(false);
        return userRepository.save(user);
    }

    /**
     * Saves the given user entity.
     *
     * @param user the user to save
     */
    public void save(User user) {
        userRepository.save(user);
    }

    /**
     * Finds a user by their activation token.
     *
     * @param token the activation token to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    public Optional<User> findByActivationToken(String token) {
        return userRepository.findByActivationToken(token);
    }
}