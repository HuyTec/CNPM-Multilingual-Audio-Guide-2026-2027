package com.application.component;
public interface TranslationProvider {
    String translate(String text,String source,String target);
    String name();
    default boolean simulated() { return false; }
}
