package sigpesq.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
// import org.hibernate.validator.constraints.br.CPF;

public class Participante {

    private Integer idParticipante;

    @NotBlank(message = "O tipo do participante eh obrigatorio.")
    @Pattern(
        regexp = "Docente|Discente|Tecnico",
        message = "RN-02/RF-03: O tipo deve ser 'Docente', 'Discente' ou 'Tecnico' (sem acento, conforme constraint do banco)."
    )
    private String tipo;

    @NotBlank(message = "O nome do participante eh obrigatorio.")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres.")
    private String nome;

    @NotBlank(message = "RN-09: O CPF eh obrigatorio.")
    @Pattern(regexp = "\\d{11}", message = "RN-09: O CPF deve conter exatamente 11 digitos numericos sem pontuacao.")
    // @CPF(message = "RN-09: CPF matematicamente invalido.")
    private String cpf;

    @NotBlank(message = "RN-14: O e-mail institucional eh obrigatorio.")
    @Email(message = "RN-14: Formato de e-mail institucional invalido.")
    @Size(max = 120, message = "O e-mail institucional deve ter no maximo 120 caracteres.")
    private String emailInstitucional;

    private boolean ativo = true;

    public Participante() {
    }

    public Participante(Integer idParticipante, String tipo, String nome, String cpf, String emailInstitucional, boolean ativo) {
        this.idParticipante = idParticipante;
        this.tipo = tipo;
        this.nome = nome;
        this.cpf = cpf;
        this.emailInstitucional = emailInstitucional;
        this.ativo = ativo;
    }

    public Participante(String tipo, String nome, String cpf, String emailInstitucional) {
        this(null, tipo, nome, cpf, emailInstitucional, true);
    }

    public Integer getIdParticipante() {
        return idParticipante;
    }

    public void setIdParticipante(Integer idParticipante) {
        this.idParticipante = idParticipante;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmailInstitucional() {
        return emailInstitucional;
    }

    public void setEmailInstitucional(String emailInstitucional) {
        this.emailInstitucional = emailInstitucional;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - CPF: %s - Email: %s",
                idParticipante, nome, tipo, cpf, emailInstitucional);
    }
}