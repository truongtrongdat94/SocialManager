import { useEffect } from "react";
import { Card } from "@/components";
import { icons } from "@/constants";
import { useSocialAccountStore } from "@/stores";

export const AccountSelection = ({ selectedIds, onToggle }) => {
  const accounts = useSocialAccountStore((state) => state.accounts);
  const fetchAccounts = useSocialAccountStore((state) => state.fetchAccounts);

  useEffect(() => {
    if (accounts.length === 0) void fetchAccounts();
  }, []);

  return (
    <Card className="flex flex-col gap-4 flex-4 h-full min-h-0">
      <div className="font-semibold">Chọn tài khoản</div>

      <div className="flex-1 min-h-0 overflow-y-auto pr-2">
        {accounts.length > 0 ? (
          <div className="flex flex-col gap-4">
            {accounts.map((account) => (
              <div className="flex items-center justify-between" key={account.id}>
                <div className="flex items-center gap-2">
                  <div className="relative">
                    <img className="w-10 h-10 rounded-full" src={account.profilePictureUrl} alt="" />
                    <img className="w-4 h-4 absolute bottom-0 right-0" src={icons[account.platform.toLowerCase()]} alt="" />
                  </div>
                  <div>{account.accountName}</div>
                </div>

                <input type="checkbox" checked={selectedIds.includes(account.id)} onChange={() => onToggle(account.id)} className="cursor-pointer" />
              </div>
            ))}
          </div>
        ) : (
          <div className="text-text-secondary">Bạn chưa kết nối tài khoản mạng xã hội nào</div>
        )}
      </div>

      <div className="text-text-secondary text-sm shrink-0">
        <span className="font-semibold">{selectedIds.length}/{accounts.length}</span> tài khoản đã chọn
      </div>
    </Card>
  );
};
