package com.problemfighter.java.base.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
public class BaseUserDTO implements Serializable {
    private Long id;
    private OffsetDateTime createDate;
    private OffsetDateTime updateDate;
    private String createdBy;
    private String updatedBy;
    private String uuid;
    private boolean deleted;
}
