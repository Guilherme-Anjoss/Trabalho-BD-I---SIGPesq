@AssertTrue(message = "RN-10: A data de término deve ser posterior à data de início.")
public boolean isPeriodoValido() {
    if (dataInicio == null || dataTermino == null) return false;
    return dataTermino.isAfter(dataInicio);
}