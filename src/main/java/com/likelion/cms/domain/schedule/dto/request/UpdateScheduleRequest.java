package com.likelion.cms.domain.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateScheduleRequest {
    @NotBlank
    @PositiveOrZero
    private Integer version;

    private String title;
    private String description;
    private LocalDate scheduleDate;
    private Boolean isAllDay;
    private LocalTime startTime;
    private String location;

    @JsonIgnore
    private boolean titleProvided;
    @JsonIgnore
    private boolean descriptionProvided;
    @JsonIgnore
    private boolean scheduleDateProvided;
    @JsonIgnore
    private boolean isAllDayProvided;
    @JsonIgnore
    private boolean startTimeProvided;
    @JsonIgnore
    private boolean locationProvided;

    @JsonSetter
    public void setVersion(Integer version){
        this.version = version;
    }

    @JsonSetter
    public void setTitle(String title){
        this.titleProvided = true;
        this.title = title;
    }

    @JsonSetter
    public void setDescription(String description){
        this.descriptionProvided = true;
        this.description = description;
    }

    @JsonSetter
    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDateProvided = true;
        this.scheduleDate = scheduleDate;
    }

    @JsonSetter
    public void setIsAllDay(Boolean isAllDay) {
        this.isAllDayProvided = true;
        this.isAllDay = isAllDay;
    }

    @JsonSetter
    public void setStartTime(LocalTime startTime) {
        this.startTimeProvided = true;
        this.startTime = startTime;
    }

    @JsonSetter
    public void setLocation(String location) {
        this.locationProvided = true;
        this.location = location;
    }

    @AssertTrue(message = "version 외에 하나 이상의 수정 필드가 필요합니다.")
    public boolean isAnyChangeProvided() {
        return titleProvided || descriptionProvided || scheduleDateProvided
                || isAllDayProvided || startTimeProvided || locationProvided;
    }

    @AssertTrue(message = "수정 필드의 값이 올바르지 않습니다.")
    public boolean isProvidedValueValid() {
        boolean validTitle = !titleProvided
                || (title != null && !title.isBlank() && title.length() <= 150);
        boolean validDescription = !descriptionProvided
                || description == null || description.length() <= 10000;
        boolean validScheduleDate = !scheduleDateProvided || scheduleDate != null;
        boolean validIsAllDay = !isAllDayProvided || isAllDay != null;
        boolean validLocation = !locationProvided
                || location == null || location.length() <= 255;
        return validTitle && validDescription && validScheduleDate && validIsAllDay && validLocation;
    }
}
