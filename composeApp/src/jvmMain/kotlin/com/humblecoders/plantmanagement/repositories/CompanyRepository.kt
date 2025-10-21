package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.humblecoders.plantmanagement.data.Company
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CompanyRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {

    private fun getCompanyCollection() =
        firestore.collection("company")

    /**
     * Save or update company information
     */
    suspend fun saveCompany(company: Company): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val docRef = getCompanyCollection().document(userId)
            
            // Use transaction to ensure atomic write
            firestore.runTransaction { transaction ->
                val companyData = mapOf(
                    "userId" to userId,
                    "companyName" to company.companyName,
                    "address" to company.address,
                    "stateFssaiLicenseNo" to company.stateFssaiLicenseNo,
                    "centreFssaiLicenseNo" to company.centreFssaiLicenseNo,
                    "gstinUin" to company.gstinUin,
                    "state" to company.state,
                    "email" to company.email,
                    "createdAt" to (company.createdAt ?: com.google.cloud.Timestamp.now()),
                    "updatedAt" to com.google.cloud.Timestamp.now()
                )
                
                transaction.set(docRef, companyData)
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get company information for the current user
     */
    suspend fun getCompany(): Result<Company?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = getCompanyCollection()
                .document(userId)
                .get()
                .get(10, TimeUnit.SECONDS)

            if (snapshot.exists()) {
                val data = snapshot.data ?: return@withContext Result.success(null)
                val company = Company(
                    id = snapshot.id,
                    companyName = data["companyName"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    stateFssaiLicenseNo = data["stateFssaiLicenseNo"] as? String ?: "",
                    centreFssaiLicenseNo = data["centreFssaiLicenseNo"] as? String ?: "",
                    gstinUin = data["gstinUin"] as? String ?: "",
                    state = data["state"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    createdAt = data["createdAt"] as? com.google.cloud.Timestamp,
                    updatedAt = data["updatedAt"] as? com.google.cloud.Timestamp
                )
                Result.success(company)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete company information
     */
    suspend fun deleteCompany(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.runTransaction { transaction ->
                transaction.delete(getCompanyCollection().document(userId))
                null
            }.get(10, TimeUnit.SECONDS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
