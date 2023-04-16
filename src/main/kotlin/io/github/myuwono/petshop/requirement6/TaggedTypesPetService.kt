package io.github.myuwono.petshop.requirement6

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.myuwono.petshop.Microchip
import io.github.myuwono.petshop.MicrochipId
import io.github.myuwono.petshop.Pet
import io.github.myuwono.petshop.PetGender
import io.github.myuwono.petshop.PetId
import io.github.myuwono.petshop.PetOwner
import io.github.myuwono.petshop.PetOwnerId
import io.github.myuwono.petshop.PetType
import java.time.LocalDate

class TaggedTypesPetService(
  private val microchipStore: MicrochipStore,
  private val petStore: PetStore,
  private val petOwnerStore: PetOwnerStore
) {
  suspend fun updatePetDetails(
    petId: PetId,
    petOwnerId: PetOwnerId,
    petUpdate: PetUpdate
  ): Either<UpdatePetDetailsFailure, Pet> = either {
    val pet = petStore.getPet(petId) ?: raise(UpdatePetDetailsFailure.PetNotFound)
    val owner = petOwnerStore.getPetOwner(petOwnerId) ?: raise(UpdatePetDetailsFailure.OwnerNotFound)
    val microchip = microchipStore.getMicrochip(pet.microchipId) ?: raise(UpdatePetDetailsFailure.MicrochipNotFound)

    ensure(microchip.petId == pet.id) { UpdatePetDetailsFailure.InvalidMicrochip }
    ensure(microchip.petOwnerId == owner.id) { UpdatePetDetailsFailure.OwnerMismatch }

    petUpdate.name?.let { checkNamePolicy(it).bind() }

    petStore.updatePet(pet.id, petUpdate)
      .mapLeft { updatePetFailure ->
        when (updatePetFailure) {
          UpdatePetFailure.IllegalUpdate -> UpdatePetDetailsFailure.InvalidUpdate
          UpdatePetFailure.NotFound -> UpdatePetDetailsFailure.PetNotFound
        }
      }
      .bind()
  }

  private fun checkNamePolicy(name: String): Either<UpdatePetDetailsFailure, Unit> = either {
    ensure(name.isNotBlank()) {
      UpdatePetDetailsFailure.InvalidUpdate
    }
  }

  interface MicrochipStore {
    suspend fun getMicrochip(microchipId: MicrochipId): Microchip?
  }

  interface PetOwnerStore {
    suspend fun getPetOwner(petOwnerId: PetOwnerId): PetOwner?
  }

  interface PetStore {
    suspend fun getPet(petId: PetId): Pet?
    suspend fun updatePet(petId: PetId, petUpdate: PetUpdate): Either<UpdatePetFailure, Pet>
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
