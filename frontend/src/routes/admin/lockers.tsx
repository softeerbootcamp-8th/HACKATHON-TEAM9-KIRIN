import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageContainer } from "@/components/layout/page";
import { Header } from "@/components/layout/header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import {
  useGetLockers,
  getGetLockersQueryKey,
  useChangeLockStatus,
  useResetLocker,
} from "@/api/generated/lockers/lockers";
import { LockStatus } from "@/api/generated/model";

export const Route = createFileRoute("/admin/lockers")({
  component: AdminLockersPage,
});

/**
 * 관리자 전용 진열함 관리 페이지. 로그인 가드는 두지 않는다 — 백엔드
 * `/api/lockers/**` 자체가 데모 편의를 위해 인증 없이 열려 있다.
 */
function AdminLockersPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [resetTarget, setResetTarget] = useState<number | null>(null);

  const { data, isLoading } = useGetLockers();
  const changeLockStatus = useChangeLockStatus();
  const resetLocker = useResetLocker();

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: getGetLockersQueryKey() });

  const handleToggleLock = (lockerId: number, current: LockStatus) => {
    const next =
      current === LockStatus.LOCKED ? LockStatus.UNLOCKED : LockStatus.LOCKED;
    changeLockStatus.mutate(
      { lockerId, data: { lockStatus: next } },
      {
        onSuccess: () => {
          toast.success(
            `${lockerId}번 진열함을 ${next === LockStatus.LOCKED ? "잠갔어요" : "열었어요"}.`,
          );
          invalidate();
        },
        onError: () => toast.error("잠금 상태 변경에 실패했어요."),
      },
    );
  };

  const handleReset = () => {
    if (resetTarget == null) return;
    resetLocker.mutate(
      { lockerId: resetTarget },
      {
        onSuccess: () => {
          toast.success(`${resetTarget}번 진열함을 초기화했어요.`);
          invalidate();
          setResetTarget(null);
        },
        onError: () => toast.error("초기화에 실패했어요."),
      },
    );
  };

  return (
    <PageContainer>
      <Header
        title="관리자 · 진열함 관리"
        onBack={() => router.navigate({ to: "/" })}
      />

      <div className="flex flex-col gap-2 px-4 pt-4 pb-8">
        {isLoading ? (
          <p className="pt-6 text-center text-sm text-[var(--color-text-muted)]">
            불러오는 중...
          </p>
        ) : (
          data?.lockers.map((locker) => (
            <div
              key={locker.lockerId}
              className="flex items-center justify-between gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-2)] p-3.5"
            >
              <div className="flex flex-col gap-1.5">
                <span className="text-sm font-semibold text-[var(--color-text)]">
                  {locker.lockerId}번 진열함
                </span>
                <div className="flex items-center gap-1.5">
                  <Badge
                    variant={
                      locker.lockStatus === LockStatus.LOCKED
                        ? "neutral"
                        : "warning"
                    }
                  >
                    {locker.lockStatus === LockStatus.LOCKED ? "잠김" : "열림"}
                  </Badge>
                  <Badge
                    variant={
                      locker.usageStatus === "AVAILABLE"
                        ? "success"
                        : locker.usageStatus === "RESERVED"
                          ? "info"
                          : "danger"
                    }
                  >
                    {locker.usageStatus === "AVAILABLE"
                      ? "비어있음"
                      : locker.usageStatus === "RESERVED"
                        ? "예약중"
                        : "사용중"}
                  </Badge>
                </div>
              </div>

              <div className="flex shrink-0 gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={changeLockStatus.isPending}
                  onClick={() =>
                    handleToggleLock(locker.lockerId, locker.lockStatus)
                  }
                >
                  {locker.lockStatus === LockStatus.LOCKED
                    ? "잠금해제"
                    : "잠금"}
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  disabled={resetLocker.isPending}
                  onClick={() => setResetTarget(locker.lockerId)}
                >
                  초기화
                </Button>
              </div>
            </div>
          ))
        )}
      </div>

      <Dialog
        open={resetTarget !== null}
        onOpenChange={(open) => !open && setResetTarget(null)}
      >
        <DialogContent className="max-w-[313px] gap-3.5 rounded-[16px] p-5">
          <DialogTitle className="text-center text-[17px]">
            {resetTarget}번 진열함을 초기화할까요?
          </DialogTitle>
          <p className="text-center text-[13px] text-[var(--color-text-muted)]">
            안에 물품이 있으면 회수 완료 상태로 되돌리고, 진열함은 잠김 +
            비어있음 상태가 돼요. 되돌릴 수 없어요.
          </p>
          <div className="flex gap-2 pt-2">
            <Button
              variant="secondary"
              size="lg"
              className="flex-1"
              onClick={() => setResetTarget(null)}
            >
              취소
            </Button>
            <Button
              variant="destructive"
              size="lg"
              className="flex-1"
              disabled={resetLocker.isPending}
              onClick={handleReset}
            >
              초기화
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </PageContainer>
  );
}
