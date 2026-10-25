package com.application.service;

import com.application.dto.AdminContentDto.*;
import com.application.repository.AdminContentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** UC-06 rules; SQL and JSONB mapping belong to the repository. */
@Service
@Profile("demo")
public class AdminContentService {
    private final AdminContentRepository repository;
    private final String editor;
    public AdminContentService(AdminContentRepository repository, @Value("${demo.admin.username:admin}") String editor) {
        this.repository = repository; this.editor = editor;
    }
    public Config config() { return repository.config(); }
    @Transactional(readOnly = true)
    public List<LocationDetail> list() { return repository.workspaces(null).stream().map(this::combine).toList(); }
    @Transactional(readOnly = true)
    public LocationDetail detail(UUID id) {
        return repository.workspaces(id).stream().findFirst().map(this::combine)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa điểm."));
    }
    private LocationDetail combine(Workspace workspace) {
        LocationDetail original = workspace.original(), current = workspace.draft() == null ? original : workspace.draft();
        Published published = original != null && "APPROVED".equals(original.status())
                ? new Published("APPROVED", original.name(), original.assets()) : null;
        return new LocationDetail(current.id(), displayName(current.names(), current.name()), current.category(), current.latitude(), current.longitude(),
                current.radius(), current.status(), current.packageVersion(), current.assets(), current.editVersion(),
                current.updatedBy(), current.updatedAt(), published, current.names(), current.reviewerComment());
    }
    @Transactional
    public LocationDetail save(UUID id, SaveRequest body) {
        if (!id.equals(body.id())) bad("ID địa điểm không khớp.");
        if (!List.of("DRAFT", "PENDING_REVIEW").contains(value(body.status()))) bad("Chỉ được lưu nháp hoặc gửi duyệt; không tự xuất bản.");
        Map<String, String> names = new LinkedHashMap<>();
        if (body.names() != null) body.names().forEach((language, text) -> {
            if (!language.matches("[a-z]{2,3}") || !repository.languageActive(language.toUpperCase(Locale.ROOT))) bad("Ngôn ngữ tên địa điểm không hoạt động.");
            if (value(text).length() > 255) bad("Tên địa điểm quá dài.");
            names.put(language, value(text).trim());
        });
        // Legacy clients still edit the single name field. New clients send the matching default name.
        if (!value(body.name()).isBlank() && (body.names() == null || !body.name().equals(displayName(names, "")))) names.put("vi", body.name().trim());
        String name = displayName(names, value(body.name())), category = value(body.category());
        for (String field : List.of(name, category, value(body.latitude()), value(body.longitude()), value(body.radius()))) {
            if (field.length() > 255) bad("Trường quá dài.");
        }
        if (!category.isBlank() && !repository.categoryActive(category)) bad("Danh mục không còn hoạt động.");
        if ("PENDING_REVIEW".equals(body.status())) {
            if (name.isBlank() || category.isBlank()) bad("Cần tên địa điểm và danh mục để gửi duyệt.");
            number(body.latitude(), -90, 90); number(body.longitude(), -180, 180);
            double radius = number(body.radius(), 1, Integer.MAX_VALUE);
            if (radius != Math.rint(radius)) bad("Bán kính phải là số nguyên.");
        }
        if (body.assets() == null || body.assets().size() > 100) bad("Kịch bản không hợp lệ.");
        Map<String, Asset> assets = new LinkedHashMap<>();
        for (var entry : body.assets().entrySet()) {
            String key = entry.getKey(); Asset source = entry.getValue();
            if (!key.matches("[a-z]{2,3}:(FULL|SHORT)") || source == null) bad("Khóa ngôn ngữ/loại bài không hợp lệ.");
            String[] variant = key.split(":"); String language = variant[0].toUpperCase(Locale.ROOT);
            if (!repository.languageActive(language)) bad("Ngôn ngữ không hoạt động.");
            if (!id.equals(source.locationId()) || !variant[0].equals(source.languageCode()) || !variant[1].equals(source.scriptType())) bad("Kịch bản không khớp địa điểm/ngôn ngữ/loại bài.");
            if (value(source.text()).length() > 100000) bad("Kịch bản quá dài.");
            String audio = value(source.audioUrl());
            if (!audio.isBlank() && !repository.audioBelongs(id, language, variant[1], audio)) bad("Audio không thuộc biến thể đang sửa.");
            assets.put(key, new Asset(id, variant[0], variant[1], "Chưa gán", value(source.text()), audio.isBlank() ? null : audio,
                    audio.isBlank() ? null : value(source.audioName()), audio.isBlank() ? null : "ready"));
        }
        var payload = new LocationDetail(id, name, category, value(body.latitude()), value(body.longitude()), value(body.radius()),
                body.status(), "Chưa gán phiên bản làm việc", assets, 0, null, null, null, names, null);
        if (!repository.save(payload, body.editVersion() == null ? 0 : body.editVersion(), editor))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bản làm việc đã đổi. Hãy mở lại địa điểm; nội dung đang nhập vẫn được giữ.");
        return detail(id);
    }
    public UploadResponse upload(UUID location, String language, String type, String filename, byte[] bytes) {
        language = language.toUpperCase(Locale.ROOT);
        if (!List.of("FULL", "SHORT").contains(type) || !repository.languageActive(language)) bad("Biến thể audio không hợp lệ.");
        if (bytes.length < 12 || bytes.length > 20 * 1024 * 1024) bad("Audio phải từ 12 byte đến 20 MB.");
        String name = value(filename); int dot = name.lastIndexOf('.');
        String format = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        boolean signature = switch (format) {
            case "wav" -> new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WAVE");
            case "ogg" -> new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("OggS");
            case "mp3" -> new String(bytes, 0, 3, StandardCharsets.US_ASCII).equals("ID3") || ((bytes[0] & 255) == 255 && (bytes[1] & 224) == 224);
            default -> false;
        };
        if (!signature || name.length() > 255) bad("Định dạng hoặc header audio không hợp lệ (MP3/WAV/OGG).");
        UUID id = UUID.randomUUID(); repository.insertAudio(id, location, language, type, name, format, bytes, editor);
        return new UploadResponse("/api/content-audio/" + id + "." + format, name, "ready");
    }
    public Audio audio(UUID id, String format, boolean preview) {
        return repository.audio(id, format, preview).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có audio công khai hoặc tệp không tồn tại."));
    }
    private static String value(String value) { return value == null ? "" : value; }
    private static String displayName(Map<String, String> names, String legacy) {
        if (names == null) return value(legacy);
        for (String language : List.of("vi", "en")) if (!value(names.get(language)).isBlank()) return names.get(language);
        return names.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                .filter(name -> !value(name).isBlank()).findFirst().orElse(value(legacy));
    }
    private static double number(String input, double min, double max) {
        try {
            double n = Double.parseDouble(value(input).replace(',', '.'));
            if (!Double.isFinite(n) || n < min || n > max) bad("Tọa độ hoặc bán kính không hợp lệ.");
            return n;
        } catch (NumberFormatException error) { bad("Tọa độ hoặc bán kính không hợp lệ."); return 0; }
    }
    private static void bad(String detail) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, detail); }
}
