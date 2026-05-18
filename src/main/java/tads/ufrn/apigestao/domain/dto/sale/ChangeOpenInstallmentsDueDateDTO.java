package tads.ufrn.apigestao.domain.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeOpenInstallmentsDueDateDTO {

    private LocalDate firstDueDate;

}