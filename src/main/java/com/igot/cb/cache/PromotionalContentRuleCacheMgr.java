package com.igot.cb.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.cassandra.CassandraOperation;
import com.igot.cb.model.CachedAccessSettingRule;
import com.igot.cb.util.CbExtServerProperties;
import com.igot.cb.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager for promotional content access rules.
 * Provides caching using Caffeine cache with configurable TTL.
 * Converts criteria values to BitSets for efficient rule evaluation.
 */
@Component
@Slf4j
public class PromotionalContentRuleCacheMgr {
    private static final String PROMOTIONAL_CONTENT_CACHE_KEY = "promotionalContentRules";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedisCacheMgr redisCacheMgr;
    private final CassandraOperation cassandraOperation;
    private final CbExtServerProperties properties;
    Map<String, CachedAccessSettingRule> cacheMap = new ConcurrentHashMap<>();
    @Value("${promotional.content.rules.cache.expiry.ms}")
    private Integer promotionalContentRulesCacheExpiryMs;
    /**
     * Constructs the cache manager with required dependencies.
     */
    public PromotionalContentRuleCacheMgr(RedisCacheMgr redisCacheMgr,
                                          CassandraOperation cassandraOperation,
                                          CbExtServerProperties properties) {
        this.redisCacheMgr = redisCacheMgr;
        this.cassandraOperation = cassandraOperation;
        this.properties = properties;
    }

    /**
     * Retrieves all cached access rules.
     * Returns values from indexed cache (O(1) per rule lookup).
     * Automatically reloads from database when cache is empty or expired.
     *
     * @return collection of cached rules, empty collection if none available
     */
    public Collection<CachedAccessSettingRule> getAccessSettingRules() {
        Collection<CachedAccessSettingRule> cachedRules = cacheMap.values();
        if (CollectionUtils.isEmpty(cachedRules)) {
            log.info("Cache is empty (size: {}), loading from database", cacheMap.size());
            loadAccessSettingRules();
            cachedRules = cacheMap.values();
            log.info("After reload, cache contains {} rules", cachedRules.size());
            return cachedRules;
        }
        for (CachedAccessSettingRule rule : cachedRules) {
            if (rule == null) {
                log.warn("Found null cached rule entry, triggering reload");
                loadAccessSettingRules();
                return cacheMap.values();
            }
            try {
                if (rule.isExpired(promotionalContentRulesCacheExpiryMs)) {
                    log.info("Found expired rule (key={}), reloading cache", rule.getCacheKey());
                    loadAccessSettingRules();
                    return cacheMap.values();
                }
            } catch (Exception e) {
                log.warn("Error checking expiry for rule {}: {}. Triggering reload.", rule.getCacheKey(), e.getMessage());
                loadAccessSettingRules();
                return cacheMap.values();
            }
        }
        return cachedRules;
    }

    /**
     * Loads access rules from Cassandra into indexed cache.
     * Filters out archived records at database level.
     * Implements pagination to fetch all records in batches for better performance.
     * Uses configurable batch size for each query and max query size as upper limit.
     * Optimized with Java 17 parallel streams for multi-core CPU utilization.
     */
    private void loadAccessSettingRules() {
        int batchSize = properties.getPromotionalContentCacheBatchSize();
        int maxQuerySize = properties.getPromotionalContentCacheMaxQuerySize();

        log.info("Loading promotional content rules from Redis or database - Batch size: {}, Max query size: {}",
                batchSize, maxQuerySize);
        cacheMap.clear();
        try {
            Map<String, String> cachedRules = redisCacheMgr.getAllCachedAccessRules(PROMOTIONAL_CONTENT_CACHE_KEY);
            if (MapUtils.isNotEmpty(cachedRules)) {
                cachedRules.forEach((cacheKey, jsonValue) -> {
                    try {
                        CachedAccessSettingRule rule = new CachedAccessSettingRule(jsonValue);
                        processAndCacheRule(rule);
                        cacheMap.put(cacheKey, rule);
                    } catch (Exception e) {
                        log.error("Failed to load promotional content rule from Redis for key: {}", cacheKey, e);
                    }
                });
                log.info("Promotional content rules loaded from Redis successfully. Total rules loaded: {}",
                        cacheMap.size());
                return;
            }

            int totalFetched = cassandraOperation.forEachRecordByPropertiesPaged(
                    Constants.KEYSPACE_SUNBIRD_COURSE,
                    Constants.PROMOTIONAL_CONTENT_RULES,
                    null,
                    null,
                    batchSize,
                    maxQuerySize,
                    rec -> {
                        try {
                            Boolean isArchived = (Boolean) rec.get(Constants.IS_ARCHIVED_KEY);
                            if (Boolean.TRUE.equals(isArchived)) {
                                return;
                            }
                            CachedAccessSettingRule rule = new CachedAccessSettingRule(
                                    (String) rec.get("contextId"),
                                    (String) rec.get("contextIdType"),
                                    (String) rec.get("contextData"),
                                    false);
                            processAndCacheRule(rule);
                            cacheMap.put(rule.getCacheKey(), rule);
                            redisCacheMgr.setHashValue(PROMOTIONAL_CONTENT_CACHE_KEY, rule.getCacheKey(),
                                    buildRedisRulePayload(rule.getContextId(), rule.getContextIdType(), rule.getContextData()));
                        } catch (Exception e) {
                            log.error("Error processing promotional content rule record from Cassandra", e);
                        }
                    }
            );
            if (totalFetched >= maxQuerySize) {
                log.warn("Reached maximum query size limit of {}. There may be more records in the database. " +
                        "Consider increasing promotional.content.cache.max.query.size", maxQuerySize);
            }
            if (cacheMap.isEmpty()) {
                log.warn("No access setting rules found in database");
                return;
            }
            log.info("Promotional Content rules loaded into cache successfully. Total rules loaded: {}",
                    cacheMap.size());
        } catch (Exception e) {
            log.error("Failed to load Promotional Content rules into Cache. Exception: ", e);
        }
    }

    /**
     * Processes a rule and converts criteria values to BitSets for efficient evaluation.
     */
    private void processAndCacheRule(CachedAccessSettingRule rule) {
        try {
            Map<String, Object> contextData = rule.getContextData();
            if (MapUtils.isEmpty(contextData)) {
                log.warn("No contextData found for rule: {}", rule.getCacheKey());
                return;
            }
            processContextData(rule.getCacheKey(), contextData);
        } catch (Exception e) {
            log.error("Error processing rule {}", rule.getCacheKey(), e);
        }
    }

    /**
     * Processes context data by extracting access control and user groups.
     */
    @SuppressWarnings("unchecked")
    private void processContextData(String cacheKey, Map<String, Object> contextData) {
        Map<String, Object> accessControl = (Map<String, Object>) contextData.get(Constants.ACCESS_CONTROL_ID);
        if (accessControl == null) {
            log.warn("No accessControl found for rule: {}", cacheKey);
            return;
        }
        List<Map<String, Object>> userGroups =
                (List<Map<String, Object>>) accessControl.get(Constants.USER_GROUPS);
        if (userGroups == null || userGroups.isEmpty()) {
            log.warn("No userGroups found for rule: {}", cacheKey);
            return;
        }
        for (Map<String, Object> userGroup : userGroups) {
            processUserGroup(userGroup, cacheKey);
        }
    }

    /**
     * Processes a user group by iterating through its criteria list.
     */
    @SuppressWarnings("unchecked")
    private void processUserGroup(Map<String, Object> userGroup, String cacheKey) {
        String userGroupId = (String) userGroup.get(Constants.USER_GROUP_ID);
        String userGroupName = (String) userGroup.get(Constants.USER_GROUP_NAME);
        List<Map<String, Object>> criteriaList =
                (List<Map<String, Object>>) userGroup.get(Constants.USER_GROUP_CRITERIA_LIST);
        if (CollectionUtils.isEmpty(criteriaList)) {
            log.warn("No userGroupCriteriaList for userGroupId {} in rule {}", userGroupId, cacheKey);
            return;
        }
        for (Map<String, Object> criteria : criteriaList) {
            processCriteria(criteria, userGroupId, userGroupName, cacheKey);
        }
    }

    /**
     * Processes a single criterion by converting its values from list to BitSet.
     * BitSets enable O(1) membership checking during rule evaluation.
     */
    private void processCriteria(Map<String, Object> criteria, String userGroupId, String userGroupName, String cacheKey) {
        String criteriaKey = (String) criteria.get(Constants.CRITERIA_KEY);
        Object criteriaValuesObj = criteria.get(Constants.CRITERIA_VALUE);
        if (criteriaValuesObj instanceof BitSet) {
            return;
        }
        if (StringUtils.isEmpty(criteriaKey) || !(criteriaValuesObj instanceof List<?> criteriaValues) || CollectionUtils.isEmpty(criteriaValues)) {
            log.warn("Missing key or values in criteria for userGroupId {} in rule {}", userGroupId, cacheKey);
            return;
        }
        log.debug("Rule {} -> UserGroup {} ({}) -> CriteriaKey {} -> Values {}",
                cacheKey, userGroupName, userGroupId, criteriaKey, criteriaValues);
        List<Integer> intValues = criteriaValues.stream()
                .map(Object::toString)
                .map(val -> parseIntegerValue(val, criteriaKey, cacheKey))
                .filter(Objects::nonNull)
                .toList();
        BitSet bitSet = createBitSetForAttribute(intValues);
        criteria.put(Constants.CRITERIA_VALUE, bitSet);
    }

    /**
     * Parses a string to Integer, returns null if parsing fails.
     */
    private Integer parseIntegerValue(String val, String criteriaKey, String cacheKey) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Non-integer criteria value '{}' for key {} in rule {}", val, criteriaKey, cacheKey);
            return null;
        }
    }

    /**
     * Creates a BitSet from integer values for efficient O(1) membership checks.
     * Example: [1, 3, 5] creates BitSet with bits 1, 3, 5 set to true.
     */
    BitSet createBitSetForAttribute(Collection<Integer> attributeValues) {
        BitSet bitSet = new BitSet();
        for (Integer part : attributeValues) {
            try {
                bitSet.set(part);
            } catch (Exception ex) {
                log.error("Failed to set the bit map positing for value: {}", part, ex);
                throw ex;
            }
        }
        return bitSet;
    }

    public void refreshRuleCache(String contextId, String contextIdType, String contextDataJson, boolean archived) {
        String cacheKey = contextId + "|" + contextIdType;
        if (archived || StringUtils.isBlank(contextDataJson)) {
            cacheMap.remove(cacheKey);
            redisCacheMgr.deleteHashField(PROMOTIONAL_CONTENT_CACHE_KEY, cacheKey);
            return;
        }

        CachedAccessSettingRule rule = new CachedAccessSettingRule(contextId, contextIdType, contextDataJson, false);
        try {
            processAndCacheRule(rule);
            cacheMap.put(cacheKey, rule);
            redisCacheMgr.setHashValue(PROMOTIONAL_CONTENT_CACHE_KEY, cacheKey,
                    buildRedisRulePayload(contextId, contextIdType, rule.getContextData()));
        } catch (Exception e) {
            log.error("Failed to refresh promotional content rule cache for key: {}", cacheKey, e);
        }
    }

    public void invalidateAll() {
        cacheMap.clear();
    }

    private String buildRedisRulePayload(String contextId, String contextIdType, Map<String, Object> contextData) {
        try {
            Map<String, Object> rulePayload = new HashMap<>();
            rulePayload.put(Constants.CONTEXT_ID, contextId);
            rulePayload.put(Constants.CONTEXT_ID_TYPE, contextIdType);
            rulePayload.put(Constants.CONTEXT_DATA, sanitizeContextDataForRedis(contextData));
            rulePayload.put(Constants.IS_ARCHIVED, false);
            return OBJECT_MAPPER.writeValueAsString(rulePayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize promotional content rule for Redis", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeContextDataForRedis(Map<String, Object> contextData) {
        Map<String, Object> sanitized = OBJECT_MAPPER.convertValue(contextData, Map.class);
        Object accessControlObj = sanitized.get(Constants.ACCESS_CONTROL_ID);
        if (!(accessControlObj instanceof Map<?, ?> accessControl)) {
            return sanitized;
        }
        Object userGroupsObj = accessControl.get(Constants.USER_GROUPS);
        if (!(userGroupsObj instanceof List<?> userGroups)) {
            return sanitized;
        }
        for (Object userGroupObj : userGroups) {
            if (!(userGroupObj instanceof Map<?, ?> userGroup)) {
                continue;
            }
            Object criteriaListObj = userGroup.get(Constants.USER_GROUP_CRITERIA_LIST);
            if (!(criteriaListObj instanceof List<?> criteriaList)) {
                continue;
            }
            for (Object criteriaObj : criteriaList) {
                if (!(criteriaObj instanceof Map<?, ?> criteriaMap)) {
                    continue;
                }
                Object valueObj = criteriaMap.get(Constants.CRITERIA_VALUE);
                if (valueObj instanceof BitSet bitSet) {
                    List<Integer> bitSetValues = bitSet.stream().boxed().toList();
                    ((Map<String, Object>) criteriaMap).put(Constants.CRITERIA_VALUE, bitSetValues);
                }
            }
        }
        return sanitized;
    }
}
