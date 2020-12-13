package com.trkapps.api.ws.services;

import com.trkapps.api.ws.domain.User;
import com.trkapps.api.ws.domain.VerificationToken;
import com.trkapps.api.ws.dto.UserDTO;
import com.trkapps.api.ws.repository.RoleRepository;
import com.trkapps.api.ws.repository.UserRepository;
import com.trkapps.api.ws.repository.VerificationTokenRepository;
import com.trkapps.api.ws.services.email.EmailService;
import com.trkapps.api.ws.services.exeption.ObjectAlreadyExistException;
import com.trkapps.api.ws.services.exeption.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(String id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new ObjectNotFoundException("Objeto não encontrado"));
    }

    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User fromUserDTO(UserDTO userDTO) {
        return new User(userDTO);
    }

    public User update(User user) {
        Optional<User> updateUser = userRepository.findById(user.getId());
        return updateUser.map(u -> userRepository.save(new User(u.getId(), user.getFirstName(),
                user.getLastName(), user.getEmail(), u.getPassword(), u.isEnabled())))
                .orElseThrow(() -> new ObjectNotFoundException("Usuário não encontrado"));
    }

    public void delete(String id) {
        userRepository.deleteById(id);
    }

    public User registerUser(User user) {
        if (emailExist(user.getEmail())) {
            throw new ObjectAlreadyExistException(String.format("Já existe uma conta com esse endereço de e-mail"));
        }
        user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER").get()));
        user.setEnabled(false);
        user = create(user);
        this.emailService.sendConfirmationHtmlEmail(user, null);
        return user;
    }

    private boolean emailExist(final String email) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            return true;
        }
        return false;
    }

    public void createVerificationTokenForUser(User user, String token) {
        final VerificationToken vToken = new VerificationToken(token, user);
        verificationTokenRepository.save(vToken);
    }

    public String validateVerificationToken(String token) {
        final Optional<VerificationToken> vToken = verificationTokenRepository.findByToken(token);

        if (!vToken.isPresent()) {
            return "invalidToken";
        }

        final User user = vToken.get().getUser();
        final Calendar cal = Calendar.getInstance();

        if ((vToken.get().getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            return "expired";
        }

        user.setEnabled(true);
        this.userRepository.save(user);
        return null;
    }

    public User findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElseThrow(() -> new ObjectNotFoundException(String.format("Usuário não encontrado")));
    }

    public VerificationToken generateNewVerificationToken(String email) {
        User user = findByEmail(email);
        Optional<VerificationToken> vToken = verificationTokenRepository.findByUser(user);
        vToken.get().updateToken(UUID.randomUUID().toString());
        VerificationToken updateVToken = verificationTokenRepository.save(vToken.get());
        emailService.sendConfirmationHtmlEmail(user, updateVToken);
        return updateVToken;
    }

}
