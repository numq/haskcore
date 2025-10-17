package io.github.numq.haskcore.stack

import kotlinx.coroutines.flow.*

internal interface StackRepository {
    val project: StateFlow<StackProject?>

    suspend fun create(path: String, name: String, template: String): Result<Flow<StackOutput>>

    suspend fun build(path: String): Result<Flow<StackOutput>>

    suspend fun run(path: String): Result<Flow<StackOutput>>

    suspend fun test(path: String): Result<Flow<StackOutput>>

    suspend fun clean(path: String): Result<Flow<StackOutput>>

    class Default(private val stackService: StackService) : StackRepository {
        private val _project = MutableStateFlow<StackProject?>(null)

        override val project = _project.asStateFlow()

        override suspend fun create(
            path: String, name: String, template: String,
        ) = stackService.create(path = path, name = name, template = template).mapCatching { flow ->
            flow.onCompletion {
                _project.value = stackService.getProject(path = path).getOrNull()
            }
        }

        override suspend fun build(path: String) = stackService.build(path = path)

        override suspend fun run(path: String) = stackService.run(path = path)

        override suspend fun test(path: String) = stackService.test(path = path)

        override suspend fun clean(path: String) = stackService.clean(path = path)
    }
}