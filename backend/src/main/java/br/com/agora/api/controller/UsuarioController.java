package br.com.agora.api.controller;

import br.com.agora.api.config.JwtService;
import br.com.agora.api.controller.dto.AuthResponseDTO;
import br.com.agora.api.controller.dto.CadastroDTO;
import br.com.agora.api.controller.dto.LoginRequestDTO;
import br.com.agora.api.domain.model.Usuario;
import br.com.agora.api.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // POST /api/auth/cadastrar
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody CadastroDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("E-mail já cadastrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRegiao(dto.getRegiao());
        userRepository.save(usuario);

        String token = jwtService.gerarToken(usuario.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponseDTO(token, usuario.getNome(), usuario.getEmail(), usuario.getId()));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        return userRepository.findByEmail(dto.getEmail())
            .filter(u -> passwordEncoder.matches(dto.getSenha(), u.getSenha()))
            .map(u -> {
                String token = jwtService.gerarToken(u.getEmail());
                return ResponseEntity.ok(new AuthResponseDTO(token, u.getNome(), u.getEmail(), u.getId()));
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
