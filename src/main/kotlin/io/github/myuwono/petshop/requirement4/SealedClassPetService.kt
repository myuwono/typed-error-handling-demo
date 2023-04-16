package io.github.myuwono.petshop.requirement4

import io.github.myuwono.petshop.Microchip
import io.github.myuwono.petshop.MicrochipId
import io.github.myuwono.petshop.Pet
import io.github.myuwono.petshop.PetGender
import io.github.myuwono.petshop.PetId
import io.github.myuwono.petshop.PetOwner
import io.github.myuwono.petshop.PetOwnerId
import io.github.myuwono.petshop.PetType
import java.time.LocalDate

class SealedClassPetService(
  private val microchipStore: MicrochipStore,
  private val petStore: PetStore,
  private val petOwnerStore: PetOwnerStore
) {
  suspend fun updatePetDetails(
    petId: PetId,
    petUpdate: PetUpdate
  ): UpdatePetDetailsResult {
    val pet = petStore.getPet(petId)
    return if (pet == null) {
      UpdatePetDetailsResult.PetNotFound
    } else {
      val microchip = microchipStore.getMicrochip(pet.microchipId)
      if (microchip == null) {
        UpdatePetDetailsResult.MicrochipNotFound
      } else {
        if (microchip.petId != pet.id) {
          UpdatePetDetailsResult.InvalidMicrochip
        } else {
          when (val updateResult = petStore.updatePet(pet.id, petUpdate)) {
            UpdatePetResult.IllegalUpdate -> UpdatePetDetailsResult.InvalidUpdate
            UpdatePetResult.NotFound -> UpdatePetDetailsResult.PetNotFound
            is UpdatePetResult.Updated -> UpdatePetDetailsResult.Success(updateResult.pet)
          }
        }
      }
    }
  }

  sealed class UpdatePetDetailsResult {
    data class Success(val pet: Pet) : UpdatePetDetailsResult()
    object PetNotFound : UpdatePetDetailsResult()
    object MicrochipNotFound : UpdatePetDetailsResult()
    object InvalidMicrochip : UpdatePetDetailsResult()
    object InvalidUpdate : UpdatePetDetailsResult()
  }

  interface MicrochipStore {
    suspend fun getMicrochip(microchipId: MicrochipId): Microchip?
  }

  interface PetOwnerStore {
    suspend fun getPetOwner(petOwnerId: PetOwnerId): PetOwner?
  }

  interface PetStore {
    suspend fun getPet(petId: PetId): Pet?
    suspend fun updatePet(petId: PetId, petUpdate: PetUpdate): UpdatePetResult
  }

  data class PetUpdate(
    val microchipId: MicrochipId? = null,
    val name: String? = null,
    val birthDate: LocalDate? = null,
    val petType: PetType? = null,
    val breed: String? = null,
    val gender: PetGender? = null
  )

  sealed class UpdatePetResult {
    data class Updated(val pet: Pet) : UpdatePetResult()
    object NotFound : UpdatePetResult()
    object IllegalUpdate : UpdatePetResult()
  }
}
