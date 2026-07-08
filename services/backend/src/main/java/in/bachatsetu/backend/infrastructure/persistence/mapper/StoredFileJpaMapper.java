package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.storage.StoredFileJpaEntity;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface StoredFileJpaMapper {

    default StoredFile toDomain(StoredFileJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StoredFile(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                entity.getProvider(),
                entity.getPath(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getSize(),
                entity.getChecksum(),
                entity.getUploadedAt(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default StoredFileJpaEntity toEntity(StoredFile domain) {
        if (domain == null) {
            return null;
        }
        return new StoredFileJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                domain.provider(),
                domain.path(),
                domain.originalFilename(),
                domain.contentType(),
                domain.checksum(),
                domain.size(),
                domain.uploadedAt());
    }
}
