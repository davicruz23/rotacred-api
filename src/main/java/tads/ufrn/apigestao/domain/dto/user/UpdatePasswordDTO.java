package tads.ufrn.apigestao.domain.dto.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

@Setter
@Getter
public class UpdatePasswordDTO {
    private String password;
}
