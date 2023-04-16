package io.github.myuwono.petshop.requirement5

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
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
    val pet = petStore.getPet(petId).getOrElse { raise(UpdatePetDetailsFailure.PetNotFound) }
    val owner = petOwnerStore.getPetOwner(petOwnerId).getOrElse { raise(UpdatePetDetailsFailure.OwnerNotFound) }
    val microchip = microchipStore.getMicrochip(pet.microchipId).getOrElse { raise(UpdatePetDetailsFailure.MicrochipNotFound) }

    ensure(microchip.petId == pet.id) { UpdatePetDetailsFailure.InvalidMicrochip }
    ensure(microchip.petOwnerId == owner.id) { UpdatePetDetailsFailure.OwnerMismatch }

    petStore.updatePet(pet.id, petUpdate)
      .mapLeft { updatePetFailure ->
        when (updatePetFailure) {
          UpdatePetFailure.IllegalUpdate -> UpdatePetDetailsFailure.InvalidUpdate
          UpdatePetFailure.NotFound -> UpdatePetDetailsFailure.PetNotFound
        }
      }
      .bind()
  }

  interface MicrochipStore {
    suspend fun getMicrochip(microchipId: MicrochipId): Option<Microchip>
  }

  interface PetOwnerStore {
    suspend fun getPetOwner(petOwnerId: PetOwnerId): Option<PetOwner>
  }

  interface PetStore {
    suspend fun getPet(petId: PetId): Option<Pet>
    suspend fun updatePet(petId: PetId, petUpdate: PetUpdate): Either<UpdatePetFailure, Pet>
  }

  data class PetUpdate(
    val microchipId: Option<MicrochipId> = none(),
    val name: Option<String> = none(),
    val birthDate: Option<LocalDate> = none(),
    val petType: Option<PetType> = none(),
    val breed: Option<String> = none(),
    val gender: Option<PetGender> = none()
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
