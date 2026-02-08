package com.example.kosmos.core.validators

import com.example.kosmos.core.database.dao.TaskDependencyDao
import com.example.kosmos.core.models.DependencyType
import com.example.kosmos.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for DependencyValidator.
 * Uses MockK to mock TaskDependencyDao.
 */
@RunWith(JUnit4::class)
class DependencyValidatorTest {

    private lateinit var taskDependencyDao: TaskDependencyDao
    private lateinit var validator: DependencyValidator

    @Before
    fun setup() {
        taskDependencyDao = mockk()
        validator = DependencyValidator(taskDependencyDao)
    }

    // ─── Self-dependency ──────────────────────────────────────────────────────

    @Test
    fun `self-dependency returns Invalid with 'cannot depend on itself'`() = runTest {
        val result = validator.validateDependency("task-1", "task-1", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).reason).contains("itself")
    }

    // ─── Already exists ───────────────────────────────────────────────────────

    @Test
    fun `already-existing dependency returns Invalid`() = runTest {
        coEvery { taskDependencyDao.dependencyExists("task-a", "task-b") } returns true

        val result = validator.validateDependency("task-a", "task-b", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).reason).contains("already exists")
    }

    // ─── No cycle (linear A→B→C) ──────────────────────────────────────────────

    @Test
    fun `linear dependency chain A to B to C has no cycle`() = runTest {
        // Existing: A blocks B (task B depends on A)
        val existing = listOf(
            TestFixtures.taskDependency(taskId = "task-b", dependsOnTaskId = "task-a")
        )
        coEvery { taskDependencyDao.dependencyExists("task-c", "task-b") } returns false
        coEvery { taskDependencyDao.getAllDependencies() } returns existing

        // Adding: C depends on B (B blocks C) — should be valid
        val result = validator.validateDependency("task-c", "task-b", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat(result.isValid).isTrue()
    }

    // ─── Direct cycle ─────────────────────────────────────────────────────────

    @Test
    fun `direct cycle A blocks B, adding B blocks A returns Invalid`() = runTest {
        // Existing: A blocks B (task B depends on A)
        val existing = listOf(
            TestFixtures.taskDependency(taskId = "task-b", dependsOnTaskId = "task-a")
        )
        coEvery { taskDependencyDao.dependencyExists("task-a", "task-b") } returns false
        coEvery { taskDependencyDao.getAllDependencies() } returns existing

        // Adding: A depends on B — would create cycle
        val result = validator.validateDependency("task-a", "task-b", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).reason).contains("circular")
    }

    // ─── Transitive cycle ─────────────────────────────────────────────────────

    @Test
    fun `transitive cycle A to B to C, adding C to A returns Invalid`() = runTest {
        // Existing: A→B, B→C
        val existing = listOf(
            TestFixtures.taskDependency(taskId = "task-b", dependsOnTaskId = "task-a"),
            TestFixtures.taskDependency(taskId = "task-c", dependsOnTaskId = "task-b")
        )
        coEvery { taskDependencyDao.dependencyExists("task-a", "task-c") } returns false
        coEvery { taskDependencyDao.getAllDependencies() } returns existing

        // Adding C→A would create A→B→C→A cycle
        val result = validator.validateDependency("task-a", "task-c", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    }

    // ─── RELATED type skips cycle check ───────────────────────────────────────

    @Test
    fun `RELATED_TO type skips DFS and returns Valid even if cycle would form`() = runTest {
        coEvery { taskDependencyDao.dependencyExists("task-a", "task-b") } returns false

        // RELATED_TO should not do DFS / depth check
        val result = validator.validateDependency("task-a", "task-b", DependencyType.RELATED_TO)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
    }

    // ─── Max depth ────────────────────────────────────────────────────────────

    @Test
    fun `dependency at max depth 10 returns Invalid`() = runTest {
        // Chain: task-1 blocks task-2, ..., task-10 blocks task-11 (10 hops from task-1)
        // existing has 10 deps: task-{i+1} depends on task-{i}, i=1..10
        val existing = (1..10).map { i ->
            TestFixtures.taskDependency(taskId = "task-${i + 1}", dependsOnTaskId = "task-$i")
        }
        // Now validateDependency("task-1", "task-0", BLOCKS):
        //   taskId="task-1" depends on "task-0"; checkDependencyDepth("task-1") follows
        //   task-1→task-2→...→task-11 = depth 10, which is >= MAX_DEPENDENCY_DEPTH(10) → Invalid
        coEvery { taskDependencyDao.dependencyExists("task-1", "task-0") } returns false
        coEvery { taskDependencyDao.getAllDependencies() } returns existing

        val result = validator.validateDependency("task-1", "task-0", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).reason).contains("depth")
    }

    @Test
    fun `dependency at depth 9 returns Valid`() = runTest {
        // Chain: task-1 blocks ... task-10 (9 hops), depth from task-1 = 9 < 10
        val existing = (1..9).map { i ->
            TestFixtures.taskDependency(taskId = "task-${i + 1}", dependsOnTaskId = "task-$i")
        }
        coEvery { taskDependencyDao.dependencyExists("task-1", "task-0") } returns false
        coEvery { taskDependencyDao.getAllDependencies() } returns existing

        val result = validator.validateDependency("task-1", "task-0", DependencyType.BLOCKS)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
    }

    // ─── ValidationResult helpers ─────────────────────────────────────────────

    @Test
    fun `ValidationResult Valid has isValid = true`() {
        assertThat(ValidationResult.Valid.isValid).isTrue()
    }

    @Test
    fun `ValidationResult Invalid has isValid = false`() {
        assertThat(ValidationResult.Invalid("some reason").isValid).isFalse()
    }
}
