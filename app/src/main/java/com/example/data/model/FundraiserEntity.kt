package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fundraisers")
data class FundraiserEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val associationId: String,
    val associationName: String,
    val associationPhone: String = "+237 670 12 34 56",
    val city: String = "Douala",
    val neighborhood: String = "Deido",
    val targetAmountFcfa: Long,
    val currentAmountFcfa: Long,
    val targetGrains: Int = 500,
    val currentGrains: Int = 120,
    val donorsCount: Int,
    val deadlineDate: String,
    val isVerifiedByAdmin: Boolean = true,
    val category: String = "ORPHELINAT",
    val status: String = FundraiserStatus.ACTIVE.name,
    val proofNote: String = "Projet validé par la coordination caritative. Factures et reçus vérifiables auprès de la chefferie et de l'administration."
)
