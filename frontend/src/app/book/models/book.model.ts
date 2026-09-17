export interface Book {
  id: number;
  title: string;
  author: string;
  difficulty: string;
  category?: string;
  tags?: string[];
  fastReadingMinutes?: number;
  slowReadingMinutes?: number;
  description?: string;
  content: any;
  duration?: string;
  chapters?: number;
}
