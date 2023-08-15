export function fakeApi<T>(data: T) {
  return new Promise<T>((resolve) => {
    setTimeout(() => resolve(data), 400);
  });
}
