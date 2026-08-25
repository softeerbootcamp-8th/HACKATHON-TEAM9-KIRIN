import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { getMyInfo } from "@/api/generated/members/members";
import { guestLogin } from "@/api/generated/auth/auth";
import type { MemberResponse } from "@/api/generated/model";

type SessionContextValue = {
  member: MemberResponse;
};

const SessionContext = createContext<SessionContextValue | null>(null);

/** 현재 로그인된(게스트 포함) 멤버 정보. SessionProvider 밖에서 쓰면 에러. */
export function useCurrentMember(): MemberResponse {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useCurrentMember는 SessionProvider 안에서만 사용할 수 있어요.");
  }
  return context.member;
}

/**
 * 앱 부팅 시 세션을 조용히 확보한다 — 로그인 화면은 두지 않는다.
 * 1. `/members/me` 로 기존 세션이 있는지 확인
 * 2. 없으면(401) `guest-login` 을 호출해 새 게스트 세션을 만든다
 * 세션이 확보되기 전까지는 children을 렌더링하지 않는다 — 이후 모든 화면은
 * 세션이 있다고 가정하고 만들 수 있다.
 */
export function SessionProvider({ children }: { children: ReactNode }) {
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function ensureSession() {
      try {
        const me = await getMyInfo();
        if (!cancelled) setMember(me);
        return;
      } catch {
        // 세션이 없으면 401 — 게스트로 새로 발급받는다.
      }
      try {
        const guest = await guestLogin();
        if (!cancelled) setMember(guest);
      } catch {
        if (!cancelled) setFailed(true);
      }
    }

    ensureSession();
    return () => {
      cancelled = true;
    };
  }, []);

  if (failed) {
    return (
      <div className="flex h-dvh items-center justify-center px-6 text-center text-sm text-[var(--color-text-muted)]">
        서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.
      </div>
    );
  }

  if (!member) {
    return (
      <div className="flex h-dvh items-center justify-center text-sm text-[var(--color-text-muted)]">
        불러오는 중...
      </div>
    );
  }

  return (
    <SessionContext.Provider value={{ member }}>
      {children}
    </SessionContext.Provider>
  );
}
