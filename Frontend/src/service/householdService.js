import BaseService from '@/service/baseService'

/**
 * Service class for managing household related API calls.
 * Handles member management, household creation, updates, deletions, and invitations.
 */
class HouseholdService extends BaseService {
  /**
   * Initializes the HouseholdService with the base API path.
   */
  constructor() {
    super('/household')
  }

  /**
   * Fetch household details by user ID.
   * Get household details by using the userId
   *
   * @returns {Promise<Object>} Household data.
   * @throws {Error} If request fails.
   */
  async getHouseholdDetailsByUserId() {
    try {
      const response = await this.post('details')
      return response
    } catch (error) {
      if (error.response?.status !== 400) {
        console.error('Error fetching household details:', error)
      }
      throw error
    }
  }

  /**
   * Add a member (registered or unregistered) to the household.
   *
   * @param {{ fullName: string, email?: string }} data - Member details.
   * @returns {Promise<Object>} Newly added member object.
   * @throws {Error} If the request fails.
   */
  async addMember(data) {
    try {
      if (data.email) {
        await this.post('add-user', {
          email: data.email
        })
        return {
          id: Date.now(),
          fullName: data.fullName,
          email: data.email,
          isRegistered: true
        }
      } else {
        await this.post('add-unregistered-member', {
          fullName: data.fullName
        })
        return {
          id: Date.now(),
          fullName: data.fullName,
          isRegistered: false
        }
      }
    } catch (error) {
      console.error('Error adding member:', error)
      throw error
    }
  }

  /**
   * Add a registered user to the household.
   *
   * @param {string} userId - User ID.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async addUserToHousehold(userId) {
    try {
      return await this.post('add-user', {
        userId
      })
    } catch (error) {
      console.error('Error adding user to household:', error)
      throw error
    }
  }

  /**
   * Update household details (name, address).
   *
   * @param {{ name: string, address: string }} data - Updated household info.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async updateHousehold(data) {
    try {
      return this.post('edit', {
        name: data.name,
        address: data.address
      })
    } catch (error) {
      console.error('Error updating household:', error)
      throw error
    }
  }

  /**
   * Update an unregistered member's name.
   *
   * @param {string} memberId - Member ID.
   * @param {{ name: string, isRegistered: boolean }} data - Updated member data.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If trying to update a registered member or request fails.
   */
  async updateUnregisteredMember(memberId, data) {
    try {
      if (data.isRegistered) {
        throw new Error("Cannot update registered members.")
      }
      const payload = {
        memberId,
        newFullName: data.name,
      }

      return this.post('edit-unregistered-member', payload)
    } catch (error) {
      console.error("Error updating unregistered member:", error)
      throw error
    }
  }

  /**
   * Remove a registered user from the household.
   *
   * @param {string} userId - User ID.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async removeRegisteredMember(userId) {
    try {
      return this.post('remove-user', {
        userId
      })
    } catch (error) {
      console.error('Error removing registered member:', error)
      throw error
    }
  }

  /**
   * Remove an unregistered member from the household.
   *
   * @param {string} memberId - Member ID.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async removeUnregisteredMember(memberId) {
    try {
      return this.post('delete-unregistered-member', { memberId })
    } catch (error) {
      console.error('Error removing unregistered member:', error)
      throw error
    }
  }

  /**
   * Invite a member by email to join the household.
   *
   * @param {string} email - Email address to invite.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async inviteMember(email) {
    try {
      return this.post('invite-user', {
        email: email
      })
    } catch (error) {
      console.error('Error inviting member:', error)
      throw error
    }
  }

  /**
   * Create a new household.
   *
   * @param {{ name: string, address: string }} data - New household data.
   * @returns {Promise<Object>} Created household with ID.
   * @throws {Error} If the request fails.
   */
  async createHousehold(data) {
    try {
      const response = await this.post('create', {
        name: data.name,
        address: data.address
      })

      return {
        id: response.id || Math.floor(Math.random() * 1000),
        name: data.name,
        address: data.address
      }
    } catch (error) {
      console.error('Error creating household:', error)
      throw error
    }
  }

  /**
   * Delete a household by ID and owner.
   *
   * @param {string} householdId - ID of the household to delete.
   * @param {string} ownerId - ID of the owner requesting deletion.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async deleteHousehold(householdId, ownerId) {
    try {
      return await this.post('delete', {
        householdId,
        ownerId
      })
    } catch (error) {
      console.error('Error deleting household:', error)
      throw error
    }
  }

  /**
   * Transfer household ownership to another user.
   *
   * @param {string} userId - ID of the new owner.
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async transferOwnership(userId) {
    try {
      return this.post('change-owner', {
        userId: userId
      })
    } catch (error) {
      console.error('Error transferring ownership:', error)
      throw error
    }
  }

  /**
   * Leave the currently joined household.
   *
   * @returns {Promise<Object>} API response.
   * @throws {Error} If the request fails.
   */
  async leaveHousehold() {
    try {
      return this.post('leave')
    } catch (error) {
      console.error('Error leaving household:', error)
      throw error
    }
  }

  /**
   * Search for a household by its ID.
   *
   * @param  {{ householdId: string }} options
   * @param  {string} options.householdId  alphanumeric household identifier
   * @throws {Error}   if `householdId` is not a nonempty alphanumeric string,
   *                   or if the backend returns a 400/404
   * @returns {Promise<{ id: string|number, name: string } | null>}
   *                   the matching household record, or `null` if none found
   */
  async searchHouseholdById({ householdId }) {
    if (typeof householdId !== 'string' || !/^[A-Za-z0-9]+$/.test(householdId)) {
      throw new Error('Ugyldig husstands-ID')
    }

    try {
      const response = await this.post('search', { householdId })

      if (!response || !response.id) {
        // API returned no match
        throw new Error('Ingen husstand funnet')
      }
      return response

    } catch (err) {
      const status = err.response?.status
      // Convert any 400/404 or our own "not found" into the friendly message
      if (status === 400 || status === 404 || err.message === 'Ingen husstand funnet') {
        throw new Error('Ingen husstand funnet')
      }
      throw err
    }
  }
}

export default new HouseholdService()