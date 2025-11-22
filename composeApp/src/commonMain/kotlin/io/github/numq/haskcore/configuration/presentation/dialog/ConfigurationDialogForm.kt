package io.github.numq.haskcore.configuration.presentation.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser

@Composable
internal fun ConfigurationDialogForm(
    title: String,
    initialPath: String,
    initialName: String,
    initialCommand: String,
    onSave: (path: String, name: String, command: String) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth(.8f).wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            val coroutineScope = rememberCoroutineScope()

            val (path, setPath) = remember { mutableStateOf(initialPath) }

            val (name, setName) = remember { mutableStateOf(initialName) }

            val (command, setCommand) = remember { mutableStateOf(initialCommand) }

            val isValid = command.isNotBlank()

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = setName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Configuration Name") },
                    placeholder = { Text("Enter a unique name") },
                    singleLine = true,
                    isError = name.isBlank()
                )

                OutlinedTextField(
                    value = command,
                    onValueChange = setCommand,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Command") },
                    placeholder = { Text("Enter command to execute") },
                    singleLine = true,
                    isError = command.isBlank()
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Working Directory",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = path,
                            onValueChange = setPath,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Select working directory") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            JFileChooser().apply {
                                                currentDirectory = if (path.isNotBlank()) File(path) else null
                                                dialogTitle = "Select working directory"
                                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                            }.run {
                                                when {
                                                    showOpenDialog(null) == JFileChooser.APPROVE_OPTION ->
                                                        selectedFile.absolutePath

                                                    else -> null
                                                }
                                            }?.let(setPath)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = "Select directory"
                                    )
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onSave(path, name, command) },
                        enabled = isValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}