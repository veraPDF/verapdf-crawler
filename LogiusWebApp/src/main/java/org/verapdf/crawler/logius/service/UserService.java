package org.verapdf.crawler.logius.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.core.email.SendEmailService;
import org.verapdf.crawler.logius.db.UserDao;
import org.verapdf.crawler.logius.dto.user.PasswordUpdateDto;
import org.verapdf.crawler.logius.dto.user.UserDto;
import org.verapdf.crawler.logius.dto.user.UserInfoDto;
import org.verapdf.crawler.logius.exception.AlreadyExistsException;
import org.verapdf.crawler.logius.exception.BadRequestException;
import org.verapdf.crawler.logius.exception.IncorrectPasswordException;
import org.verapdf.crawler.logius.exception.NotFoundException;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.model.User;
import org.verapdf.crawler.logius.tools.SecretKeyUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SendEmailService sendEmailService;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder, TokenService tokenService, SendEmailService sendEmailService) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sendEmailService = sendEmailService;
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
    public User registerUser(UserDto dto) {
        if (userDao.getByEmail(dto.getEmail()) != null) {
            throw new AlreadyExistsException(String.format("user with email %s already exists", dto.getEmail()));
        }
        User user = new User(dto.getEmail(), passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setValidationJobPriority(LocalDateTime.now());
        user = saveUserWithUpdateSecret(user);
        sendEmailService.sendEmailConfirm(tokenService.encodeEmailVerificationToken(user), user.getEmail());
        return user;

    }

    @Transactional
    public List<UserInfoDto> getUsers(String emailFilter, Integer start, Integer limit) {
        return userDao.getUsers(emailFilter, Role.USER, start, limit).stream()
                      .map(UserInfoDto::new).collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(String email, boolean status) {
        User user = findUserByEmail(email);
        user.setEnabled(status);
        saveUserWithUpdateSecret(user);
    }

    @Transactional
    public void updateEmailVerificationStatus(String email, boolean status) {
        User user = findUserByEmail(email);
        user.setActivated(status);
        saveUserWithUpdateSecret(user);
    }


    private User saveUserWithUpdateSecret(User user) {
        user.setSecret(SecretKeyUtils.generateSecret());
        return userDao.save(user);
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

    @Transactional
    public String confirmUserEmail(String token) {
        String email = tokenService.getSubject(tokenService.decode(token));
        User user = findUserByEmail(email);
        tokenService.verify(token, user.getSecret());
        user.setActivated(true);
        saveUserWithUpdateSecret(user);
        return tokenService.encode(user);
    }

    @Transactional
    public void confirmResetPassword(UUID uuid, String password) {
        User user = findUserById(uuid);
        user.setPassword(passwordEncoder.encode(password));
        saveUserWithUpdateSecret(user);
    }

    @Transactional
    public void resetPassword(String email) {
        User user = findUserByEmail(email);
        String token = tokenService.encodePasswordToken(user);
        sendEmailService.sendPasswordResetToken(token, user.getEmail());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = findUserByEmail(email);
        if (user.isActivated()){
            throw new BadRequestException("user already activated");
        }
        saveUserWithUpdateSecret(user);
        sendEmailService.sendEmailConfirm(tokenService.encodeEmailVerificationToken(user), user.getEmail());
    }
}