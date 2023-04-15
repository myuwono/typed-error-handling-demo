package io.github.myuwono.petshop

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.none
import arrow.core.right
import java.time.LocalDate

class TaggedTypesFlatMapPetService(
  private val microchipStore: MicrochipStore,
  private val petStore: PetStore,
  private val petOwnerStore: PetOwnerStore
) {
  suspend fun updatePetDetails(
    petId: PetId,
    petOwnerId: PetOwnerId,
    petUpdate: PetUpdate
  ): Either<UpdatePetDetailsFailure, Pet> =
    petStore.getPet(petId)
      .toEither { UpdatePetDetailsFailure.PetNotFound }
      .flatMap { pet ->
        petOwnerStore.getPetOwner(petOwnerId)
          .toEither { UpdatePetDetailsFailure.OwnerNotFound }
          .flatMap { owner ->
            microchipStore.getMicrochip(pet.microchipId)
              .toEither { UpdatePetDetailsFailure.MicrochipNotFound }
              .flatMap { microchip ->
                run { if (microchip.petId == pet.id) Unit.right() else UpdatePetDetailsFailure.InvalidMicrochip.left() }
                  .flatMap { if (microchip.petOwnerId == owner.id) Unit.right() else UpdatePetDetailsFailure.OwnerMismatch.left() }
                  .flatMap { petUpdate.name.map { checkNamePolicy(it) }.getOrElse { Unit.right() } }
                  .flatMap {
                    petStore.updatePet(pet.id, petUpdate).mapLeft { updatePetFailure ->
                      when (updatePetFailure) {
                        UpdatePetFailure.IllegalUpdate -> UpdatePetDetailsFailure.InvalidUpdate
                        UpdatePetFailure.NotFound -> UpdatePetDetailsFailure.PetNotFound
                      }
                    }
                  }
              }
          }
      }

  private fun checkNamePolicy(name: String): Either<UpdatePetDetailsFailure.InvalidUpdate, Unit> =
    if (name.isNotBlank()) Unit.right() else UpdatePetDetailsFailure.InvalidUpdate.left()

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
