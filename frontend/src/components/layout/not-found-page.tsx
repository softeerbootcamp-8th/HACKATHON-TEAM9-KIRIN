import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { PageContainer } from "@/components/layout/page";

/** 매칭되는 라우트가 없을 때 보여줄 기본 404 화면 */
export function NotFoundPage() {
  return (
    <PageContainer className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center">
      <h1 className="text-2xl font-bold">페이지를 찾을 수 없습니다</h1>
      <p className="text-sm text-[var(--color-text-sub)]">
        요청한 페이지가 존재하지 않거나 이동되었습니다.
      </p>
      <Button asChild>
        <Link to="/">홈으로 이동</Link>
      </Button>
    </PageContainer>
  );
}
