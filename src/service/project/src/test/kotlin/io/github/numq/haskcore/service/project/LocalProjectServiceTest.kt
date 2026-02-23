package io.github.numq.haskcore.service.project

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalProjectServiceTest {
    private val projectDataSource = mockk<ProjectDataSource>()
    private val projectDataFlow = MutableStateFlow(ProjectData())
    private val testProjectPath = "/home/user/project"
    private lateinit var service: LocalProjectService

    @BeforeEach
    fun setup() {
        every { projectDataSource.projectData } returns projectDataFlow
        service = LocalProjectService(testProjectPath, projectDataSource)
    }

    @Test
    fun `project flow should map project data with provided path`() = runTest {
        val projectData = ProjectData(name = "HaskellProject")
        projectDataFlow.value = projectData

        val result = service.project.first()

        Assertions.assertEquals(testProjectPath, result.path)
        Assertions.assertEquals("HaskellProject", result.name)
    }

    @Test
    fun `renameProject should update name in data source`() = runTest {
        val newName = "NewProjectName"
        coEvery { projectDataSource.update(any()) } answers {
            val transform = firstArg<(ProjectData) -> ProjectData>()
            val updated = transform(ProjectData(name = "OldName"))
            Assertions.assertEquals(newName, updated.name)
            Either.Right(updated)
        }

        val result = service.renameProject(newName)

        Assertions.assertTrue(result.isRight())
        coVerify { projectDataSource.update(any()) }
    }

    @Test
    fun `openDocument should add path and set it as active`() = runTest {
        val docPath = "$testProjectPath/Main.hs"
        coEvery { projectDataSource.update(any()) } answers {
            val transform = firstArg<(ProjectData) -> ProjectData>()
            val updated = transform(ProjectData())
            Assertions.assertTrue(updated.openedDocumentPaths.contains(docPath))
            Assertions.assertEquals(docPath, updated.activeDocumentPath)
            Either.Right(updated)
        }

        val result = service.openDocument(docPath)

        Assertions.assertTrue(result.isRight())
    }

    @Test
    fun `openDocument should only set active if path already opened`() = runTest {
        val docPath = "$testProjectPath/Main.hs"
        val initialData = ProjectData(openedDocumentPaths = listOf(docPath))

        coEvery { projectDataSource.update(any()) } answers {
            val transform = firstArg<(ProjectData) -> ProjectData>()
            val updated = transform(initialData)
            Assertions.assertEquals(1, updated.openedDocumentPaths.size)
            Assertions.assertEquals(docPath, updated.activeDocumentPath)
            Either.Right(updated)
        }

        service.openDocument(docPath)
    }

    @Test
    fun `closeDocument should remove path and update active document`() = runTest {
        val doc1 = "File1.hs"
        val doc2 = "File2.hs"
        val initialData = ProjectData(
            openedDocumentPaths = listOf(doc1, doc2), activeDocumentPath = doc2
        )

        coEvery { projectDataSource.update(any()) } answers {
            val transform = firstArg<(ProjectData) -> ProjectData>()
            val updated = transform(initialData)

            Assertions.assertTrue(updated.openedDocumentPaths.none { it == doc2 })
            Assertions.assertEquals(doc1, updated.activeDocumentPath)
            Either.Right(updated)
        }

        val result = service.closeDocument(doc2)

        Assertions.assertTrue(result.isRight())
    }

    @Test
    fun `closeDocument should set active to null if no documents left`() = runTest {
        val doc = "Last.hs"
        val initialData = ProjectData(
            openedDocumentPaths = listOf(doc), activeDocumentPath = doc
        )

        coEvery { projectDataSource.update(any()) } answers {
            val transform = firstArg<(ProjectData) -> ProjectData>()
            val updated = transform(initialData)

            Assertions.assertTrue(updated.openedDocumentPaths.isEmpty())
            Assertions.assertEquals(null, updated.activeDocumentPath)
            Either.Right(updated)
        }

        service.closeDocument(doc)
    }
}