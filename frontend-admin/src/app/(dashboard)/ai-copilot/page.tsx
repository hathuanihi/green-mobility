"use client";

import React, { useState, useRef, useEffect } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {
  Bot,
  User,
  Send,
  Sparkles,
  RefreshCw,
  Copy,
  Check,
  ShieldCheck,
  Zap,
  TrendingDown,
  Layers,
  HelpCircle,
} from "lucide-react";

interface Message {
  id: string;
  role: "user" | "model";
  content: string;
  timestamp: string;
}

const SUGGESTED_PROMPTS = [
  {
    title: "Hiệu suất Đội xe VinFast",
    prompt: "Phân tích hiệu suất năng lượng và tình trạng pin của các xe điện VinFast Feliz S và VF e34 hiện tại.",
    icon: Zap,
  },
  {
    title: "Cảnh báo Gian lận GPS",
    prompt: "Đánh giá mức độ rủi ro của các cảnh báo GPS Mocking và tốc độ bất thường vừa được hệ thống ghi nhận.",
    icon: ShieldCheck,
  },
  {
    title: "Chuẩn Giảm Phát thải IPCC",
    prompt: "Giải thích chi tiết công thức tính lượng CO2 giảm trừ khi khách hàng chuyển từ xe xăng sang xe máy điện Feliz S.",
    icon: TrendingDown,
  },
  {
    title: "Điều phối Giờ Cao điểm",
    prompt: "Đưa ra khuyến nghị phân bổ vùng tìm kiếm tài xế (Geohash Level 7) để tối ưu thời gian đón khách và tiết kiệm pin.",
    icon: Layers,
  },
];

export default function AiCopilotPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      role: "model",
      content:
        "Xin chào! Tôi là **Green Mobility AI Copilot** (tích hợp Google Gemini 3.1 Flash). Tôi có thể hỗ trợ bạn giám sát điều phối đội xe thuần điện, phân tích cảnh báo gian lận GPS và đo đạc định lượng tín chỉ Carbon thời gian thực. Hãy chọn câu hỏi nhanh bên dưới hoặc nhập yêu cầu điều hành của bạn!",
      timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
    },
  ]);
  const [inputValue, setInputValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const handleSend = async (textToSend?: string) => {
    const text = textToSend || inputValue;
    if (!text.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: "user",
      content: text.trim(),
      timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, userMessage]);
    if (!textToSend) setInputValue("");
    setIsLoading(true);

    try {
      const response = await fetch("/api/ai-copilot/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: [...messages, userMessage].map((m) => ({
            role: m.role,
            content: m.content,
          })),
        }),
      });

      const data = await response.json();

      if (response.ok && data.reply) {
        const aiMessage: Message = {
          id: (Date.now() + 1).toString(),
          role: "model",
          content: data.reply,
          timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
        };
        setMessages((prev) => [...prev, aiMessage]);
      } else {
        throw new Error(data.error || "Không nhận được phản hồi từ AI");
      }
    } catch (err: any) {
      console.error("AI Error:", err);
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: "model",
        content: `Rất tiếc, đã có sự cố khi kết nối tới AI Engine: ${err.message}. Vui lòng thử lại.`,
        timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleResetChat = () => {
    setMessages([
      {
        id: "welcome",
        role: "model",
        content:
          "Hội thoại đã được làm mới. Tôi sẵn sàng hỗ trợ bạn quản lý nền tảng giao thông xanh!",
        timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
      },
    ]);
  };

  return (
    <div className="flex flex-col h-[calc(100vh-5rem)] max-w-6xl mx-auto space-y-4 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-800 pb-4 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-emerald-500/20 to-teal-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shadow-lg shadow-emerald-950/40">
            <Bot className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-extrabold text-xl text-white tracking-tight">
                AI Operations Copilot
              </h1>
              <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                Gemini 3.1 Flash
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Trợ lý điều hành ảo chuyên sâu cho hệ sinh thái Gọi xe Điện & Tín chỉ Carbon
            </p>
          </div>
        </div>

        <button
          onClick={handleResetChat}
          className="flex items-center gap-2 px-3 py-1.5 text-xs text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-700 rounded-xl border border-slate-700 transition"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Tạo phiên mới
        </button>
      </div>

      {/* Suggested Prompts Grid */}
      {messages.length <= 2 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 shrink-0">
          {SUGGESTED_PROMPTS.map((item, idx) => {
            const Icon = item.icon;
            return (
              <button
                key={idx}
                onClick={() => handleSend(item.prompt)}
                disabled={isLoading}
                className="text-left p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 hover:border-emerald-500/40 hover:bg-slate-800/60 transition group cursor-pointer"
              >
                <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
                  <Icon className="w-4 h-4 text-emerald-400 group-hover:scale-110 transition" />
                  <span>{item.title}</span>
                </div>
                <p className="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                  {item.prompt}
                </p>
              </button>
            );
          })}
        </div>
      )}

      {/* Chat Messages Stream */}
      <div className="flex-1 overflow-y-auto pr-2 space-y-4 rounded-2xl bg-slate-900/30 border border-slate-800/60 p-4 backdrop-blur-sm">
        {messages.map((m) => {
          const isUser = m.role === "user";
          return (
            <div
              key={m.id}
              className={`flex items-start gap-3 ${isUser ? "flex-row-reverse" : "flex-row"}`}
            >
              {/* Avatar */}
              <div
                className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 border ${
                  isUser
                    ? "bg-emerald-500/20 border-emerald-500/40 text-emerald-300"
                    : "bg-slate-800 border-slate-700 text-emerald-400 shadow-md"
                }`}
              >
                {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
              </div>

              {/* Message Bubble */}
              <div
                className={`max-w-[85%] rounded-2xl p-4 text-sm leading-relaxed relative group ${
                  isUser
                    ? "bg-emerald-600 text-white shadow-lg shadow-emerald-950/30 rounded-tr-sm"
                    : "bg-slate-900/90 border border-slate-800 text-slate-200 shadow-xl rounded-tl-sm"
                }`}
              >
                {isUser ? (
                  <div className="whitespace-pre-wrap font-sans">{m.content}</div>
                ) : (
                  <div className="text-sm leading-relaxed">
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      components={{
                        p: ({ children }) => (
                          <p className="mb-2.5 last:mb-0 leading-relaxed text-slate-200">{children}</p>
                        ),
                        h1: ({ children }) => (
                          <h1 className="text-lg font-bold text-white mt-4 mb-2 pb-1 border-b border-slate-800">
                            {children}
                          </h1>
                        ),
                        h2: ({ children }) => (
                          <h2 className="text-base font-bold text-emerald-300 mt-3.5 mb-1.5 flex items-center gap-2">
                            {children}
                          </h2>
                        ),
                        h3: ({ children }) => (
                          <h3 className="text-sm font-semibold text-emerald-400 mt-3 mb-1">
                            {children}
                          </h3>
                        ),
                        ul: ({ children }) => (
                          <ul className="list-disc pl-5 my-2 space-y-1 text-slate-200">{children}</ul>
                        ),
                        ol: ({ children }) => (
                          <ol className="list-decimal pl-5 my-2 space-y-1 text-slate-200">{children}</ol>
                        ),
                        li: ({ children }) => <li className="leading-relaxed">{children}</li>,
                        strong: ({ children }) => <strong className="font-semibold text-white">{children}</strong>,
                        em: ({ children }) => <em className="italic text-emerald-300/90">{children}</em>,
                        blockquote: ({ children }) => (
                          <blockquote className="border-l-4 border-emerald-500/70 pl-3.5 py-1 my-2 text-slate-300 bg-emerald-500/5 rounded-r">
                            {children}
                          </blockquote>
                        ),
                        table: ({ children }) => (
                          <div className="overflow-x-auto my-3 rounded-xl border border-slate-800 bg-slate-950/60 shadow-inner">
                            <table className="min-w-full text-xs divide-y divide-slate-800 text-left">
                              {children}
                            </table>
                          </div>
                        ),
                        thead: ({ children }) => (
                          <thead className="bg-slate-800/80 text-emerald-400 font-semibold">{children}</thead>
                        ),
                        tbody: ({ children }) => (
                          <tbody className="divide-y divide-slate-800/60">{children}</tbody>
                        ),
                        tr: ({ children }) => (
                          <tr className="hover:bg-slate-800/30 transition">{children}</tr>
                        ),
                        th: ({ children }) => (
                          <th className="px-3.5 py-2.5 text-xs font-semibold uppercase tracking-wider">
                            {children}
                          </th>
                        ),
                        td: ({ children }) => (
                          <td className="px-3.5 py-2.5 text-xs text-slate-300 font-mono">{children}</td>
                        ),
                        code: ({ inline, className, children, ...props }: any) => {
                          if (inline) {
                            return (
                              <code
                                className="px-1.5 py-0.5 rounded bg-slate-950 border border-slate-800 text-emerald-300 font-mono text-xs font-medium"
                                {...props}
                              >
                                {children}
                              </code>
                            );
                          }
                          return (
                            <pre className="p-3 my-2 rounded-xl bg-slate-950 border border-slate-800 overflow-x-auto text-xs font-mono text-emerald-300">
                              <code {...props}>{children}</code>
                            </pre>
                          );
                        },
                        hr: () => <hr className="border-slate-800 my-3.5" />,
                      }}
                    >
                      {m.content}
                    </ReactMarkdown>
                  </div>
                )}

                <div
                  className={`flex items-center gap-2 mt-2 pt-2 border-t text-[11px] ${
                    isUser
                      ? "border-emerald-500/40 text-emerald-200 justify-end"
                      : "border-slate-800/80 text-slate-400 justify-between"
                  }`}
                >
                  <span>{m.timestamp}</span>

                  {!isUser && (
                    <button
                      onClick={() => handleCopy(m.content, m.id)}
                      className="opacity-0 group-hover:opacity-100 transition flex items-center gap-1 hover:text-emerald-400 text-slate-400 text-[10px]"
                    >
                      {copiedId === m.id ? (
                        <>
                          <Check className="w-3 h-3 text-emerald-400" /> Đã sao chép
                        </>
                      ) : (
                        <>
                          <Copy className="w-3 h-3" /> Sao chép
                        </>
                      )}
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {isLoading && (
          <div className="flex items-start gap-3">
            <div className="w-9 h-9 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-emerald-400 animate-pulse">
              <Bot className="w-4 h-4" />
            </div>
            <div className="bg-slate-900 border border-slate-800 rounded-2xl rounded-tl-sm p-4 text-sm text-slate-400 flex items-center gap-3">
              <Sparkles className="w-4 h-4 text-emerald-400 animate-spin" />
              <span>AI Copilot đang phân tích số liệu nền tảng...</span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="shrink-0 bg-slate-900/80 border border-slate-800 rounded-2xl p-2.5 backdrop-blur-md shadow-2xl">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="flex items-center gap-3"
        >
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Hỏi AI Copilot về vận hành đội xe, tín chỉ Carbon, GPS an toàn..."
            disabled={isLoading}
            className="flex-1 bg-transparent px-4 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none"
          />

          <button
            type="submit"
            disabled={!inputValue.trim() || isLoading}
            className="flex items-center gap-2 px-5 py-2.5 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-40 disabled:hover:bg-emerald-500 text-white font-semibold text-sm rounded-xl transition shadow-lg shadow-emerald-950/40 active:scale-95"
          >
            <span>Gửi</span>
            <Send className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
}
