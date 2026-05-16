import { useEffect, useState } from "react";
import axiosInstance from "@/libs/axios";

const envRow = (key: string) => (
  <div className="rounded-md border border-border bg-surface-secondary px-3 py-2 text-sm font-mono">
    {key}
  </div>
);

export const SetupGuide = () => {
  const [config, setConfig] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadConfig = async () => {
      try {
        const response = await axiosInstance.get<{ data: Record<string, boolean> }>("/social-accounts/config");
        setConfig(response.data.data || {});
      } catch {
        setConfig({});
      } finally {
        setLoading(false);
      }
    };

    loadConfig();
  }, []);

  const statusLabel = (key: string) => {
    if (loading) return "Đang kiểm tra";
    return config[key] ? "Sẵn sàng" : "Thiếu cấu hình";
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div>
        <div className="text-2xl font-bold">Hướng dẫn cấu hình</div>
        <div className="text-text-secondary text-sm">Thiết lập khóa OAuth và Cloudinary để kết nối tài khoản và upload ảnh/video.</div>
      </div>

      <div className="rounded-xl border border-border bg-surface-primary p-4">
        <div className="flex items-center justify-between">
          <div className="font-semibold">Trạng thái nhà cung cấp</div>
          <div className="text-xs text-text-secondary">Tải lại trang sau khi cập nhật biến môi trường</div>
        </div>
        <div className="mt-3 grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
          <div className="flex items-center justify-between rounded-md border border-border px-3 py-2">
            <span>Facebook</span>
            <span className="text-text-secondary">{statusLabel("facebook")}</span>
          </div>
          <div className="flex items-center justify-between rounded-md border border-border px-3 py-2">
            <span>Instagram</span>
            <span className="text-text-secondary">{statusLabel("instagram")}</span>
          </div>
          <div className="flex items-center justify-between rounded-md border border-border px-3 py-2">
            <span>Threads</span>
            <span className="text-text-secondary">{statusLabel("threads")}</span>
          </div>
          <div className="flex items-center justify-between rounded-md border border-border px-3 py-2">
            <span>TikTok</span>
            <span className="text-text-secondary">{statusLabel("tiktok")}</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-xl border border-border bg-surface-primary p-4">
          <div className="font-semibold">Meta / Facebook OAuth</div>
          <ol className="mt-3 list-decimal space-y-2 pl-4 text-sm text-text-secondary">
            <li>Vào Meta Developer Console và tạo app.</li>
            <li>Lấy App ID và App Secret trong App Settings.</li>
            <li>Đặt OAuth Redirect URI: http://localhost:8080/api/social-accounts/callback/facebook</li>
            <li>Set biến môi trường bên dưới và restart backend.</li>
          </ol>
          <div className="mt-4 flex flex-col gap-2">
            {envRow("FACEBOOK_CLIENT_ID")}
            {envRow("FACEBOOK_CLIENT_SECRET")}
            {envRow("FACEBOOK_REDIRECT_URI")}
          </div>
        </div>

        <div className="rounded-xl border border-border bg-surface-primary p-4">
          <div className="font-semibold">Cloudinary (Upload media)</div>
          <ol className="mt-3 list-decimal space-y-2 pl-4 text-sm text-text-secondary">
            <li>Tạo tài khoản Cloudinary.</li>
            <li>Lấy Cloud Name, API Key, API Secret từ dashboard.</li>
            <li>Set biến môi trường bên dưới và restart backend.</li>
          </ol>
          <div className="mt-4 flex flex-col gap-2">
            {envRow("CLOUDINARY_CLOUD_NAME")}
            {envRow("CLOUDINARY_API_KEY")}
            {envRow("CLOUDINARY_API_SECRET")}
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border bg-surface-primary p-4">
        <div className="font-semibold">Lệnh PowerShell (User scope)</div>
        <pre className="mt-3 overflow-x-auto rounded-md bg-surface-secondary p-3 text-xs">
{`setx FACEBOOK_CLIENT_ID "your_app_id"
setx FACEBOOK_CLIENT_SECRET "your_app_secret"
setx FACEBOOK_REDIRECT_URI "http://localhost:8080/api/social-accounts/callback/facebook"

setx CLOUDINARY_CLOUD_NAME "your_cloud_name"
setx CLOUDINARY_API_KEY "your_api_key"
setx CLOUDINARY_API_SECRET "your_api_secret"`}
        </pre>
        <div className="mt-2 text-sm text-text-secondary">Đóng và mở lại terminal sau khi setx.</div>
      </div>
    </div>
  );
};
