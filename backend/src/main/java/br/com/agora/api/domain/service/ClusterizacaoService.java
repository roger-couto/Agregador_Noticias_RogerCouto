package br.com.agora.api.domain.service;

import br.com.agora.api.domain.model.ClusterCentroide;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.repository.ClusterCentroideRepository;
import br.com.agora.api.domain.repository.NewsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** K-means inicial e atribuição estável às médias persistidas. */
@Service
public class ClusterizacaoService {
    private static final int VERSAO = 1;
    private static final int MAX_CLUSTERS = 5;
    private static final int MAX_ITERACOES = 50;

    private final NewsRepository newsRepository;
    private final ClusterCentroideRepository centroideRepository;

    public ClusterizacaoService(NewsRepository newsRepository,
                                ClusterCentroideRepository centroideRepository) {
        this.newsRepository = newsRepository;
        this.centroideRepository = centroideRepository;
    }

    @Transactional
    public void atualizarClusters() {
        List<News> noticias = newsRepository.findAllByEmbeddingIsNotNullOrderByIdAsc();
        if (noticias.isEmpty()) return;

        List<ClusterCentroide> centros = centroideRepository.findByVersaoModeloOrderByOrdemClusterAsc(VERSAO);
        if (centros.isEmpty()) {
            if (noticias.size() < 2) return; // Com uma notícia não existe agrupamento significativo.
            centros = treinar(noticias);
        }
        final List<ClusterCentroide> centrosAtivos = centros;

        int dimensao = centrosAtivos.get(0).getCentroide().size();
        for (News noticia : noticias) {
            if (noticia.getEmbedding().size() != dimensao) continue;
            if (noticia.getClusterId() == null || noticia.getClusterVersion() == null
                    || noticia.getClusterVersion() != VERSAO) {
                ClusterCentroide maisProximo = centrosAtivos.stream()
                        .min(Comparator.comparingDouble(c -> distancia(noticia.getEmbedding(), c.getCentroide())))
                        .orElseThrow();
                noticia.setClusterId(maisProximo.getId());
                noticia.setClusterVersion(VERSAO);
            }
        }
        newsRepository.saveAll(noticias);
    }

    private List<ClusterCentroide> treinar(List<News> noticias) {
        List<List<Double>> pontos = noticias.stream()
                .filter(n -> n.getEmbedding() != null)
                .map(News::getEmbedding)
                .toList();
        int k = Math.min(MAX_CLUSTERS, Math.max(2, (int) Math.ceil(Math.sqrt(pontos.size()))));
        k = Math.min(k, pontos.size());
        List<List<Double>> centros = inicializarDeterministico(pontos, k);

        for (int iteracao = 0; iteracao < MAX_ITERACOES; iteracao++) {
            List<List<Double>> somas = new ArrayList<>();
            int[] quantidades = new int[k];
            for (int c = 0; c < k; c++) somas.add(new ArrayList<>(java.util.Collections.nCopies(centros.get(0).size(), 0.0)));
            for (List<Double> ponto : pontos) {
                int grupo = indiceMaisProximo(ponto, centros);
                quantidades[grupo]++;
                for (int d = 0; d < ponto.size(); d++) somas.get(grupo).set(d, somas.get(grupo).get(d) + ponto.get(d));
            }
            boolean mudou = false;
            for (int c = 0; c < k; c++) {
                if (quantidades[c] == 0) continue;
                int quantidadeCluster = quantidades[c];
                List<Double> novo = somas.get(c).stream().map(v -> v / quantidadeCluster).toList();
                if (distancia(centros.get(c), novo) > 1e-10) mudou = true;
                centros.set(c, novo);
            }
            if (!mudou) break;
        }

        News amostra = noticias.get(0);
        List<ClusterCentroide> entidades = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            ClusterCentroide centro = new ClusterCentroide();
            centro.setVersaoModelo(VERSAO);
            centro.setOrdemCluster(c);
            centro.setCentroide(centros.get(c));
            int quantidade = 0;
            for (List<Double> ponto : pontos) if (indiceMaisProximo(ponto, centros) == c) quantidade++;
            centro.setQuantidadeTreino(quantidade);
            centro.setEmbeddingModel(amostra.getEmbeddingModel() == null ? "desconhecido" : amostra.getEmbeddingModel());
            centro.setEmbeddingRevision(amostra.getEmbeddingRevision() == null ? "desconhecida" : amostra.getEmbeddingRevision());
            centro.setCriadoEm(LocalDateTime.now());
            entidades.add(centro);
        }
        return centroideRepository.saveAllAndFlush(entidades);
    }

    private List<List<Double>> inicializarDeterministico(List<List<Double>> pontos, int k) {
        List<List<Double>> centros = new ArrayList<>();
        centros.add(pontos.get(0));
        while (centros.size() < k) {
            List<Double> proximo = pontos.stream()
                    .max(Comparator.comparingDouble(p -> centros.stream()
                            .mapToDouble(c -> distancia(p, c)).min().orElse(0)))
                    .orElseThrow();
            centros.add(proximo);
        }
        return centros;
    }

    private int indiceMaisProximo(List<Double> ponto, List<List<Double>> centros) {
        int indice = 0;
        double menor = Double.MAX_VALUE;
        for (int i = 0; i < centros.size(); i++) {
            double d = distancia(ponto, centros.get(i));
            if (d < menor) { menor = d; indice = i; }
        }
        return indice;
    }

    private double distancia(List<Double> a, List<Double> b) {
        double soma = 0;
        for (int i = 0; i < a.size(); i++) {
            double diferenca = a.get(i) - b.get(i);
            soma += diferenca * diferenca;
        }
        return soma;
    }
}
