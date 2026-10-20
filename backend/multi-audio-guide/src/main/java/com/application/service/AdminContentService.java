package com.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Authenticated UC-06 workspace in PostgreSQL; never edits a published revision in place. */
@Service
@Profile("demo")
public class AdminContentService {
    private final JdbcTemplate jdbc;
    private final String editor;
    private final JsonMapper mapper = JsonMapper.builder().build();
    public AdminContentService(JdbcTemplate jdbc, @Value("${demo.admin.username:admin}") String editor) { this.jdbc = jdbc; this.editor = editor; }

    public Map<String, Object> config() {
        return Map.of("categories", jdbc.queryForList("SELECT name FROM categories WHERE is_active ORDER BY name", String.class),
                "languages", jdbc.query("SELECT code,display_name FROM languages WHERE is_active ORDER BY code", (rs, row) ->
                        Map.of("code", rs.getString(1).toLowerCase(Locale.ROOT), "label", rs.getString(2))));
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        List<UUID> ids = jdbc.queryForList("SELECT id FROM locations UNION SELECT location_id FROM admin_content_drafts ORDER BY 1", UUID.class);
        return ids.stream().map(this::detail).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> detail(UUID id) {
        Map<String, Object> published = published(id);
        var drafts = jdbc.query("SELECT payload::text,status,edit_version,updated_by,updated_at FROM admin_content_drafts WHERE location_id=?",
                (rs, row) -> {
                    Map<String, Object> result = json(rs.getString(1));
                    result.put("status", rs.getString(2)); result.put("editVersion", rs.getInt(3));
                    result.put("updatedBy", rs.getString(4)); result.put("updatedAt", rs.getTimestamp(5).toInstant().toString());
                    return result;
                }, id);
        Map<String, Object> result = drafts.isEmpty() ? published : drafts.getFirst();
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa điểm.");
        result = new LinkedHashMap<>(result);
        if (published != null && "APPROVED".equals(published.get("status"))) {
            result.put("published", Map.of("version", "APPROVED", "name", published.get("name"), "assets", published.get("assets")));
        }
        return result;
    }
    private Map<String, Object> published(UUID id) {
        var records = jdbc.query("""
                SELECT l.id,l.name,c.name AS category,l.status,ST_Y(l.coordinates::geometry) AS lat,
                       ST_X(l.coordinates::geometry) AS lng,l.geofence_radius_m AS radius
                FROM locations l LEFT JOIN categories c ON c.id=l.category_id WHERE l.id=?
                """, (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("id", id.toString()); item.put("name", value(rs.getString("name")));
            item.put("category", value(rs.getString("category"))); item.put("latitude", value(rs.getString("lat")));
            item.put("longitude", value(rs.getString("lng"))); item.put("radius", value(rs.getString("radius")));
            String status = rs.getString("status"); item.put("status", status.equals("APPROVED") ? status : "DRAFT");
            item.put("packageVersion", "Chưa gán phiên bản làm việc"); item.put("editVersion", 0);
            Map<String, Object> assets = new LinkedHashMap<>();
            jdbc.query("""
                    SELECT DISTINCT ON (g.language_code,g.script_type) g.language_code,g.script_type,g.script_text,a.storage_key
                    FROM guide_content_revisions g LEFT JOIN audio_assets a ON a.content_id=g.id
                    WHERE g.location_id=? AND g.status<>'ARCHIVED' AND (g.status='APPROVED' OR ?<>'APPROVED')
                    ORDER BY g.language_code,g.script_type,g.revision_no DESC,g.id
                    """, guide -> {
                String language = guide.getString(1).toLowerCase(Locale.ROOT), type = guide.getString(2);
                Map<String, Object> asset = new LinkedHashMap<>(); asset.put("locationId", id.toString()); asset.put("languageCode", language);
                asset.put("scriptType", type); asset.put("text", value(guide.getString(3))); asset.put("packageVersion", "APPROVED");
                String audio = guide.getString(4);
                if (audio != null) { asset.put("audioUrl", audio); asset.put("audioName", audio.substring(audio.lastIndexOf('/')+1)); asset.put("audioStatus", "ready"); }
                assets.put(language+":"+type, asset);
            }, id, status);
            item.put("assets", assets); return item;
        }, id);
        return records.isEmpty() ? null : records.getFirst();
    }
    @Transactional
    public Map<String, Object> save(UUID id, Map<String, Object> body) {
        if (!id.toString().equals(body.get("id"))) bad("ID địa điểm không khớp.");
        String status = text(body, "status");
        if (!List.of("DRAFT", "PENDING_REVIEW").contains(status)) bad("Chỉ được lưu nháp hoặc gửi duyệt; không tự xuất bản.");
        Map<String, Object> payload = new LinkedHashMap<>(); payload.put("id", id.toString());
        for (String field : List.of("name", "category", "latitude", "longitude", "radius")) {
            String input = text(body, field); if (input.length() > 255) bad("Trường " + field + " quá dài."); payload.put(field, input);
        }
        String category = text(body,"category");
        if (!category.isBlank() && jdbc.queryForObject("SELECT count(*) FROM categories WHERE name=? AND is_active", Integer.class, category)==0) bad("Danh mục không còn hoạt động.");
        if (status.equals("PENDING_REVIEW")) {
            if (text(body,"name").isBlank() || category.isBlank()) bad("Cần tên địa điểm và danh mục để gửi duyệt.");
            number(body,"latitude",-90,90); number(body,"longitude",-180,180); number(body,"radius",1,Integer.MAX_VALUE);
            double radius = Double.parseDouble(text(body,"radius").replace(',', '.')); if (radius != Math.rint(radius)) bad("Bán kính phải là số nguyên.");
        }
        Object raw = body.get("assets"); if (!(raw instanceof Map<?, ?>) || ((Map<?, ?>) raw).size()>100) bad("Kịch bản không hợp lệ.");
        Map<String, Object> assets = new LinkedHashMap<>();
        for (var entry : ((Map<?, ?>)raw).entrySet()) {
            if (!(entry.getKey() instanceof String key) || !key.matches("[a-z]{2,3}:(FULL|SHORT)")) bad("Khóa ngôn ngữ/loại bài không hợp lệ.");
            String key = entry.getKey().toString(); String[] variant = key.split(":");
            if (jdbc.queryForObject("SELECT count(*) FROM languages WHERE code=? AND is_active", Integer.class, variant[0].toUpperCase(Locale.ROOT))==0) bad("Ngôn ngữ không hoạt động.");
            if (!(entry.getValue() instanceof Map<?, ?>)) bad("Kịch bản không hợp lệ.");
            @SuppressWarnings("unchecked") Map<String,Object> source=(Map<String,Object>)entry.getValue();
            if (!id.toString().equals(source.get("locationId")) || !variant[0].equals(source.get("languageCode")) || !variant[1].equals(source.get("scriptType"))) bad("Kịch bản không khớp địa điểm/ngôn ngữ/loại bài.");
            String script = text(source,"text"); if (script.length()>100000) bad("Kịch bản quá dài.");
            Map<String,Object> asset=new LinkedHashMap<>(); asset.put("locationId",id.toString()); asset.put("languageCode",variant[0]); asset.put("scriptType",variant[1]);
            asset.put("text",script); asset.put("packageVersion","Chưa gán");
            String audio=text(source,"audioUrl");
            if (!audio.isBlank()) {
                boolean own = jdbc.queryForObject("SELECT count(*) FROM admin_audio_uploads WHERE location_id=? AND language_code=? AND script_type=? AND '/api/content-audio/'||id::text||'.'||format=?", Integer.class,id,variant[0].toUpperCase(Locale.ROOT),variant[1],audio)>0;
                boolean original=jdbc.queryForObject("SELECT count(*) FROM audio_assets a JOIN guide_content_revisions g ON g.id=a.content_id WHERE g.location_id=? AND g.language_code=? AND g.script_type=? AND a.storage_key=?",Integer.class,id,variant[0].toUpperCase(Locale.ROOT),variant[1],audio)>0;
                if (!own && !original) bad("Audio không thuộc biến thể đang sửa.");
                asset.put("audioUrl",audio); asset.put("audioName",text(source,"audioName")); asset.put("audioStatus","ready");
            }
            assets.put(key,asset);
        }
        payload.put("assets",assets); payload.put("packageVersion","Chưa gán phiên bản làm việc");
        int expected = body.get("editVersion") instanceof Number n ? n.intValue() : 0;
        int rows;
        if (expected==0) rows=jdbc.update("INSERT INTO admin_content_drafts(location_id,payload,status,edit_version,updated_by) VALUES (?,?::jsonb,?,1,?) ON CONFLICT DO NOTHING",id,mapper.writeValueAsString(payload),status,editor);
        else rows=jdbc.update("UPDATE admin_content_drafts SET payload=?::jsonb,status=?,edit_version=edit_version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE location_id=? AND edit_version=?",mapper.writeValueAsString(payload),status,editor,id,expected);
        if (rows!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Bản làm việc đã đổi. Hãy mở lại địa điểm; nội dung đang nhập vẫn được giữ.");
        return detail(id);
    }
    public Map<String,Object> upload(UUID location,String language,String type,String filename,byte[] bytes) {
        language=language.toUpperCase(Locale.ROOT);
        if (!List.of("FULL","SHORT").contains(type) || jdbc.queryForObject("SELECT count(*) FROM languages WHERE code=? AND is_active",Integer.class,language)==0) bad("Biến thể audio không hợp lệ.");
        if (bytes.length<12 || bytes.length>20*1024*1024) bad("Audio phải từ 12 byte đến 20 MB.");
        String name=value(filename); int dot=name.lastIndexOf('.'); String format=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        boolean signature=switch(format) { case "wav" -> new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WAVE");
            case "ogg" -> new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("OggS");
            case "mp3" -> new String(bytes,0,3,java.nio.charset.StandardCharsets.US_ASCII).equals("ID3") || ((bytes[0]&255)==255 && (bytes[1]&224)==224); default -> false; };
        if (!signature || name.length()>255) bad("Định dạng hoặc header audio không hợp lệ (MP3/WAV/OGG).");
        UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO admin_audio_uploads(id,location_id,language_code,script_type,filename,format,data,uploaded_by) VALUES (?,?,?,?,?,?,?,?)",id,location,language,type,name,format,bytes,editor);
        return Map.of("audioUrl","/api/content-audio/"+id+"."+format,"audioName",name,"audioStatus","ready");
    }
    public record Audio(byte[] bytes,String format) { }
    public Audio audio(UUID id,String format,boolean preview) {
        String url="/api/content-audio/"+id+"."+format;
        var files=jdbc.query("SELECT data,format FROM admin_audio_uploads u WHERE u.id=? AND u.format=? AND (? OR EXISTS(SELECT 1 FROM audio_assets a JOIN guide_content_revisions g ON g.id=a.content_id JOIN locations l ON l.id=g.location_id WHERE a.storage_key=? AND g.status='APPROVED' AND l.status='APPROVED'))",(rs,row)->new Audio(rs.getBytes(1),rs.getString(2)),id,format,preview,url);
        if(files.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Chưa có audio công khai hoặc tệp không tồn tại."); return files.getFirst();
    }
    @SuppressWarnings("unchecked") private Map<String,Object> json(String text) { return mapper.readValue(text,Map.class); }
    private static String value(String value) { return value==null?"":value; }
    private static String text(Map<String,Object> value,String key) { Object item=value.get(key); if(item==null)return ""; if(!(item instanceof String))bad("Trường "+key+" phải là text.");return item.toString(); }
    private static void number(Map<String,Object> value,String key,double min,double max) { try { String s=text(value,key); double n=Double.parseDouble(s.replace(',','.'));if(s.isBlank()||!Double.isFinite(n)||n<min||n>max)bad("Giá trị "+key+" không hợp lệ."); } catch(NumberFormatException error){bad("Giá trị "+key+" không hợp lệ.");} }
    private static void bad(String detail) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,detail); }
}
