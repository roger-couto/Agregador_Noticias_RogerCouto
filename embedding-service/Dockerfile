# Imagem Python enxuta para executar apenas o serviço local de embeddings.
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    HF_HOME=/model-cache

WORKDIR /app
COPY requirements.txt .
# Instala dependências dentro do container para não alterar o Python do computador.
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py .

# Inicia a API interna na porta que o backend acessa pela rede do Compose.
EXPOSE 8001
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
