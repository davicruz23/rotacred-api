package tads.ufrn.apigestao;


import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tads.ufrn.apigestao.domain.*;

import tads.ufrn.apigestao.enums.UserType;
import tads.ufrn.apigestao.repository.*;
import tads.ufrn.apigestao.service.*;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class ApiGestaoApplication {

    public final PasswordEncoder passwordEncoder;

    public ApiGestaoApplication(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Transactional
    public CommandLineRunner commandLineRunner(
            UserService userService,
            UserRepository userRepository,
            InspectorService inspectorService,
            InspectorRepository inspectorRepository,
            PreSaleService preSaleService,
            PreSaleRepository preSaleRepository,
            ProductService productService,
            ProductRepository productRepository,
            SaleService saleService,
            SellerRepository sellerRepository,
            ChargingService changingService,
            ChargingRepository chargingRepository,
            ChargingItemRepository chargingItemRepository,
            ClientRepository clientRepository,

            CollectorRepository collectorRepository) {
        return args -> {
            try {
                User user = userService.findUserById(1L);
            } catch (Exception e) {

                //salva os usuarios
                List<User> users = new ArrayList<>();
                users.add(new User(null,"Marlene Balbino","1",passwordEncoder.encode("123456"),UserType.SUPERADMIN));
                users.add(new User(null,"Miriam Balbino","2",passwordEncoder.encode("123456"),UserType.FUNCIONARIO));
                users.add(new User(null,"Larissa Barbosa","08202116481",passwordEncoder.encode("123456"),UserType.FISCAL));
                users.add(new User(null,"Davi Cruz","14522293763",passwordEncoder.encode("123456"),UserType.VENDEDOR));
                users.add(new User(null,"Elizabete Souza","49832824400",passwordEncoder.encode("123456"),UserType.COBRADOR));
                userRepository.saveAll(users);

                User carregador = userService.findUserById(2L);
                User admin = userService.findUserById(1L);
                User vendedor = userService.findUserById(4L);
                User inspector = userService.findUserById(3L);
                User cobrador = userService.findUserById(5L);

                //salva os produtos
                List<Product> products = new ArrayList<>();
                products.add(new Product(null,"Panela Inox","Tramontina",100, BigDecimal.valueOf(50.00),admin,null));
                products.add(new Product(null,"Travesseiro","Coteminas",100,BigDecimal.valueOf(30.00),admin,null));
                products.add(new Product(null,"Ventilador","Arno",50,BigDecimal.valueOf(200.00),admin,null));
                products.add(new Product(null,"Jogo de Talheres","Tramontina",50,BigDecimal.valueOf(100.00),admin,null));
                productRepository.saveAll(products);

                //salva o id de um vendedor
                Seller seller = new Seller();
                seller.setUser(vendedor);
                sellerRepository.save(seller);

                Inspector inspector1 = new Inspector();
                inspector1.setUser(inspector);
                inspectorRepository.save(inspector1);

                Collector collector1 = new Collector();
                collector1.setUser(cobrador);
                collectorRepository.save(collector1);

                Charging charging1 = new Charging();
                charging1.setUser(carregador);
                charging1.setDescription("CARREGAMENTO ATUAL");
                charging1.setDate(LocalDate.now());
                charging1.setCreatedAt(OffsetDateTime.now());
                chargingRepository.save(charging1);

            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGestaoApplication.class, args);
    }

}
