package sigpesq.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidatorEntidade {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private ValidatorEntidade() {
    }

    public static <T> void validar(T entidade) {
        Set<ConstraintViolation<T>> violacoes = VALIDATOR.validate(entidade);
        if (!violacoes.isEmpty()) {
            String mensagemErro = violacoes.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(" | "));
            throw new IllegalArgumentException("Erro de validacao: " + mensagemErro);
        }
    }
}