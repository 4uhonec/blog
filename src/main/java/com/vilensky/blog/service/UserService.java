package com.vilensky.blog.service;

import com.vilensky.blog.entity.User;
import com.vilensky.blog.entity.enums.ERole;
import com.vilensky.blog.exceptions.UserExistsException;
import com.vilensky.blog.payload.request.SignupRequest;
import com.vilensky.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }
    public User createUser(SignupRequest userIn){
        User user = new User();
        user.setEmail(userIn.getEmail());
        user.setName(userIn.getFirstname());
        user.setLastname(userIn.getLastname());
        user.setUsername(userIn.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(userIn.getPassword()));
        user.getRoles().add(ERole.ROLE_USER);

        try{
            LOGGER.info("Saving user {}", userIn.getEmail());
            return userRepository.save(user);
        }catch(Exception e){
            LOGGER.error("Error during registration. {}", e.getMessage());
            throw new UserExistsException("The user " + user.getUsername() + " already exists");
        }

    }

}
