package io.github.numq.haskcore.output

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

internal interface OutputRepository {
    suspend fun observe(id: String): Result<Flow<List<OutputLine>>>

    suspend fun enqueue(id: String, line: OutputLine): Result<Unit>

    suspend fun clear(id: String): Result<Unit>

    suspend fun remove(id: String): Result<Unit>

    class Default : OutputRepository {
        private val _outputs = MutableStateFlow(persistentMapOf<String, PersistentList<OutputLine>>())

        private val outputs = _outputs.asStateFlow()

        override suspend fun observe(id: String) = runCatching {
            if (!_outputs.value.containsKey(id)) {
                _outputs.value = _outputs.value.mutate { mutator ->
                    mutator.put(id, persistentListOf())
                }
            }

            outputs.map { outputLines ->
                outputLines.getOrDefault(id, persistentListOf())
            }
        }

        override suspend fun enqueue(id: String, line: OutputLine) = runCatching {
            if (_outputs.value.containsKey(id)) {
                _outputs.value = _outputs.value.mutate { mutator ->
                    mutator.put(id, mutator[id]?.mutate { list ->
                        list.add(line)
                    } ?: persistentListOf(line))
                }
            }
        }

        override suspend fun clear(id: String) = runCatching {
            if (_outputs.value.containsKey(id)) {
                _outputs.value = _outputs.value.mutate { mutator ->
                    mutator.put(id, persistentListOf())
                }
            }
        }

        override suspend fun remove(id: String) = runCatching {
            _outputs.value = _outputs.value.mutate { mutator -> mutator.remove(id) }
        }
    }
}