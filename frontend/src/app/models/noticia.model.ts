export interface Noticia {
  id?: number;
  titulo: string;
  descricao: string;
  url: string;
  imageUrl?: string;
  portal: string;
  tag?: string;
  publicadoEm: string;
  gostei: number;
  lerDepois: number;
  // Estado local (não vem do backend)
  likedByUser?: boolean;
  savedByUser?: boolean;
}
