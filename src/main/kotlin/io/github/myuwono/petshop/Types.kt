package io.github.myuwono.petshop

import java.time.LocalDate

data class PetId(val value: String)
data class PetOwnerId(val value: String)
data class MicrochipId(val value: String)

data class MicrochipData(val id: MicrochipId, val petOwnerId: PetOwnerId, val petId: PetId)

enum class PetType { Dog, Cat }
enum class PetGender { Male, Female }

data class Pet(
  val id: PetId,
  val microchipId: MicrochipId,
  val name: String,
  val birthDate: LocalDate,
  val petType: PetType,
  val breed: String,
  val gender: PetGender
)

data class PetOwner(val id: PetOwnerId, val name: String)
