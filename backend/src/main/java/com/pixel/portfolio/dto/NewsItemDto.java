package com.pixel.portfolio.dto;

import java.time.Instant;

public class NewsItemDto {
    private long id;
    private String headline;
    private String summary;
    private String url;
    private String source;
    private String image;
    private Instant publishedAt;

    public NewsItemDto() {}

    public NewsItemDto(long id, String headline, String summary, String url, String source, String image, Instant publishedAt) {
        this.id = id;
        this.headline = headline;
        this.summary = summary;
        this.url = url;
        this.source = source;
        this.image = image;
        this.publishedAt = publishedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}

