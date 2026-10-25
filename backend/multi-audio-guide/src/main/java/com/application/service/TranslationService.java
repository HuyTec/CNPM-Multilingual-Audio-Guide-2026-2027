package com.application.service;
import com.application.component.TranslationProvider;
import com.application.dto.TranslationDto.*;
import com.application.repository.AdminContentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service @Profile("demo")
public class TranslationService {
    private final TranslationProvider provider;private final AdminContentRepository repository;
    public TranslationService(TranslationProvider provider,AdminContentRepository repository) { this.provider=provider;this.repository=repository; }
    /** Stateless: provider failure cannot overwrite a saved draft or the browser form. */
    public Result translate(Request request) {
        List<String> languages=new ArrayList<>(request.targetLanguages());languages.add(request.sourceLanguage());
        for(String language:languages) if(!repository.languageActive(language.toUpperCase(Locale.ROOT))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ngôn ngữ không hoạt động.");
        try {
            var variants=request.targetLanguages().stream().distinct().filter(l->!l.equals(request.sourceLanguage())).map(l->new Variant(l,
                provider.translate(request.name(),request.sourceLanguage(),l),provider.translate(request.full(),request.sourceLanguage(),l),provider.translate(request.shortText(),request.sourceLanguage(),l))).toList();
            return new Result(provider.name(),provider.simulated(),variants);
        } catch(Exception error) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Dịch chưa thành công hoặc quá thời gian. Nội dung được giữ; bạn có thể nhập thủ công."); }
    }
}
