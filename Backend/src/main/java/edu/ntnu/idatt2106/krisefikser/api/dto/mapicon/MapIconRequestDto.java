package edu.ntnu.idatt2106.krisefikser.api.dto.mapicon;

import edu.ntnu.idatt2106.krisefikser.persistance.enums.MapIconType;

/**
 * DTO for creating or updating a MapIcon.
 */
public class MapIconRequestDto {

  private MapIconType type;
  private String address;
  private Double latitude;
  private Double longitude;
  private String description;
  private String openingHours;
  private String contactInfo;

  /**
   * Gets the type of the map icon.
   *
   * @return the type of the map icon
   */
  public MapIconType getType() {
    return type;
  }

  /**
   * Sets the type of the map icon.
   *
   * @param type the type of the map icon
   */
  public void setType(MapIconType type) {
    this.type = type;
  }

  /**
   * Gets the address of the map icon.
   *
   * @return the address of the map icon
   */
  public String getAddress() {
    return address;
  }

  /**
   * Sets the address of the map icon.
   *
   * @param address the address of the map icon
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * Gets the latitude of the map icon.
   *
   * @return the latitude of the map icon
   */
  public Double getLatitude() {
    return latitude;
  }

  /**
   * Sets the latitude of the map icon.
   *
   * @param latitude the latitude of the map icon
   */
  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  /**
   * Gets the longitude of the map icon.
   *
   * @return the longitude of the map icon
   */
  public Double getLongitude() {
    return longitude;
  }

  /**
   * Sets the longitude of the map icon.
   *
   * @param longitude the longitude of the map icon
   */
  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  /**
   * Gets the description of the map icon.
   *
   * @return the description of the map icon
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the map icon.
   *
   * @param description the description of the map icon
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets the opening hours of the map icon.
   *
   * @return the opening hours of the map icon
   */
  public String getOpeningHours() {
    return openingHours;
  }

  /**
   * Sets the opening hours of the map icon.
   *
   * @param openingHours the opening hours of the map icon
   */
  public void setOpeningHours(String openingHours) {
    this.openingHours = openingHours;
  }

  /**
   * Gets the contact information of the map icon.
   *
   * @return the contact information of the map icon
   */
  public String getContactInfo() {
    return contactInfo;
  }

  /**
   * Sets the contact information of the map icon.
   *
   * @param contactInfo the contact information of the map icon
   */
  public void setContactInfo(String contactInfo) {
    this.contactInfo = contactInfo;
  }
}
