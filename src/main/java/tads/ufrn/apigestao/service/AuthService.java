package tads.ufrn.apigestao.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.domain.User;
import tads.ufrn.apigestao.domain.dto.loginDTO.LoginRequestDTO;
import tads.ufrn.apigestao.domain.dto.loginDTO.LoginResponseDTO;
import tads.ufrn.apigestao.domain.dto.user.UpsertUserDTO;
import tads.ufrn.apigestao.enums.UserType;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.exception.ResourceNotFoundException;
import tads.ufrn.apigestao.repository.UserRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SellerService sellerService;
    private final CollectorService collectorService;
    private final InspectorService inspectorService;

    public User login(LoginRequestDTO loginDTO) {
        Optional<User> userOptional = userRepository.findByCpf(loginDTO.getCpf());

        if (userOptional.isPresent()) {
            System.out.println("usuario logado: " + userOptional.get());
            User user = userOptional.get();
            if (passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                return user;
            } else {
                throw new BusinessException("Senha incorreta!");
            }
        } else {
            throw new ResourceNotFoundException("Usuário não existe!");
        }
    }


    public User register(UpsertUserDTO store) {
        if (userRepository.findByCpf(store.getCpf()).isPresent()) {
            throw new BusinessException("CPF já cadastrado!");
        }

        User user = new User();
        user.setName(store.getName());
        user.setCpf(store.getCpf());
        user.setPassword(passwordEncoder.encode(store.getPassword()));
        user.setPosition(UserType.fromValue(store.getPosition()));

        // Salva usuário primeiro
        user = userRepository.save(user);

        // Cria o registro correspondente ao tipo de usuário
        switch (user.getPosition()) {
            case VENDEDOR    -> sellerService.createFromUser(user);
            case COBRADOR    -> collectorService.createFromUser(user);
            case FISCAL      -> inspectorService.createFromUser(user);
            default -> { /* Tipo 1 (ADMIN) ou outros não criam nada */ }
        }

        return user;
    }


}

