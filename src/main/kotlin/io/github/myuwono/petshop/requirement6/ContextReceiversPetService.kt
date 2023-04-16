package io.github.myuwono.petshop.requirement6

import arrow.core.None
import arrow.core.Option
import arrow.core.none
import arrow.core.raise.Raise
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
    val pet = arrow.core.raise.recover({ petStore.getPet(petId) }) { raise(UpdatePetDetailsFailure.PetNotFound) }
    val owner =
      arrow.core.raise.recover({ petOwnerStore.getPetOwner(petOwnerId) }) { raise(UpdatePetDetailsFailure.OwnerNotFound) }
    val microchip =
      arrow.core.raise.recover({ microchipStore.getMicrochip(pet.microchipId) }) { raise(UpdatePetDetailsFailure.MicrochipNotFound) }

    ensure(microchip.petId == pet.id) { UpdatePetDetailsFailure.InvalidMicrochip }
    ensure(microchip.petOwnerId == owner.id) { UpdatePetDetailsFailure.OwnerMismatch }

    petUpdate.name.onSome { checkNamePolicy(it) }

    return arrow.core.raise.recover({ petStore.updatePet(pet.id, petUpdate) }) { updatePetFailure ->
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
    context(Raise<None>)
    suspend fun getMicrochip(microchipId: MicrochipId): Microchip
  }

  interface PetOwnerStore {
    context(Raise<None>)
    suspend fun getPetOwner(petOwnerId: PetOwnerId): PetOwner
  }

  interface PetStore {
    context(Raise<None>)
    suspend fun getPet(petId: PetId): Pet

    context(Raise<UpdatePetFailure>)
    suspend fun updatePet(petId: PetId, petUpdate: PetUpdate): Pet
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
