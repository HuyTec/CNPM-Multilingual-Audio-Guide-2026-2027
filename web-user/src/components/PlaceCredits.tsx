export function PlaceCredits({ locationId }: { locationId: string }) {
  if (locationId !== '20000000-0000-0000-0000-000000000108') return null;
  return <p className="player-hint place-credits">
    Nội dung biên soạn và dịch từ <a href="https://vi.wikipedia.org/w/index.php?title=Chùa_Xá_Lợi&oldid=75512845">Wikipedia, Chùa Xá Lợi</a> · <a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a>.
    {' '}Ảnh: <a href="https://commons.wikimedia.org/wiki/File:Chùa_Xá_Lợi.JPG">Nguyễn Thanh Quang</a> · <a href="https://creativecommons.org/licenses/by-sa/3.0/">CC BY-SA 3.0</a>, đã cắt khung và giảm kích thước.
  </p>;
}
