package com.likelion.cms.domain.cohort.service;

import com.likelion.cms.domain.cohort.dto.response.CohortSummary;
import com.likelion.cms.domain.cohort.entity.Cohort;
import com.likelion.cms.domain.cohort.repository.CohortRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CohortServiceTest {

    @Mock
    private CohortRepository cohortRepository;

    @InjectMocks
    private CohortService cohortService;

    @Test
    void getCohortsReturnsSummariesInRepositoryOrder() {
        // 정렬은 리포지토리(findAllByOrderByNumberDesc)가 담당하므로,
        // 서비스는 그 순서를 그대로 CohortSummary로 매핑하는지만 검증한다.
        when(cohortRepository.findAllByOrderByNumberDesc())
                .thenReturn(List.of(cohort(2L, 13, "13기"), cohort(1L, 12, "12기")));

        List<CohortSummary> result = cohortService.getCohorts();

        assertThat(result).extracting(CohortSummary::getNumber).containsExactly(13, 12);
        assertThat(result).extracting(CohortSummary::getName).containsExactly("13기", "12기");
        assertThat(result.get(0).getCohortId()).isEqualTo(2L);
    }

    @Test
    void getCohortsReturnsEmptyListWhenNoCohort() {
        when(cohortRepository.findAllByOrderByNumberDesc()).thenReturn(List.of());

        assertThat(cohortService.getCohorts()).isEmpty();
    }

    private Cohort cohort(Long id, int number, String name) {
        Cohort cohort = Cohort.builder().number(number).name(name).build();
        ReflectionTestUtils.setField(cohort, "cohortId", id);
        return cohort;
    }
}
