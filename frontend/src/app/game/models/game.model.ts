export interface GameSession {
  id: number;
  bookId: number;
  savedSectionId: number;
  savedHealth: number;
  activeSectionId: number;
  activeHealth: number;
  status: string;
  moveHistory: number[];
  createdAt: string;
  updatedAt: string;
}

export interface Section {
  id: number | string;
  text: string;
  type: string;
  options: Option[];
}

export interface Option {
  description: string;
  gotoId: number | string;
  consequence?: Consequence;
}

export interface Consequence {
  type: string;
  value: number | string;
  text: string;
}
