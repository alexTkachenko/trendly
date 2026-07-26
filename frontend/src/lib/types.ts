export type MeUser = {
  id: number;
  email: string;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  createdAt: string;
};

export type AuthResponse = {
  token: string;
  user: MeUser;
};

export type UserSummary = {
  id: number;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
};

export type Post = {
  id: number;
  textContent: string;
  imageUrl: string | null;
  author: UserSummary;
  createdAt: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
