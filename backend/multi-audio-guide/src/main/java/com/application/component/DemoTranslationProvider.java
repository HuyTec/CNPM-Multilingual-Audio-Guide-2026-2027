package com.application.component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="translation.provider",havingValue="demo",matchIfMissing=true)
public class DemoTranslationProvider implements TranslationProvider {
    public String translate(String text,String source,String target) { return text==null || text.isBlank()?"":"[DEMO "+target+"] "+text; }
    public String name() { return "demo"; }
    public boolean simulated() { return true; }
}
