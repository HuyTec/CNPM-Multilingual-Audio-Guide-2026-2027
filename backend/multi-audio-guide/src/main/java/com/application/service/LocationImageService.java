package com.application.service;

import com.application.repository.LocationImageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service @Profile("demo")
public class LocationImageService {
    public record Upload(String imageUrl) { }
    private final LocationImageRepository repository;
    public LocationImageService(LocationImageRepository repository) { this.repository=repository; }
    /** Inspect dimensions before decoding; re-encode to discard metadata and appended content. */
    public Upload upload(UUID location, byte[] bytes, String actor) {
        if(bytes.length==0 || bytes.length>5*1024*1024) throw invalid();
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw invalid();
            var reader=readers.next();
            try {
                String format=reader.getFormatName();
                if(!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) throw invalid();
                reader.setInput(input);
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if(w<1 || h<1 || w>4096 || h>4096 || (long)w*h>12_000_000) throw invalid();
                var image=reader.read(0);
                var output=new ByteArrayOutputStream();
                if(!ImageIO.write(image,"png",output) || output.size()>10*1024*1024) throw invalid();
                UUID id=UUID.randomUUID();repository.insert(id,location,output.toByteArray(),actor);
                return new Upload("/api/content-images/"+id+".png");
            } finally { reader.dispose(); }
        } catch(java.io.IOException | IllegalArgumentException error) { throw invalid(); }
    }
    public byte[] read(UUID id,boolean preview) {
        return repository.read(id,preview).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ảnh chưa công khai hoặc không tồn tại."));
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chọn ảnh JPEG/PNG tối đa 5 MB, mỗi chiều tối đa 4096 px và tối đa 12 triệu điểm ảnh.");
    }
}
