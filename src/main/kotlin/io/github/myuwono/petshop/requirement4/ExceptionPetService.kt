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

class ExceptionPetService(
  private val microchipStore: MicrochipStore,
  private val petStore: PetStore,
  private val petOwnerStore: PetOwnerStore
) {

  suspend fun updatePetDetails(
    petId: PetId,
    petUpdate: PetUpdate
  ): Pet = try {
    val pet = petStore.getPet(petId)
    val microchip = microchipStore.getMicrochip(pet.microchipId)

    if (microchip.petId != pet.id) {
      throw UpdatePetDetailsException.InvalidMicrochip()
    }

    petStore.updatePet(petId, petUpdate)
  } catch (ex: Throwable) {
    when (ex) {
      is PetNotFoundException -> throw UpdatePetDetailsException.PetNotFound(ex)
      is MicrochipNotFoundException -> throw UpdatePetDetailsException.MicrochipNotFound(ex)
      is CheckNameFailedException -> throw UpdatePetDetailsException.InvalidUpdate(ex)
      is UpdatePetException -> when (ex) {
        is UpdatePetException.IllegalUpdate -> throw UpdatePetDetailsException.InvalidUpdate(ex)
        is UpdatePetException.NotFound -> throw UpdatePetDetailsException.PetNotFound(ex)
      }

      else -> throw ex
    }
  }

  data class CheckNameFailedException(override val cause: Throwable? = null) : RuntimeException()

  interface MicrochipStore {
    suspend fun getMicrochip(microchipId: MicrochipId): Microchip
  }

  data class MicrochipNotFoundException(override val cause: Throwable? = null) : RuntimeException()

  interface PetOwnerStore {
    suspend fun getPetOwner(petOwnerId: PetOwnerId): PetOwner
  }

  data class PetOwnerNotFoundException(override val cause: Throwable? = null) : RuntimeException()

  interface PetStore {
    suspend fun getPet(petId: PetId): Pet
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

  data class PetNotFoundException(override val cause: Throwable? = null) : RuntimeException()

  sealed class UpdatePetException : RuntimeException() {
    data class NotFound(override val cause: Throwable? = null) : UpdatePetException()
    data class IllegalUpdate(override val cause: Throwable? = null) : UpdatePetException()
  }

  sealed class UpdatePetDetailsException : RuntimeException() {
    data class PetNotFound(override val cause: Throwable? = null) : UpdatePetDetailsException()
    data class MicrochipNotFound(override val cause: Throwable? = null) : UpdatePetDetailsException()
    data class InvalidMicrochip(override val cause: Throwable? = null) : UpdatePetDetailsException()
    data class InvalidUpdate(override val cause: Throwable? = null) : UpdatePetDetailsException()
  }
}
