package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryEntity
import com.example.data.model.ContactEntity
import com.example.data.model.DepartmentEntity
import com.example.data.model.TagEntity
import com.example.ui.MainViewModel

@Composable
fun DirectoriesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("directories_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("المصالح والمكاتب", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("التصنيفات", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("دليل المراسلين", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("الوسوم", fontSize = 12.sp) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(departments, key = { it.id }) { dept ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dept.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (dept.directorate.isNotBlank()) {
                                            Text("المديرية: ${dept.directorate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (dept.headName.isNotBlank()) {
                                            Text("المسؤول: ${dept.headName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteDepartment(dept) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (cat.description.isNotBlank()) {
                                            Text(cat.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (contact.organization.isNotBlank()) {
                                            Text("الهيئة: ${contact.organization}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (contact.phone.isNotBlank() || contact.email.isNotBlank()) {
                                            Text("الاتصال: ${contact.phone} ${contact.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteContact(contact) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
                3 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags, key = { it.id }) { tag ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("# ${tag.name}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    IconButton(onClick = { viewModel.deleteTag(tag) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة")
            }
        }
    }

    // ADD DIALOG
    if (showAddDialog) {
        when (selectedTab) {
            0 -> {
                var name by remember { mutableStateOf("") }
                var directorate by remember { mutableStateOf("") }
                var head by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة مصلحة أو مكتب إداري", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المصلحة / المكتب *") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = directorate, onValueChange = { directorate = it }, label = { Text("المديرية التابعة لها") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = head, onValueChange = { head = it }, label = { Text("اسم المسؤول") }, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addDepartment(DepartmentEntity(name = name, directorate = directorate, headName = head))
                                showAddDialog = false
                            }
                        }) { Text("إضافة") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
                )
            }
            1 -> {
                var name by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة تصنيف إداري جديد", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم التصنيف *") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addCategory(CategoryEntity(name = name, description = desc))
                                showAddDialog = false
                            }
                        }) { Text("إضافة") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
                )
            }
            2 -> {
                var name by remember { mutableStateOf("") }
                var org by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة جهة أو مراسل جديد", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم / الصفة *") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = org, onValueChange = { org = it }, label = { Text("الهيئة أو المؤسسة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addContact(ContactEntity(name = name, organization = org, phone = phone, email = email))
                                showAddDialog = false
                            }
                        }) { Text("إضافة") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
                )
            }
            3 -> {
                var tagName by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة وسم جديد", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(value = tagName, onValueChange = { tagName = it }, label = { Text("اسم الوسم *") }, modifier = Modifier.fillMaxWidth())
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (tagName.isNotBlank()) {
                                viewModel.addTag(TagEntity(name = tagName.trim()))
                                showAddDialog = false
                            }
                        }) { Text("إضافة") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
                )
            }
        }
    }
}
