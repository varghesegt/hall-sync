/**
 * Robust helper to extract a clean, Windows-safe filename from a Content-Disposition header.
 */
export function extractFilename(contentDisposition?: string, fallback = "download"): string {
  if (!contentDisposition) return fallback;

  // 1. Try RFC 5987 / RFC 6266 filename*=UTF-8''encoded_name
  const rfc5987Match = contentDisposition.match(/filename\*=(?:UTF-8|utf-8)''([^;]+)/i);
  if (rfc5987Match && rfc5987Match[1]) {
    try {
      const decoded = decodeURIComponent(rfc5987Match[1].trim());
      if (decoded) return sanitizeFilename(decoded);
    } catch {}
  }

  // 2. Try RFC 2047 MIME Q/B-encoded word (e.g. =?UTF-8?Q?Internal_Exam.pdf?=)
  const mimeMatch = contentDisposition.match(/filename="?=\?UTF-8\?[QB]\?([^?"]+)\?="?/i);
  if (mimeMatch && mimeMatch[1]) {
    const decoded = mimeMatch[1].replace(/_/g, " ");
    return sanitizeFilename(decoded);
  }

  // 3. Try standard quoted filename="..."
  const quotedMatch = contentDisposition.match(/filename="([^"]+)"/i);
  if (quotedMatch && quotedMatch[1]) {
    return sanitizeFilename(quotedMatch[1]);
  }

  // 4. Try unquoted filename=...
  const unquotedMatch = contentDisposition.match(/filename=([^;]+)/i);
  if (unquotedMatch && unquotedMatch[1]) {
    return sanitizeFilename(unquotedMatch[1]);
  }

  return sanitizeFilename(fallback);
}

/**
 * Strips quotes, illegal Windows characters (< > : " / \ | ? *), and trims whitespace.
 */
export function sanitizeFilename(name: string): string {
  return name
    .replace(/^["']|["']$/g, "")
    .replace(/[<>:"/\\|?*]/g, "_")
    .trim();
}

/**
 * Safely triggers a browser file download from a Blob.
 * If the server returned an error JSON inside a Blob (e.g., 404/500/400),
 * this extracts the server error message and throws an Error.
 */
export async function triggerFileDownload(blob: Blob, filename: string): Promise<void> {
  if (blob.type === "application/json" || (blob.type.includes("json") && blob.size < 2048)) {
    try {
      const text = await blob.text();
      const parsed = JSON.parse(text);
      const msg = parsed.message || parsed.error || "Failed to download file.";
      throw new Error(msg);
    } catch (e: any) {
      if (e.message && e.message !== "Failed to download file.") {
        throw e;
      }
    }
  }

  const clean = sanitizeFilename(filename);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.style.display = "none";
  a.href = url;
  a.download = clean;
  document.body.appendChild(a);
  a.click();

  setTimeout(() => {
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, 1000);
}
