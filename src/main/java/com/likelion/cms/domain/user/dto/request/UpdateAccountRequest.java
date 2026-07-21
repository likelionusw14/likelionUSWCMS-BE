package com.likelion.cms.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.likelion.cms.common.type.PartType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateAccountRequest {

    @NotNull
    @PositiveOrZero
    private Integer version;

    private String name;
    private String department;
    private PartType part;
    private Long cohortId;

    @JsonIgnore
    private boolean nameProvided;
    @JsonIgnore
    private boolean departmentProvided;
    @JsonIgnore
    private boolean partProvided;
    @JsonIgnore
    private boolean cohortIdProvided;

    @JsonSetter
    public void setVersion(Integer version) {
        this.version = version;
    }

    @JsonSetter
    public void setName(String name) {
        this.nameProvided = true;
        this.name = name;
    }

    @JsonSetter
    public void setDepartment(String department) {
        this.departmentProvided = true;
        this.department = department;
    }

    @JsonSetter
    public void setPart(PartType part) {
        this.partProvided = true;
        this.part = part;
    }

    @JsonSetter
    public void setCohortId(Long cohortId) {
        this.cohortIdProvided = true;
        this.cohortId = cohortId;
    }

    @AssertTrue(message = "version 외에 하나 이상의 수정 필드가 필요합니다.")
    public boolean isAnyChangeProvided() {
        return nameProvided || departmentProvided || partProvided || cohortIdProvided;
    }

    @AssertTrue(message = "수정 필드의 값이 올바르지 않습니다.")
    public boolean isProvidedValueValid() {
        boolean validName = !nameProvided
                || (name != null && !name.isBlank() && name.length() <= 50);
        boolean validDepartment = !departmentProvided
                || (department != null && !department.isBlank() && department.length() <= 100);
        boolean validPart = !partProvided || part != null;
        boolean validCohortId = !cohortIdProvided
                || (cohortId != null && cohortId > 0);
        return validName && validDepartment && validPart && validCohortId;
    }
}
