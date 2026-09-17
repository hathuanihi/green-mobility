"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import apiClient from "./api";
import { ApiResponse, AuthResponseData, User } from "@/types";

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  login: (phoneNumber: string, password: string) => Promise<void>;
  logout: () => void;
}

const defaultAuthContext: AuthContextType = {
  user: null,
  token: null,
  isLoading: false,
  login: async () => {},
  logout: () => {},
};

const AuthContext = createContext<AuthContextType>(defaultAuthContext);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    try {
      const savedToken = localStorage.getItem("gm_admin_token");
      const savedUser = localStorage.getItem("gm_admin_user");

      if (savedToken && savedUser) {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      }
    } catch (e) {
      console.error("Lỗi đọc dữ liệu auth từ localStorage", e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isLoading) {
      const isAuthPage = pathname?.startsWith("/login");
      if (!token && !isAuthPage) {
        router.replace("/login");
      } else if (token && isAuthPage) {
        router.replace("/");
      }
    }
  }, [token, isLoading, pathname, router]);

  const login = async (phoneNumber: string, password: string) => {
    const response = await apiClient.post<ApiResponse<AuthResponseData>>("/auth/login", {
      phoneNumber,
      password,
    });

    if (response.data.success && response.data.data) {
      const data = response.data.data;

      // Kiểm tra quyền truy cập Admin hoặc Operator
      if (data.role !== "ROLE_ADMIN" && data.role !== "ROLE_OPERATOR") {
        throw new Error("Tài khoản không có quyền truy cập Cổng Quản trị viên (Admin Portal)!");
      }

      const loggedInUser: User = {
        id: data.userId,
        phoneNumber: data.phoneNumber,
        fullName: data.fullName,
        role: data.role,
      };

      setToken(data.token);
      setUser(loggedInUser);
      localStorage.setItem("gm_admin_token", data.token);
      localStorage.setItem("gm_admin_user", JSON.stringify(loggedInUser));

      router.replace("/");
    } else {
      throw new Error(response.data.message || "Đăng nhập thất bại");
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem("gm_admin_token");
    localStorage.removeItem("gm_admin_user");
    router.replace("/login");
  };

  return (
    <AuthContext.Provider value={{ user, token, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
