package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.Company
import com.humblecoders.plantmanagement.data.UserRole
import com.humblecoders.plantmanagement.viewmodels.CompanyViewModel
import java.time.format.DateTimeFormatter

@Composable
fun CompanyInfoScreen(
    companyViewModel: CompanyViewModel,
    userRole: UserRole? = null
) {
    val companyState = companyViewModel.companyState
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    val isAdmin = userRole == UserRole.ADMIN

    // Clear messages after showing
    LaunchedEffect(companyState.successMessage, companyState.error) {
        if (companyState.successMessage != null || companyState.error != null) {
            kotlinx.coroutines.delay(3000)
            companyViewModel.clearSuccessMessage()
            companyViewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Company Information",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
                
                Row {
                    if (isAdmin) {
                        Button(
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF10B981)
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Messages
            companyState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFDC2626),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            companyState.successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF10B981),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading indicator
            if (companyState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else {
                // Company Information Display
                if (companyState.company == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF374151),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = "No Company Info",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No company information found",
                                color = Color(0xFF9CA3AF),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAdmin) "Click 'Edit' to add company information" else "Contact admin to add company information",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    CompanyInfoCard(
                        company = companyState.company!!,
                        isAdmin = isAdmin,
                        onDelete = { showDeleteConfirmDialog = true }
                    )
                }
            }
        }
    }

    // Edit Company Dialog
    if (showEditDialog) {
        EditCompanyDialog(
            company = companyState.company,
            onDismiss = { showEditDialog = false },
            onSaveCompany = { company ->
                companyViewModel.saveCompany(company)
                showEditDialog = false
            },
            isLoading = companyState.isSaving
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Company Information", color = Color(0xFFF9FAFB)) },
            text = { Text("Are you sure you want to delete all company information?", color = Color(0xFF9CA3AF)) },
            confirmButton = {
                Button(
                    onClick = {
                        companyViewModel.deleteCompany()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            },
            backgroundColor = Color(0xFF1F2937)
        )
    }
}

@Composable
fun CompanyInfoCard(
    company: Company,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Company Details",
                    color = Color(0xFFF9FAFB),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Company Information Fields
            CompanyInfoField("Company Name", company.companyName)
            CompanyInfoField("Address", company.address)
            CompanyInfoField("State FSSAI License No.", company.stateFssaiLicenseNo)
            CompanyInfoField("Centre FSSAI License No.", company.centreFssaiLicenseNo)
            CompanyInfoField("GSTIN/UIN", company.gstinUin)
            CompanyInfoField("State", company.state)
            CompanyInfoField("Email", company.email)
            
            if (company.updatedAt != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Last Updated: ${company.updatedAt.toDate().toString()}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CompanyInfoField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (value.isNotEmpty()) value else "Not provided",
            color = if (value.isNotEmpty()) Color(0xFFF9FAFB) else Color(0xFF6B7280),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
