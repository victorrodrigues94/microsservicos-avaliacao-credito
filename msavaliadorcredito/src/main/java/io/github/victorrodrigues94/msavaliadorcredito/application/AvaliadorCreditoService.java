package io.github.victorrodrigues94.msavaliadorcredito.application;

import feign.FeignException;
import io.github.victorrodrigues94.msavaliadorcredito.application.ex.DadosClientesNotFoundException;
import io.github.victorrodrigues94.msavaliadorcredito.application.ex.ErroComunicacaoMicroserviceException;
import io.github.victorrodrigues94.msavaliadorcredito.application.ex.ErroSolicitacaoCartaoException;
import io.github.victorrodrigues94.msavaliadorcredito.domain.model.*;
import io.github.victorrodrigues94.msavaliadorcredito.infra.clients.CartoesResourceClient;
import io.github.victorrodrigues94.msavaliadorcredito.infra.clients.ClientesResourceClient;
import io.github.victorrodrigues94.msavaliadorcredito.infra.mqueue.SolicitacaoEmissaoCartaoPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvaliadorCreditoService {

    private final ClientesResourceClient clientesClient;
    private final CartoesResourceClient cartoesClient;
    private final SolicitacaoEmissaoCartaoPublisher emissaoCartaoPublisher;

    public SituacaoCliente obterSituacaoCliente(String cpf)
            throws DadosClientesNotFoundException, ErroComunicacaoMicroserviceException{
        try {
            ResponseEntity<DadosCliente> dadosClienteResponse = clientesClient.dadosCliente(cpf);
            ResponseEntity<List<CartaoCliente>> cartaoClienteResponse = cartoesClient.getCartoesByCliente(cpf);
            return SituacaoCliente
                    .builder()
                    .cliente(dadosClienteResponse.getBody())
                    .cartoes(cartaoClienteResponse.getBody())
                    .build();
        }catch (FeignException.FeignClientException e) {
            int status = e.status();
            if(HttpStatus.NOT_FOUND.value() == status){
                throw new DadosClientesNotFoundException();
            }
            throw new ErroComunicacaoMicroserviceException(e.getMessage(), status);
        }
    }

    public RetornoAvaliacaoCliente realizarAvaliacao(String cpf, Long renda) throws DadosClientesNotFoundException, ErroComunicacaoMicroserviceException{
        try {
            ResponseEntity<DadosCliente> dadosClienteResponse = clientesClient.dadosCliente(cpf);
            ResponseEntity<List<Cartao>> cartaoResponse = cartoesClient.getCartoesRendaAteh(renda);

            List<Cartao> cartoes = cartaoResponse.getBody();
            var listaCartoesAprovados = cartoes.stream().map(cartao -> {
                
                DadosCliente dadosCliente = dadosClienteResponse.getBody();
                
                BigDecimal limiteBasico = cartao.getLimiteBasico();
                BigDecimal idadeBD = BigDecimal.valueOf(dadosCliente.getIdade());
                var fator = idadeBD.divide(BigDecimal.valueOf(10));
                BigDecimal limiteAprovado = fator.multiply(limiteBasico);

                CartaoAprovado aprovado = new CartaoAprovado();
                aprovado.setCartao(cartao.getNome());
                aprovado.setBandeira(cartao.getBandeira());
                aprovado.setLimiteAprovado(limiteAprovado);

                return  aprovado;
            }).collect(Collectors.toList());

            return new RetornoAvaliacaoCliente(listaCartoesAprovados);

        } catch (FeignException.FeignClientException e) {
            int status = e.status();
            if(HttpStatus.NOT_FOUND.value() == status){
                throw new DadosClientesNotFoundException();
            }
            throw new ErroComunicacaoMicroserviceException(e.getMessage(), status);
        }
    }

    public ProtocoloSolicitacaoCartao solicitarEmissaoCartao(DadosSolicitacaoEmissaoCartao dados){
        try {
            emissaoCartaoPublisher.solicitarCartao(dados);
            var protocolo = UUID.randomUUID().toString();
            return new ProtocoloSolicitacaoCartao(protocolo);
        }catch (Exception e){
            throw new ErroSolicitacaoCartaoException(e.getMessage());
        }
    }
}
