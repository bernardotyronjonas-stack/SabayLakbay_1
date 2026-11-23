package sabaylakbay_1.model;

/** Status of a booking (request to join). APPROVED = member. LEFT = was a member, then left. */
public enum RequestStatus {
    PENDING, APPROVED, REJECTED, CANCELLED, LEFT
}
