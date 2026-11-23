package com.supplygraph.application.platform;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.platform.port.out.ApiKeyRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.platform.ApiKeyId;
import com.supplygraph.domain.model.platform.ApiKeyRecord;
import com.supplygraph.domain.model.platform.ApiScope;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiDeveloperCentreService {

    private final ApiKeyRepositoryPort apiKeyRepository;

    public ApiDeveloperCentreService(ApiKeyRepositoryPort apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public record ApiKeyGenerationResult(
            String rawApiKey,
            ApiKeyRecord record
    ) {}

    public ApiKeyGenerationResult generateApiKey(WorkspaceId workspaceId, String keyName, List<ApiScope> scopes) {
        String rawToken = "sg_live_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String hashed = hashToken(rawToken);

        ApiKeyRecord record = new ApiKeyRecord(
                ApiKeyId.generate(),
                workspaceId,
                keyName,
                hashed,
                scopes,
                true,
                Instant.now()
        );
        apiKeyRepository.save(record);
        return new ApiKeyGenerationResult(rawToken, record);
    }

    public List<ApiKeyRecord> listApiKeys(WorkspaceId workspaceId) {
        return apiKeyRepository.findByWorkspaceId(workspaceId);
    }

    public ApiKeyRecord revokeApiKey(ApiKeyId id) {
        ApiKeyRecord record = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ValidationException("API Key not found: " + id.value()));

        ApiKeyRecord revoked = record.withActive(false);
        apiKeyRepository.save(revoked);
        return revoked;
    }

    public boolean validateApiKey(String rawToken, ApiScope requiredScope) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String hashed = hashToken(rawToken);
        Optional<ApiKeyRecord> opt = apiKeyRepository.findByHashedToken(hashed);
        if (opt.isEmpty()) {
            return false;
        }

        ApiKeyRecord key = opt.get();
        if (!key.active()) {
            return false;
        }

        return key.scopes().contains(ApiScope.ADMIN_ALL) || key.scopes().contains(requiredScope);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

