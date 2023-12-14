export const getHeight = (legendEntriesCount: number) => {
  const { bottom } = getPadding(legendEntriesCount);
  return 150 + bottom;
};
export const getPadding = (legendEntriesCount: number) => ({
  bottom: 35 + 32 * legendEntriesCount,
  top: 5,
  left: 70,
  right: 30,
});
