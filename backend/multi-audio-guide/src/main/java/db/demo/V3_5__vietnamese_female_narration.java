package db.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;

/** Demo-only recordings, bound to the exact published Vietnamese text. */
public class V3_5__vietnamese_female_narration extends BaseJavaMigration {
    private byte[] resource(String name) throws Exception {
        return new ClassPathResource("demo-narration/" + name).getContentAsByteArray();
    }
    private String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    @Override public Integer getChecksum() {
        try {
            var checksum = new CRC32();
            byte[] manifest = resource("manifest.txt"); checksum.update(manifest);
            for (String line : new String(manifest, StandardCharsets.UTF_8).lines().toList()) {
                checksum.update(resource(line.split("\\|")[2]));
            }
            return (int) checksum.getValue();
        } catch (Exception error) { throw new IllegalStateException("Missing demo narration", error); }
    }
    @Override public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        for (String line : new String(resource("manifest.txt"), StandardCharsets.UTF_8).lines().toList()) {
            String[] entry = line.split("\\|");
            UUID contentId = UUID.fromString(entry[0]);
            byte[] bytes = resource(entry[2]);
            if (!sha(bytes).equals(entry[3])) throw new IllegalStateException("Demo audio checksum mismatch");
            try (var query = connection.prepareStatement("SELECT location_id,script_type,script_text FROM guide_content_revisions g WHERE id=? AND language_code='VI' AND status='APPROVED' AND NOT EXISTS(SELECT 1 FROM audio_assets a WHERE a.content_id=g.id)")) {
                query.setObject(1, contentId);
                try (var rows = query.executeQuery()) {
                    // Never attach stale speech to edited text or overwrite an admin's audio.
                    if (!rows.next() || !sha(rows.getString(3).getBytes(StandardCharsets.UTF_8)).equals(entry[1])) continue;
                    UUID audioId = UUID.nameUUIDFromBytes(("vi-HoaiMy-demo:" + contentId).getBytes(StandardCharsets.UTF_8));
                    try (var insert = connection.prepareStatement("INSERT INTO admin_audio_uploads(id,location_id,language_code,script_type,filename,format,data,uploaded_by) VALUES (?,?,'VI',?,?,'mp3',?,'demo-vi-HoaiMyNeural')")) {
                        insert.setObject(1, audioId); insert.setObject(2, rows.getObject(1)); insert.setString(3, rows.getString(2));
                        insert.setString(4, entry[2]); insert.setBytes(5, bytes); insert.executeUpdate();
                    }
                    try (var insert = connection.prepareStatement("INSERT INTO audio_assets(content_id,storage_key,format) VALUES (?,?,'mp3')")) {
                        insert.setObject(1, contentId); insert.setString(2, "/api/content-audio/" + audioId + ".mp3"); insert.executeUpdate();
                    }
                }
            }
        }
    }
}
