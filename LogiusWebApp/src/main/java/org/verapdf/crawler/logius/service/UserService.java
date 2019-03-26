package org.verapdf.crawler.logius.service;


import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.db.UserDao;
import org.verapdf.crawler.logius.dto.user.PasswordUpdateDto;
import org.verapdf.crawler.logius.dto.user.UserDto;
import org.verapdf.crawler.logius.dto.user.UserInfoDto;
import org.verapdf.crawler.logius.exception.AlreadyExistsException;
import org.verapdf.crawler.logius.exception.IncorrectPasswordException;
import org.verapdf.crawler.logius.exception.NotFoundException;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.model.User;
import org.verapdf.crawler.logius.tools.SecretKeyUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updatePassword(String username, PasswordUpdateDto passwordUpdateDto) {
        User user = findUserByEmail(username);
        if (!passwordEncoder.matches(passwordUpdateDto.getOldPassword(), user.getPassword())) {
            throw new IncorrectPasswordException("Incorrect password");
        }
        user.setPassword(passwordEncoder.encode(passwordUpdateDto.getNewPassword()));
        saveUserWithUpdateSecret(user);
    }

    @Transactional
    public UserInfoDto save(UserDto dto) {
        try {
            User user = new User(dto.getEmail(), passwordEncoder.encode(dto.getPassword()));
            user.setRole(Role.USER);
            return saveUserWithUpdateSecret(user);
        } catch (Throwable e) {
            throw new AlreadyExistsException(String.format("user with email %s already exists", dto.getEmail()));
        }
    }

    @Transactional
    public List<UserInfoDto> getUsers(String emailFilter, Integer start, Integer limit) {
        return userDao.getUsers(emailFilter, start, limit).stream()
                .map(UserInfoDto::new).collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(String email, boolean status) {
        User user = findUserByEmail(email);
        user.setEnabled(status);
        saveUserWithUpdateSecret(user);
    }

    private UserInfoDto saveUserWithUpdateSecret(User user) {
        user.setSecret(SecretKeyUtils.generateSecret());
        return new UserInfoDto(userDao.save(user));
    }

    private UserInfoDto saveUserWithoutUpdateSecret(User user) {
        return new UserInfoDto(userDao.save(user));
    }

    @Transactional
    public User findUserById(UUID uuid) {
        User user = userDao.getById(uuid);
        if (user == null) {
            throw new NotFoundException(String.format("user with uuid %s not exists", uuid));
        }
        return user;
    }


    @Transactional
    public User findUserByEmail(String email) {
        User user = userDao.getByEmail(email);
        if (user == null) {
            throw new NotFoundException(String.format("user with email %s not exists", email));
        }
        return user;
    }

    @Transactional
    public long count(String emailFilter) {
        return userDao.count(emailFilter);
    }

}