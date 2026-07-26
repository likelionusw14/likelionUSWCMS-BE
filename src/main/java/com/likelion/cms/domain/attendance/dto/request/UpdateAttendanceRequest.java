package com.likelion.cms.domain.attendance.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.likelion.cms.domain.attendance.entity.AdminSettableAttendanceStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateAttendanceRequest {

    @NotNull
    private AdminSettableAttendanceStatus status;

    private String memo;

    @NotNull
    @PositiveOrZero
    private Integer version;

    @JsonIgnore
    private boolean memoProvided;

    @JsonSetter
    public void setStatus(AdminSettableAttendanceStatus status) {
        this.status = status;
    }

    @JsonSetter
    public void setMemo(String memo) {
        this.memoProvided = true;
        this.memo = memo;
    }

    @JsonSetter
    public void setVersion(Integer version) {
        this.version = version;
    }

    @AssertTrue(message = "memo는 1~1000자여야 합니다.")
    public boolean isMemoValid() {
        return !memoProvided
                || memo == null
                || (!memo.isBlank() && memo.length() <= 1000);
    }
}