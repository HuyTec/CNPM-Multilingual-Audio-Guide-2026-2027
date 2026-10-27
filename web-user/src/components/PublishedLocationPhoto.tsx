import { useEffect, useState } from "react";
import { locationPhoto } from "../lib/locationPhotos";

export function PublishedLocationPhoto({
  id,
  name,
}: {
  id: string;
  name: string;
}) {
  const [result, setResult] = useState<{ id: string; imageUrl?: string }>();
  useEffect(() => {
    const controller = new AbortController();
    void fetch(`/api/locations/${id}`, {
      signal: controller.signal,
      cache: "no-store",
    })
      .then(async (response) => {
        if (!response.ok) return;
        const value = (await response.json()) as {
          locationId?: string;
          imageUrl?: string;
        };
        if (!controller.signal.aborted && value.locationId === id)
          setResult({ id, imageUrl: value.imageUrl });
      })
      .catch(() => {
        /* Bundled reference photos remain available offline. */
      });
    return () => controller.abort();
  }, [id]);
  const photo = locationPhoto(
    id,
    result?.id === id ? result.imageUrl : undefined,
  );
  return photo ? <img className="guide-photo" {...photo} alt={name} /> : null;
}
