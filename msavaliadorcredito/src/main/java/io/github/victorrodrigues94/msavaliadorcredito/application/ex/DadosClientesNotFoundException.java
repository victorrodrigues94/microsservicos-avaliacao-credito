package io.github.victorrodrigues94.msavaliadorcredito.application.ex;

public class DadosClientesNotFoundException extends Exception{
    public DadosClientesNotFoundException() {
        super("Dados do cliente não encontrados para o cpf informado!");
    }
}
