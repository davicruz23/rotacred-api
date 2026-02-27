package tads.ufrn.apigestao.domain.dto.user;

import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

@Setter
public class UpdatePasswordDTO {
    private String password;
}
