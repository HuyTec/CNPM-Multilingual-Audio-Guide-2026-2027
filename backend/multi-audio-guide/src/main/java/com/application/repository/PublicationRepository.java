package com.application.repository;

import com.application.dto.AdminContentDto.*;
import com.application.dto.ContentPackageDto.*;
import com.application.dto.PublicationDto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** Durable jobs, immutable package bytes and SQL for promoting an editor snapshot. */
@Repository @Profile("demo")
public class PublicationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json=JsonMapper.builder().build();
    public PublicationRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public boolean decide(UUID location,int expected,String status,String comment,String reviewer) {
        return jdbc.update("""
                UPDATE admin_content_drafts SET status=:s,reviewer_comment=:c,updated_by=:u,
                edit_version=edit_version+1,updated_at=CURRENT_TIMESTAMP
                WHERE location_id=:id AND edit_version=:e AND status='PENDING_REVIEW'
                """,Map.of("s",status,"c",comment==null?"":comment,"u",reviewer,"id",location,"e",expected))==1;
    }
    public Job enqueue(LocationDetail payload) {
        UUID id=UUID.randomUUID();
        var params=new HashMap<String,Object>();params.put("id",id);params.put("l",payload==null?null:payload.id());params.put("e",payload==null?null:payload.editVersion());params.put("p",payload==null?null:json.writeValueAsString(payload));
        jdbc.update("INSERT INTO publication_jobs(id,location_id,edit_version,payload,status) VALUES(:id,:l,:e,CAST(:p AS jsonb),'BUILDING')",params);
        for(String scope:List.of("BASE","OPTIONAL")) jdbc.update("INSERT INTO content_package_versions(version,job_id,scope,status) VALUES(:v,:id,:s,'BUILDING')",Map.of("v","building-"+id+"-"+scope,"id",id,"s",scope));
        return job(id).orElseThrow();
    }
    public List<Job> jobs() { return query("ORDER BY created_at DESC LIMIT 50",Map.of()); }
    public Optional<Job> job(UUID id) { return query("WHERE id=:id",Map.of("id",id)).stream().findFirst(); }
    public List<Job> pending() { return query("WHERE status='BUILDING' ORDER BY created_at LIMIT 10",Map.of()); }
    private List<Job> query(String suffix,Map<String,?> params) {
        return jdbc.query("SELECT id,location_id,edit_version,payload::text,status,error_log,created_at FROM publication_jobs "+suffix,params,
                (r,n)->new Job(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,Integer.class),r.getString(4)==null?null:json.readValue(r.getString(4),LocationDetail.class),r.getString(5),r.getString(6),r.getTimestamp(7).toInstant()));
    }
    public void lock() { jdbc.queryForObject("SELECT pg_advisory_xact_lock(7100626)",Map.of(),(r,n)->0); }
    /** Called only inside the same transaction as building and marking all scopes READY. */
    public void promote(LocationDetail p) {
        var params=new HashMap<String,Object>();params.put("id",p.id());params.put("n",p.name());params.put("c",p.category());
        params.put("image",p.imageUrl());
        params.put("lat",Double.parseDouble(p.latitude().replace(',','.')));params.put("lng",Double.parseDouble(p.longitude().replace(',','.')));params.put("r",(int)Double.parseDouble(p.radius().replace(',','.')));
        jdbc.update("""
                INSERT INTO locations(id,name,category_id,coordinates,geofence_radius_m,status,image_url)
                VALUES(:id,:n,(SELECT id FROM categories WHERE name=:c AND is_active),ST_SetSRID(ST_MakePoint(:lng,:lat),4326)::geography,:r,'APPROVED',:image)
                ON CONFLICT(id) DO UPDATE SET name=EXCLUDED.name,category_id=EXCLUDED.category_id,coordinates=EXCLUDED.coordinates,geofence_radius_m=EXCLUDED.geofence_radius_m,status='APPROVED',image_url=EXCLUDED.image_url
                """,params);
        jdbc.update("DELETE FROM location_translations WHERE location_id=:id",Map.of("id",p.id()));
        for(var entry:p.names().entrySet()) if(!entry.getValue().isBlank()) jdbc.update("INSERT INTO location_translations(location_id,language_code,name) VALUES(:id,:l,:n)",Map.of("id",p.id(),"l",entry.getKey().toUpperCase(Locale.ROOT),"n",entry.getValue()));
        // Retain historical revisions for old offline packages and playback receipts.
        jdbc.update("UPDATE guide_content_revisions SET is_current=FALSE WHERE location_id=:id",Map.of("id",p.id()));
        for(var a:p.assets().values()) {
            if(a.text()==null || a.text().isBlank()) continue;
            UUID content=UUID.randomUUID();String lang=a.languageCode().toUpperCase(Locale.ROOT);
            jdbc.update("""
                    INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,script_text,status,revision_no)
                    SELECT :id,:l,:lang,:t,:text,'APPROVED',COALESCE(MAX(revision_no),0)+1 FROM guide_content_revisions
                    WHERE location_id=:l AND language_code=:lang AND script_type=:t
                    """,Map.of("id",content,"l",p.id(),"lang",lang,"t",a.scriptType(),"text",a.text()));
            if(a.audioUrl()!=null && !a.audioUrl().isBlank()) {
                String format=a.audioUrl().substring(a.audioUrl().lastIndexOf('.')+1);
                jdbc.update("INSERT INTO audio_assets(content_id,storage_key,format) VALUES(:c,:u,:f)",Map.of("c",content,"u",a.audioUrl(),"f",format));
            }
        }
    }
    public void ready(UUID job,Bundle bundle) {
        var m=bundle.manifest();
        jdbc.update("DELETE FROM content_package_versions WHERE job_id=:j AND scope=:s AND status='BUILDING'",Map.of("j",job,"s",m.scope()));
        jdbc.update("""
                INSERT INTO content_package_versions(version,job_id,scope,status,checksum,signature,size_bytes,manifest,payload,ready_at)
                VALUES(:v,:j,:s,'READY',:c,:sig,:size,CAST(:m AS jsonb),:p,clock_timestamp())
                ON CONFLICT(version) DO UPDATE SET ready_at=EXCLUDED.ready_at
                """,Map.of("v",m.version(),"j",job,"s",m.scope(),"c",m.sha256(),"sig",m.signature(),"size",m.sizeBytes(),"m",json.writeValueAsString(m),"p",bundle.bytes()));
    }
    public void complete(Job job) {
        jdbc.update("UPDATE publication_jobs SET status='READY',completed_at=CURRENT_TIMESTAMP,error_log=NULL WHERE id=:j",Map.of("j",job.id()));
        if(job.locationId()!=null) jdbc.update("UPDATE admin_content_drafts SET status='PUBLISHED',updated_at=CURRENT_TIMESTAMP WHERE location_id=:l AND status='APPROVED' AND edit_version=:e",Map.of("l",job.locationId(),"e",job.editVersion()));
    }
    public void fail(UUID id,String error) {
        String safe=error==null?"Không tạo được gói":error.substring(0,Math.min(error.length(),2000));
        jdbc.update("UPDATE publication_jobs SET status='FAILED',error_log=:e WHERE id=:j AND status='BUILDING'",Map.of("j",id,"e",safe));
        jdbc.update("UPDATE content_package_versions SET status='FAILED',error_log=:e WHERE job_id=:j AND status='BUILDING'",Map.of("j",id,"e",safe));
    }
    public boolean retry(UUID id) {
        int changed=jdbc.update("UPDATE publication_jobs SET status='BUILDING',error_log=NULL WHERE id=:j AND status='FAILED'",Map.of("j",id));
        if(changed==1) jdbc.update("UPDATE content_package_versions SET status='BUILDING',error_log=NULL WHERE job_id=:j AND status='FAILED'",Map.of("j",id));
        return changed==1;
    }
    public List<Manifest> manifests() {
        return jdbc.query("SELECT DISTINCT ON(scope) manifest::text FROM content_package_versions WHERE status='READY' ORDER BY scope,ready_at DESC,version DESC",Map.of(),(r,n)->json.readValue(r.getString(1),Manifest.class));
    }
    public Optional<Bundle> bundle(String scope,String version) {
        return jdbc.query("SELECT manifest::text,payload FROM content_package_versions WHERE status='READY' AND scope=:s AND version=:v",Map.of("s",scope,"v",version),(r,n)->new Bundle(json.readValue(r.getString(1),Manifest.class),r.getBytes(2))).stream().findFirst();
    }
}
