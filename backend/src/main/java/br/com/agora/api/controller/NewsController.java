package br.com.agora.api.controller;

import br.com.agora.api.controller.dto.NoticiaDTO;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.repository.NewsRepository;
import br.com.agora.api.domain.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "http://localhost:4200")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private NewsRepository newsRepository;

    // GET /api/news/recentes - noticias mais recentes (headline Brasil)
    @GetMapping("/recentes")
    public ResponseEntity<List<NoticiaDTO>> recentes() {
        return ResponseEntity.ok(newsService.buscarRecentes());
    }

    // GET /api/news/tag/{tag} - filtrar por topico
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<NoticiaDTO>> porTag(@PathVariable String tag) {
        return ResponseEntity.ok(newsService.buscarPorTag(tag));
    }

    // GET /api/news/portal/{portal} - filtrar por portal
    @GetMapping("/portal/{portal}")
    public ResponseEntity<List<NoticiaDTO>> porPortal(@PathVariable String portal) {
        return ResponseEntity.ok(newsService.buscarPorPortal(portal));
    }

    // GET /api/news/banco - lista do banco local (noticias salvas)
    @GetMapping("/banco")
    public ResponseEntity<List<NoticiaDTO>> banco() {
        List<NoticiaDTO> dtos = newsRepository.findAllByOrderByPublicadoEmDesc()
            .stream().map(newsService::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // POST /api/news - salva uma noticia no banco local
    @PostMapping
    public ResponseEntity<NoticiaDTO> salvar(@RequestBody News news) {
        return ResponseEntity.ok(newsService.toDTO(newsRepository.save(news)));
    }

    // PATCH /api/news/{id}/gostei - curtir
    @PatchMapping("/{id}/gostei")
    public ResponseEntity<NoticiaDTO> curtir(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.curtirNoticia(id));
    }

    // PATCH /api/news/{id}/ler-depois - salvar para ler depois
    @PatchMapping("/{id}/ler-depois")
    public ResponseEntity<NoticiaDTO> lerDepois(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.salvarParaDepois(id));
    }

    @PostMapping("/porIds")
    public ResponseEntity<List<NoticiaDTO>> porIds(@RequestBody List<Long> ids) {
        List<NoticiaDTO> dtos = ids.stream()
                .map(id -> newsRepository.findById(id))
                .filter(Optional::isPresent)
                .map(opt -> newsService.toDTO(opt.get()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
