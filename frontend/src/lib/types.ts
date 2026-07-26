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
