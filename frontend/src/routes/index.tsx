import { createFileRoute } from "@tanstack/react-router";
import { Gnb } from "@/components/layout/gnb";
import { PageContainer } from "@/components/layout/page";

export const Route = createFileRoute("/")({
  component: HomePage,
});

/** 임시 홈 화면 — 실제 화면 구성이 정해지면 교체한다. */
function HomePage() {
  return (
    <div>
      <Gnb />
      <PageContainer>
        <h1 className="text-2xl font-bold">시작하세요</h1>
        <p className="mt-2 text-sm text-[var(--color-text-sub)]">
          src/routes/index.tsx 를 편집해 첫 화면을 구성하세요.
        </p>
      </PageContainer>
    </div>
  );
}
