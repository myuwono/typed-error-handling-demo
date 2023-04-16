package io.github.myuwono.petshop.requirement7

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.recover
import io.github.myuwono.petshop.Microchip
import io.github.myuwono.petshop.MicrochipId
import io.github.myuwono.petshop.Pet
import io.github.myuwono.petshop.PetGender
import io.github.myuwono.petshop.PetId
import io.github.myuwono.petshop.PetOwner
import io.github.myuwono.petshop.PetOwnerId
import io.github.myuwono.petshop.PetType
import java.time.LocalDate

class ContextReceiversPetService(
  private val microchipStore: MicrochipStore,
  private val petStore: PetStore,
  private val petOwnerStore: PetOwnerStore
) {

  context(Raise<UpdatePetDetailsFailure>)
  suspend fun updatePetDetails(
    petId: PetId,
    petOwnerId: PetOwnerId,
    petUpdate: PetUpdate
  ): Pet {
    val pet = petStore.getPet(petId) ?: raise(UpdatePetDetailsFailure.PetNotFound)
    val owner = petOwnerStore.getPetOwner(petOwnerId) ?: raise(UpdatePetDetailsFailure.OwnerNotFound)
    val microchip = microchipStore.getMicrochip(pet.microchipId) ?: raise(UpdatePetDetailsFailure.MicrochipNotFound)

    ensure(microchip.petId == pet.id) { UpdatePetDetailsFailure.InvalidMicrochip }
    ensure(microchip.petOwnerId == owner.id) { UpdatePetDetailsFailure.OwnerMismatch }

    petUpdate.name?.let { checkNamePolicy(it) }

    return recover({ petStore.updatePet(pet.id, petUpdate) }) { updatePetFailure ->
      when (updatePetFailure) {
        UpdatePetFailure.IllegalUpdate -> raise(UpdatePetDetailsFailure.InvalidUpdate)
        UpdatePetFailure.NotFound -> raise(UpdatePetDetailsFailure.PetNotFound)
      }
    }
  }

  context(Raise<UpdatePetDetailsFailure.InvalidUpdate>)
  private fun checkNamePolicy(name: String): Unit = ensure(name.isNotBlank()) {
    UpdatePetDetailsFailure.InvalidUpdate
  }

  interface MicrochipStore {
    suspend fun getMicrochip(microchipId: MicrochipId): Microchip?
  }

  interface PetOwnerStore {
    suspend fun getPetOwner(petOwnerId: PetOwnerId): PetOwner?
  }

  interface PetStore {
    suspend fun getPet(petId: PetId): Pet?

    context(Raise<UpdatePetFailure>)
    suspend fun updatePet(petId: PetId, petUpdate: PetUpdate): Pet
  }

  data class PetUpdate(
    val microchipId: MicrochipId? = null,
    val name: String? = null,
    val birthDate: LocalDate? = null,
    val petType: PetType? = null,
    val breed: String? = null,
    val gender: PetGender? = null
  )

  sealed class UpdatePetFailure {
    object NotFound : UpdatePetFailure()
    object IllegalUpdate : UpdatePetFailure()
  }

  sealed class UpdatePetDetailsFailure {
    object OwnerNotFound : UpdatePetDetailsFailure()
    object PetNotFound : UpdatePetDetailsFailure()
    object MicrochipNotFound : UpdatePetDetailsFailure()
    object InvalidMicrochip : UpdatePetDetailsFailure()
    object OwnerMismatch : UpdatePetDetailsFailure()
    object InvalidUpdate : UpdatePetDetailsFailure()
  }
}
