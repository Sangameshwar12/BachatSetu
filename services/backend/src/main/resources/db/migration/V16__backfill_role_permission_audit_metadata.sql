-- V2 seeded identity.roles and identity.permissions with created_by/updated_by left NULL, since no
-- real actor performs a platform bootstrap insert. Every other write path in this codebase requires
-- both columns to be non-null (see JpaMappingSupport.auditInfo, used by every *JpaMapper.toDomain),
-- so any read of a seeded role or permission (for example, assigning a default role during signup)
-- fails with PersistenceMappingException. Backfill the same fixed system-actor identifier that the
-- "local" profile's CurrentAuditorProvider now assigns to future writes.
UPDATE identity.roles
SET created_by = '00000000-0000-0000-0000-000000000001',
    updated_by = '00000000-0000-0000-0000-000000000001'
WHERE created_by IS NULL OR updated_by IS NULL;

UPDATE identity.permissions
SET created_by = '00000000-0000-0000-0000-000000000001',
    updated_by = '00000000-0000-0000-0000-000000000001'
WHERE created_by IS NULL OR updated_by IS NULL;
