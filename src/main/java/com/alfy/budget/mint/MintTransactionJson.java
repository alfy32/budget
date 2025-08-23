package com.alfy.budget.mint;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class MintTransactionJson {

    public String[] associations;
    public String transactionId;
    public String[] compliances;
    public String id;
    public BigDecimal amount;
    public String categoryId;
    public List<Map<String, Object>> categorizationAlternatives;
    public String categoryName;
    public String createdBy;
    public String createdTimestamp;
    public String description;
    public String lastModifiedBy;
    public String lastModifiedTimestamp;
    public String memo;
    public String merchant;
    public String merchantId;
    public Map<String, Object> metadata;
    public String methodId;
    public String postedDate;
    public String scheduleC;
    public String scheduleCId;
    public String standardIndustrialClassificationCode;
    public String status;
    public String transactionAccountId;
    public String transactionDate;
    public String L10_provider_description;
    public List<Map<String, Object>> categorizationMetadata;
    public String transactionAccountCategory;

}
