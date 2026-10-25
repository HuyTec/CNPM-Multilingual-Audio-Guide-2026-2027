package com.application.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public final class TranslationDto {
    private TranslationDto() { }
    public record Request(@NotBlank @Pattern(regexp="[a-z]{2,3}") String sourceLanguage,@NotNull @Size(min=1,max=5) List<@NotBlank @Pattern(regexp="[a-z]{2,3}") String> targetLanguages,
                          @NotBlank @Size(max=255) String name,@Size(max=10000) String full,@Size(max=10000) String shortText) { }
    public record Variant(String language,String name,String full,String shortText) { }
    public record Result(String provider,boolean simulated,List<Variant> variants) { }
}
