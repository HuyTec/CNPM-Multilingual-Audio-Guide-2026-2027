package com.application.dto;
import jakarta.validation.constraints.*;
import com.application.dto.AdminContentDto.LocationDetail;
import java.time.Instant;
import java.util.UUID;
public final class PublicationDto {
    private PublicationDto() { }
    public record Decision(@Min(1) int editVersion,@Size(max=4000) String comment) { }
    public record Job(UUID id,UUID locationId,Integer editVersion,LocationDetail payload,String status,String errorLog,Instant createdAt) { }
}
