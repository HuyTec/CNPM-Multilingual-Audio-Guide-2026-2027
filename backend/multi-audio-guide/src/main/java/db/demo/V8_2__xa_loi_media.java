package db.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;

/** Content-owner-authorized media for the Xá Lợi guide, not a geofence survey. */
public class V8_2__xa_loi_media extends BaseJavaMigration {
    private static final UUID LOCATION = UUID.fromString("20000000-0000-0000-0000-000000000108");
    private byte[] resource(String name) throws Exception {
        return new ClassPathResource("xa-loi/" + name).getContentAsByteArray();
    }
    private String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private UUID id(String label) { return UUID.nameUUIDFromBytes(label.getBytes(StandardCharsets.UTF_8)); }
    @Override public Integer getChecksum() {
        try {
            var checksum = new CRC32();
            byte[] manifest = resource("manifest.txt"); checksum.update(manifest); checksum.update(resource("photo.png"));
            for (String line : new String(manifest, StandardCharsets.UTF_8).lines().toList()) checksum.update(resource(line.split("\\|")[2]));
            return (int) checksum.getValue();
        } catch (Exception error) { throw new IllegalStateException("Missing Xá Lợi media", error); }
    }
    @Override public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        for (String line : new String(resource("manifest.txt"), StandardCharsets.UTF_8).lines().toList()) {
            String[] entry = line.split("\\|");
            byte[] bytes = resource(entry[2]);
            if (!sha(bytes).equals(entry[3])) throw new IllegalStateException("Xá Lợi audio checksum mismatch");
            UUID contentId = UUID.fromString(entry[0]), audioId = id("xa-loi-audio:" + contentId);
            try (var query = connection.prepareStatement("SELECT script_text,script_type FROM guide_content_revisions WHERE id=? AND location_id=? AND language_code='VI' AND status='APPROVED'")) {
                query.setObject(1, contentId); query.setObject(2, LOCATION);
                try (var rows = query.executeQuery()) {
                    if (!rows.next() || !sha(rows.getString(1).getBytes(StandardCharsets.UTF_8)).equals(entry[1])) throw new IllegalStateException("Xá Lợi narration does not match published text");
                    try (var insert = connection.prepareStatement("INSERT INTO admin_audio_uploads(id,location_id,language_code,script_type,filename,format,data,uploaded_by) VALUES (?,?,'VI',?,?,'mp3',?,'xa-loi-vi-HoaiMyNeural')")) {
                        insert.setObject(1,audioId); insert.setObject(2,LOCATION); insert.setString(3,rows.getString(2)); insert.setString(4,entry[2]); insert.setBytes(5,bytes); insert.executeUpdate();
                    }
                    try (var insert = connection.prepareStatement("INSERT INTO audio_assets(content_id,storage_key,format) VALUES (?,?,'mp3')")) {
                        insert.setObject(1,contentId); insert.setString(2,"/api/content-audio/" + audioId + ".mp3"); insert.executeUpdate();
                    }
                }
            }
        }
        UUID photoId=id("xa-loi-photo:Nguyen-Thanh-Quang-2010");
        try (var insert=connection.prepareStatement("INSERT INTO location_image_uploads(id,location_id,data,uploaded_by) VALUES (?,?,?,'Nguyễn Thanh Quang / Wikimedia Commons / CC BY-SA 3.0')")) {
            insert.setObject(1,photoId); insert.setObject(2,LOCATION); insert.setBytes(3,resource("photo.png")); insert.executeUpdate();
        }
        try (var update=connection.prepareStatement("UPDATE locations SET image_url=? WHERE id=?")) {
            update.setString(1,"/api/content-images/"+photoId+".png"); update.setObject(2,LOCATION); update.executeUpdate();
        }
        // Rebuild signed BASE and OPTIONAL packages through the existing worker.
        // This also publishes the removal of the retired fictional locations.
        UUID job=id("catalog-refresh:xa-loi:2026-10-27");
        try (var insert=connection.prepareStatement("INSERT INTO publication_jobs(id,status) VALUES (?,'BUILDING')")) {
            insert.setObject(1,job); insert.executeUpdate();
        }
        for (String scope : new String[]{"BASE","OPTIONAL"}) {
            try (var insert=connection.prepareStatement("INSERT INTO content_package_versions(version,job_id,scope,status) VALUES (?,?,?,'BUILDING')")) {
                insert.setString(1,"building-"+job+"-"+scope); insert.setObject(2,job); insert.setString(3,scope); insert.executeUpdate();
            }
        }
    }
}
