export interface Tema {
  nome: string;
  classe: string;
  cor: string;
}

export const TEMAS: Tema[] = [
  { nome: 'Esmeralda', classe: 'tema-esmeralda', cor: '#10b981' },
  { nome: 'Ametista',  classe: 'tema-ametista',  cor: '#a855f7' },
  { nome: 'Solar',     classe: 'tema-solar',      cor: '#f59e0b' },
  { nome: 'Oceano',    classe: 'tema-oceano',     cor: '#3b82f6' },
  { nome: 'Amandita', classe: 'tema-amandita', cor: '#e8d5b0' },
  { nome: 'Eclipse',   classe: 'tema-eclipse',    cor: '#6b7280' },
  { nome: 'Volcanic',  classe: 'tema-volcanic',   cor: '#ef4444' },
];
