package com.example.eventsphere.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum FileType {

    IMAGE(List.of("image/jpeg", "image/png", "image/jpg")),
    DOCUMENT(List.of("application/pdf", "application/msword")),
    VIDEO(List.of("video/quicktime", "video/mp4","video/webm","video/mpeg"));

    private final List<String> mimeTypes;

    FileType(List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

}
