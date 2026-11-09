package br.ufes.sigpesq.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CPF;

public class Participante {

    private Integer idParticipante;

    @NotBlank(message = "O tipo de vínculo é obrigatório.")
    @Pattern(regexp = "Docente|Discente|Tecnico", message = "RN-02/RF-03: O tipo deve ser Docente, Discente ou Tecnico.")
    private String tipo;

    @NotBlank(message = "O nome do participante é obrigatório.")
    private String nome;

    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "RN-09: O CPF informado é inválido.")
    private String cpf;

    @NotBlank(message = "O e-mail institucional é obrigatório.")
    @Email(message = "RN-14: Formato de e-mail institucional inválido.")
    private String emailInstitucional;

    // Construtor, Getters e Setters
    public Participante(String tipo, String nome, String cpf, String emailInstitucional) {
        this.tipo = tipo;
        this.nome = nome;
        this.cpf = cpf;
        this.emailInstitucional = emailInstitucional;
    }
}