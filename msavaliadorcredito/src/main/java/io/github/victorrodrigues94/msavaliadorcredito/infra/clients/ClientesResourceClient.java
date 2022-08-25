package io.github.victorrodrigues94.msavaliadorcredito.infra.clients;

import io.github.victorrodrigues94.msavaliadorcredito.domain.model.DadosCliente;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "msclientes", path = "/clientes")
public interface ClientesResourceClient {

    @GetMapping(params = "cpf")
    ResponseEntity<DadosCliente> dadosCliente(@RequestParam("cpf") String cpf);

}
