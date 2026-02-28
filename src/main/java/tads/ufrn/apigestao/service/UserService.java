package tads.ufrn.apigestao.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tads.ufrn.apigestao.domain.Collector;
import tads.ufrn.apigestao.domain.Inspector;
import tads.ufrn.apigestao.domain.Seller;
import tads.ufrn.apigestao.domain.User;
import tads.ufrn.apigestao.domain.dto.user.UpdatePasswordDTO;
import tads.ufrn.apigestao.domain.dto.user.UpsertUserDTO;
import tads.ufrn.apigestao.domain.dto.user.UserDTO;
import tads.ufrn.apigestao.enums.UserType;
import tads.ufrn.apigestao.exception.BusinessException;
import tads.ufrn.apigestao.exception.ResourceNotFoundException;
import tads.ufrn.apigestao.repository.CollectorRepository;
import tads.ufrn.apigestao.repository.InspectorRepository;
import tads.ufrn.apigestao.repository.SellerRepository;
import tads.ufrn.apigestao.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final ModelMapper mapper;
    public final PasswordEncoder passwordEncoder;
    private final InspectorRepository inspectorRepository;
    private final SellerRepository sellerRepository;
    private final CollectorRepository collectorRepository;

    public List<User> allUsers() {
        return repository.findAll();
    }

    public User findUserById(Long id) {
        Optional<User> user = repository.findById(id);
        return user.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }

    public User store(UpsertUserDTO userDTO) {
        User user = mapper.map(userDTO, User.class);

        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        if (userDTO.getPosition() != null) {
            user.setPosition(UserType.fromValue(userDTO.getPosition()));
        }

        User savedUser = repository.save(user);


        switch (savedUser.getPosition()){
            case FISCAL:
                Inspector inspector = new Inspector();
                inspector.setUser(savedUser);
                inspectorRepository.save(inspector);
                break;
            case VENDEDOR:
                Seller seller = new Seller();
                seller.setUser(savedUser);
                sellerRepository.save(seller);
                break;
            case COBRADOR:
                Collector collector = new Collector();
                collector.setUser(savedUser);
                collectorRepository.save(collector);
                break;
            default:
                break;
        }

        return savedUser;
    }

    public void updatePassword(Long id, UpdatePasswordDTO dto) {

        User user = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não existe!"));

        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        repository.save(user);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
