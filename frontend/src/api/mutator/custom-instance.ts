import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { updateServerClockOffset } from "@/lib/server-clock";

/**
 * Orval 이 생성하는 API 클라이언트가 사용하는 커스텀 axios 인스턴스.
 * orval.config.ts 의 `override.mutator` 가 이 파일의 `customInstance` 를 가리킨다.
 */
export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

// 응답마다 Date 헤더로 서버-클라이언트 시계 오차를 갱신한다 (server-clock.ts 참고).
axiosInstance.interceptors.response.use(
  (response) => {
    const serverDate = response.headers?.date;
    if (typeof serverDate === "string") updateServerClockOffset(serverDate);
    return response;
  },
  (error: AxiosError) => {
    const serverDate = error.response?.headers?.date;
    if (typeof serverDate === "string") updateServerClockOffset(serverDate);
    return Promise.reject(error);
  },
);

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const source = axios.CancelToken.source();
  const promise = axiosInstance({
    ...config,
    ...options,
    cancelToken: source.token,
  }).then(({ data }: AxiosResponse<T>) => data);

  // TanStack Query 취소 연동
  // @ts-expect-error — cancel 메서드를 프로미스에 부착 (orval 규약)
  promise.cancel = () => source.cancel("Query was cancelled");

  return promise;
};

export type ErrorType<Error> = AxiosError<Error>;
export type BodyType<BodyData> = BodyData;
