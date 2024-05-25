package de.fimatas.home.controller.service;

import de.fimatas.home.library.model.TaskState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TasksServiceTest {

    @InjectMocks
    private TasksService tasksService;

    @Mock
    private UniqueTimestampService uniqueTimestampService;

    @BeforeEach
    void beforeEach(){
        lenient().when(uniqueTimestampService.getNonUnique())
                .thenReturn(LocalDateTime.of(2024, 5, 1, 12, 0, 0));
    }

    @Test
    void computeTaskState_Unknown() {
        var duration = Duration.ofDays(60);
        assertEquals(TaskState.UNKNOWN, tasksService.computeTaskState(duration, null));
    }

    @Test
    void computeTaskState_InRange() {
        var next = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        var duration = Duration.ofDays(60);
        assertEquals(TaskState.IN_RANGE, tasksService.computeTaskState(duration, next));
    }

    @Test
    void computeTaskState_NearBeforeExecution() {
        var next = LocalDateTime.of(2024, 5, 5, 12, 0, 0);
        var duration = Duration.ofDays(60);
        assertEquals(TaskState.NEAR_BEFORE_EXECUTION, tasksService.computeTaskState(duration, next));
    }

    @Test
    void computeTaskState_LittleOutOfRange() {
        var next = LocalDateTime.of(2024, 4, 30, 12, 0, 0);
        var duration = Duration.ofDays(60);
        assertEquals(TaskState.LITTLE_OUT_OF_RANGE, tasksService.computeTaskState(duration, next));
    }

    @Test
    void computeTaskState_FarOutOfRange() {
        var next = LocalDateTime.of(2024, 3, 1, 12, 0, 0);
        var duration = Duration.ofDays(60);
        assertEquals(TaskState.FAR_OUT_OF_RANGE, tasksService.computeTaskState(duration, next));
    }
}