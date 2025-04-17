package com.brandongcobb.vyrtuous.utils.handlers;

public class MessageContent {
    private final String type;
    private final String text;
    private final String imageData; // Only for images
    private final String contentType; // Content type for attachments
    public MessageContent(String type, String content) {
        this.type = type;
        this.text = content;
        this.imageData = null;
        this.contentType = null;
    }
    public MessageContent(String type, String imageData, String contentType) {
        this.type = type;
        this.text = null;
        this.imageData = imageData;
        this.contentType = contentType;
    }
    public String getType() {
        return type;
    }
    public String getText() {
        return text;
    }
    public String getImageData() {
        return imageData;
    }
    public String getContentType() {
        return contentType;
    }
}
