package com.pixel.portfolio.util;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class ResponseUtil {

	private ResponseUtil() {
	}

	public static ResponseEntity<byte[]> csvDownload(String filename, String csvContent) {
		byte[] payload = csvContent.getBytes(StandardCharsets.UTF_8);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
		headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
		headers.setContentLength(payload.length);
		return ResponseEntity.ok().headers(headers).body(payload);
	}
}

