package br.com.agora.api.controller;

import br.com.agora.api.config.JwtService;
import br.com.agora.api.domain.model.Interacao;
import br.com.agora.api.domain.repository.UserRepository;
import br.com.agora.api.domain.service.InteracaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interacoes")
@CrossOrigin(origins = "http://localhost:4200")
public class InteracaoController {

    @Autowired
    private InteracaoService interacaoService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private Long extrairUsuarioId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token ausente ou inválido");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extrairEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                .getId();
    }

    @PostMapping("/{newsId}/curtir")
    public ResponseEntity<Interacao> curtir(
            @PathVariable Long newsId,
            @RequestHeader("Authorization") String authHeader) {
        Long usuarioId = extrairUsuarioId(authHeader);
        return ResponseEntity.ok(interacaoService.curtir(usuarioId, newsId));
    }

    @PostMapping("/{newsId}/salvar")
    public ResponseEntity<Interacao> salvar(
            @PathVariable Long newsId,
            @RequestHeader("Authorization") String authHeader) {
        Long usuarioId = extrairUsuarioId(authHeader);
        return ResponseEntity.ok(interacaoService.salvar(usuarioId, newsId));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<Interacao>> minhas(
            @RequestHeader("Authorization") String authHeader) {
        Long usuarioId = extrairUsuarioId(authHeader);
        return ResponseEntity.ok(interacaoService.listarTodas(usuarioId));
    }
}