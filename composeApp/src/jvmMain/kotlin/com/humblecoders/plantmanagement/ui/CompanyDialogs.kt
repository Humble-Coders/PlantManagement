package com.humblecoders.plantmanagement.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.Company

@Composable
fun EditCompanyDialog(
    company: Company?,
    onDismiss: () -> Unit,
    onSaveCompany: (Company) -> Unit,
    isLoading: Boolean
) {
    var companyName by remember { mutableStateOf(company?.companyName ?: "") }
    var address by remember { mutableStateOf(company?.address ?: "") }
    var stateFssaiLicenseNo by remember { mutableStateOf(company?.stateFssaiLicenseNo ?: "") }
    var centreFssaiLicenseNo by remember { mutableStateOf(company?.centreFssaiLicenseNo ?: "") }
    var gstinUin by remember { mutableStateOf(company?.gstinUin ?: "") }
    var state by remember { mutableStateOf(company?.state ?: "") }
    var email by remember { mutableStateOf(company?.email ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (company == null) "Add Company Information" else "Edit Company Information",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Company Name Field
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Address Field
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 3,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // State FSSAI License No Field
                OutlinedTextField(
                    value = stateFssaiLicenseNo,
                    onValueChange = { stateFssaiLicenseNo = it },
                    label = { Text("State FSSAI License No.", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Centre FSSAI License No Field
                OutlinedTextField(
                    value = centreFssaiLicenseNo,
                    onValueChange = { centreFssaiLicenseNo = it },
                    label = { Text("Centre FSSAI License No.", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GSTIN/UIN Field
                OutlinedTextField(
                    value = gstinUin,
                    onValueChange = { gstinUin = it },
                    label = { Text("GSTIN/UIN", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // State Field
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        cursorColor = Color(0xFF10B981),
                        textColor = Color(0xFFF9FAFB)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF9CA3AF))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onSaveCompany(
                                Company(
                                    id = company?.id ?: "",
                                    companyName = companyName.trim(),
                                    address = address.trim(),
                                    stateFssaiLicenseNo = stateFssaiLicenseNo.trim(),
                                    centreFssaiLicenseNo = centreFssaiLicenseNo.trim(),
                                    gstinUin = gstinUin.trim(),
                                    state = state.trim(),
                                    email = email.trim(),
                                    createdAt = company?.createdAt
                                )
                            )
                        },
                        enabled = !isLoading && companyName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (company == null) "Save" else "Update", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
