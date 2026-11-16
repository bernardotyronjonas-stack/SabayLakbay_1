package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** A rating/review. revieweeId > 0 = rating of a companion, revieweeId = 0 = review of the trip. */
public class Review {

    private int id;
    private int groupId;
    private int reviewerId;
    private String reviewerName;
    private int revieweeId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public Review() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public int getReviewerId() { return reviewerId; }
    public void setReviewerId(int reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public int getRevieweeId() { return revieweeId; }
    public void setRevieweeId(int revieweeId) { this.revieweeId = revieweeId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
