package com.example.speech.aiservice.vn.model.enums;

public enum FontSizeType {

    ULTRA_LEGEND(180f),

    HUGE(130f),
    TITLE_LARGE(100f), // Very large, for prominent titles
    TITLE(90f), // Main title
    CHAPTER_LARGE(70f), // Larger chapter title
    CHAPTER(60f), // Chapter title
    SUBTEXT(45f), // Subtitle or subdescription
    CAPTION(35f), // Small caption text
    NOTE(30f); // Very small notes

    private final float size;

    FontSizeType(float size) {
        this.size = size;
    }

    public float getSize() {
        return size;
    }
}
