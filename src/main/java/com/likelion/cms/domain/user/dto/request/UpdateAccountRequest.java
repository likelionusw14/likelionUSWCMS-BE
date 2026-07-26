package com.likelion.cms.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.likelion.cms.common.type.PartType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PATCH 부분 수정 요청. 스펙의 UpdateAccountRequest 스키마 기준 (name/department/
// studentId/cohortId/part). record가 아니라 일반 클래스인 이유:
// "필드를 아예 안 보냄"과 "필드를 null로 보냄"을 구분해야 하는데,
// record/일반 setter로는 이 구분이 안 돼서 @JsonSetter를 직접 오버라이드함.
// 필드마다 붙어있는 xxxProvided 플래그가 "요청 JSON에 이 키가 실제로 있었는지"를 나타냄.
@Getter
@NoArgsConstructor
public class UpdateAccountRequest {

    @NotNull
    @PositiveOrZero
    private Integer version;

    private String name;
    private String department;
    private String studentId;
    private PartType part;
    private Long cohortId;

    @JsonIgnore
    private boolean nameProvided;
    @JsonIgnore
    private boolean departmentProvided;
    @JsonIgnore
    private boolean studentIdProvided;
    @JsonIgnore
    private boolean partProvided;
    @JsonIgnore
    private boolean cohortIdProvided;

    @JsonSetter
    public void setVersion(Integer version) {
        this.version = version;
    }

    // Jackson이 JSON을 파싱하면서 "name" 키를 실제로 만났을 때만 이 setter가 불림.
    // 그래서 provided 플래그를 여기서 true로 세팅하면 "값이 왔었다"는 뜻이 됨.
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
    public void setStudentId(String studentId) {
        this.studentIdProvided = true;
        this.studentId = studentId;
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

    // version 말고 최소 하나의 실제 수정 필드는 있어야 함 (빈 PATCH 요청 방지).
    @AssertTrue(message = "version 외에 하나 이상의 수정 필드가 필요합니다.")
    public boolean isAnyChangeProvided() {
        return nameProvided || departmentProvided || studentIdProvided || partProvided || cohortIdProvided;
    }

    // "필드가 왔으면" 그 값 자체는 유효해야 함 (안 왔으면 검증 스킵 - !xxxProvided).
    @AssertTrue(message = "수정 필드의 값이 올바르지 않습니다.")
    public boolean isProvidedValueValid() {
        boolean validName = !nameProvided
                || (name != null && !name.isBlank() && name.length() <= 50);
        boolean validDepartment = !departmentProvided
                || (department != null && !department.isBlank() && department.length() <= 100);
        boolean validStudentId = !studentIdProvided
                || (studentId != null && studentId.matches("^[A-Za-z0-9-]{1,30}$"));
        boolean validPart = !partProvided || part != null;
        boolean validCohortId = !cohortIdProvided
                || (cohortId != null && cohortId > 0);
        return validName && validDepartment && validStudentId && validPart && validCohortId;
    }
}
