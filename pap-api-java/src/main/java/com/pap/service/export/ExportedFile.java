package com.pap.service.export;

import org.springframework.http.MediaType;

/** A generated download: content, its media type and the suggested file name. */
public record ExportedFile(String content, MediaType mediaType, String fileName) {
}
