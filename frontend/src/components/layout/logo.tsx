import { User } from "lucide-react";
import { cn } from "@/lib/utils";
import { useCurrentMember } from "@/lib/auth/session-provider";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuLabel,
} from "@/components/ui/dropdown-menu";

/**
 * 홈 화면 상단 워드마크 (Figma "01 홈 · 사물함 현황"의 `Logo`, DESIGN.md §4.2).
 * 하위 화면에서는 이 자리를 `Header` 가 대신한다. 아이콘은 파비콘(`public/favicon.png`)과
 * 같은 마스코트 이미지라 별도 에셋 없이 그대로 재사용한다.
 * 우측 프로필 아이콘도 Figma에 이미 자리가 있는 요소(`gg:profile`)로, 누르면
 * 닉네임을 알려주는 툴팁을 띄운다. 별도 Tooltip 라이브러리 없이, 이미 쓰고 있는
 * DropdownMenu(팝오버)를 항목 없이 안내 문구 하나만 담아 재사용한다.
 */
export function Logo({ className }: { className?: string }) {
  const member = useCurrentMember();

  return (
    <div
      className={cn(
        "flex h-[var(--header-height)] w-full items-center justify-between gap-1 px-5",
        className,
      )}
    >
      <div className="flex items-center gap-1">
        <img
          src="/favicon.png"
          alt=""
          className="size-10 shrink-0 object-cover"
        />
        <span className="font-brand text-2xl leading-7 font-bold text-[var(--color-text)]">
          오다가다
        </span>
      </div>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            aria-label="내 프로필"
            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-surface-2)] text-[var(--color-text-muted)]"
          >
            <User className="size-5" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuLabel className="text-sm text-[var(--color-text-sub)]">
            안녕하세요 {member.nickname}님
          </DropdownMenuLabel>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
